package com.tonywww.quadcarve;

import com.mojang.logging.LogUtils;
import com.tonywww.quadcarve.network.ModNetwork;
import com.tonywww.quadcarve.registry.ModCreativeTabs;
import com.tonywww.quadcarve.registry.ModItems;
import com.tonywww.quadcarve.registry.ModMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(QuadCarveMod.MOD_ID)
public class QuadCarveMod {

    public static final String MOD_ID = "quadcarve";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QuadCarveMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        ModItems.ITEMS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            LOGGER.info("[QuadCarve] Network channels registered.");
        });
    }
}
