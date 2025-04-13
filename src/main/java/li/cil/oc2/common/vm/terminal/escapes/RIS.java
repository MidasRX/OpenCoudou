package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.ModeState;
import li.cil.oc2.common.vm.terminal.PrivateModeState;
import li.cil.oc2.common.vm.terminal.Terminal;

import java.util.Arrays;

public class RIS {
    public static void execute(Terminal terminal) {
        terminal.currentForegroundColorMode = Terminal.ColorMode.SIXTEEN_COLOR;
        terminal.currentBackgroundColorMode = Terminal.ColorMode.SIXTEEN_COLOR;
        terminal.Use1006 = false;
        terminal.sixteenColor = Terminal.DEFAULT_COLORS.Copy();
        terminal.backgroundColor = Terminal.DEFAULT_TRUE_COLOR_BACKGROUND.Copy();
        terminal.foregroundColor = Terminal.DEFAULT_TRUE_COLOR_FOREGROUND.Copy();
        terminal.twoFiftySixColor = Terminal.DEFAULT_256_COLORS.Copy();
        terminal.style = Terminal.DEFAULT_STYLE;
        terminal.currentModeState = new ModeState();
        terminal.currentPrivateModeState = new PrivateModeState();
        terminal.lastRowToDisplay = 24;
        terminal.lastRowToDisplayMax = 24;
        terminal.drawingModeG0 = Terminal.DrawingMode.ASCII;
        terminal.drawingModeG1 = Terminal.DrawingMode.ASCII;
        terminal.useG0 = true;
        terminal.clear();
        terminal.clearAlt();
        Arrays.fill(terminal.tabs, false);
        Arrays.fill(terminal.altTabs, false);
        for (int i = 1; i < Terminal.WIDTH; i++) {
            if (i % Terminal.TAB_WIDTH == 0) {
                terminal.tabs[i] = true;
                terminal.altTabs[i] = true;
            }
        }
    }
}
