package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class IL {
    public static void execute(Terminal terminal, int lines) {
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();
        lines = Math.max(lines, 1);
        if (useAltBuffer) {
            terminal.shiftLines(terminal.y, terminal.scrollLast - lines, lines);
        }
        else {
            terminal.shiftLines(useAltBuffer ? terminal.y : terminal.y + (terminal.lastRowToDisplayMax - Terminal.HEIGHT), useAltBuffer ? terminal.scrollLast - 1 : (Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT) - 2, (Math.max(1, lines)));
        }
    }
}
