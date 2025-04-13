package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

import java.util.Arrays;

public class TBC {
    public static void execute(Terminal terminal, int value) {
        switch (value) {
            case 0 -> { // Clear tab at current column
                if (terminal.x >= 0 && terminal.x < Terminal.WIDTH) {
                    terminal.tabs[terminal.x] = false;
                }
            }
            case 3 -> // Clear all tabs
                Arrays.fill(terminal.tabs, false);
        }
    }
}
