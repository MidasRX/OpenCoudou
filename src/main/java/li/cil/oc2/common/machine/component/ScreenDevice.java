/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.component;

import li.cil.oc2.api.bus.device.object.Callback;

import java.util.List;

/**
 * OpenComputers-style {@code screen} component. The actual pixels live in the GPU's shadow buffer /
 * the in-world {@link li.cil.oc2.common.vm.terminal.Terminal}; this mostly advertises the associated
 * keyboard(s) and basic on/off state so the OS's term library initialises.
 */
@SuppressWarnings("unused")
public final class ScreenDevice {
    private final List<String> keyboards;
    private boolean on = true;
    private boolean precise;
    private boolean touchInverted;

    public ScreenDevice(final List<String> keyboards) {
        this.keyboards = keyboards;
    }

    @Callback(synchronize = false, description = "Returns whether the screen is currently on.")
    public boolean isOn() {
        return on;
    }

    @Callback(synchronize = false, description = "Turns the screen on. Returns true if it was off.")
    public boolean turnOn() {
        final boolean wasOff = !on;
        on = true;
        return wasOff;
    }

    @Callback(synchronize = false, description = "Turns the screen off. Returns true if it was on.")
    public boolean turnOff() {
        final boolean wasOn = on;
        on = false;
        return wasOn;
    }

    @Callback(synchronize = false, description = "The aspect ratio of the screen.")
    public Object[] getAspectRatio() {
        return new Object[]{1, 1};
    }

    @Callback(synchronize = false, description = "The list of keyboards attached to the screen.")
    public List<String> getKeyboards() {
        return keyboards;
    }

    @Callback(synchronize = false, description = "Set whether to use high-precision mode (sub-pixel).")
    public boolean setPrecise(@li.cil.oc2.api.bus.device.object.Parameter("enabled") final boolean enabled) {
        final boolean old = precise;
        precise = enabled;
        return old;
    }

    @Callback(synchronize = false, description = "Check whether high-precision mode is enabled.")
    public boolean isPrecise() {
        return precise;
    }

    @Callback(synchronize = false, description = "Sets whether touch mode is inverted.")
    public boolean setTouchModeInverted(@li.cil.oc2.api.bus.device.object.Parameter("value") final boolean value) {
        final boolean old = touchInverted;
        touchInverted = value;
        return old;
    }

    @Callback(synchronize = false, description = "Checks whether touch mode is inverted.")
    public boolean isTouchModeInverted() {
        return touchInverted;
    }
}
