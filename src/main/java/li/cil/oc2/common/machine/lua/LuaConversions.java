/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.lua;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts between LuaJ {@link LuaValue}s and plain Java objects so {@link MachineHost}
 * implementations never have to touch the LuaJ API directly.
 * <p>
 * Java -> Lua mapping: {@code null}->nil, Boolean, Number->double (or long for integral),
 * String, byte[] (Lua string), Map/List (tables). Lua -> Java mapping is the inverse;
 * Lua strings become {@link String} (callers that need raw bytes can re-encode).
 */
public final class LuaConversions {
    private LuaConversions() {
    }

    public static Varargs toVarargs(final List<Object> values) {
        if (values.isEmpty()) {
            return LuaValue.NONE;
        }
        final LuaValue[] result = new LuaValue[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = toLua(values.get(i));
        }
        return LuaValue.varargsOf(result);
    }

    public static LuaValue toLua(final Object value) {
        if (value == null) {
            return LuaValue.NIL;
        }
        if (value instanceof final LuaValue lua) {
            return lua;
        }
        if (value instanceof final LuaUserdata userdata) {
            // Exposed to the sandbox as OC-style userdata; machine.lua wraps it and routes method
            // calls back through the host `userdata` API.
            return LuaValue.userdataOf(userdata);
        }
        if (value instanceof final Boolean b) {
            return LuaValue.valueOf(b);
        }
        if (value instanceof final byte[] bytes) {
            return LuaValue.valueOf(bytes);
        }
        if (value instanceof final String s) {
            return LuaValue.valueOf(s);
        }
        if (value instanceof Integer || value instanceof Long
            || value instanceof Short || value instanceof Byte) {
            // LuaJ has no valueOf(long) and is 5.2 (no 64-bit ints); use int where it fits.
            final long l = ((Number) value).longValue();
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return LuaValue.valueOf((int) l);
            }
            return LuaValue.valueOf((double) l);
        }
        if (value instanceof final Number n) {
            return LuaValue.valueOf(n.doubleValue());
        }
        if (value instanceof final Map<?, ?> map) {
            final LuaTable table = new LuaTable();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                table.set(toLua(entry.getKey()), toLua(entry.getValue()));
            }
            return table;
        }
        if (value instanceof final Iterable<?> iterable) {
            final LuaTable table = new LuaTable();
            int i = 1;
            for (final Object element : iterable) {
                table.set(i++, toLua(element));
            }
            return table;
        }
        // Fallback: stringify unknown types rather than leaking userdata into the sandbox.
        return LuaValue.valueOf(String.valueOf(value));
    }

    public static List<Object> toJavaList(final Varargs values, final int from) {
        final List<Object> result = new ArrayList<>();
        for (int i = from; i <= values.narg(); i++) {
            result.add(toJava(values.arg(i)));
        }
        return result;
    }

    public static Object toJava(final LuaValue value) {
        switch (value.type()) {
            case LuaValue.TNIL:
                return null;
            case LuaValue.TBOOLEAN:
                return value.toboolean();
            case LuaValue.TNUMBER:
                // Preserve integers where possible for cleaner round-trips.
                if (value.isint() || value.tolong() == value.todouble()) {
                    return value.tolong();
                }
                return value.todouble();
            case LuaValue.TSTRING:
                return value.tojstring();
            case LuaValue.TTABLE:
                return tableToJava(value.checktable());
            default:
                return value.tojstring();
        }
    }

    private static Object tableToJava(final LuaTable table) {
        // Heuristic: a table with a contiguous 1..n integer keyset becomes a List, else a Map.
        final int length = table.length();
        boolean isArray = length > 0;
        for (int i = 1; i <= length && isArray; i++) {
            if (table.get(i).isnil()) {
                isArray = false;
            }
        }
        if (isArray && table.keyCount() == length) {
            final List<Object> list = new ArrayList<>(length);
            for (int i = 1; i <= length; i++) {
                list.add(toJava(table.get(i)));
            }
            return list;
        }

        final Map<Object, Object> map = new LinkedHashMap<>();
        LuaValue key = LuaValue.NIL;
        while (true) {
            final Varargs next = table.next(key);
            key = next.arg1();
            if (key.isnil()) {
                break;
            }
            map.put(toJava(key), toJava(next.arg(2)));
        }
        return map;
    }
}
