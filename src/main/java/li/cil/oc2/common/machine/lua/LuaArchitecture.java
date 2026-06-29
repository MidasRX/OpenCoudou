/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * A sandboxed Lua machine in the style of the original OpenComputers, running on the pure-Java
 * LuaJ interpreter. It loads OpenComputers' {@code machine.lua} kernel (vendored under
 * {@code assets/oc2r/lua/}) as a coroutine and drives it through the standard
 * yield/resume signal protocol, delegating all in-world interaction to a {@link MachineHost}.
 * <p>
 * This class is deliberately decoupled from Minecraft and from the Sedna/RISC-V stack so it can be
 * unit-tested in isolation. The design was validated against a standalone LuaJ harness before being
 * brought into the mod; see the {@code oc2r-luaj-machine-protocol} note for the contract details.
 */
public final class LuaArchitecture {
    private static final String MACHINE_SOURCE_PATH = "/assets/oc2r/lua/machine.lua";

    /** Outcome of a single {@link #step} of the kernel coroutine. */
    public enum Status {
        /** Kernel yielded without a sleep request; resume again as soon as possible. */
        CONTINUE,
        /** Guest is waiting for a signal for up to {@link StepResult#sleepSeconds} seconds. */
        SLEEP,
        /** Guest requested shutdown (or halted normally). */
        SHUTDOWN,
        /** Guest requested reboot. */
        REBOOT,
        /** Kernel raised an unrecoverable error; see {@link StepResult#error}. */
        ERROR
    }

    public static final class StepResult {
        public final Status status;
        public final double sleepSeconds;
        @Nullable public final String error;

        private StepResult(final Status status, final double sleepSeconds, @Nullable final String error) {
            this.status = status;
            this.sleepSeconds = sleepSeconds;
            this.error = error;
        }

        static StepResult cont() {
            return new StepResult(Status.CONTINUE, 0, null);
        }

        static StepResult sleep(final double seconds) {
            return new StepResult(Status.SLEEP, seconds, null);
        }

        static StepResult shutdown() {
            return new StepResult(Status.SHUTDOWN, 0, null);
        }

        static StepResult reboot() {
            return new StepResult(Status.REBOOT, 0, null);
        }

        static StepResult error(@Nullable final String message) {
            return new StepResult(Status.ERROR, 0, message != null ? message : "unknown error");
        }
    }

    ///////////////////////////////////////////////////////////////////

    private final MachineHost host;

    @Nullable private Globals globals;
    @Nullable private LuaThread kernel;
    @Nullable private String lastError;

    ///////////////////////////////////////////////////////////////////

    public LuaArchitecture(final MachineHost host) {
        this.host = host;
    }

    /**
     * Build the sandbox, load the kernel, and run it up to its initial baseline yield.
     *
     * @return {@code true} on success; on failure {@link #getLastError()} describes the problem.
     */
    public boolean initialize() {
        try {
            final Globals g = JsePlatform.standardGlobals();
            g.load(new DebugLib()); // machine.lua relies on debug.sethook count hooks
            g.set("luajava", LuaValue.NIL); // defense in depth: never expose Java reflection
            installCompatShims(g); // bridge Lua 5.2 (LuaJ) vs 5.3 differences OpenOS relies on

            installComputerApi(g);
            installComponentApi(g);
            installSystemApi(g);
            installUnicodeApi(g);
            installUserdataApi(g);
            g.set("persistKey", LuaValue.NIL); // selects machine.lua's LuaJ (non-persisting) path

            final LuaValue chunk = g.load(readMachineSource(), "=machine");
            final LuaThread thread = new LuaThread(g, chunk);

            // Run to the first coroutine.yield() (the kernel's memory baseline).
            thread.resume(LuaValue.NONE);

            this.globals = g;
            this.kernel = thread;
            return true;
        } catch (final Throwable t) {
            lastError = String.valueOf(t.getMessage());
            return false;
        }
    }

