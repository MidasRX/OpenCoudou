/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.component;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * OpenComputers-style {@code gpu} component. Maintains a character/color shadow buffer and renders
 * changes to the existing {@link li.cil.oc2.common.vm.terminal.Terminal} by emitting ANSI/VT escape
 * sequences through the supplied sink (which also forwards them to tracking clients). Colors use
 * 24-bit RGB and are emitted as truecolor SGR sequences.
 */
@SuppressWarnings("unused")
public final class GpuDevice {
    private final int maxWidth;
    private final int maxHeight;
    private final Consumer<byte[]> output;

    private int width;
    private int height;
    private int[] chars;
    private int[] fg;
    private int[] bg;

    private int foreground = 0xFFFFFF;
    private int background = 0x000000;
    @Nullable private String boundScreen;

    public GpuDevice(final int maxWidth, final int maxHeight, final Consumer<byte[]> output) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.output = output;
        resize(maxWidth, maxHeight);
    }

    private void resize(final int w, final int h) {
        width = w;
        height = h;
        chars = new int[w * h];
        fg = new int[w * h];
        bg = new int[w * h];
        Arrays.fill(chars, ' ');
        Arrays.fill(fg, foreground);
        Arrays.fill(bg, background);
    }

    ///////////////////////////////////////////////////////////////////

    @Callback(synchronize = false, description = "Binds the GPU to the screen with the specified address.")
    public boolean bind(@Parameter("address") final String address, @Nullable @Parameter("reset") final Boolean reset) {
        boundScreen = address;
        if (reset == null || reset) {
            emit("\033[2J"); // clear screen
            setResolution(maxWidth, maxHeight);
        }
        return true;
    }

    @Callback(synchronize = false, description = "Get the address of the screen the GPU is bound to.")
    @Nullable
    public String getScreen() {
        return boundScreen;
    }

    @Callback(synchronize = false, description = "Get the current resolution.")
    public Object[] getResolution() {
        return new Object[]{width, height};
    }

    @Callback(synchronize = false, description = "Set the resolution.")
    public boolean setResolution(@Parameter("width") final int w, @Parameter("height") final int h) {
        final int nw = Math.max(1, Math.min(maxWidth, w));
        final int nh = Math.max(1, Math.min(maxHeight, h));
        if (nw == width && nh == height) {
            return false;
        }
        resize(nw, nh);
        emit("\033[2J");
        return true;
    }

    @Callback(synchronize = false, description = "Get the maximum supported resolution.")
    public Object[] maxResolution() {
        return new Object[]{maxWidth, maxHeight};
    }

    @Callback(synchronize = false, description = "Get the current viewport resolution.")
    public Object[] getViewport() {
        return new Object[]{width, height};
    }

    @Callback(synchronize = false, description = "Set the viewport resolution.")
    public boolean setViewport(@Parameter("width") final int w, @Parameter("height") final int h) {
        return setResolution(w, h);
    }

    @Callback(synchronize = false, description = "Get the current color depth.")
    public int getDepth() {
        return 8;
    }

    @Callback(synchronize = false, description = "Set the color depth.")
    public int setDepth(@Parameter("depth") final int depth) {
        return 8;
    }

    @Callback(synchronize = false, description = "Get the maximum supported color depth.")
    public int maxDepth() {
        return 8;
    }

    @Callback(synchronize = false, description = "Get the current background color.")
    public Object[] getBackground() {
        return new Object[]{background, false};
    }

    @Callback(synchronize = false, description = "Set the background color.")
    public Object[] setBackground(@Parameter("value") final int value, @Nullable @Parameter("palette") final Boolean palette) {
        final int old = background;
        background = value & 0xFFFFFF;
        return new Object[]{old, false};
    }

    @Callback(synchronize = false, description = "Get the current foreground color.")
    public Object[] getForeground() {
        return new Object[]{foreground, false};
    }

    @Callback(synchronize = false, description = "Set the foreground color.")
    public Object[] setForeground(@Parameter("value") final int value, @Nullable @Parameter("palette") final Boolean palette) {
        final int old = foreground;
        foreground = value & 0xFFFFFF;
        return new Object[]{old, false};
    }

    @Callback(synchronize = false, description = "Get the palette color at the specified index.")
    public int getPaletteColor(@Parameter("index") final int index) {
        return 0;
    }

    @Callback(synchronize = false, description = "Set the palette color at the specified index.")
    public int setPaletteColor(@Parameter("index") final int index, @Parameter("value") final int value) {
        return 0;
    }

    @Callback(synchronize = false, description = "Get the value displayed at the specified coordinates.")
    public Object[] get(@Parameter("x") final int x, @Parameter("y") final int y) {
        if (!inBounds(x, y)) {
            throw new IllegalArgumentException("index out of bounds");
        }
        final int index = (y - 1) * width + (x - 1);
        return new Object[]{new String(Character.toChars(chars[index])), fg[index], bg[index]};
    }

    @Callback(synchronize = false, description = "Write text to the screen at the specified position.")
    public boolean set(@Parameter("x") final int x, @Parameter("y") final int y, @Parameter("value") final String value,
                       @Nullable @Parameter("vertical") final Boolean vertical) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        final int[] cps = value.codePoints().toArray();
        int cx = x;
        int cy = y;
        moveTo(cx, cy);
        applyColors();
        final StringBuilder run = new StringBuilder();
        for (final int cp : cps) {
            if (cx >= 1 && cx <= width && cy >= 1 && cy <= height) {
                final int index = (cy - 1) * width + (cx - 1);
                chars[index] = cp;
                fg[index] = foreground;
                bg[index] = background;
                run.appendCodePoint(cp);
            }
            if (vertical != null && vertical) {
                cy++;
            } else {
                cx++;
            }
        }
        if (vertical != null && vertical) {
            // Emit per-cell for vertical runs (cursor must move each row).
            emitVertical(x, y, cps);
        } else {
            emit(run.toString());
        }
        return true;
    }

    @Callback(synchronize = false, description = "Fill a rectangle with the specified character.")
    public boolean fill(@Parameter("x") final int x, @Parameter("y") final int y,
                        @Parameter("width") final int w, @Parameter("height") final int h,
                        @Parameter("char") final String ch) {
        if (ch == null || ch.isEmpty()) {
            return false;
        }
        final int cp = ch.codePointAt(0);
        applyColors();
        final StringBuilder line = new StringBuilder();
        for (int i = 0; i < w; i++) {
            line.appendCodePoint(cp);
        }
        for (int row = y; row < y + h; row++) {
            if (row < 1 || row > height) {
                continue;
            }
            moveTo(x, row);
            emit(line.toString());
            for (int col = x; col < x + w; col++) {
                if (col >= 1 && col <= width) {
                    final int index = (row - 1) * width + (col - 1);
                    chars[index] = cp;
                    fg[index] = foreground;
                    bg[index] = background;
                }
            }
        }
        return true;
    }

    @Callback(synchronize = false, description = "Copy a portion of the screen to another location.")
    public boolean copy(@Parameter("x") final int x, @Parameter("y") final int y,
                       @Parameter("width") final int w, @Parameter("height") final int h,
                       @Parameter("tx") final int tx, @Parameter("ty") final int ty) {
        // Snapshot source from the shadow buffer, then write to the destination and re-render.
        final int[] sc = new int[w * h];
        final int[] sf = new int[w * h];
        final int[] sb = new int[w * h];
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                final int sx = x + col;
                final int sy = y + row;
                final int k = row * w + col;
                if (inBounds(sx, sy)) {
                    final int index = (sy - 1) * width + (sx - 1);
                    sc[k] = chars[index];
                    sf[k] = fg[index];
                    sb[k] = bg[index];
                } else {
                    sc[k] = ' ';
                    sf[k] = foreground;
                    sb[k] = background;
                }
            }
        }
        for (int row = 0; row < h; row++) {
            final int dy = ty + y + row;
            if (dy < 1 || dy > height) {
                continue;
            }
            for (int col = 0; col < w; col++) {
                final int dx = tx + x + col;
                if (dx < 1 || dx > width) {
                    continue;
                }
                final int k = row * w + col;
                final int index = (dy - 1) * width + (dx - 1);
                chars[index] = sc[k];
                fg[index] = sf[k];
                bg[index] = sb[k];
            }
            // Re-render this destination row segment.
            renderRowSegment(Math.max(1, tx + x), dy, w);
        }
        return true;
    }

    ///////////////////////////////////////////////////////////////////

    private void renderRowSegment(final int startX, final int row, final int count) {
        moveTo(startX, row);
        for (int col = startX; col < startX + count && col <= width; col++) {
            final int index = (row - 1) * width + (col - 1);
            emitColors(fg[index], bg[index]);
            emit(new String(Character.toChars(chars[index])));
        }
    }

    private void emitVertical(final int x, final int y, final int[] cps) {
        for (int i = 0; i < cps.length; i++) {
            final int cy = y + i;
            if (cy < 1 || cy > height || x < 1 || x > width) {
                continue;
            }
            moveTo(x, cy);
            applyColors();
            emit(new String(Character.toChars(cps[i])));
        }
    }

    private boolean inBounds(final int x, final int y) {
        return x >= 1 && x <= width && y >= 1 && y <= height;
    }

    private void moveTo(final int x, final int y) {
        emit("\033[" + y + ";" + x + "H");
    }

    private void applyColors() {
        emitColors(foreground, background);
    }

    private void emitColors(final int fgColor, final int bgColor) {
        emit("\033[38;2;" + ((fgColor >> 16) & 0xFF) + ";" + ((fgColor >> 8) & 0xFF) + ";" + (fgColor & 0xFF) + "m");
        emit("\033[48;2;" + ((bgColor >> 16) & 0xFF) + ";" + ((bgColor >> 8) & 0xFF) + ";" + (bgColor & 0xFF) + "m");
    }

    private void emit(final String ansi) {
        output.accept(ansi.getBytes(StandardCharsets.UTF_8));
    }
}
