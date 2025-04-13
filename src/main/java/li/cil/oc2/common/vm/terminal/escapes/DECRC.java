package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DECRC {
    public static void execute(Terminal terminal) {
        System.out.print("Restored cursor");
        terminal.x = terminal.savedX;
        terminal.y = terminal.savedY;
    }
}
