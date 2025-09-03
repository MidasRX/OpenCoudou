/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization;

import li.cil.oc2.api.API;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.util.AsyncUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraftforge.fml.common.Mod;

/**
 * This class facilitates storing binary chunks of data in an efficient, parallelized fashion.
 */
@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlobStorage {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final LevelResource BLOBS_FOLDER_NAME = new LevelResource(API.MOD_ID + "-blobs");
    private static final Map<UUID, MappedByteBuffer> BLOBS = new ConcurrentHashMap<>();
    private static final Map<UUID, CompletableFuture<MappedByteBuffer>> PENDING_OPERATIONS = new ConcurrentHashMap<>();
    private static final int MAX_MEMORY_MAPPED_SIZE = 64 * 1024 * 1024; // 64MB max size per blob
    private static volatile Path dataDirectory; // Directory blobs get saved to.

    ///////////////////////////////////////////////////////////////////

    static {
        // Register shutdown hook to ensure resources are cleaned up
        Runtime.getRuntime().addShutdownHook(new Thread(BlobStorage::close, "OC2 BlobStorage Shutdown"));
    }

    /**
     * Gets the file system path for a blob with the given handle.
     *
     * @param handle the handle of the blob.
     * @return the path to the blob file.
     */
    private static Path getBlobPath(final UUID handle) {
        if (dataDirectory == null) {
            throw new IllegalStateException("Data directory not initialized. Server not set?");
        }
        return dataDirectory.resolve(handle.toString());
    }

    /**
     * Sets the currently running server.
     * <p>
     * This is used to configure the directory blobs get stored in.
     * <p>
     * We strongly assume that there will never be more than one active Minecraft server per process.
     *
     * @param server the currently active server.
     */
    public static void setServer(final MinecraftServer server) {
        final Path newDataDir = server.getWorldPath(BLOBS_FOLDER_NAME);
        if (!newDataDir.equals(dataDirectory)) {
            // Close all open handles if the directory changes
            close();
            dataDirectory = newDataDir;

            try {
                // Create directories synchronously since this is called during server startup
                // when the config system might not be fully initialized yet
                Files.createDirectories(dataDirectory);
                LOGGER.info("Blob storage directory initialized at: {}", dataDirectory);
            } catch (final IOException e) {
                LOGGER.error("Failed to create blob storage directory", e);
            }
        }
    }

    /**
     * Closes all currently open blobs.
     */
    public static void close() {
        // Cancel all pending operations with minimal blocking
        for (final Map.Entry<UUID, CompletableFuture<MappedByteBuffer>> entry : PENDING_OPERATIONS.entrySet()) {
            try {
                entry.getValue().cancel(true);
            } catch (final Exception e) {
                LOGGER.trace("Error cancelling pending operation", e);
            }
        }
        PENDING_OPERATIONS.clear();

        // Clean up all mapped buffers
        Map<UUID, MappedByteBuffer> buffersToClean = new HashMap<>(BLOBS);
        BLOBS.clear();

        for (final MappedByteBuffer buffer : buffersToClean.values()) {
            if (buffer != null) {
                try {
                    // Clean up the mapped buffer
                    ((java.nio.Buffer) buffer).clear();
                } catch (final Exception e) {
                    LOGGER.trace("Error cleaning up mapped buffer", e);
                }
            }
        }

        // Force cleanup of mapped buffers
        System.gc();
        System.runFinalization();

        // Clear any interrupted status that might have been set by cancellation
        Thread.interrupted();

        // Safely check debug config if available
        boolean debug = false;
        try {
            debug = AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get();
        } catch (IllegalStateException ignored) {
            // Config system might be shutting down, continue with debug disabled
        }

        if (debug) {
            LOGGER.info("Closed all blob storage resources");
        }

        // Force a GC to help clean up any lingering file handles
        System.gc();
    }

    /**
     * Allocates a new handle for a blob to store.
     * <p>
     * Use this in a call to {@link #getOrOpen(UUID)} to open the blob storage.
     *
     * @return a new handle.
     */
    public static UUID allocateHandle() {
        return UUID.randomUUID();
    }

    /**
     * Validates a blob handle, returning a new one if it is {@code null} or invalid.
     *
     * @param handle the handle to validate.
     * @return the {@code handle} if valid; a new blob handle otherwise.
     */
    public static UUID validateHandle(@Nullable final UUID handle) {
        if (handle == null || (handle.getMostSignificantBits() == 0 && handle.getLeastSignificantBits() == 0)) {
            return allocateHandle();
        } else {
            return handle;
        }
    }

    /**
     * Get or opens a file channel for the blob with the specified handle asynchronously.
     *
     * @param handle the handle to obtain the file channel for.
     * @return a CompletableFuture that will complete with the file channel.
     */
    public static CompletableFuture<MappedByteBuffer> getOrOpenAsync(final UUID handle) {
        // Check if already open
        MappedByteBuffer buffer = BLOBS.get(handle);
        if (buffer != null && buffer.capacity() > 0) {
            return CompletableFuture.completedFuture(buffer);
        } else if (buffer != null) {
            // Remove stale reference
            BLOBS.remove(handle);
        }

        // Check if there's already a pending operation
        return PENDING_OPERATIONS.computeIfAbsent(handle, h -> {
            return AsyncUtils.runAsync(() -> {
                if (dataDirectory == null) {
                    throw new CompletionException("Data directory not initialized. Server not ready?", null);
                }

                final Path path = dataDirectory.resolve(h.toString());
                try {
                    // Ensure parent directory exists
                    Files.createDirectories(path.getParent());

                    // Use memory-mapped files instead of direct file channel access
                    try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw");
                         FileChannel channel = file.getChannel()) {

                        // Map the file into memory
                        MappedByteBuffer mappedBuffer = channel.map(
                            FileChannel.MapMode.READ_WRITE,
                            0,
                            Math.min(channel.size(), MAX_MEMORY_MAPPED_SIZE)
                        );

                        // Store the mapped buffer
                        BLOBS.put(h, mappedBuffer);
                        return mappedBuffer;
                    } catch (final IOException e) {
                        LOGGER.error("Failed to open or map blob: " + h, e);
                        throw new CompletionException("Failed to open or map blob: " + h, e);
                    } finally {
                        PENDING_OPERATIONS.remove(h);
                    }
                } catch (final IOException e) {
                    LOGGER.error("Failed to open or map blob: " + h, e);
                    throw new CompletionException("Failed to open or map blob: " + h, e);
                }
            }, "Open blob " + h);
        });
    }

    /**
     * Get or opens a file channel for the blob with the specified handle.
     * <p>
     * The returned file channel supports random access.
     *
     * @param handle the handle to obtain the file channel for.
     * @return the file channel for the requested blob.
     * @throws IOException if opening the blob fails.
     * @deprecated Use {@link #getOrOpenAsync(UUID)} for non-blocking access. This method will be removed in 1.21.1.
     */
    @Deprecated(since = "1.21.1", forRemoval = true)
    public static synchronized MappedByteBuffer getOrOpen(final UUID handle) throws IOException {
        try {
            return getOrOpenAsync(handle).join();
        } catch (final CompletionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException("Failed to open blob: " + handle, e);
        }
    }

    /**
     * Closes the blob with the specified handle asynchronously.
     *
     * @param handle the handle of the blob to close.
     * @return a CompletableFuture that completes when the blob is closed.
     */
    public static CompletableFuture<Void> closeAsync(final UUID handle) {
        return CompletableFuture.runAsync(() -> {
            final MappedByteBuffer buffer = BLOBS.remove(handle);
            if (buffer != null) {
                try {
                    ((java.nio.Buffer) buffer).clear();
                } catch (final Exception e) {
                    LOGGER.trace("Error cleaning up mapped buffer for blob: " + handle, e);
                }
            }
        }, AsyncUtils.getAsyncExecutor());
    }

    /**
     * Closes the blob with the specified handle.
     *
     * @param handle the handle of the blob to close.
     * @deprecated Use {@link #closeAsync(UUID)} for non-blocking operation. This method will be removed in 1.21.1.
     */
    @Deprecated(since = "1.21.1", forRemoval = true)
    public static void close(final UUID handle) {
        final MappedByteBuffer buffer = BLOBS.remove(handle);
        if (buffer != null) {
            try {
                ((java.nio.Buffer) buffer).clear();
            } catch (final Exception e) {
                LOGGER.trace("Error cleaning up mapped buffer for blob: " + handle, e);
            }
        }
    }

    /**
     * Deletes the blob with the specified handle asynchronously.
     *
     * @param handle the handle of the blob to delete.
     * @return a CompletableFuture that completes when the blob is deleted.
     */
    public static CompletableFuture<Void> deleteAsync(final UUID handle) {
        return closeAsync(handle).thenRunAsync(() -> {
            final Path path = getBlobPath(handle);
            try {
                Files.deleteIfExists(path);
            } catch (final IOException e) {
                LOGGER.error("Failed to delete blob: " + handle, e);
            }
        }, AsyncUtils.getAsyncExecutor());
    }

    /**
     * Deletes the blob with the specified handle.
     *
     * @param handle the handle of the blob to delete.
     * @deprecated Use {@link #deleteAsync(UUID)} for non-blocking operation. This method will be removed in 1.21.1.
     */
    @Deprecated(since = "1.21.1", forRemoval = true)
    public static void delete(final UUID handle) {
        try {
            deleteAsync(handle).join();
        } catch (final CompletionException e) {
            LOGGER.error("Error in delete operation for blob: " + handle, e);
        }
    }

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void handleServerAboutToStart(final ServerAboutToStartEvent event) {
        BlobStorage.setServer(event.getServer());
    }

    @SubscribeEvent
    public static void handleServerStopped(final ServerStoppedEvent event) {
        try {
            // Close all open resources when server stops
            close();
        } catch (Exception e) {
            // Don't let exceptions during shutdown prevent server from stopping
            LOGGER.error("Error during blob storage shutdown:", e);
        } finally {
            // Always clear the data directory reference
            dataDirectory = null;

            // Force a GC to help clean up any lingering file handles
            System.gc();

            // Give GC a chance to run
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
