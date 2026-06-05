package com.tonywww.quadcarve;

import com.mojang.logging.LogUtils;
import com.tonywww.quadcarve.api.finalizer.FinalizerRegistry;
import com.tonywww.quadcarve.network.ModNetwork;
import com.tonywww.quadcarve.registry.ModCreativeTabs;
import com.tonywww.quadcarve.registry.ModItems;
import com.tonywww.quadcarve.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(QuadCarveMod.MOD_ID)
public class QuadCarveMod {

    public static final String MOD_ID = "quadcarve";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Creates a {@link ResourceLocation} in the {@code quadcarve} namespace.
     * Use instead of {@code new ResourceLocation(MOD_ID, path)} which is deprecated in 1.20+.
     */
    @SuppressWarnings("removal")
    public static ResourceLocation prefix(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

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

            // Default finaliser: slimeball.
            // Other mods can add their own via QuadCarveAPI.registerFinalizer().
            FinalizerRegistry.registerItem(Items.SLIME_BALL);

            LOGGER.info("[QuadCarve] Setup complete. Finalisers registered: {}",
                    FinalizerRegistry.registeredItems().size());
        });
    }
}
