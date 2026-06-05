package com.tonywww.quadcarve.client;

import com.sighs.apricityui.event.Test;
import com.tonywww.quadcarve.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import static com.tonywww.quadcarve.QuadCarveMod.MOD_ID;

/**
 * Client-only setup.
 */
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.CHISEL_MENU.get(), ChiselScreen::new);

            // ── Workaround: ApricityUI 1.1.4 dev-mode bug ────────────────────
            // Test.ensureTooltip() calls ArrayList.get(0) on an empty list every
            // client tick, crashing with IndexOutOfBoundsException in dev builds.
            // The Test class is only registered when !FMLEnvironment.production,
            // so unregistering it here is safe and has no effect on release builds.
            if (!FMLEnvironment.production) {
                MinecraftForge.EVENT_BUS.unregister(Test.class);
            }
        });
    }
}
