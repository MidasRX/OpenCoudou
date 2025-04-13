package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DECSTBM {
    public static void execute(Terminal terminal, int[] args, int argCount) {
        final int first, last;
        if (argCount == 2) {
            first = args[0] - 1;
            last = args[1] - 1;
        } else {
            first = 0;
            last = Terminal.HEIGHT - 1;
        }
        if (first < 0 || last > Terminal.HEIGHT - 1 || last - first <= 0) {
            return;
        }
        terminal.scrollFirst = first; // to index
        terminal.scrollLast = last; // to index
        terminal.setRelativeCursorPos(0, 0); // send cursor home
    }
}
