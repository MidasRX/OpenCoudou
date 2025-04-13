package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CUU {
    public static void execute(Terminal terminal, int value) {
        terminal.setClampedCursorPos(terminal.x, terminal.y - Math.max(1, value));
    }
}
