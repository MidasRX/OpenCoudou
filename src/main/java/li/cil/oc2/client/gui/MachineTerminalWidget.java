/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import li.cil.oc2.common.vm.terminal.modes.MouseMode;
import li.cil.oc2.common.vm.terminal.modes.PrivateMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@OnlyIn(Dist.CLIENT)
public final class MachineTerminalWidget {
    private static final int MARGIN_SIZE = 8;
    private static final int TERMINAL_X = MARGIN_SIZE;
    private static final int TERMINAL_Y = MARGIN_SIZE;

    // Black panel that fills the screen; the terminal is rendered inside it preserving aspect ratio
    // (centered, never stretched) so it's as large as the viewport allows without distortion.
    private int panelWidth = Sprites.TERMINAL_SCREEN.width;
    private int panelHeight = Sprites.TERMINAL_SCREEN.height;
    private int renderWidth = panelWidth - MARGIN_SIZE * 2;
    private int renderHeight = panelHeight - MARGIN_SIZE * 2;
    private int renderX = MARGIN_SIZE;
    private int renderY = MARGIN_SIZE;

    ///////////////////////////////////////////////////////////////////

    private final AbstractMachineTerminalScreen<?> parent;
    private final AbstractMachineTerminalContainer container;
    private final Terminal terminal;
    private int leftPos, topPos;
    private boolean isMouseOverTerminal;
    private Terminal.RendererView rendererView;
    private boolean isOver;

    ///////////////////////////////////////////////////////////////////

    public MachineTerminalWidget(final AbstractMachineTerminalScreen<?> parent) {
        this.parent = parent;
        this.container = this.parent.getMenu();
        this.terminal = this.container.getTerminal();
    }

    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        isMouseOverTerminal = isMouseOverTerminal(mouseX, mouseY);

