package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DECRC {
    public static void execute(Terminal terminal) {
        terminal.x = terminal.savedX;
        terminal.y = terminal.savedY;
    }
}
