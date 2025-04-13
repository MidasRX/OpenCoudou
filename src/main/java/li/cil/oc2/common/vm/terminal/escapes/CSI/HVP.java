package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class HVP {
    public static void execute(Terminal terminal, int y, int x) {
        CUP.execute(terminal, y, x);
    }
}
