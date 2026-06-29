/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine;

import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.bus.device.rpc.item.CPUItemDevice;
import li.cil.oc2.common.machine.component.ComputerDevice;
import li.cil.oc2.common.machine.component.EepromDevice;
import li.cil.oc2.common.machine.component.FileSystemDevice;
import li.cil.oc2.common.machine.component.GpuDevice;
import li.cil.oc2.common.machine.component.InternetDevice;
import li.cil.oc2.common.machine.component.KeyboardDevice;
import li.cil.oc2.common.machine.component.ScreenDevice;
import li.cil.oc2.common.machine.fs.InMemoryFileSystem;
import li.cil.oc2.common.machine.fs.ResourceFileSystems;
import li.cil.oc2.common.machine.lua.DeviceBusComponentBridge;
import li.cil.oc2.common.machine.lua.LuaArchitecture;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * An OpenComputers-style Lua computer, replacing the former Sedna RISC-V/Linux virtual machine.
 * Owns the device bus, the {@link LuaArchitecture} sandbox, and the component bridge, and drives the
 * machine cooperatively on the server thread (time-sliced per game tick). Persistence is
 * reboot-on-reload: only the running flag is saved, and the OS re-boots after a world reload.
 */
public final class LuaComputerVirtualMachine implements VirtualMachine, DeviceBusComponentBridge.Environment {
    private static final String RUN_FLAG_TAG_NAME = "run";
    private static final String BIOS_RESOURCE_PATH = "/assets/oc2r/lua/bios.lua";
    private static final int MAX_STEPS_PER_TICK = 2000;
    private static final long DEFAULT_TOTAL_MEMORY = 512L * 1024 * 1024;
    // Matches the in-world Terminal dimensions the GPU renders to.
    private static final int TERMINAL_WIDTH = 80;
    private static final int TERMINAL_HEIGHT = 24;

    /** Hooks back into the owning block entity for energy and client-side state notifications. */
    public interface Listener {
        double energy();

        double maxEnergy();

        /** Total RAM in bytes from the installed memory sticks (0 if none). */
        long installedMemory();

        void onBusStateChanged(CommonDeviceBusController.BusState value);

        void onRunStateChanged(VMRunState value);

        void onBootErrorChanged(@Nullable Component value);

        /** Receives terminal output bytes (ANSI) produced by the GPU; applies + forwards to clients. */
        void terminalOutput(byte[] bytes);

        /** Next pending terminal input byte, or {@code -1} if none (drives keyboard signals). */
        int readInput();
    }

    ///////////////////////////////////////////////////////////////////

    public final CommonDeviceBusController busController;

    private final DeviceBusComponentBridge bridge;
    private final Listener listener;
    private final KeyInputDecoder keyDecoder = new KeyInputDecoder();
    private final String address;
    private final String keyboardAddress;

    @Nullable private LuaArchitecture architecture;
    private CommonDeviceBusController.BusState busState = CommonDeviceBusController.BusState.SCAN_PENDING;
    private VMRunState runState = VMRunState.STOPPED;
    @Nullable private Component bootError = Component.literal("");

    private long bootNanos;
    private boolean sleeping;
    private double wakeDeadlineUptime;

    ///////////////////////////////////////////////////////////////////

