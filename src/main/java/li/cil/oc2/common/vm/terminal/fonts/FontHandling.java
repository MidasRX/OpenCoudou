package li.cil.oc2.common.vm.terminal.fonts;

import li.cil.oc2.common.Main;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class FontHandling {
    public static final Font TerminusFont = loadFont("/assets/oc2r/fonts/terminus.ttf", 32f);
    public static final UnicodeFontRenderer unicodeFontRenderer = new UnicodeFontRenderer(TerminusFont);

    public static Glyph getGlyph(int character) {
        return unicodeFontRenderer.getGlyph(character);
    }

    public static ResourceLocation getAtlas() {
        return unicodeFontRenderer.TerminusFontAtlas.getTextureId();
    }

    public static Font loadFont(String path, float size) {
        try (InputStream is = Main.class.getResourceAsStream(path)) {
            if (is == null) {
                return new Font("Arial", Font.PLAIN, (int) size);
            }
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            return new Font("Arial", Font.PLAIN, (int) size); // fallback
        }
    }
}
