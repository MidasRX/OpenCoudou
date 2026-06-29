/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.common.container.RobotTerminalContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RobotTerminalScreen extends AbstractMachineTerminalScreen<RobotTerminalContainer> {
    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("all") private EditBox focusIndicatorEditBox;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalScreen(final RobotTerminalContainer container, final Inventory inventory, final Component title) {
        super(container, inventory, title);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        final int slotsX = (getPanelWidth() - Sprites.HOTBAR.width) / 2;
        final int slotsY = getPanelHeight() - 1;
        Sprites.HOTBAR.draw(graphics, leftPos + slotsX, topPos + slotsY);
        RobotContainerScreen.renderSelection(graphics, menu.getRobot().getSelectedSlot(), leftPos + slotsX + 4, topPos + slotsY + 4, 12);

        super.renderBg(graphics, partialTicks, mouseX, mouseY);
    }

    @Override
    protected void setFocusIndicatorEditBox(final EditBox editBox) {
        focusIndicatorEditBox = editBox;
    }
}
