package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DECSCUSR {
    public static void execute(Terminal terminal, int cursorStyle) {
        if (cursorStyle < 0 || cursorStyle > 6) {
            terminal.cursorMode = Terminal.CursorMode.DEFAULT;
            return;
        }
        terminal.cursorMode = cursorStyle;
    }
}
