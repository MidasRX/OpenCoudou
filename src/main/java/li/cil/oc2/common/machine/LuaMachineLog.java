/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lightweight append-only debug log for the Lua machine rewrite, written to a plain text file in
 * the game's working directory ({@code oc2r-lua-debug.log}) so it can be easily shared while
 * bringing up OpenComputers/MineOS compatibility.
 * <p>
 * This is intentionally separate from the Log4j logger: it gives a single, self-contained,
 * human-readable trace of machine lifecycle + guest output that a tester can grab as a {@code .txt}.
 */
public final class LuaMachineLog {
    private static final String FILE_NAME = "oc2r-lua-debug.log";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Object LOCK = new Object();

    private static boolean enabled = true;

    private LuaMachineLog() {
    }

    public static void setEnabled(final boolean value) {
        enabled = value;
    }

    public static void log(final String category, final String message) {
        if (!enabled) {
            return;
        }
        final String line = "[" + LocalDateTime.now().format(TIMESTAMP) + "] [" + category + "] " + message + System.lineSeparator();
        synchronized (LOCK) {
            try (final Writer writer = Files.newBufferedWriter(
                Path.of(FILE_NAME),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(line);
            } catch (final IOException ignored) {
                // Never let logging break the machine.
            }
        }
    }

    public static void log(final String category, final String format, final Object... args) {
        log(category, String.format(format, args));
    }
}
