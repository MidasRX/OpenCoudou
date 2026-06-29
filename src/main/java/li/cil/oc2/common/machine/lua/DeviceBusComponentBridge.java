/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.lua;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.bus.device.rpc.RPCDeviceList;
import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import li.cil.oc2.common.machine.LuaMachineLog;
import li.cil.oc2.common.serialization.gson.SideJsonDeserializer;
import li.cil.oc2.common.serialization.gson.UnsignedByteArrayJsonSerializer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Implements {@link MachineHost} on top of OC2R's existing device bus and RPC layer, exposing each
 * UUID-addressed {@link RPCDevice} to the Lua sandbox as an OpenComputers-style component.
 * <p>
 * This is the direct, in-process replacement for the old JSON-over-serial {@code RPCDeviceBusAdapter}:
 * the device discovery/grouping and {@code findOverload}/{@code invoke} logic is reused verbatim, but
 * arguments and results cross the boundary as Java objects (via Gson for fidelity with the previous
 * serialized shapes) instead of being marshalled through a virtio serial port.
 * <p>
 * Because the Lua machine is driven on the server thread, every invocation is already main-thread
 * safe, so all methods are reported as {@code direct} and run inline.
 */
public final class DeviceBusComponentBridge implements MachineHost {
    /** Computer-level state the bridge can't derive from the bus itself. */
    public interface Environment {
        String address();

        long totalMemory();

        long freeMemory();

        double energy();

        double maxEnergy();

        double uptime();

        double realTime();
    }

    ///////////////////////////////////////////////////////////////////

    private final CommonDeviceBusController controller;
    private final Environment environment;
    private final Gson gson;

    private final HashMap<UUID, RPCDevice> devicesById = new HashMap<>();
    // Components synthesized by the computer itself (eeprom, gpu, screen, ...) rather than
    // discovered on the item/block bus. These persist across bus rescans.
    private final LinkedHashMap<UUID, RPCDevice> builtinDevices = new LinkedHashMap<>();
    private final ConcurrentLinkedDeque<List<Object>> signalQueue = new ConcurrentLinkedDeque<>();

    ///////////////////////////////////////////////////////////////////

