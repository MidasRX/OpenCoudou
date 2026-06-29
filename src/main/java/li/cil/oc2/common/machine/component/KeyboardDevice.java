/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.component;

/**
 * OpenComputers-style {@code keyboard} component. Keyboards have no callable methods; they exist so
 * the screen can advertise them and the OS associates key signals with them. Key input is delivered
 * to the guest as {@code key_down}/{@code key_up} signals carrying this component's address.
 */
public final class KeyboardDevice {
}
