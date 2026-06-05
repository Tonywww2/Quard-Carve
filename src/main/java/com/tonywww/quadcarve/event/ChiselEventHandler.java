package com.tonywww.quadcarve.event;

import com.tonywww.quadcarve.QuadCarveMod;
import com.tonywww.quadcarve.menu.ChiselMenu;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Listens to container lifecycle events on the game (FORGE) event bus.
 */
@Mod.EventBusSubscriber(modid = QuadCarveMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChiselEventHandler {

    /**
     * When a ChiselMenu is closed server-side, log it.
     * Actual NBT persistence is handled inside ChiselMenu.removed().
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getContainer() instanceof ChiselMenu) {
            // NBT write-back already handled in ChiselMenu.removed().
            // Add any additional server-side teardown logic here (e.g., drop items on death).
        }
    }
}
