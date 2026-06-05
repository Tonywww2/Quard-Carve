package com.tonywww.quadcarve.client;

import com.tonywww.quadcarve.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.tonywww.quadcarve.QuadCarveMod.MOD_ID;

/**
 * Client-only setup. Runs on {@link Dist#CLIENT} only.
 * Registers screen factories so Minecraft's MenuScreens registry is satisfied.
 */
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
            MenuScreens.register(ModMenus.CHISEL_MENU.get(), ChiselScreen::new)
        );
    }
}
