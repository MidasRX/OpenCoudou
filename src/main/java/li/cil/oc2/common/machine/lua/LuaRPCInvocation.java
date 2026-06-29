/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.lua;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import li.cil.oc2.api.bus.device.rpc.RPCInvocation;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;

import java.util.Optional;

/**
 * {@link RPCInvocation} backed by a Gson {@link JsonArray} of parameters, mirroring the
 * implementation the (now bypassed) JSON serial adapter used. Lets the Lua component bridge reuse
 * the existing {@code RPCMethodGroup#findOverload} / {@code RPCMethod#invoke} machinery unchanged:
 * Lua arguments are converted to a {@link JsonArray} and matched/deserialized exactly as before.
 */
public record LuaRPCInvocation(JsonArray parameters, Gson gson) implements RPCInvocation {
    @Override
    public JsonArray getParameters() {
        return parameters;
    }

    @Override
    public Gson getGson() {
        return gson;
    }

    @Override
    public Optional<Object[]> tryDeserializeParameters(final RPCParameter... parameterTypes) {
        if (parameterTypes.length != parameters.size()) {
            return Optional.empty();
        }

        final Object[] result = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            try {
                result[i] = gson.fromJson(parameters.get(i), parameterTypes[i].getType());
            } catch (final Throwable e) {
                return Optional.empty();
            }
        }
        return Optional.of(result);
    }
}
