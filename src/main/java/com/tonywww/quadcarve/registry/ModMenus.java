package com.tonywww.quadcarve.registry;

import com.tonywww.quadcarve.QuadCarveMod;
import com.tonywww.quadcarve.menu.ChiselMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, QuadCarveMod.MOD_ID);

    public static final RegistryObject<MenuType<ChiselMenu>> CHISEL_MENU =
            MENUS.register("chisel_menu",
                    () -> IForgeMenuType.create((id, inv, buf) -> new ChiselMenu(id, inv)));
}