    /**
     * Resume the kernel once.
     *
     * @param signal a queued signal as {@code [name, args...]} to deliver to {@code pullSignal}, or
     *               {@code null} to resume with no signal (e.g. after a sleep timeout elapsed).
     */
    public StepResult step(@Nullable final List<Object> signal) {
        if (kernel == null) {
            return StepResult.error("machine not initialized");
        }

        final Varargs resumeArgs = signal != null ? LuaConversions.toVarargs(signal) : LuaValue.NONE;

        final Varargs result;
        try {
            result = kernel.resume(resumeArgs);
        } catch (final Throwable t) {
            lastError = String.valueOf(t.getMessage());
            return StepResult.error(lastError);
        }

        if (LuaThread.STATUS_NAMES[LuaThread.STATUS_DEAD].equals(kernel.getStatus())) {
            // The kernel chunk returned pcallTimeoutCheck(pcall(main)): (resumeOk, mainOk, value...).
            final boolean mainOk = result.arg(2).toboolean();
            if (!mainOk) {
                lastError = result.arg(3).tojstring();
                return StepResult.error(lastError);
            }
            return StepResult.shutdown(); // "computer halted"
        }

        // Otherwise the kernel yielded a "sysval" describing what it wants next.
        final LuaValue sysval = result.arg(2);
        if (sysval.isnumber()) {
            return StepResult.sleep(sysval.todouble());
        }
        if (sysval.isboolean()) {
            final boolean reboot = sysval.toboolean();
            host.onShutdown(reboot);
            return reboot ? StepResult.reboot() : StepResult.shutdown();
        }
        return StepResult.cont();
    }

    @Nullable
    public String getLastError() {
        return lastError;
    }

    public boolean isRunning() {
        return kernel != null
            && !LuaThread.STATUS_NAMES[LuaThread.STATUS_DEAD].equals(kernel.getStatus());
    }

    ///////////////////////////////////////////////////////////////////

    private void installComputerApi(final Globals g) {
        final LuaTable computer = new LuaTable();
        computer.set("realTime", zero(() -> LuaValue.valueOf(host.realTime())));
        computer.set("uptime", zero(() -> LuaValue.valueOf(host.uptime())));
        computer.set("address", zero(() -> LuaValue.valueOf(host.address())));
        computer.set("tmpAddress", zero(() -> LuaValue.NIL));
        computer.set("freeMemory", zero(() -> LuaValue.valueOf((int) Math.min(Integer.MAX_VALUE, host.freeMemory()))));
        computer.set("totalMemory", zero(() -> LuaValue.valueOf((int) Math.min(Integer.MAX_VALUE, host.totalMemory()))));
        computer.set("energy", zero(() -> LuaValue.valueOf(host.energy())));
        computer.set("maxEnergy", zero(() -> LuaValue.valueOf(host.maxEnergy())));
        computer.set("isRobot", zero(() -> LuaValue.FALSE));
        computer.set("users", varargs(a -> LuaValue.NONE));
        computer.set("pushSignal", varargs(a -> {
            final String name = a.optjstring(1, "");
            final Object[] args = LuaConversions.toJavaList(a, 2).toArray();
            host.pushSignal(name, args);
            return LuaValue.TRUE;
        }));
        // The Lua bios overrides these via the eeprom; provide harmless defaults.
        computer.set("getBootAddress", zero(() -> LuaValue.NIL));
        computer.set("setBootAddress", varargs(a -> LuaValue.NONE));
        computer.set("getArchitectures", varargs(a -> new LuaTable()));
        computer.set("getArchitecture", varargs(a -> LuaValue.valueOf("Lua")));
        computer.set("setArchitecture", varargs(a -> LuaValue.TRUE));
        g.set("computer", computer);
    }

