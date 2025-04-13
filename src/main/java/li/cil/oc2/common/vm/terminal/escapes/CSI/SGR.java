package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

public class SGR {
    public static void execute(Terminal terminal, int[] args, int argCount) {
        for (int i = 0; i < argCount; i++) {
            if (args[i] == 38 || args[i] == 48) {
                if (args[i] == 38) {
                    int v2 = ++i;
                    if (args[v2] == 5) {
                        terminal.currentForegroundColorMode = Terminal.ColorMode.TWO_FIFTY_SIX_COLOR;
                        terminal.twoFiftySixColor.R = args[++i];
                    } else if (args[v2] == 2) {
                        terminal.currentForegroundColorMode = Terminal.ColorMode.TRUE_COLOR;
                        terminal.foregroundColor = new Terminal.ColorData(args[++i], args[++i], args[++i], Terminal.ColorMode.TRUE_COLOR);
                    }
                } else {
                    int v2 = ++i;
                    if (args[v2] == 5) {
                        terminal.currentBackgroundColorMode = Terminal.ColorMode.TWO_FIFTY_SIX_COLOR;
                        terminal.twoFiftySixColor.G = args[++i];
                    } else if (args[v2] == 2) {
                        terminal.currentBackgroundColorMode = Terminal.ColorMode.TRUE_COLOR;
                        terminal.backgroundColor = new Terminal.ColorData(args[++i], args[++i], args[++i], Terminal.ColorMode.TRUE_COLOR);
                    }
                }
                return;
            }
            selectStyle(terminal, args[i]);
        }
    }

    private static void selectStyle(Terminal terminal, int arg) {
        switch (arg) {
            case 0 -> { // Reset / Normal
                terminal.sixteenColor = Terminal.DEFAULT_COLORS.Copy();
                terminal.style = Terminal.DEFAULT_STYLE;
                terminal.currentForegroundColorMode = Terminal.ColorMode.SIXTEEN_COLOR;
                terminal.currentBackgroundColorMode = Terminal.ColorMode.SIXTEEN_COLOR;
                terminal.twoFiftySixColor = Terminal.DEFAULT_256_COLORS.Copy();
                terminal.foregroundColor = Terminal.DEFAULT_TRUE_COLOR_FOREGROUND.Copy();
                terminal.backgroundColor = Terminal.DEFAULT_TRUE_COLOR_BACKGROUND.Copy();
            }
            case 1 -> // Bold or increased intensity
                terminal.style |= Terminal.STYLE_BOLD_MASK;
            case 2 -> // Faint or decreased intensity
                terminal.style |= Terminal.STYLE_DIM_MASK;
            case 4 -> // Underscore
                terminal.style |= Terminal.STYLE_UNDERLINE_MASK;
            case 5 -> // Blink
                terminal.style |= Terminal.STYLE_BLINK_MASK;
            case 7 -> // Negative (reverse) image
                terminal.style |= Terminal.STYLE_INVERT_MASK;
            case 8 -> // Conceal aka Hide
                terminal.style |= Terminal.STYLE_HIDDEN_MASK;
            case 22 -> // Normal color or intensity
                terminal.style &= ~(Terminal.STYLE_BOLD_MASK | Terminal.STYLE_DIM_MASK);
            case 24 -> // Underline off
                terminal.style &= ~Terminal.STYLE_UNDERLINE_MASK;
            case 25 -> // Blink off
                terminal.style &= ~Terminal.STYLE_BLINK_MASK;
            case 27 -> // Reverse/invert off
                terminal.style &= ~Terminal.STYLE_INVERT_MASK;
            case 28 -> // Reveal conceal off
                terminal.style &= ~Terminal.STYLE_HIDDEN_MASK;
            case 30, 31, 32, 33, 34, 35, 36, 37 -> { // Set foreground color
                terminal.currentForegroundColorMode = Terminal.ColorMode.SIXTEEN_COLOR;
                terminal.sixteenColor.R = arg - 30;
            }
            case 40, 41, 42, 43, 44, 45, 46, 47 -> { //–47 Set background color
                terminal.currentBackgroundColorMode = Terminal.ColorMode.SIXTEEN_COLOR;
                terminal.sixteenColor.G = arg - 40;
            }
        }
    }
}
