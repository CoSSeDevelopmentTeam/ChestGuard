package net.comorevi.cosse.chestguard;

import cn.nukkit.Player;
import cn.nukkit.block.BlockChest;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.math.BlockFace;
import net.comorevi.cosse.chestguard.api.ChestGuardAPI;
import net.comorevi.cosse.chestguard.util.DataCenter;
import net.comorevi.cosse.chestguard.util.FormManager;
import net.comorevi.cosse.chestguard.util.MessageType;
import net.comorevi.cosse.chestguard.util.ProtectType;

import java.util.Arrays;

import static cn.nukkit.block.BlockID.CHEST;

public class EventListener implements Listener {

    private Main plugin;
    private FormManager form;
    private ChestGuardAPI pluginAPI = ChestGuardAPI.getInstance();

    public EventListener(Main plugin) {
        this.plugin = plugin;
        this.form = new FormManager(plugin);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (plugin.ignoreWorlds.contains(block.getLevel().getName())) return;
        if (block.getId() != CHEST) return;
        if (!checkSideBlock(block, event.getPlayer())) {
            event.getPlayer().sendMessage(MessageType.ALERT + plugin.translateString("error-place-chest"));
            event.setCancelled();
            return;
        }

        pluginAPI.addChestGuard((BlockChest) block, event.getPlayer());
        event.getPlayer().sendMessage(MessageType.INFO + plugin.translateString("place-chest"));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (plugin.ignoreWorlds.contains(block.getLevel().getName())) return;
        if (block.getId() != CHEST) return;
        if (!pluginAPI.existsChestData((BlockChest) block) && !event.getPlayer().isOp()) {
            event.setCancelled();
            event.getPlayer().sendMessage(MessageType.ERROR + plugin.translateString("error-non-registered-chest"));
            return;
        } else if (!pluginAPI.existsChestData((BlockChest) block) && event.getPlayer().isOp()) {
            return;
        }

        if (pluginAPI.isOwner((BlockChest) block, event.getPlayer())) {
            pluginAPI.removeChestGuard((BlockChest) block);
            event.getPlayer().sendMessage(MessageType.INFO + plugin.translateString("break-chest"));
        } else if (event.getPlayer().isOp()) {
            pluginAPI.removeChestGuard((BlockChest) block);
            event.getPlayer().sendMessage(MessageType.ALERT + plugin.translateString("player-chest-break-byOp"));
        } else {
            event.setCancelled();
            event.getPlayer().sendMessage(MessageType.ALERT + plugin.translateString("error-not-own-chest"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getBlock();
        if (plugin.ignoreWorlds.contains(block.getLevel().getName())) return;
        if (block.getId() != CHEST) return;

        if (DataCenter.existCmdQueue(event.getPlayer())) {
            event.setCancelled();
            DataCenter.registerChestToCmdQueue(event.getPlayer(), (BlockChest) block);
            if (!pluginAPI.existsChestData((BlockChest) block)) {
                if (event.getPlayer().isOp()) {
                    form.sendAddChestWindow(event.getPlayer());
                } else {
                    event.getPlayer().sendMessage(MessageType.ERROR + plugin.translateString("error-non-permission"));
                }
            } else {
                if (pluginAPI.isOwner((BlockChest) block, event.getPlayer()) || event.getPlayer().isOp()) {
                    form.sendHomeWindow(event.getPlayer());
                } else {
                    form.sendStatusWindow((BlockChest) block, event.getPlayer());
                }
            }
            return;
        }

        if (!pluginAPI.existsChestData((BlockChest) block)) {
            event.setCancelled();
            event.getPlayer().sendMessage(MessageType.ERROR + plugin.translateString("error-non-registered-chest"));
            return;
        }

        if (!pluginAPI.isOwner((BlockChest) block, event.getPlayer()) && event.getPlayer().isOp()) {
            event.getPlayer().sendMessage(MessageType.ALERT + plugin.translateString("player-chest-interact-byOp"));
            return;
        }

        switch (ProtectType.getById((Integer) pluginAPI.getRegisteredDataMap((BlockChest) block).get("typeId"))) {
            case PROTECT_TYPE_DEFAULT:
                if (!pluginAPI.isOwner((BlockChest) block, event.getPlayer())) {
                    event.setCancelled();
                    event.getPlayer().sendMessage(MessageType.ALERT + plugin.translateString("error-not-own-chest"));
                }
                break;
            case PROTECT_TYPE_PASSWORD:
                if (pluginAPI.isOwner((BlockChest) block, event.getPlayer())) return;
                DataCenter.addCmdQueue(event.getPlayer(), (BlockChest) block);
                form.sendPassAuthenticationWindow(event.getPlayer());
                event.setCancelled();
                break;
            case PROTECT_TYPE_SHARE:
                if (!pluginAPI.isOwner((BlockChest) block, event.getPlayer())) {
                    String data = (String) pluginAPI.getRegisteredDataMap((BlockChest) block).get("data");
                    String[] shared = data.split(",");
                    if (!Arrays.asList(shared).contains(event.getPlayer().getName())) {
                        event.setCancelled();
                        event.getPlayer().sendMessage(MessageType.ALERT + plugin.translateString("error-not-shared"));
                    } else {
                        event.getPlayer().addWindow(((BlockEntityChest) block.getLevel().getBlockEntity(block.getLocation())).getInventory());
                        event.getPlayer().sendMessage(MessageType.INFO + plugin.translateString("player-certified"));
                    }
                }
                break;
            case PROTECT_TYPE_PUBLIC:
                if (!pluginAPI.isOwner((BlockChest) block, event.getPlayer())) {
                    event.getPlayer().addWindow(((BlockEntityChest) block.getLevel().getBlockEntity(block.getLocation())).getInventory());
                    event.getPlayer().sendMessage(MessageType.INFO + plugin.translateString("player-open-public-chest", (String) pluginAPI.getRegisteredDataMap((BlockChest) block).get("owner")));
                }
                break;
        }
    }

    private boolean checkSideBlock(Block block, Player player) {
        for (BlockFace face : BlockFace.values()) {
            if (block.getSide(face).getId() == CHEST) {
                if (!pluginAPI.isOwner((BlockChest) block.getSide(face), player) && !((BlockEntityChest) block.getLevel().getBlockEntity(block.getSide(face).getLocation())).isPaired()) {
                    return false;
                }
            }
        }
        return true;
    }
}
