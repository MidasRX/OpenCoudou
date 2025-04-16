package li.cil.oc2.common.vm.terminal.fonts;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class UnicodeFontRenderer {
    public final Font font;
    public final FontAtlas TerminusFontAtlas = new FontAtlas(512, 512);
    private final Map<Integer, Glyph> glyphCache = new HashMap<>();
    private final FontRenderContext frc = new FontRenderContext(null, false, false);

    public UnicodeFontRenderer(Font font) {
        this.font = font;

        String initialSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+-=_.,:;<>?;':\"\\|`~[]{}1234567890△▽ ";
        int[] characters = initialSet.codePoints().toArray();
        for (final int character : characters) {
            getGlyph(character);
        }
    }

    public Glyph getGlyph(int character) {
        return glyphCache.computeIfAbsent(character, this::rasterizeGlyph);
    }

    private Glyph rasterizeGlyph(int character) {
        GlyphVector gv = font.createGlyphVector(frc, Character.toChars(character));
        BufferedImage img = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB); // size can be dynamic
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setFont(font);
        g.setColor(Color.WHITE);

        FontMetrics metrics = g.getFontMetrics();
        int ascent = metrics.getAscent();

        g.drawGlyphVector(gv, 0, ascent);
        g.dispose();

        Glyph glyph = new Glyph(img, 16, 32, (int) gv.getGlyphMetrics(0).getAdvance());

        TerminusFontAtlas.addGlyph(glyph);
        return glyph;
    }
}
