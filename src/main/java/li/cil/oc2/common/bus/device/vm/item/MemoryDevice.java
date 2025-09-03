/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.device.memory.ByteBufferMemory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.io.RandomAccessFile;

public final class MemoryDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String BLOB_HANDLE_TAG_NAME = "blob";
    private static final String ADDRESS_TAG_NAME = "address";

    ///////////////////////////////////////////////////////////////

    private final int size;
    private PhysicalMemory device;

    ///////////////////////////////////////////////////////////////

    private final OptionalAddress address = new OptionalAddress();
    private UUID blobHandle;

    ///////////////////////////////////////////////////////////////

    public MemoryDevice(final ItemStack identity, final int capacity) {
        super(identity);
        size = capacity;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (!allocateDevice(context)) {
            return VMDeviceLoadResult.fail();
        }

        if (!address.claim(context, device)) {
            return VMDeviceLoadResult.fail();
        }

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        closeDevice();

        if (blobHandle != null) {
            BlobStorage.close(blobHandle);
        }
    }

    @Override
    public void dispose() {
        // Memory is volatile, so free up our persisted blob when device is disposed.
        if (blobHandle != null) {
            BlobStorage.delete(blobHandle);
            blobHandle = null;
        }

        address.clear();
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();

        if (blobHandle != null) {
            tag.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }
        if (address.isPresent()) {
            tag.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        if (tag.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = tag.getUUID(BLOB_HANDLE_TAG_NAME);
        }
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
    }

    ///////////////////////////////////////////////////////////////

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(Constants.PAGE_SIZE)) {
            return false;
        }

        try {
            // Ensure we have a valid blob handle
            blobHandle = BlobStorage.validateHandle(blobHandle);

            try {
                // Try to get or open the blob
                final MappedByteBuffer buffer = BlobStorage.getOrOpen(blobHandle);
                if (buffer != null) {
                    buffer.limit(size);
                    device = new ByteBufferMemory(size, buffer);
                    return true;
                }
            } catch (final Exception e) {
                // If we get here, the async operation failed, likely due to executor shutdown
                LOGGER.warn("Async blob operation failed, falling back to direct file access: {}", e.getMessage());

                // Fallback to using a direct byte buffer since we can't access the blob file directly
                LOGGER.warn("Using fallback memory allocation for blob: {}", blobHandle);
                final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(size);
                device = new ByteBufferMemory(size, buffer);
                return true;
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to allocate memory device", e);
        }

        // If we get here, something went wrong
        closeDevice();
        return false;
    }

    private void closeDevice() {
        if (device == null) {
            return;
        }

        try {
            // Close the device first
            device.close();

            // Release the memory mapping
            if (blobHandle != null) {
                try {
                    BlobStorage.close(blobHandle);
                } catch (Exception e) {
                    LOGGER.warn("Failed to close blob storage", e);
                }
                blobHandle = null;
            }
        } catch (final Exception e) {
            LOGGER.error("Error closing memory device", e);
        } finally {
            device = null;

            // Suggest garbage collection to help with memory cleanup
            System.gc();
        }
    }
}
