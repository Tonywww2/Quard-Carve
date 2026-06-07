package com.tonywww.quadcarve.client;


import com.sighs.apricityui.init.Window;
import com.tonywww.quadcarve.network.CarveActionPacket;
import com.tonywww.quadcarve.network.ModNetwork;
import com.tonywww.quadcarve.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;

import static com.tonywww.quadcarve.QuadCarveMod.MOD_ID;

/**
 * Client-only setup.
 * Registers the ChiselScreen factory and the JS→Java carve-action bridge.
 *
 * JS dispatches carve actions as:
 * <pre>
 *   window.dispatchEvent(Object.assign({ type: 'quadcarve:action' }, { path, action }));
 * </pre>
 * The event object's public fields ({@code path}, {@code action}) are read directly.
 */
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.CHISEL_MENU.get(), ChiselScreen::new);

            Window.window.addEventListener("quadcarve:action", e -> {
                try {
                    // ApricityUI bridge: JS plain objects arrive as Map<String,Object>
                    // or as a Java object with public fields matching the JS properties.
                    String path   = readProp(e, "path");
                    Number action = readProp(e, "action");
                    if (path == null || action == null) return;

                    CarveActionPacket packet;
                    int act = action.intValue();
                    if (act == CarveActionPacket.SPLIT)       packet = CarveActionPacket.split(path);
                    else if (act == CarveActionPacket.REMOVE) packet = CarveActionPacket.remove(path);
                    else return;

                    ModNetwork.CHANNEL.sendToServer(packet);
                } catch (Exception ignored) { }
            });
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T readProp(Object obj, String key) {
        if (obj instanceof Map<?,?> map) return (T) map.get(key);
        try { return (T) obj.getClass().getField(key).get(obj); }
        catch (Exception e) { return null; }
    }
}
