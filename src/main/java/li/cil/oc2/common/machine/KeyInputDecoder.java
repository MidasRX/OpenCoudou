/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodes a stream of terminal input bytes (as produced by the in-world {@link
 * li.cil.oc2.common.vm.terminal.Terminal}) into OpenComputers-style {@code key_down} key events,
 * translating printable characters and the common ANSI control/escape sequences (enter, backspace,
 * tab, arrows, etc.) into (char, LWJGL keycode) pairs the OS expects.
 */
public final class KeyInputDecoder {
    /** A decoded key press: unicode codepoint (0 if none) + LWJGL keyboard code. */
    public record Key(int character, int code) {
    }

    // LWJGL2 keyboard codes used by OpenComputers' keyboard library.
    private static final int KEY_ESCAPE = 1;
    private static final int KEY_BACK = 14;
    private static final int KEY_TAB = 15;
    private static final int KEY_RETURN = 28;
    private static final int KEY_UP = 200;
    private static final int KEY_LEFT = 203;
    private static final int KEY_RIGHT = 205;
    private static final int KEY_DOWN = 208;
    private static final int KEY_HOME = 199;
    private static final int KEY_END = 207;
    private static final int KEY_PRIOR = 201; // page up
    private static final int KEY_NEXT = 209;  // page down
    private static final int KEY_DELETE = 211;

    private final StringBuilder escape = new StringBuilder();
    private boolean inEscape;

    /**
     * Feed one input byte; returns a decoded key, or {@code null} if more bytes are needed
     * (mid escape sequence) or the byte was consumed without producing a key.
     */
    public List<Key> push(final int b) {
        final List<Key> out = new ArrayList<>(1);
        if (inEscape) {
            escape.append((char) b);
            // CSI sequences: ESC [ ... final-byte in 0x40-0x7E (we only need the simple ones).
            if (escape.length() >= 2 && b >= 0x40 && b <= 0x7E) {
                final Key key = decodeEscape(escape.toString());
                inEscape = false;
                escape.setLength(0);
                if (key != null) {
                    out.add(key);
                }
            } else if (escape.length() > 8) {
                inEscape = false; // give up on an over-long sequence
                escape.setLength(0);
            }
            return out;
        }

        switch (b) {
            case 0x1B -> { // ESC
                inEscape = true;
                escape.setLength(0);
            }
            case '\r', '\n' -> out.add(new Key(13, KEY_RETURN));
            case 0x08, 0x7F -> out.add(new Key(8, KEY_BACK));
            case '\t' -> out.add(new Key(9, KEY_TAB));
            default -> {
                if (b >= 0x20) {
                    out.add(new Key(b, 0));
                } else if (b > 0) {
                    // Other control characters (e.g. Ctrl+C = 3): pass the char, no special code.
                    out.add(new Key(b, 0));
                }
            }
        }
        return out;
    }

    private static Key decodeEscape(final String seq) {
        // seq starts after ESC; typically "[A", "[B", "[1~", etc.
        if (seq.equals("[A")) return new Key(0, KEY_UP);
        if (seq.equals("[B")) return new Key(0, KEY_DOWN);
        if (seq.equals("[C")) return new Key(0, KEY_RIGHT);
        if (seq.equals("[D")) return new Key(0, KEY_LEFT);
        if (seq.equals("[H") || seq.equals("[1~")) return new Key(0, KEY_HOME);
        if (seq.equals("[F") || seq.equals("[4~")) return new Key(0, KEY_END);
        if (seq.equals("[5~")) return new Key(0, KEY_PRIOR);
        if (seq.equals("[6~")) return new Key(0, KEY_NEXT);
        if (seq.equals("[3~")) return new Key(0, KEY_DELETE);
        if (seq.equals("[2~")) return new Key(0, KEY_ESCAPE); // insert -> no good code, ignore-ish
        return null;
    }
}
