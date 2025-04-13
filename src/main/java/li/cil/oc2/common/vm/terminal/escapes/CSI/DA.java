package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DA {
    public static void execute(Terminal terminal) {
        terminal.putResponse("\033[?1;0c");
    }
}
