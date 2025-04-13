package li.cil.oc2.common.vm.terminal.escapes.CSI;

import li.cil.oc2.common.vm.terminal.Terminal;

import java.util.Arrays;

public class CSIManager {
    private final int[] args = new int[10];
    private int argCount = 0;
    private boolean questionMark = false;
    private boolean greaterThan = false;
    private boolean dollarSign = false;
    private boolean hash = false;
    private boolean quote = false;
    private boolean singleQuote = false;
    private boolean space = false;

    public void handle(final char ch, final Terminal terminal) {
        if (ch >= '0' && ch <= '9') {
            if (argCount < args.length) {
                final int digit = ch - '0';
                if (args[argCount] < (Integer.MAX_VALUE - digit) / 10) {
                    args[argCount] = args[argCount] * 10 + digit;
                } else {
                    args[argCount] = Integer.MAX_VALUE;
                }
            }
        } else {
            if (ch == '?') {
                questionMark = true;
                return;
            }

            if (ch == '>') {
                greaterThan = true;
                return;
            }

            if (ch == '$') {
                hash = true;
                return;
            }

            if (ch == '#') {
                hash = true;
                return;
            }

            if (ch == '"') {
                quote = true;
                return;
            }
            if (ch == '\'') {
                singleQuote = true;
                return;
            }

            if (ch == ' ') {
                space = true;
                return;
            }

            if (argCount < args.length) {
                argCount++;
            }

            if (ch == ';') {
                return; // Keep going, we have another argument.
            }

            terminal.state = Terminal.State.NORMAL;

            switch (ch) {
                case 'A' -> CUU.execute(terminal, args[0]); // CUU - Cursor Up
                case 'B' -> CUD.execute(terminal, args[0]); // CUD – Cursor Down
                case 'C' -> CUF.execute(terminal, args[0]); // CUF – Cursor Forward
                case 'D' -> CUB.execute(terminal, args[0]); // CUB – Cursor Backward
                case 'H' -> CUP.execute(terminal, args[0] - 1, args[1] - 1); // CUP - Cursor Position
                case 'f' -> HVP.execute(terminal, args[0] - 1, args[1] - 1); // HVP – Horizontal and Vertical Position
                case 'm' -> SGR.execute(terminal, args, argCount); // SGR – Select Graphic Rendition
                case 'K' -> EL.execute(terminal, args[0]);  // EL – Erase In Line
                case 'J' -> ED.execute(terminal, args[0]);  // ED – Erase In Display
                case 'r' -> {
                    if (questionMark) {
                        XTRESTORE.executePrivate(terminal, args[0]);
                    }
                    else if (argCount == 2) {
                        DECSTBM.execute(terminal, args, argCount);
                    }
                } // DECSTBM – Set Top and Bottom Margins (DEC public)
                case 'g' -> TBC.execute(terminal, args[0]); // TBC – Tabulation Clear
                case 'h' -> {
                    if (questionMark) {
                        SM.executeDECSET(terminal, args, argCount);
                    } else {
                        SM.execute(terminal, args, argCount);
                    }
                }  // SM – Set Mode
                case 'l' -> {
                    if (questionMark) {
                        RM.executeDECSET(terminal, args, argCount);
                    } else {
                        RM.execute(terminal, args, argCount);
                    }
                } // RM – Reset Mode
                case 'n' -> DSR.execute(terminal, args[0]); // DSR – Device Status Report
                case 'c' -> DA.execute(terminal);  // DA – Device Attributes
                case 'd' -> terminal.setClampedCursorPos(terminal.x, args[0] - 1);
                case 'G' -> terminal.setClampedCursorPos(args[0] - 1, terminal.y);
                case 't' -> {
                    if (args[0] == 14) {
                        terminal.putResponse("\033[4;80;24t");
                    }
                }
                case 'p' -> {
                    int mode = args[0];
                    if (questionMark && dollarSign) { // DECSET
                        terminal.putResponse("\033[?" + mode + ";" + (terminal.currentPrivateModeState.getMode(mode) ? 1 : 0) + "$y");
                    }
                    else if (dollarSign) { // SM
                        terminal.putResponse("\033[" + mode + ";" + (terminal.currentModeState.getMode(mode) ? 1 : 0) + "$y");
                    }
                }
                case 's' -> {
                    if (questionMark) {
                        XTSAVE.executePrivate(terminal, args[0]);
                    }
                    else if (greaterThan) {
                        // Set/reset shift-escape options
                    }
                    else if (argCount == 2) {
                        // Set left/right margins if DECLRMM is enabled
                    }
                    else if (argCount == 0) {
                        if (!terminal.currentPrivateModeState.DECLRMM) {
                            terminal.savedX = terminal.x;
                            terminal.savedY = terminal.y;
                        }
                    }
                }
                case 'X' -> DELCHAR.execute(terminal, args[0]);
                case 'q' -> DECSCUSR.execute(terminal, args[0]);
                case 'L' -> IL.execute(terminal, args[0]);
                case 'M' -> DL.execute(terminal, args[0]);
                case 'S' -> UNKN2.execute(terminal, args, argCount);
                case 'T' -> UNKN.execute(terminal, args, argCount);
                default -> System.out.println("Control sequence: " + ch);
            }
        }
    }

    public void reset() {
        questionMark = false;
        greaterThan = false;
        dollarSign = false;
        hash = false;
        quote = false;
        singleQuote = false;
        space = false;
        argCount = 0;
        Arrays.fill(args, 0);
    }
}
