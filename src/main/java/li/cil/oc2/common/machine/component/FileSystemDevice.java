/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.component;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.machine.fs.InMemoryFileSystem;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenComputers-style {@code filesystem} component over an {@link InMemoryFileSystem}, implementing
 * the handle-based open/read/write/seek API the OS and the Lua BIOS use to load {@code /init.lua}.
 */
@SuppressWarnings("unused")
public final class FileSystemDevice {
    private static final class Handle {
        final String path;
        final boolean writing;
        final boolean append;
        @Nullable byte[] readData;
        int position;
        @Nullable ByteArrayOutputStream writeBuffer;

        Handle(final String path, final boolean writing, final boolean append) {
            this.path = path;
            this.writing = writing;
            this.append = append;
        }
    }

    private final InMemoryFileSystem fs;
    private final boolean readonly;
    private String label;
    private final Map<Integer, Handle> handles = new HashMap<>();
    private int nextHandle = 1;

    public FileSystemDevice(final InMemoryFileSystem fs, final String label, final boolean readonly) {
        this.fs = fs;
        this.label = label;
        this.readonly = readonly;
    }

    ///////////////////////////////////////////////////////////////////

    @Callback(synchronize = false, description = "Opens a file and returns a handle.")
    public int open(@Parameter("path") final String path, @Nullable @Parameter("mode") final String mode) {
        final String m = mode == null ? "r" : mode.toLowerCase();
        final boolean writing = m.startsWith("w") || m.startsWith("a");
        if (writing && readonly) {
            throw new IllegalStateException("filesystem is read-only");
        }

        final Handle handle = new Handle(path, writing, m.startsWith("a"));
        if (writing) {
            handle.writeBuffer = new ByteArrayOutputStream();
            if (handle.append) {
                final byte[] existing = fs.read(path);
                if (existing != null) {
                    handle.writeBuffer.writeBytes(existing);
                }
            }
        } else {
            final byte[] data = fs.read(path);
            if (data == null) {
                throw new IllegalArgumentException("file not found");
            }
            handle.readData = data;
        }

        final int id = nextHandle++;
        handles.put(id, handle);
        return id;
    }

    @Callback(synchronize = false, description = "Reads up to the specified amount of data from a handle.")
    @Nullable
    public String read(@Parameter("handle") final int handle, @Parameter("count") final double count) {
        final Handle h = handles.get(handle);
        if (h == null || h.writing || h.readData == null) {
            throw new IllegalArgumentException("bad file descriptor");
        }
        if (h.position >= h.readData.length) {
            return null; // EOF -> nil
        }
        final int available = h.readData.length - h.position;
        final int toRead = (Double.isInfinite(count) || count >= available) ? available : (int) count;
        final String result = new String(h.readData, h.position, toRead, StandardCharsets.UTF_8);
        h.position += toRead;
        return result;
    }

    @Callback(synchronize = false, description = "Writes the given data to a handle.")
    public boolean write(@Parameter("handle") final int handle, @Parameter("value") final String value) {
        final Handle h = handles.get(handle);
        if (h == null || !h.writing || h.writeBuffer == null) {
            throw new IllegalArgumentException("bad file descriptor");
        }
        h.writeBuffer.writeBytes(value.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    @Callback(synchronize = false, description = "Seeks within a handle, returns the new position.")
    public int seek(@Parameter("handle") final int handle, @Parameter("whence") final String whence, @Parameter("offset") final int offset) {
        final Handle h = handles.get(handle);
        if (h == null) {
            throw new IllegalArgumentException("bad file descriptor");
        }
        final int length = h.readData != null ? h.readData.length : (h.writeBuffer != null ? h.writeBuffer.size() : 0);
        final int base = switch (whence) {
            case "set" -> 0;
            case "cur" -> h.position;
            case "end" -> length;
            default -> throw new IllegalArgumentException("invalid mode");
        };
        h.position = Math.max(0, base + offset);
        return h.position;
    }

    @Callback(synchronize = false, description = "Closes an open handle.")
    public void close(@Parameter("handle") final int handle) {
        final Handle h = handles.remove(handle);
        if (h != null && h.writing && h.writeBuffer != null) {
            fs.write(h.path, h.writeBuffer.toByteArray(), false);
        }
    }

    @Callback(synchronize = false, description = "Checks whether a file or directory exists.")
    public boolean exists(@Parameter("path") final String path) {
        return fs.exists(path);
    }

    @Callback(synchronize = false, description = "Gets the size of a file.")
    public long size(@Parameter("path") final String path) {
        return fs.size(path);
    }

    @Callback(synchronize = false, description = "Checks whether a path is a directory.")
    public boolean isDirectory(@Parameter("path") final String path) {
        return fs.isDirectory(path);
    }

    @Callback(synchronize = false, description = "Lists the contents of a directory.")
    @Nullable
    public List<String> list(@Parameter("path") final String path) {
        if (!fs.exists(path)) {
            return null;
        }
        return new ArrayList<>(fs.list(path));
    }

    @Callback(synchronize = false, description = "Creates a directory.")
    public boolean makeDirectory(@Parameter("path") final String path) {
        if (readonly) {
            throw new IllegalStateException("filesystem is read-only");
        }
        return fs.makeDirectory(path);
    }

    @Callback(synchronize = false, description = "Removes a file or directory.")
    public boolean remove(@Parameter("path") final String path) {
        if (readonly) {
            throw new IllegalStateException("filesystem is read-only");
        }
        return fs.remove(path);
    }

    @Callback(synchronize = false, description = "Renames/moves a file or directory.")
    public boolean rename(@Parameter("from") final String from, @Parameter("to") final String to) {
        if (readonly) {
            throw new IllegalStateException("filesystem is read-only");
        }
        return fs.rename(from, to);
    }

    @Callback(synchronize = false, description = "Gets the last modification time of a file.")
    public long lastModified(@Parameter("path") final String path) {
        return fs.lastModified(path);
    }

    @Callback(synchronize = false, description = "Total capacity of the filesystem in bytes.")
    public long spaceTotal() {
        return fs.spaceTotal();
    }

    @Callback(synchronize = false, description = "Used space on the filesystem in bytes.")
    public long spaceUsed() {
        return fs.spaceUsed();
    }

    @Callback(synchronize = false, description = "Whether the filesystem is read-only.")
    public boolean isReadOnly() {
        return readonly;
    }

    @Callback(synchronize = false, description = "Gets the filesystem label.")
    public String getLabel() {
        return label;
    }

    @Callback(synchronize = false, description = "Sets the filesystem label.")
    public String setLabel(@Nullable @Parameter("value") final String value) {
        if (readonly) {
            throw new IllegalStateException("filesystem is read-only");
        }
        label = value != null ? value : "";
        return label;
    }

}
