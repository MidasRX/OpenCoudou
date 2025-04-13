package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DSR {
    public static void execute(Terminal terminal, int value) {
        switch (value) {
            case 5 -> // Report console status
                terminal.putResponse("\033[0n"); // Ready, No malfunctions detected
            case 6 -> { // Report cursor position
                terminal.putResponse(String.format("\033[?%d;%dR", terminal.y + 1, terminal.x + 1));
            }
        }
    }
}
