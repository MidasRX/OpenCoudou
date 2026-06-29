/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.fs;

import li.cil.oc2.common.machine.LuaMachineLog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Builds {@link InMemoryFileSystem}s from files bundled in the mod jar. Jar directories can't be
 * enumerated at runtime, so a precomputed manifest (one absolute path per line) lists the members.
 */
public final class ResourceFileSystems {
    private ResourceFileSystems() {
    }

    /**
     * Load a resource tree into a fresh filesystem.
     *
     * @param manifestPath classpath path of the newline-separated file list (e.g. {@code /assets/oc2r/openos.manifest}).
     * @param rootPath     classpath path the listed files are relative to (e.g. {@code /assets/oc2r/openos}).
     * @param capacity     reported filesystem capacity in bytes.
     */
    public static InMemoryFileSystem fromManifest(final String manifestPath, final String rootPath, final long capacity) {
        final InMemoryFileSystem fs = new InMemoryFileSystem(capacity);
        int loaded = 0;
        try (final InputStream manifestStream = ResourceFileSystems.class.getResourceAsStream(manifestPath)) {
            if (manifestStream == null) {
                LuaMachineLog.log("fs", "missing manifest %s", manifestPath);
                return fs;
            }
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(manifestStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String entry = line.trim();
                    if (entry.isEmpty()) {
                        continue;
                    }
                    try (final InputStream fileStream = ResourceFileSystems.class.getResourceAsStream(rootPath + entry)) {
                        if (fileStream == null) {
                            LuaMachineLog.log("fs", "manifest entry missing: %s", entry);
                            continue;
                        }
                        fs.write(entry, fileStream.readAllBytes(), false);
                        loaded++;
                    }
                }
            }
        } catch (final Exception e) {
            LuaMachineLog.log("fs", "error loading %s: %s", manifestPath, e.getMessage());
        }
        LuaMachineLog.log("fs", "loaded %d files from %s", loaded, rootPath);
        return fs;
    }
}
