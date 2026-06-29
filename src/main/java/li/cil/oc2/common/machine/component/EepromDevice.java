/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.component;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * OpenComputers-style {@code eeprom} component. Holds the Lua BIOS code plus a small data section
 * (conventionally the boot filesystem address). The machine kernel reads {@code get()} to obtain the
 * BIOS to run; {@code bios.lua} uses {@code getData/setData} to remember the boot device.
 */
@SuppressWarnings("unused")
public final class EepromDevice {
    private static final int CODE_SIZE = 4096;
    private static final int DATA_SIZE = 256;

    private String code;
    private String data;
    private String label;
    private boolean readonly;

    public EepromDevice(final String code, final String label) {
        this.code = code;
        this.data = "";
        this.label = label;
    }

    @Callback(synchronize = false, description = "Get the currently stored byte array (BIOS code).")
    public String get() {
        return code;
    }

    @Callback(synchronize = false, description = "Overwrite the currently stored byte array.")
    public void set(@Nullable @Parameter("data") final String data) {
        if (readonly) {
            throw new IllegalStateException("storage is readonly");
        }
        final String value = data != null ? data : "";
        if (value.getBytes(StandardCharsets.UTF_8).length > CODE_SIZE) {
            throw new IllegalArgumentException("not enough space");
        }
        this.code = value;
    }

    @Callback(synchronize = false, description = "Get the label of the EEPROM.")
    public String getLabel() {
        return label;
    }

    @Callback(synchronize = false, description = "Set the label of the EEPROM.")
    public String setLabel(@Nullable @Parameter("data") final String label) {
        if (readonly) {
            throw new IllegalStateException("storage is readonly");
        }
        this.label = label != null ? label : "";
        return this.label;
    }

    @Callback(synchronize = false, description = "Get the storage capacity of this EEPROM.")
    public int getSize() {
        return CODE_SIZE;
    }

    @Callback(synchronize = false, description = "Get the size of the volatile data storage area.")
    public int getDataSize() {
        return DATA_SIZE;
    }

    @Callback(synchronize = false, description = "Get the currently stored byte array (volatile data).")
    public String getData() {
        return data;
    }

    @Callback(synchronize = false, description = "Overwrite the currently stored volatile data byte array.")
    public void setData(@Nullable @Parameter("data") final String data) {
        final String value = data != null ? data : "";
        if (value.getBytes(StandardCharsets.UTF_8).length > DATA_SIZE) {
            throw new IllegalArgumentException("not enough space");
        }
        this.data = value;
    }

    @Callback(synchronize = false, description = "Make this EEPROM readonly if it isn't already.")
    public void makeReadonly(@Parameter("checksum") final String checksum) {
        readonly = true;
    }

    @Callback(synchronize = false, description = "Get the checksum of the data on this EEPROM.")
    public String getChecksum() {
        return Integer.toHexString(code.hashCode());
    }
}