    public LuaComputerVirtualMachine(final CommonDeviceBusController busController, final Listener listener) {
        this.busController = busController;
        this.listener = listener;
        this.bridge = new DeviceBusComponentBridge(busController, this);

        // The "computer" component's address IS computer.address(); register it first so the BIOS's
        // computer.beep and computer.getDeviceInfo resolve.
        this.address = bridge.addBuiltinDevice(new ObjectDevice(new ComputerDevice(), "computer")).toString();

        // For now the core components are built into every computer. Later phases move the EEPROM
        // onto a flash item and the GPU/screen onto cards/blocks.
        bridge.addBuiltinDevice(new ObjectDevice(new EepromDevice(readBios(), "EEPROM (Lua BIOS)"), "eeprom"));

        final UUID keyboardId = bridge.addBuiltinDevice(new ObjectDevice(new KeyboardDevice(), "keyboard"));
        this.keyboardAddress = keyboardId.toString();
        bridge.addBuiltinDevice(new ObjectDevice(new ScreenDevice(List.of(keyboardId.toString())), "screen"));
        bridge.addBuiltinDevice(new ObjectDevice(
            new GpuDevice(TERMINAL_WIDTH, TERMINAL_HEIGHT, listener::terminalOutput), "gpu"));

        // Boot filesystem: OpenOS, bundled in the mod jar (read-only for now). The Lua BIOS scans
        // filesystems for /init.lua, so this becomes the boot medium.
        final InMemoryFileSystem openOs = ResourceFileSystems.fromManifest(
            "/assets/oc2r/openos.manifest", "/assets/oc2r/openos", 256L * 1024 * 1024);
        // Writable so /home and /etc work (changes are per-session; reboot-on-reload). Avoids the
        // "home directory is readonly" warning and lets programs/installers write to the root.
        bridge.addBuiltinDevice(new ObjectDevice(
            new FileSystemDevice(openOs, "openos", false), "filesystem"));

        // A writable scratch filesystem (OpenOS uses /tmp). Not persisted (reboot-on-reload).
        bridge.addBuiltinDevice(new ObjectDevice(
            new FileSystemDevice(new InMemoryFileSystem(256L * 1024 * 1024), "tmpfs", false), "filesystem"));

        // Internet access (HTTP/TCP) for wget/oppm/installers. Gated on Config.internetCardEnabled.
        bridge.addBuiltinDevice(new ObjectDevice(new InternetDevice(), "internet"));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public CommonDeviceBusController.BusState getBusState() {
        return busState;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setBusStateClient(final CommonDeviceBusController.BusState value) {
        busState = value;
    }

    @Override
    public VMRunState getRunState() {
        return runState;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setRunStateClient(final VMRunState value) {
        runState = value;
    }

    @Override
    @Nullable
    public Component getBootError() {
        return bootError;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setBootErrorClient(@Nullable final Component value) {
        bootError = value;
    }

    @Override
    @Nullable
    public Component getError() {
        return switch (busState) {
            case SCAN_PENDING, INCOMPLETE -> Component.literal("incomplete bus");
            case TOO_COMPLEX -> Component.literal("bus too complex");
            case MULTIPLE_CONTROLLERS -> Component.literal("multiple controllers");
            case READY -> (runState != VMRunState.RUNNING) ? bootError : null;
        };
    }

    @Override
    public boolean isRunning() {
        return busState == CommonDeviceBusController.BusState.READY && runState == VMRunState.RUNNING;
    }

    @Override
    public void start() {
        if (runState == VMRunState.RUNNING) {
            return;
        }
        setBootError(Component.literal(""));
        setRunState(VMRunState.LOADING_DEVICES);
        LuaMachineLog.log("machine", "start requested (address=%s)", address);
    }

    @Override
    public void stop() {
        architecture = null;
        sleeping = false;
        setRunState(VMRunState.STOPPED);
        LuaMachineLog.log("machine", "stopped");
    }

    ///////////////////////////////////////////////////////////////////

    public void tick() {
        busController.scan();
        setBusState(busController.getState());
        if (busState != CommonDeviceBusController.BusState.READY) {
            return;
        }

        // A computer requires a CPU to run; if it's pulled while running, halt (like OpenComputers).
        if (runState != VMRunState.STOPPED && !hasCpu()) {
            if (runState == VMRunState.RUNNING) {
                LuaMachineLog.log("machine", "CPU removed; halting");
            }
            error("missing a CPU");
            return;
        }

        switch (runState) {
            case LOADING_DEVICES -> load();
            case RUNNING -> run();
            default -> {
            }
        }
    }

    private boolean hasCpu() {
        for (final var device : busController.getDevices()) {
            if (device instanceof CPUItemDevice) {
                return true;
            }
        }
        return false;
    }

    public void suspend() {
        // Reboot-on-reload: nothing to persist beyond the running flag.
        architecture = null;
    }

    public void dispose() {
        architecture = null;
        busController.dispose();
    }

    public CompoundTag serialize() {
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean(RUN_FLAG_TAG_NAME, runState != VMRunState.STOPPED);
        return tag;
    }

    public void deserialize(final CompoundTag tag) {
        if (tag.getBoolean(RUN_FLAG_TAG_NAME)) {
            // Re-boot the OS where it left off (state itself is not persisted).
            runState = VMRunState.LOADING_DEVICES;
        } else {
            runState = VMRunState.STOPPED;
        }
    }

    ///////////////////////////////////////////////////////////////////
    // DeviceBusComponentBridge.Environment

    @Override
    public String address() {
        return address;
    }

    @Override
    public long totalMemory() {
        // Scale with installed RAM sticks (like OpenComputers), with a sane floor.
        return Math.max(DEFAULT_TOTAL_MEMORY, listener.installedMemory());
    }

    @Override
    public long freeMemory() {
        // We can't measure the LuaJ state's heap per-machine, so report a plausible usage so the
        // OS's RAM% looks sensible (and never "out of memory"): ~25% used.
        final long total = totalMemory();
        return total - (total / 4);
    }

    @Override
    public double energy() {
        return listener.energy();
    }

    @Override
    public double maxEnergy() {
        return listener.maxEnergy();
    }

    @Override
    public double uptime() {
        return bootNanos == 0 ? 0 : (System.nanoTime() - bootNanos) / 1e9;
    }

    @Override
    public double realTime() {
        return System.nanoTime() / 1e9;
    }

    ///////////////////////////////////////////////////////////////////

    private void load() {
        bridge.rebuild();

        bootNanos = System.nanoTime();
        sleeping = false;
        wakeDeadlineUptime = 0;

        architecture = new LuaArchitecture(bridge);
        if (!architecture.initialize()) {
            error("kernel init failed: " + architecture.getLastError());
            architecture = null;
            return;
        }

        setRunState(VMRunState.RUNNING);
        LuaMachineLog.log("machine", "kernel initialized, entering RUNNING");
    }

    private void run() {
        final LuaArchitecture arch = architecture;
        if (arch == null) {
            setRunState(VMRunState.STOPPED);
            return;
        }

        pumpKeyboardInput();

        for (int i = 0; i < MAX_STEPS_PER_TICK; i++) {
            if (sleeping) {
                if (!bridge.hasSignal() && uptime() < wakeDeadlineUptime) {
                    return; // still waiting for a signal or the sleep deadline
                }
            }

            final List<Object> signal = sleeping ? bridge.pollSignal() : null;
            final LuaArchitecture.StepResult result = arch.step(signal);
            sleeping = false;

            switch (result.status) {
                case CONTINUE -> {
                    // keep stepping
                }
                case SLEEP -> {
                    sleeping = true;
                    wakeDeadlineUptime = uptime() + Math.max(0, result.sleepSeconds);
                    if (!bridge.hasSignal()) {
                        return; // nothing to do until a signal arrives / deadline elapses
                    }
                }
                case SHUTDOWN -> {
                    LuaMachineLog.log("machine", "halted (shutdown)");
                    stop();
                    return;
                }
                case REBOOT -> {
                    LuaMachineLog.log("machine", "reboot requested");
                    stop();
                    start();
                    return;
                }
                case ERROR -> {
                    error(result.error);
                    return;
                }
            }
        }
        LuaMachineLog.log("machine", "step budget exhausted this tick (guest not yielding?)");
    }

    private void pumpKeyboardInput() {
        int b;
        while ((b = listener.readInput()) != -1) {
            for (final KeyInputDecoder.Key key : keyDecoder.push(b)) {
                // OpenComputers key_down signal: (keyboardAddress, char, code, playerName)
                bridge.pushSignal("key_down", keyboardAddress, key.character(), key.code(), "");
            }
        }
    }

    private void error(@Nullable final String message) {
        LuaMachineLog.log("machine", "ERROR: %s", message);
        architecture = null;
        setRunState(VMRunState.STOPPED);
        setBootError(Component.literal(message != null ? message : "unknown error"));
    }

    private void setBusState(final CommonDeviceBusController.BusState value) {
        if (value == busState) {
            return;
        }
        busState = value;
        listener.onBusStateChanged(value);
    }

    private void setRunState(final VMRunState value) {
        if (value == runState) {
            return;
        }
        runState = value;
        listener.onRunStateChanged(value);
    }

    private void setBootError(@Nullable final Component value) {
        bootError = value;
        listener.onBootErrorChanged(value);
    }

    private static String readBios() {
        try (final InputStream stream = LuaComputerVirtualMachine.class.getResourceAsStream(BIOS_RESOURCE_PATH)) {
            if (stream == null) {
                LuaMachineLog.log("machine", "missing BIOS resource %s", BIOS_RESOURCE_PATH);
                return "error('no BIOS', 0)";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LuaMachineLog.log("machine", "failed to read BIOS: %s", e.getMessage());
            return "error('no BIOS', 0)";
        }
    }
}
