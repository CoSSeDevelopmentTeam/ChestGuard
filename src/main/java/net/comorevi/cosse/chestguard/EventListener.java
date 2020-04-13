package net.comorevi.cosse.chestguard;

import FormAPI.api.FormAPI;
import cn.nukkit.Player;
import cn.nukkit.block.BlockChest;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.*;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.math.BlockFace;
import net.comorevi.cosse.chestguard.api.ChestGuardAPI;
import net.comorevi.cosse.chestguard.util.DataCenter;
import net.comorevi.cosse.chestguard.util.MessageType;
import net.comorevi.cosse.chestguard.util.ProtectType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static cn.nukkit.block.BlockID.CHEST;

public class EventListener implements Listener {

    private Main plugin;
    private ChestGuardAPI pluginAPI = ChestGuardAPI.getSingletonInstance();
    private FormAPI formAPI = new FormAPI();

    public EventListener(Main plugin) {
        this.plugin = plugin;
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
                    formAPI.add("add-chest", getAddChestWindow());
                    event.getPlayer().showFormWindow(formAPI.get("add-chest"), formAPI.getId("add-chest"));
                } else {
                    event.getPlayer().sendMessage(MessageType.ERROR + plugin.translateString("error-non-permission"));
                }
            } else {
                if (pluginAPI.isOwner((BlockChest) block, event.getPlayer()) || event.getPlayer().isOp()) {
                    formAPI.add("home", getHomeWindow());
                    event.getPlayer().showFormWindow(formAPI.get("home"), formAPI.getId("home"));
                } else {
                    formAPI.add("status", getStatusWindow((BlockChest) block, event.getPlayer()));
                    event.getPlayer().showFormWindow(formAPI.get("status"), formAPI.getId("status"));
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
                formAPI.add("pass-auth", getPasswordAuthenticationWindow());
                event.getPlayer().showFormWindow(formAPI.get("pass-auth"), formAPI.getId("pass-auth"));
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

    @EventHandler
    public void onFormResponded(PlayerFormRespondedEvent event) {
        if (event.getResponse() instanceof FormResponseModal) {
            FormResponseModal response = (FormResponseModal) event.getResponse();
            if (event.getFormID() == formAPI.getId("status")) {
                if (event.wasClosed()) return;
                if (response.getClickedButtonText().equals(plugin.translateString("button-open-editor"))) {
                    if (pluginAPI.isOwner(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), event.getPlayer()) || event.getPlayer().isOp()) {
                        formAPI.add("editor", getChangeGuardOptionWindow());
                        event.getPlayer().showFormWindow(formAPI.get("editor"), formAPI.getId("editor"));
                    } else {
                        event.getPlayer().sendMessage(MessageType.ALERT + plugin.translateString("error-non-permission"));
                    }
                }
            }
        } else if (event.getResponse() instanceof FormResponseSimple) {
            FormResponseSimple response = (FormResponseSimple) event.getResponse();
            if (event.getFormID() == formAPI.getId("home")) {
                if (event.wasClosed()) return;
                if (response.getClickedButton().getText().equals(plugin.translateString("button-see-information"))) {
                    formAPI.add("status", getStatusWindow(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), event.getPlayer()));
                    event.getPlayer().showFormWindow(formAPI.get("status"), formAPI.getId("status"));
                } else if (response.getClickedButton().getText().equals(plugin.translateString("button-open-editor"))) {
                    formAPI.add("editor", getChangeGuardOptionWindow());
                    event.getPlayer().showFormWindow(formAPI.get("editor"), formAPI.getId("editor"));
                }
            }
        } else if (event.getResponse() instanceof FormResponseCustom) {
            FormResponseCustom response = (FormResponseCustom) event.getResponse();
            if (event.getFormID() == formAPI.getId("editor")) {
                if (event.wasClosed()) return;
                switch (response.getDropdownResponse(1).getElementContent()) {
                    case "DEFAULT":
                        pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_DEFAULT, null);
                        event.getPlayer().sendMessage(MessageType.INFO + plugin.translateString("player-set-protect-type1", "DEFAULT"));
                        break;
                    case "PASSWORD":
                        if (response.getInputResponse(3).equals("")) {
                            event.getPlayer().sendMessage(MessageType.ERROR + plugin.translateString("error-lack-of-data"));
                            break;
                        }
                        pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_PASSWORD, response.getInputResponse(3));
                        event.getPlayer().sendMessage(MessageType.INFO + plugin.translateString("player-set-protect-type2", (String) pluginAPI.getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())).get("data")));
                        break;
                    case "SHARE":
                        if (response.getInputResponse(3).equals("")) {
                            event.getPlayer().sendMessage(MessageType.ERROR + plugin.translateString("error-lack-of-data"));
                            break;
                        }
                        pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_SHARE, response.getInputResponse(3));
                        event.getPlayer().sendMessage(MessageType.INFO + plugin.translateString("player-set-protect-type3", (String) pluginAPI.getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())).get("data")));
                        break;
                    case "PUBLIC":
                        pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_PUBLIC, null);
                        event.getPlayer().sendMessage(MessageType.INFO + plugin.translateString("player-set-protect-type1", "PUBLIC"));
                        break;
                }
            } else if (event.getFormID() == formAPI.getId("add-chest")) {
                if (event.wasClosed()) return;
                if (response.getInputResponse(1).equals("")) {
                    event.getPlayer().sendMessage(MessageType.ERROR + plugin.translateString("error-lack-of-data"));
                } else {
                    pluginAPI.addChestGuard(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), response.getInputResponse(1));
                    event.getPlayer().sendMessage(MessageType.INFO + plugin.translateString("player-chest-added"));
                }
            } else if (event.getFormID() == formAPI.getId("pass-auth")) {
                if (event.wasClosed()) return;
                if (response.getInputResponse(1).equals("")) {
                    DataCenter.removeCmdQueue(event.getPlayer());
                    event.getPlayer().sendMessage(MessageType.ERROR + plugin.translateString("error-password-not-entered"));
                } else {
                    if (pluginAPI.getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())).get("data").equals(response.getInputResponse(1))) {
                        event.getPlayer().addWindow(((BlockEntityChest) event.getPlayer().getLevel().getBlockEntity(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()).getLocation())).getInventory());
                        DataCenter.removeCmdQueue(event.getPlayer());
                        event.getPlayer().sendMessage(MessageType.INFO + plugin.translateString("player-certified"));
                    } else {
                        DataCenter.removeCmdQueue(event.getPlayer());
                        event.getPlayer().sendMessage(MessageType.ERROR + plugin.translateString("error-missed-password"));
                    }
                }
            }
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

    private FormWindowSimple getHomeWindow() {
        List<ElementButton> buttons = new LinkedList<>();
        buttons.add(new ElementButton(plugin.translateString("button-see-information")));
        buttons.add(new ElementButton(plugin.translateString("button-open-editor")));
        return new FormWindowSimple("ChestGuard", plugin.translateString("label-home"), buttons);
    }
    private FormWindowModal getStatusWindow(BlockChest chest, Player player) {
        String status;
        if (pluginAPI.isOwner(chest, player) || player.isOp()) {
            status = (String) pluginAPI.getRegisteredDataMap(chest).getOrDefault("data", "null");
        } else {
            status = "*****";
        }
        return new FormWindowModal("Info - ChestGuard",
                "owner: "+pluginAPI.getRegisteredDataMap(chest).get("owner")+"\nLocation: "+pluginAPI.getRegisteredDataMap(chest).get("location")+"\nType: "+pluginAPI.getRegisteredDataMap(chest).get("typeString")+"\nOptional data: "+status,
                plugin.translateString("button-open-editor2"),
                plugin.translateString("button-close"));
    }
    private FormWindowCustom getChangeGuardOptionWindow() {
        List<Element> elements = new LinkedList<Element>() {
            {
                add(new ElementLabel(plugin.translateString("label-editor1")));
                add(new ElementDropdown("Type", new LinkedList<String>() {
                    {
                        add("DEFAULT");
                        add("PASSWORD");
                        add("SHARE");
                        add("PUBLIC");
                    }
                }));
                add(new ElementLabel(plugin.translateString("label-editor2")));
                add(new ElementInput("Additional data", "enter data..."));
            }
        };
        return new FormWindowCustom("Editor - ChestGuard", elements);
    }
    private FormWindowCustom getAddChestWindow() {
        List<Element> elements = new LinkedList<Element>() {
            {
                add(new ElementLabel(plugin.translateString("label-add")));
                add(new ElementInput("Owner", "enter name..."));
            }
        };
        return new FormWindowCustom("Add - ChestGuard", elements);
    }
    private FormWindowCustom getPasswordAuthenticationWindow() {
        List<Element> elements = new LinkedList<Element>() {
            {
                add(new ElementLabel(plugin.translateString("label-pass")));
                add(new ElementInput("Password", "enter password..."));
            }
        };
        return new FormWindowCustom("Authentication - ChestGuard", elements);
    }
}