    private void installComponentApi(final Globals g) {
        final LuaTable component = new LuaTable();
        component.set("list", varargs(a -> {
            final String filter = a.isstring(1) ? a.tojstring(1) : null;
            final boolean exact = a.toboolean(2);
            final LuaTable table = new LuaTable();
            for (final Map.Entry<String, String> entry : host.componentList(filter, exact).entrySet()) {
                table.set(entry.getKey(), LuaValue.valueOf(entry.getValue()));
            }
            return table;
        }));
        component.set("type", varargs(a -> {
            final String type = host.componentType(a.checkjstring(1));
            return type != null ? LuaValue.valueOf(type) : LuaValue.NIL;
        }));
        component.set("slot", varargs(a -> LuaValue.valueOf(host.componentSlot(a.checkjstring(1)))));
        component.set("doc", varargs(a -> {
            final String doc = host.componentDoc(a.checkjstring(1), a.checkjstring(2));
            return doc != null ? LuaValue.valueOf(doc) : LuaValue.NIL;
        }));
        component.set("methods", varargs(a -> {
            final LuaTable methods = new LuaTable();
            for (final Map.Entry<String, MachineHost.MethodInfo> entry : host.componentMethods(a.checkjstring(1)).entrySet()) {
                final MachineHost.MethodInfo info = entry.getValue();
                final LuaTable infoTable = new LuaTable();
                infoTable.set("direct", LuaValue.valueOf(info.direct()));
                if (info.getter()) {
                    infoTable.set("getter", LuaValue.TRUE);
                }
                if (info.setter()) {
                    infoTable.set("setter", LuaValue.TRUE);
                }
                methods.set(entry.getKey(), infoTable);
            }
            return methods;
        }));
        // Contract: return (true, results...) on success or (false, error) on failure.
        component.set("invoke", varargs(a -> {
            final String address = a.checkjstring(1);
            final String method = a.checkjstring(2);
            try {
                final List<Object> results = host.componentInvoke(address, method, LuaConversions.toJavaList(a, 3));
                final LuaValue[] out = new LuaValue[results.size() + 1];
                out[0] = LuaValue.TRUE;
                for (int i = 0; i < results.size(); i++) {
                    out[i + 1] = LuaConversions.toLua(results.get(i));
                }
                return LuaValue.varargsOf(out);
            } catch (final Throwable t) {
                final String message = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf(message));
            }
        }));
        g.set("component", component);
    }

    private void installSystemApi(final Globals g) {
        final LuaTable system = new LuaTable();
        system.set("allowGC", zero(() -> LuaValue.FALSE));
        system.set("allowBytecode", zero(() -> LuaValue.TRUE));
        system.set("timeout", zero(() -> LuaValue.valueOf(5)));
        g.set("system", system);
    }

    private void installUnicodeApi(final Globals g) {
        final LuaTable unicode = new LuaTable();
        unicode.set("char", varargs(a -> {
            final StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= a.narg(); i++) {
                sb.appendCodePoint(a.checkint(i));
            }
            return LuaValue.valueOf(sb.toString());
        }));
        unicode.set("len", one(s -> LuaValue.valueOf(s.checkjstring().codePointCount(0, s.checkjstring().length()))));
        unicode.set("upper", one(s -> LuaValue.valueOf(s.checkjstring().toUpperCase())));
        unicode.set("lower", one(s -> LuaValue.valueOf(s.checkjstring().toLowerCase())));
        unicode.set("reverse", one(s -> LuaValue.valueOf(new StringBuilder(s.checkjstring()).reverse().toString())));
        unicode.set("sub", varargs(a -> {
            final String s = a.checkjstring(1);
            final int[] cp = s.codePoints().toArray();
            int i = a.optint(2, 1);
            int j = a.optint(3, cp.length);
            if (i < 0) i = Math.max(cp.length + i + 1, 1);
            else if (i == 0) i = 1;
            if (j < 0) j = cp.length + j + 1;
            else if (j > cp.length) j = cp.length;
            if (i > j) {
                return LuaValue.valueOf("");
            }
            final StringBuilder sb = new StringBuilder();
            for (int k = i; k <= j; k++) {
                sb.appendCodePoint(cp[k - 1]);
            }
            return LuaValue.valueOf(sb.toString());
        }));
        unicode.set("isWide", varargs(a -> LuaValue.FALSE));
        unicode.set("charWidth", varargs(a -> LuaValue.valueOf(1)));
        unicode.set("wlen", one(s -> LuaValue.valueOf(s.checkjstring().codePointCount(0, s.checkjstring().length()))));
        unicode.set("wtrunc", varargs(a -> a.arg1()));
        g.set("unicode", unicode);
    }

    /**
     * Patches LuaJ's Lua 5.2 standard library to behave like the Lua 5.3 environment OpenOS/MineOS
     * were written against. Most importantly, {@code string.format("%s", t)} must accept non-string
     * values (5.3 applies {@code tostring}); LuaJ is strict and would error "string expected".
     */
    private void installCompatShims(final Globals g) {
        final String shim =
            "local rawformat = string.format\n" +
            "string.format = function(fmt, ...)\n" +
            "  if type(fmt) ~= 'string' then return rawformat(fmt, ...) end\n" +
            "  local args = table.pack(...)\n" +
            "  local argi, i, len = 0, 1, #fmt\n" +
            "  while i <= len do\n" +
            "    if fmt:sub(i, i) == '%' then\n" +
            "      local j = i + 1\n" +
            "      while j <= len and fmt:sub(j, j):match('[%-%+ #0-9%.]') do j = j + 1 end\n" +
            "      local spec = fmt:sub(j, j)\n" +
            "      if spec ~= '%' then\n" +
            "        argi = argi + 1\n" +
            "        if (spec == 's' or spec == 'q') then\n" +
            "          local v = args[argi]\n" +
            "          if type(v) ~= 'string' and type(v) ~= 'number' then args[argi] = tostring(v) end\n" +
            "        end\n" +
            "      end\n" +
            "      i = j + 1\n" +
            "    else\n" +
            "      i = i + 1\n" +
            "    end\n" +
            "  end\n" +
            "  return rawformat(fmt, table.unpack(args, 1, args.n))\n" +
            "end\n";
        g.load(shim, "=compat").call();
    }

    private void installUserdataApi(final Globals g) {
        final LuaTable userdata = new LuaTable();
        userdata.set("methods", varargs(a -> {
            final LuaUserdata ud = asUserdata(a.arg(1));
            final LuaTable table = new LuaTable();
            if (ud != null) {
                ud.methods().forEach((name, direct) -> table.set(name, LuaValue.valueOf(direct)));
            }
            return table;
        }));
        userdata.set("invoke", varargs(a -> {
            final LuaUserdata ud = asUserdata(a.arg(1));
            if (ud == null) {
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("not userdata"));
            }
            final String method = a.checkjstring(2);
            try {
                final List<Object> results = ud.invoke(method, LuaConversions.toJavaList(a, 3));
                final LuaValue[] out = new LuaValue[results.size() + 1];
                out[0] = LuaValue.TRUE;
                for (int i = 0; i < results.size(); i++) {
                    out[i + 1] = LuaConversions.toLua(results.get(i));
                }
                return LuaValue.varargsOf(out);
            } catch (final Throwable t) {
                final String message = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf(message));
            }
        }));
        userdata.set("doc", varargs(a -> {
            final LuaUserdata ud = asUserdata(a.arg(1));
            final String doc = ud != null ? ud.doc(a.checkjstring(2)) : null;
            return doc != null ? LuaValue.valueOf(doc) : LuaValue.NIL;
        }));
        userdata.set("dispose", varargs(a -> {
            final LuaUserdata ud = asUserdata(a.arg(1));
            if (ud != null) {
                ud.dispose();
            }
            return LuaValue.NONE;
        }));
        // Index/call/persistence on userdata are unsupported by our handles.
        final VarArgFunction unsupported = varargs(a -> {
            throw new org.luaj.vm2.LuaError("operation not supported on this value");
        });
        userdata.set("apply", unsupported);
        userdata.set("unapply", unsupported);
        userdata.set("call", unsupported);
        userdata.set("save", unsupported);
        userdata.set("load", unsupported);
        g.set("userdata", userdata);
    }

    @Nullable
    private static LuaUserdata asUserdata(final LuaValue value) {
        if (value.isuserdata()) {
            final Object o = value.touserdata();
            if (o instanceof final LuaUserdata ud) {
                return ud;
            }
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////

    private static String readMachineSource() throws IOException {
        try (final InputStream stream = LuaArchitecture.class.getResourceAsStream(MACHINE_SOURCE_PATH)) {
            if (stream == null) {
                throw new IOException("missing kernel resource: " + MACHINE_SOURCE_PATH);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @FunctionalInterface
    private interface ZeroFn {
        LuaValue call();
    }

    @FunctionalInterface
    private interface OneFn {
        LuaValue call(LuaValue arg);
    }

    @FunctionalInterface
    private interface VarFn {
        Varargs call(Varargs args);
    }

    private static ZeroArgFunction zero(final ZeroFn fn) {
        return new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return fn.call();
            }
        };
    }

    private static OneArgFunction one(final OneFn fn) {
        return new OneArgFunction() {
            @Override
            public LuaValue call(final LuaValue arg) {
                return fn.call(arg);
            }
        };
    }

    private static VarArgFunction varargs(final VarFn fn) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return fn.call(args);
            }
        };
    }
}
