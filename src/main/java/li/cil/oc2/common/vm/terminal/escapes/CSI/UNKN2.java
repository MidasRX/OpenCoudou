package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class UNKN2 {
    public static void execute(Terminal terminal, int[] args, int argCount) {
        if (argCount == 1) {
            for (int i = 0; i < args[0]; i++) {
                terminal.shiftUpOne();
            }
        }
    }
}
