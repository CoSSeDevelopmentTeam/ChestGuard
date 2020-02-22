package net.comorevi.cosse.chestguard;

import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.PlayerInteractEvent;

import static cn.nukkit.block.BlockID.CHEST;

public class EventListener implements Listener {

    private ChestGuard plugin;

    public EventListener(ChestGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getId() != CHEST) return;

        plugin.addGuard(event.getPlayer().getName(), block.getLocation());
        event.getPlayer().sendMessage(plugin.translateString("place-chest"));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getId() != CHEST) return;
        if (!plugin.existsChestData(block.getLocation()) && !event.getPlayer().isOp()) {
            event.setCancelled();
            event.getPlayer().sendMessage(plugin.translateString("error-non-registered-chest"));
            return;
        }

        if (plugin.isOwnChest(event.getPlayer().getName(), block.getLocation())) {
            plugin.removeGuard(event.getBlock().getLocation());
            event.getPlayer().sendMessage(plugin.translateString("break-chest"));
        } else if (event.getPlayer().isOp()) {
            plugin.removeGuard(event.getBlock().getLocation());
            event.getPlayer().sendMessage(plugin.translateString("player-chest-break-byOp"));
        } else {
            event.setCancelled();
            event.getPlayer().sendMessage(plugin.translateString("error-not-own-chest"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getBlock();
        if (block.getId() != CHEST) return;
        if (!plugin.existsChestData(block.getLocation())) {
            event.setCancelled();
            event.getPlayer().sendMessage(plugin.translateString("error-non-registered-chest"));
            return;
        }

        if (plugin.isOwnChest(event.getPlayer().getName(), block.getLocation())) {
            /*
            if (event.getPlayer().getInventory().getItemInHand().getId() == ItemID.GHAST_TEAR) {
                //チェスト管理GUI表示
            }*/
        } else if (event.getPlayer().isOp()) {
            event.getPlayer().sendMessage(plugin.translateString("player-chest-interact-byOp"));
        } else {
            event.setCancelled();
            event.getPlayer().sendMessage(plugin.translateString("error-not-own-chest"));
        }
    }
}
