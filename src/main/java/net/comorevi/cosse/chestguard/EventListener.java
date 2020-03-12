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
import net.comorevi.cosse.chestguard.api.ChestGuardAPI;
import net.comorevi.cosse.chestguard.util.DataCenter;
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

        pluginAPI.addChestGuard((BlockChest) block, event.getPlayer());
        event.getPlayer().sendMessage(plugin.translateString("place-chest"));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (plugin.ignoreWorlds.contains(block.getLevel().getName())) return;
        if (block.getId() != CHEST) return;
        if (!pluginAPI.existsChestData((BlockChest) block) && !event.getPlayer().isOp()) {
            event.setCancelled();
            event.getPlayer().sendMessage(plugin.translateString("error-non-registered-chest"));
            return;
        }

        if (pluginAPI.isOwner((BlockChest) block, event.getPlayer())) {
            pluginAPI.removeChestGuard((BlockChest) block);
            event.getPlayer().sendMessage(plugin.translateString("break-chest"));
        } else if (event.getPlayer().isOp()) {
            pluginAPI.removeChestGuard((BlockChest) block);
            event.getPlayer().sendMessage(plugin.translateString("player-chest-break-byOp"));
        } else {
            event.setCancelled();
            event.getPlayer().sendMessage(plugin.translateString("error-not-own-chest"));
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
                }
            } else {
                if (pluginAPI.isOwner((BlockChest) block, event.getPlayer()) || event.getPlayer().isOp()) {
                    formAPI.add("home", getHomeWindow(event.getPlayer()));
                    event.getPlayer().showFormWindow(formAPI.get("home"), formAPI.getId("home"));
                } else {
                    formAPI.add("status", getStatusWindow((BlockChest) block));
                    event.getPlayer().showFormWindow(formAPI.get("status"), formAPI.getId("status"));
                }
            }
            return;
        }

        if (!pluginAPI.existsChestData((BlockChest) block)) {
            event.setCancelled();
            event.getPlayer().sendMessage(plugin.translateString("error-non-registered-chest"));
            return;
        }

        if (!pluginAPI.isOwner((BlockChest) block, event.getPlayer()) && event.getPlayer().isOp()) {
            event.getPlayer().sendMessage(plugin.translateString("player-chest-interact-byOp"));
            return;
        }

        switch (ProtectType.getById((Integer) pluginAPI.getRegisteredDataMap((BlockChest) block).get("typeId"))) {
            case PROTECT_TYPE_DEFAULT:
                if (!pluginAPI.isOwner((BlockChest) block, event.getPlayer())) {
                    event.setCancelled();
                    event.getPlayer().sendMessage(plugin.translateString("error-not-own-chest"));
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
                        event.getPlayer().sendMessage(plugin.translateString("error-not-shared"));
                    } else {
                        event.getPlayer().addWindow(((BlockEntityChest) block.getLevel().getBlockEntity(block.getLocation())).getInventory());
                        event.getPlayer().sendMessage(plugin.translateString("player-certified"));
                    }
                }
                break;
            case PROTECT_TYPE_PUBLIC:
                if (!pluginAPI.isOwner((BlockChest) block, event.getPlayer())) {
                    event.getPlayer().addWindow(((BlockEntityChest) block.getLevel().getBlockEntity(block.getLocation())).getInventory());
                    event.getPlayer().sendMessage(plugin.translateString("player-open-public-chest", (String) pluginAPI.getRegisteredDataMap((BlockChest) block).get("owner")));
                }
                break;
        }
    }

    @EventHandler
    public void onFormResponded(PlayerFormRespondedEvent event) {
        if (event.getFormID() == formAPI.getId("home")) {
            if (event.wasClosed()) return;
            FormResponseSimple responseSimple = (FormResponseSimple) event.getResponse();
            if (responseSimple.getClickedButton().getText().equals(plugin.translateString("button-see-information"))) {
                formAPI.add("status", getStatusWindow(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())));
                event.getPlayer().showFormWindow(formAPI.get("status"), formAPI.getId("status"));
            } else if (responseSimple.getClickedButton().getText().equals(plugin.translateString("button-open-editor"))) {
                formAPI.add("editor", getChangeGuardOptionWindow());
                event.getPlayer().showFormWindow(formAPI.get("editor"), formAPI.getId("editor"));
            } else if (responseSimple.getClickedButton().getText().equals(plugin.translateString("button-add-new-chest"))) {
                if (pluginAPI.existsChestData(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()))) {
                    event.getPlayer().sendMessage(plugin.translateString("player-chest-added"));
                } else {
                    formAPI.add("add-chest", getAddChestWindow());
                    event.getPlayer().showFormWindow(formAPI.get("add-chest"), formAPI.getId("add-chest"));
                }
            }
        } else if (event.getFormID() == formAPI.getId("status")) {
            FormResponseModal responseModal = (FormResponseModal) event.getResponse();
            if (responseModal.getClickedButtonText().equals(plugin.translateString("button-open-editor2"))) {
                if (pluginAPI.isOwner(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), event.getPlayer()) || event.getPlayer().isOp()) {
                    formAPI.add("editor", getChangeGuardOptionWindow());
                    event.getPlayer().showFormWindow(formAPI.get("editor"), formAPI.getId("editor"));
                } else {
                    event.getPlayer().sendMessage(plugin.translateString("error-non-permission"));
                }
            }
        } else if (event.getFormID() == formAPI.getId("editor")) {
            if (event.wasClosed()) return;
            FormResponseCustom responseCustom = (FormResponseCustom) event.getResponse();
            switch (responseCustom.getDropdownResponse(1).getElementContent()) {
                case "DEFAULT":
                    pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_DEFAULT, null);
                    event.getPlayer().sendMessage(plugin.translateString("player-set-protect-type1", "DEFAULT"));
                    break;
                case "PASSWORD":
                    if (responseCustom.getInputResponse(3).equals("")) {
                        event.getPlayer().sendMessage(plugin.translateString("error-lack-of-data"));
                        break;
                    }
                    pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_PASSWORD, responseCustom.getInputResponse(3));
                    event.getPlayer().sendMessage(plugin.translateString("player-set-protect-type2", (String) pluginAPI.getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())).get("data")));
                    break;
                case "SHARE":
                    if (responseCustom.getInputResponse(3).equals("")) {
                        event.getPlayer().sendMessage(plugin.translateString("error-lack-of-data"));
                        break;
                    }
                    pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_SHARE, responseCustom.getInputResponse(3));
                    event.getPlayer().sendMessage(plugin.translateString("player-set-protect-type3", (String) pluginAPI.getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())).get("data")));
                case "PUBLIC":
                    pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_PUBLIC, null);
                    event.getPlayer().sendMessage(plugin.translateString("player-set-protect-type1"));
                    break;
            }
        } else if (event.getFormID() == formAPI.getId("add-chest")) {
            if (event.wasClosed()) return;
            FormResponseCustom responseCustom = (FormResponseCustom) event.getResponse();
            if (responseCustom.getInputResponse(1).equals("")) {
                event.getPlayer().sendMessage(plugin.translateString("error-lack-of-data"));
            } else {
                pluginAPI.addChestGuard(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), responseCustom.getInputResponse(1));
                event.getPlayer().sendMessage(plugin.translateString("player-chest-added"));
            }
        } else if (event.getFormID() == formAPI.getId("pass-auth")) {
            if (event.wasClosed()) {
                DataCenter.removeCmdQueue(event.getPlayer());
                return;
            }
            FormResponseCustom responseCustom = (FormResponseCustom) event.getResponse();
            if (responseCustom.getInputResponse(1).equals("")) {
                DataCenter.removeCmdQueue(event.getPlayer());
                event.getPlayer().sendMessage(plugin.translateString("error-password-not-entered"));
            } else {
                if (pluginAPI.getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())).get("data").equals(responseCustom.getInputResponse(1))) {
                    event.getPlayer().addWindow(((BlockEntityChest) event.getPlayer().getLevel().getBlockEntity(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()).getLocation())).getInventory());
                    DataCenter.removeCmdQueue(event.getPlayer());
                    event.getPlayer().sendMessage(plugin.translateString("player-certified"));
                } else {
                    DataCenter.removeCmdQueue(event.getPlayer());
                    event.getPlayer().sendMessage(plugin.translateString("error-missed-password"));
                }
            }
        }
    }

    private FormWindowSimple getHomeWindow(Player player) {
        List<ElementButton> buttons = new LinkedList<>();
        buttons.add(new ElementButton(plugin.translateString("button-see-information")));
        buttons.add(new ElementButton(plugin.translateString("button-open-editor")));
        if (player.isOp()) buttons.add(new ElementButton(plugin.translateString("button-add-new-chest")));
        return new FormWindowSimple("ChestGuard", plugin.translateString("label-home"), buttons);
    }
    private FormWindowModal getStatusWindow(BlockChest chest) {
        return new FormWindowModal("Info - ChestGuard",
                "owner: "+pluginAPI.getRegisteredDataMap(chest).get("owner")+"\nLocation: "+pluginAPI.getRegisteredDataMap(chest).get("location")+"\nType: "+pluginAPI.getRegisteredDataMap(chest).get("typeString")+"\nOptional data: "+pluginAPI.getRegisteredDataMap(chest).getOrDefault("data", "null"),
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
