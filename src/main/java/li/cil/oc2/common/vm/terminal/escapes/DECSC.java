package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DECSC {
    public static void execute(Terminal terminal) {
        System.out.print("Saved cursor");
        terminal.savedX = terminal.x;
        terminal.savedY = terminal.y;
    }
}
