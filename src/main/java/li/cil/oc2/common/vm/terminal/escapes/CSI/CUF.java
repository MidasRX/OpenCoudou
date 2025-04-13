package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CUF {
    public static void execute(Terminal terminal, int value) {
        terminal.setClampedCursorPos(terminal.x + Math.max(1, value), terminal.y);
    }
}
