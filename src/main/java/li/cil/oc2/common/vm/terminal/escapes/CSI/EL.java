package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class EL {
    public static void execute(Terminal terminal, int value) {
        switch (value) {
            case 0 ->  // From cursor to end of line
                terminal.clearLine(terminal.y, terminal.x, Terminal.WIDTH);
            case 1 ->  // From beginning of line to cursor
                terminal.clearLine(terminal.y, 0, terminal.x + 1);
            case 2 ->  // Entire line containing cursor
                terminal.clearLine(terminal.y);
        }
    }
}
