package com.tonywww.quadcarve.client;

import com.tonywww.quadcarve.menu.ChiselMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Chisel UI.
 *
 * Actual rendering is delegated to ApricityUI's HTML engine (assets/quadcarve/ui/chisel.html).
 * This class exists so that Minecraft's MenuScreens registry is satisfied and does not emit
 * "Failed to create screen for menu type: quadcarve:chisel_menu" warnings.
 *
 * Registration: {@link ClientSetup#onClientSetup}.
 */
public class ChiselScreen extends AbstractContainerScreen<ChiselMenu> {

    public ChiselScreen(ChiselMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Background rendering is handled by ApricityUI's HTML renderer.
        // If ApricityUI is not present, draw a plain dark background as fallback.
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