        // Solid black terminal panel filling the screen (no distracting border).
        graphics.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, 0xFF000000);
    }

    public void render(final GuiGraphics graphics, @Nullable final Component error) {
        if (container.getVirtualMachine().isRunning()) {
            final PoseStack terminalStack = new PoseStack();
            terminalStack.translate(leftPos + renderX, topPos + renderY, 0);
            terminalStack.scale(renderWidth / (float) terminal.getWidth(), renderHeight / (float) terminal.getHeight(), 1f);

            if (rendererView == null) {
                rendererView = terminal.getRenderer();
            }

            //final Matrix4f projectionMatrix = orthographic(0, parent.width, 0, parent.height, -10, 10f);
            final Matrix4f projectionMatrix = (new Matrix4f()).setOrtho(0, parent.width, parent.height, 0, -10f, 10f);
            rendererView.render(terminalStack, projectionMatrix);
        } else {
            final Font font = getClient().font;
            if (error != null) {
                final int textWidth = font.width(error);
                final int textOffsetX = (renderWidth - textWidth) / 2;
                final int textOffsetY = (renderHeight - font.lineHeight) / 2;
                drawShadow(
                    font,
                    graphics,
                    error,
                    leftPos + renderX + textOffsetX,
                    topPos + renderY + textOffsetY
                );
            }
        }
    }

    private void drawShadow(Font font, GuiGraphics graphics, Component text, float x, float y) {
        var batch = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        font.drawInBatch(text, x, y, 15610658, true, graphics.pose().last().pose(), batch, Font.DisplayMode.NORMAL, 0, 15728880);
        batch.endBatch();
    }

    public void tick() {
        final ByteBuffer input = terminal.getInput();
        if (input != null) {
            container.sendTerminalInputToServer(input);
        }
    }

    public boolean mouseScrolled(double dir) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) return false;
        if (dir < 0) {
            terminal.incrementLastLineToDisplay(true);
        } else {
            terminal.decrementLastLineToDisplay();
        }
        return true;
    }

    public void mouseMoved(double x, double y) {
        if (isMouseOverTerminal((int)x, (int)y)) {
            if (!isOver && terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT) {
                isOver = true;
                terminal.putInput("\033[I");
            }
        } else {
            if(isOver && terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT) {
                terminal.putInput("\033[O");
            }
        }
    }

    public boolean mouseClicked(double x, double y, int button) {
        MouseMode currentMouseMode = terminal.currentPrivateModeState.getMouseMode();
        if (currentMouseMode.isMouseDisabled()) return false;
        Vector2i position = getMousePosition(x, y);
        boolean overTerminal = isMouseOverTerminal((int)x, (int)y);
        if (overTerminal && shouldCaptureInput()) {
            switch(currentMouseMode.PrimaryMode) {
                case PrivateMode.X11MM, PrivateMode.CELL_MOTION_MOUSE -> {
                    if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.SGR_MOUSE)) {
                        terminal.putInput("\033[<" + button + ";" + position.x + ";" + position.y + "M");
                        return true;
                    }
                    else if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.UTF8_MOUSE))
                    {
                        byte[] csiMBytes = "\033[M".getBytes(StandardCharsets.UTF_8);
                        byte[] buttonBytes = utf8(button + 32);
                        byte[] colBytes = utf8(position.x + 32);
                        byte[] rowBytes = utf8(position.y + 32);
                        byte[] finalBytes = new byte[csiMBytes.length + buttonBytes.length + colBytes.length + rowBytes.length];

                        System.arraycopy(csiMBytes, 0, finalBytes, 0, csiMBytes.length);
                        System.arraycopy(buttonBytes, 0, finalBytes, csiMBytes.length, buttonBytes.length);
                        System.arraycopy(colBytes, 0, finalBytes, csiMBytes.length + buttonBytes.length, colBytes.length);
                        System.arraycopy(rowBytes, 0, finalBytes, csiMBytes.length + buttonBytes.length + colBytes.length, rowBytes.length);

                        terminal.putInput(ByteBuffer.wrap(finalBytes));
                        return true;
                    }
                    else if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.URXVT_MOUSE))
                    {
                        terminal.putInput("\033[" + (button + 32) + ";" + position.x + ";" + position.y + "M");
                    }
                    else
                    {
                        terminal.putInput('\033');
                        terminal.putInput('[');
                        terminal.putInput('M');
                        terminal.putInput((byte) (button + 32));
                        terminal.putInput((byte) (position.x + 32));
                        terminal.putInput((byte) (position.y + 32));
                        return true;
                    }
                }
                default -> System.out.println("ERR: Unsupported primary mode");
            }
        }
        return false;
    }

    public boolean mouseReleased(double x, double y, int button) {
        MouseMode currentMouseMode = terminal.currentPrivateModeState.getMouseMode();
        if (currentMouseMode.isMouseDisabled()) return false;
        Vector2i position = getMousePosition(x, y);
        boolean overTerminal = isMouseOverTerminal((int)x, (int)y);
        if (overTerminal && shouldCaptureInput()) {
            switch(currentMouseMode.PrimaryMode) {
                case PrivateMode.X11MM, PrivateMode.CELL_MOTION_MOUSE -> {
                    if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.SGR_MOUSE)) {
                        terminal.putInput("\033[<" + button + ";" + position.x + ";" + position.y + "m");
                        return true;
                    }
                    else if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.UTF8_MOUSE))
                    {
                        byte[] csiMBytes = "\033[M".getBytes(StandardCharsets.UTF_8);
                        byte[] buttonBytes = utf8(35);
                        byte[] colBytes = utf8(position.x + 32);
                        byte[] rowBytes = utf8(position.y + 32);
                        byte[] finalBytes = new byte[csiMBytes.length + buttonBytes.length + colBytes.length + rowBytes.length];

                        System.arraycopy(csiMBytes, 0, finalBytes, 0, csiMBytes.length);
                        System.arraycopy(buttonBytes, 0, finalBytes, csiMBytes.length, buttonBytes.length);
                        System.arraycopy(colBytes, 0, finalBytes, csiMBytes.length + buttonBytes.length, colBytes.length);
                        System.arraycopy(rowBytes, 0, finalBytes, csiMBytes.length + buttonBytes.length + colBytes.length, rowBytes.length);

                        terminal.putInput(ByteBuffer.wrap(finalBytes));
                        return true;
                    }
                    else if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.URXVT_MOUSE))
                    {
                        terminal.putInput("\033[" + 35 + ";" + position.x + ";" + position.y + "M");
                    }
                    else
                    {
                        terminal.putInput('\033');
                        terminal.putInput('[');
                        terminal.putInput('M');
                        terminal.putInput((byte) 35);
                        terminal.putInput((byte) (position.x + 32));
                        terminal.putInput((byte) (position.y + 32));
                        return true;
                    }
                }
                default -> System.out.println("ERR: Unsupported primary mode");
            }
        }
        return false;
    }

    private byte[] utf8(int value) {
        return new String(new int[]{value}, 0, 1).getBytes(StandardCharsets.UTF_8);
    }

    private Vector2i getMousePosition(double x, double y) {
        final double tx = renderWidth / (double) Terminal.WIDTH;
        final double ty = renderHeight / (double) Terminal.HEIGHT;
        int sx = (int)(((x - leftPos) - renderX) / tx) + 1;
        int sy = (int)(((y - topPos) - renderY) / ty) + 1;

        return new Vector2i(sx, sy);
    }

    public boolean charTyped(final char ch, final int modifier) {
        if (modifier == 0 || modifier == GLFW.GLFW_MOD_SHIFT) {
            terminal.putInput((byte) ch);
        }
        return true;
    }

    @SuppressWarnings("unused")
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (!shouldCaptureInput() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && terminal.currentPrivateModeState.APPLICATION_ESC_MODE) {
            terminal.putInput("\033[0[");
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V) {
            final String value = getClient().keyboardHandler.getClipboard();
            boolean bracketed = terminal.currentPrivateModeState.SET_BRACKETED_PASTE;
            if (bracketed) terminal.putInput("\033[200~");
            for (final char ch : value.toCharArray()) {
                terminal.putInput((byte) ch);
            }
            if (bracketed) terminal.putInput("\033[201~");
        } else {
            byte[] sequence;
            if (terminal.currentPrivateModeState.DECCKM && (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT))
                sequence = TerminalInput.getDECCKMSequence(keyCode, modifiers);
            else sequence = TerminalInput.getSequence(keyCode, modifiers);
            if (sequence != null) {
                for (final byte b : sequence) {
                    terminal.putInput(b);
                }
            }
        }

        return true;
    }

    public void init() {
        this.panelWidth = parent.getPanelWidth();
        this.panelHeight = parent.getPanelHeight();
        this.leftPos = parent.getPanelLeft();
        this.topPos = parent.getPanelTop();

        final int availWidth = panelWidth - MARGIN_SIZE * 2;
        final int availHeight = panelHeight - MARGIN_SIZE * 2;
        final float nativeWidth = terminal.getWidth();   // WIDTH * CHAR_WIDTH
        final float nativeHeight = terminal.getHeight();  // HEIGHT * CHAR_HEIGHT
        final float scale = Math.min(availWidth / nativeWidth, availHeight / nativeHeight);
        this.renderWidth = Math.round(nativeWidth * scale);
        this.renderHeight = Math.round(nativeHeight * scale);
        this.renderX = (panelWidth - renderWidth) / 2;
        this.renderY = (panelHeight - renderHeight) / 2;
    }

    public void onClose() {
        if (rendererView != null) {
            terminal.releaseRenderer(rendererView);
            rendererView = null;
        }
    }

    ///////////////////////////////////////////////////////////////////

    private Minecraft getClient() {
        return parent.getMinecraft();
    }

    private boolean shouldCaptureInput() {
        return isMouseOverTerminal && container.getCaptureInputState() &&
            container.getVirtualMachine().isRunning();
    }

    private boolean isMouseOverTerminal(final int mouseX, final int mouseY) {
        return parent.isMouseOver(mouseX, mouseY,
            renderX, renderY, renderWidth, renderHeight);
    }
}
