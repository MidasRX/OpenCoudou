package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DL {
    public static void execute(Terminal terminal, int lines) {
        terminal.setCursorPos(0, terminal.y);

        lines = Math.max(lines, 1);

        for (int i = 0; i < lines; i++) {
            terminal.clearLine(terminal.y + i);
        }

        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        if (useAltBuffer) {
            terminal.shiftLines(terminal.y + lines, terminal.scrollLast, -lines);
        } else {
            terminal.shiftLines(useAltBuffer ? terminal.y - 1 : terminal.y + lines, useAltBuffer ? terminal.scrollLast : (Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT) - 1, -lines);
        }
    }
}
