/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.fs;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple in-memory file tree backing the OpenComputers-style {@code filesystem} component. Stores
 * file contents as raw bytes keyed by normalized absolute path; directories are tracked separately.
 * <p>
 * This is the first-pass storage for the Lua machine. It is not yet persisted to the world save
 * (reboot-on-reload); Phase 3 will back it with {@code BlobStorage} for durability.
 */
public final class InMemoryFileSystem {
    private final TreeMap<String, byte[]> files = new TreeMap<>();
    private final java.util.TreeSet<String> directories = new java.util.TreeSet<>();
    private final long capacity;
    private long lastModified;

    public InMemoryFileSystem(final long capacity) {
        this.capacity = capacity;
        directories.add("/");
    }

    ///////////////////////////////////////////////////////////////////

    public static String normalize(final String path) {
        final String[] parts = path.replace('\\', '/').split("/");
        final ArrayList<String> stack = new ArrayList<>();
        for (final String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
            } else {
                stack.add(part);
            }
        }
        return "/" + String.join("/", stack);
    }

    public boolean exists(final String path) {
        final String p = normalize(path);
        return files.containsKey(p) || directories.contains(p);
    }

    public boolean isDirectory(final String path) {
        return directories.contains(normalize(path));
    }

    public long size(final String path) {
        final byte[] data = files.get(normalize(path));
        return data != null ? data.length : 0;
    }

    @Nullable
    public byte[] read(final String path) {
        return files.get(normalize(path));
    }

    public List<String> list(final String path) {
        final String dir = normalize(path);
        if (!directories.contains(dir)) {
            return List.of();
        }
        final String prefix = dir.equals("/") ? "/" : dir + "/";
        final java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        for (final String d : directories) {
            if (!d.equals(dir) && d.startsWith(prefix)) {
                final String rest = d.substring(prefix.length());
                if (!rest.contains("/")) {
                    names.add(rest + "/");
                }
            }
        }
        for (final String f : files.keySet()) {
            if (f.startsWith(prefix)) {
                final String rest = f.substring(prefix.length());
                if (!rest.contains("/")) {
                    names.add(rest);
                }
            }
        }
        return new ArrayList<>(names);
    }

    public boolean makeDirectory(final String path) {
        final String p = normalize(path);
        if (exists(p)) {
            return false;
        }
        makeParents(p);
        directories.add(p);
        touch();
        return true;
    }

    public boolean remove(final String path) {
        final String p = normalize(path);
        boolean removed = false;
        if (files.remove(p) != null) {
            removed = true;
        }
        // Remove directory and everything under it.
        final String prefix = p + "/";
        removed |= directories.remove(p);
        files.keySet().removeIf(f -> f.startsWith(prefix));
        directories.removeIf(d -> d.startsWith(prefix));
        if (removed) {
            touch();
        }
        return removed;
    }

    public boolean rename(final String from, final String to) {
        final String src = normalize(from);
        final String dst = normalize(to);
        if (files.containsKey(src)) {
            final byte[] data = files.remove(src);
            makeParents(dst);
            files.put(dst, data);
            touch();
            return true;
        }
        if (directories.contains(src)) {
            final String srcPrefix = src + "/";
            final Map<String, byte[]> moved = new TreeMap<>();
            files.entrySet().removeIf(e -> {
                if (e.getKey().startsWith(srcPrefix)) {
                    moved.put(dst + e.getKey().substring(src.length()), e.getValue());
                    return true;
                }
                return false;
            });
            final List<String> movedDirs = new ArrayList<>();
            directories.removeIf(d -> {
                if (d.equals(src) || d.startsWith(srcPrefix)) {
                    movedDirs.add(dst + d.substring(src.length()));
                    return true;
                }
                return false;
            });
            makeParents(dst);
            directories.add(dst);
            directories.addAll(movedDirs);
            files.putAll(moved);
            touch();
            return true;
        }
        return false;
    }

    /** Write bytes to a file, creating parents. When {@code append}, concatenates to existing content. */
    public void write(final String path, final byte[] data, final boolean append) {
        final String p = normalize(path);
        makeParents(p);
        if (append) {
            final byte[] existing = files.get(p);
            if (existing != null) {
                final byte[] combined = new byte[existing.length + data.length];
                System.arraycopy(existing, 0, combined, 0, existing.length);
                System.arraycopy(data, 0, combined, existing.length, data.length);
                files.put(p, combined);
                touch();
                return;
            }
        }
        files.put(p, data);
        touch();
    }

    public long lastModified(final String path) {
        return exists(path) ? lastModified : 0;
    }

    public long spaceTotal() {
        return capacity;
    }

    public long spaceUsed() {
        long used = 0;
        for (final byte[] data : files.values()) {
            used += data.length;
        }
        return used;
    }

    ///////////////////////////////////////////////////////////////////

    private void makeParents(final String path) {
        final int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return;
        }
        String dir = path.substring(0, lastSlash);
        while (!dir.isEmpty() && !directories.contains(dir)) {
            directories.add(dir);
            final int slash = dir.lastIndexOf('/');
            dir = slash <= 0 ? "" : dir.substring(0, slash);
        }
    }

    private void touch() {
        lastModified++;
    }
}
