/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.lua;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * A rich handle object exposed to the Lua sandbox as OpenComputers-style <em>userdata</em> (a value
 * with callable methods, e.g. the object returned by {@code internet.request}). The kernel's
 * {@code machine.lua} wraps these and dispatches method calls back through the {@code userdata} host
 * API installed by {@link LuaArchitecture}.
 * <p>
 * Component methods that wish to return such a handle simply return an implementation of this
 * interface; {@link DeviceBusComponentBridge} passes it through untouched (no Gson conversion).
 */
public interface LuaUserdata {
    /** @return map of method name to whether it may run inline ({@code direct}); used by {@code userdata.methods}. */
    Map<String, Boolean> methods();

    /** Invoke a method on this handle; return its result values (the bridge prepends the success flag). */
    List<Object> invoke(String method, List<Object> args) throws Exception;

    @Nullable
    default String doc(final String method) {
        return null;
    }

    default void dispose() {
    }
}
