/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.component;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The OpenComputers-style {@code computer} component. Its address equals {@code computer.address()},
 * so calls like {@code computer.beep} (which the Lua BIOS issues on successful boot) and
 * {@code computer.getDeviceInfo} resolve to it.
 */
@SuppressWarnings("unused")
public final class ComputerDevice {
    @Callback(synchronize = false, description = "Plays a tone (currently a no-op placeholder).")
    public void beep(@Nullable @Parameter("frequency") final Double frequency, @Nullable @Parameter("duration") final Double duration) {
        // TODO: route to the in-world sound system once a Lua sound path exists.
    }

    @Callback(synchronize = false, description = "Returns a table describing the installed hardware.")
    public Map<String, Object> getDeviceInfo() {
        final Map<String, Object> info = new LinkedHashMap<>();
        return info;
    }

    @Callback(synchronize = false, description = "Returns the list of known program install locations.")
    public List<Object> getProgramLocations() {
        return List.of();
    }

    @Callback(synchronize = false, description = "Whether this computer is a robot.")
    public boolean isRobot() {
        return false;
    }
}
