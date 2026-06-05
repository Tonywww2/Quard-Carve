package com.tonywww.quadcarve.registry;

import com.tonywww.quadcarve.QuadCarveMod;
import com.tonywww.quadcarve.item.CarvedItem;
import com.tonywww.quadcarve.item.ChiselItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, QuadCarveMod.MOD_ID);

    public static final RegistryObject<ChiselItem> CHISEL =
            ITEMS.register("chisel", () -> new ChiselItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CarvedItem> CARVED_ITEM =
            ITEMS.register("carved_item", () -> new CarvedItem(new Item.Properties().stacksTo(64)));
}