    public DeviceBusComponentBridge(final CommonDeviceBusController controller, final Environment environment) {
        this.controller = controller;
        this.environment = environment;
        this.gson = RPCMethodParameterTypeAdapters.beginBuildGson()
            .registerTypeAdapter(byte[].class, new UnsignedByteArrayJsonSerializer())
            .registerTypeAdapter(Side.class, new SideJsonDeserializer())
            // The Lua BIOS reads with math.huge as the count; allow Infinity through the arg pipeline.
            .serializeSpecialFloatingPointValues()
            .create();
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * Rebuild the address->device map from the current bus state. Mirrors the grouping logic of
     * {@code RPCDeviceBusAdapter.resume}: devices are grouped by their (possibly multiple) bus
     * identifiers, empty/synthetic groups dropped, and a deterministic identifier chosen per group.
     */
    public void rebuild() {
        final HashMap<UUID, ArrayList<RPCDevice>> devicesByIdentifier = new HashMap<>();
        for (final Device device : controller.getDevices()) {
            if (device instanceof final RPCDevice rpcDevice) {
                for (final UUID identifier : controller.getDeviceIdentifiers(device)) {
                    devicesByIdentifier
                        .computeIfAbsent(identifier, unused -> new ArrayList<>())
                        .add(rpcDevice);
                }
            }
        }

        final HashMap<RPCDeviceList, ArrayList<UUID>> identifiersByDevice = new HashMap<>();
        devicesByIdentifier.forEach((identifier, devices) -> {
            final RPCDeviceList device = new RPCDeviceList(devices);
            if (device.getMethodGroups().isEmpty()) {
                return; // synthetic device with no callable methods
            }
            identifiersByDevice.computeIfAbsent(device, unused -> new ArrayList<>()).add(identifier);
        });

        devicesById.clear();
        identifiersByDevice.forEach((device, identifiers) ->
            devicesById.put(selectIdentifierDeterministically(identifiers), device));
        devicesById.putAll(builtinDevices);
    }

    /** Register a component synthesized by the computer (assigned a stable UUID address). */
    public UUID addBuiltinDevice(final RPCDevice device) {
        final UUID id = UUID.randomUUID();
        builtinDevices.put(id, device);
        devicesById.put(id, device);
        return id;
    }

    /** Pop the next queued signal as {@code [name, args...]}, or {@code null} if none. */
    @Nullable
    public List<Object> pollSignal() {
        return signalQueue.pollFirst();
    }

    public boolean hasSignal() {
        return !signalQueue.isEmpty();
    }

    ///////////////////////////////////////////////////////////////////
    // MachineHost

    @Override
    public double realTime() {
        return environment.realTime();
    }

    @Override
    public double uptime() {
        return environment.uptime();
    }

    @Override
    public String address() {
        return environment.address();
    }

    @Override
    public long totalMemory() {
        return environment.totalMemory();
    }

    @Override
    public long freeMemory() {
        return environment.freeMemory();
    }

    @Override
    public double energy() {
        return environment.energy();
    }

    @Override
    public double maxEnergy() {
        return environment.maxEnergy();
    }

    @Override
    public Map<String, String> componentList(@Nullable final String filter, final boolean exact) {
        final Map<String, String> result = new LinkedHashMap<>();
        devicesById.forEach((id, device) -> {
            final String type = primaryType(device);
            if (type == null) {
                return;
            }
            if (filter == null || (exact ? type.equals(filter) : type.contains(filter))) {
                result.put(id.toString(), type);
            }
        });
        return result;
    }

    @Override
    @Nullable
    public String componentType(final String address) {
        final RPCDevice device = lookup(address);
        return device != null ? primaryType(device) : null;
    }

    @Override
    public int componentSlot(final String address) {
        return -1; // OC2R does not expose a meaningful slot index per component.
    }

    @Override
    @Nullable
    public String componentDoc(final String address, final String method) {
        final RPCDevice device = lookup(address);
        if (device == null) {
            return null;
        }
        for (final RPCMethodGroup group : device.getMethodGroups()) {
            if (group.getName().equals(method)) {
                for (final RPCMethod overload : group.getOverloads()) {
                    if (overload.getDescription().isPresent()) {
                        return overload.getDescription().get();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, MethodInfo> componentMethods(final String address) {
        final RPCDevice device = lookup(address);
        final Map<String, MethodInfo> result = new LinkedHashMap<>();
        if (device == null) {
            return result;
        }
        for (final RPCMethodGroup group : device.getMethodGroups()) {
            // Machine runs on the server thread, so everything is "direct" (runs inline).
            result.putIfAbsent(group.getName(), MethodInfo.method(true));
        }
        return result;
    }

    @Override
    public List<Object> componentInvoke(final String address, final String method, final List<Object> args) throws Exception {
        final RPCDevice device = lookup(address);
        if (device == null) {
            throw new IllegalArgumentException("no such component");
        }

        final JsonArray parameters = new JsonArray();
        for (final Object arg : args) {
            parameters.add(gson.toJsonTree(arg));
        }
        final LuaRPCInvocation invocation = new LuaRPCInvocation(parameters, gson);

        boolean nameMatched = false;
        // OpenComputers callers omit optional trailing arguments. The RPC layer matches overloads by
        // exact argument count, so we first try an exact match (preserves multi-overload devices like
        // the sound card), then fall back to the overload with the fewest extra params, padding the
        // missing trailing arguments with nil.
        RPCMethod lenientBest = null;
        int lenientBestCount = Integer.MAX_VALUE;
        for (final RPCMethodGroup group : device.getMethodGroups()) {
            if (!group.getName().equals(method)) {
                continue;
            }
            nameMatched = true;
            final Optional<RPCMethod> exact = group.findOverload(invocation);
            if (exact.isPresent()) {
                return invokeMethod(exact.get(), invocation);
            }
            for (final RPCMethod overload : group.getOverloads()) {
                final int paramCount = overload.getParameters().length;
                if (paramCount > args.size() && paramCount < lenientBestCount) {
                    lenientBest = overload;
                    lenientBestCount = paramCount;
                }
            }
        }

        if (!nameMatched) {
            throw new IllegalArgumentException("no such method");
        }

        if (lenientBest != null) {
            final JsonArray padded = parameters.deepCopy();
            while (padded.size() < lenientBestCount) {
                padded.add(com.google.gson.JsonNull.INSTANCE);
            }
            final LuaRPCInvocation paddedInvocation = new LuaRPCInvocation(padded, gson);
            final Optional<RPCMethod> overload = lenientBest.findOverload(paddedInvocation);
            if (overload.isPresent()) {
                return invokeMethod(overload.get(), paddedInvocation);
            }
        }

        throw new IllegalArgumentException("invalid arguments");
    }

    private List<Object> invokeMethod(final RPCMethod method, final LuaRPCInvocation invocation) throws Exception {
        try {
            return resultToList(method.invoke(invocation));
        } catch (final Throwable t) {
            throw new Exception(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(), t);
        }
    }

    @Override
    public void pushSignal(final String name, final Object... args) {
        final List<Object> signal = new ArrayList<>(args.length + 1);
        signal.add(name);
        for (final Object arg : args) {
            signal.add(arg);
        }
        signalQueue.addLast(signal);
    }

    @Override
    public void onShutdown(final boolean reboot) {
        LuaMachineLog.log("machine", "guest requested %s", reboot ? "reboot" : "shutdown");
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    private RPCDevice lookup(final String address) {
        try {
            return devicesById.get(UUID.fromString(address));
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private static String primaryType(final RPCDevice device) {
        final List<String> typeNames = device.getTypeNames();
        return typeNames.isEmpty() ? null : typeNames.get(0);
    }

    private List<Object> resultToList(@Nullable final Object result) {
        if (result == null) {
            return List.of();
        }
        // An Object[] is our convention for "multiple Lua return values" (e.g. gpu.getResolution
        // returns width, height). No existing RPC method returns Object[], so this is unambiguous.
        if (result instanceof final Object[] array) {
            final List<Object> out = new ArrayList<>(array.length);
            for (final Object element : array) {
                out.add(convertResult(element));
            }
            return out;
        }
        final List<Object> out = new ArrayList<>(1);
        out.add(convertResult(result));
        return out;
    }

    @Nullable
    private Object convertResult(@Nullable final Object value) {
        if (value == null) {
            return null;
        }
        // Rich handles (e.g. internet request handles) pass straight through to become Lua userdata.
        if (value instanceof LuaUserdata) {
            return value;
        }
        // Convert through Gson so component results match the shapes the previous serial bridge
        // produced (e.g. ItemStack/Direction become the same tables guest code already expects).
        return jsonToJava(gson.toJsonTree(value));
    }

    @Nullable
    private static Object jsonToJava(final JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            final JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            if (primitive.isNumber()) {
                return primitive.getAsDouble();
            }
            return primitive.getAsString();
        }
        if (element.isJsonArray()) {
            final List<Object> list = new ArrayList<>();
            element.getAsJsonArray().forEach(child -> list.add(jsonToJava(child)));
            return list;
        }
        final Map<String, Object> map = new LinkedHashMap<>();
        element.getAsJsonObject().entrySet().forEach(entry -> map.put(entry.getKey(), jsonToJava(entry.getValue())));
        return map;
    }

    private static UUID selectIdentifierDeterministically(final ArrayList<UUID> identifiers) {
        UUID lowest = identifiers.get(0);
        for (int i = 1; i < identifiers.size(); i++) {
            if (identifiers.get(i).compareTo(lowest) < 0) {
                lowest = identifiers.get(i);
            }
        }
        return lowest;
    }
}
