/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.lua;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Bridge between the sandboxed {@link LuaArchitecture} and the rest of the mod.
 * <p>
 * The architecture injects host globals ({@code computer}, {@code component}, ...) into the Lua
 * sandbox that delegate to this interface. This keeps the Lua engine completely decoupled from the
 * Minecraft device bus: Phase 2 implements this over {@code CommonDeviceBusController}/the RPC layer,
 * while tests can provide a trivial in-memory implementation.
 * <p>
 * See {@code ~/.claude} memory {@code oc2r-luaj-machine-protocol} for the validated contract.
 */
public interface MachineHost {
    /**
     * Description of a single callback on a component, as reported to {@code component.methods}.
     *
     * @param direct whether the method may run inline on the VM worker thread (vs. being deferred
     *               to the server thread); reported as {@code info.direct} to Lua.
     * @param getter whether this entry is a readable field rather than a method.
     * @param setter whether this entry is a writable field rather than a method.
     */
    record MethodInfo(boolean direct, boolean getter, boolean setter) {
        public static MethodInfo method(final boolean direct) {
            return new MethodInfo(direct, false, false);
        }
    }

    // --- computer state ---------------------------------------------------

    /** Monotonic wall-clock time in seconds; used by the sandbox deadline machinery. */
    double realTime();

    /** Seconds the machine has been running; drives {@code computer.uptime} and signal timeouts. */
    double uptime();

    /** This computer's own component address. */
    String address();

    long totalMemory();

    long freeMemory();

    double energy();

    double maxEnergy();

    // --- components -------------------------------------------------------

    /**
     * @return map of component address to component type, optionally filtered. When {@code exact} is
     * false the filter is a substring match (matching OpenComputers' {@code component.list}).
     */
    Map<String, String> componentList(@Nullable String filter, boolean exact);

    @Nullable
    String componentType(String address);

    int componentSlot(String address);

    @Nullable
    String componentDoc(String address, String method);

    /** @return map of method/field name to its {@link MethodInfo}. */
    Map<String, MethodInfo> componentMethods(String address);

    /**
     * Invoke a component method.
     *
     * @return the result values. The architecture prepends the {@code true} success flag the Lua
     * kernel expects; throw to signal failure (the message is surfaced to the guest).
     */
    List<Object> componentInvoke(String address, String method, List<Object> args) throws Exception;

    // --- signals ----------------------------------------------------------

    /** Queue a signal to be delivered to the guest via {@code computer.pullSignal}. */
    void pushSignal(String name, Object... args);

    // --- lifecycle --------------------------------------------------------

    /** Request machine shutdown ({@code reboot=false}) or reboot ({@code reboot=true}). */
    default void onShutdown(final boolean reboot) {
    }
}
