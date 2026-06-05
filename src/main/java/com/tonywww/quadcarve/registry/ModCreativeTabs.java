package com.tonywww.quadcarve.registry;

import com.tonywww.quadcarve.QuadCarveMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, QuadCarveMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> QUADCARVE_TAB =
            TABS.register("quadcarve_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.quadcarve"))
                    .icon(() -> ModItems.CHISEL.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.CHISEL.get());
                        output.accept(ModItems.CARVED_ITEM.get());
                    })
                    .build());
}
