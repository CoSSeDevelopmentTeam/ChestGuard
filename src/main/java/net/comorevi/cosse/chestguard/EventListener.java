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
    private String br = System.getProperty("line.separator");
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
                        event.getPlayer().sendMessage("[ChestGuard] This chest is not shared with you.");
                    } else {
                        event.getPlayer().addWindow(((BlockEntityChest) block.getLevel().getBlockEntity(block.getLocation())).getInventory());
                        event.getPlayer().sendMessage("[ChestGuard] Certified.");
                    }
                }
                break;
            case PROTECT_TYPE_PUBLIC:
                if (!pluginAPI.isOwner((BlockChest) block, event.getPlayer())) {
                    event.getPlayer().addWindow(((BlockEntityChest) block.getLevel().getBlockEntity(block.getLocation())).getInventory());
                    event.getPlayer().sendMessage("[ChestGuard] You opened PUBLIC chest. Provided by "+pluginAPI.getRegisteredDataMap((BlockChest) block).get("owner")+".");
                }
                break;
        }
    }

    @EventHandler
    public void onFormResponded(PlayerFormRespondedEvent event) {
        if (event.getFormID() == formAPI.getId("home")) {
            if (event.wasClosed()) return;
            FormResponseSimple responseSimple = (FormResponseSimple) event.getResponse();
            if (responseSimple.getClickedButton().getText().equals("See information")) {
                formAPI.add("status", getStatusWindow(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())));
                event.getPlayer().showFormWindow(formAPI.get("status"), formAPI.getId("status"));
            } else if (responseSimple.getClickedButton().getText().equals("Open setting editor")) {
                formAPI.add("editor", getChangeGuardOptionWindow());
                event.getPlayer().showFormWindow(formAPI.get("editor"), formAPI.getId("editor"));
            } else if (responseSimple.getClickedButton().getText().equals("Add new chest")) {
                if (pluginAPI.existsChestData(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()))) {
                    event.getPlayer().sendMessage("[ChestGuard] This chest had been registered.");
                } else {
                    formAPI.add("add-chest", getAddChestWindow());
                    event.getPlayer().showFormWindow(formAPI.get("add-chest"), formAPI.getId("add-chest"));
                }
            }
        } else if (event.getFormID() == formAPI.getId("status")) {
            FormResponseModal responseModal = (FormResponseModal) event.getResponse();
            if (responseModal.getClickedButtonText().equals("Edit option")) {
                if (pluginAPI.isOwner(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), event.getPlayer()) || event.getPlayer().isOp()) {
                    formAPI.add("editor", getChangeGuardOptionWindow());
                    event.getPlayer().showFormWindow(formAPI.get("editor"), formAPI.getId("editor"));
                } else {
                    event.getPlayer().sendMessage("[ChestGuard] You have not enough permission.");
                }
            }
        } else if (event.getFormID() == formAPI.getId("editor")) {
            if (event.wasClosed()) return;
            FormResponseCustom responseCustom = (FormResponseCustom) event.getResponse();
            switch (responseCustom.getDropdownResponse(1).getElementContent()) {
                case "DEFAULT":
                    pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_DEFAULT, null);
                    event.getPlayer().sendMessage("[ChestGuard] Set guard type to DEFAULT.");
                    break;
                case "PASSWORD":
                    if (responseCustom.getInputResponse(3).equals("")) {
                        event.getPlayer().sendMessage("[ChestGuard] Enter additional data(password)");
                        break;
                    }
                    pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_PASSWORD, responseCustom.getInputResponse(3));
                    event.getPlayer().sendMessage("[ChestGuard] Set guard type to PASSWORD."+br+"Your password is: "+pluginAPI.getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())).get("data"));
                    break;
                case "SHARE":
                    if (responseCustom.getInputResponse(3).equals("")) {
                        event.getPlayer().sendMessage("[ChestGuard] Enter additional data(friends' name)");
                        break;
                    }
                    pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_SHARE, responseCustom.getInputResponse(3));
                    event.getPlayer().sendMessage("[ChestGuard] Set guard type to SHARE."+br+"Your chest is shared with: "+pluginAPI.getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())).get("data"));
                    break;
                case "PUBLIC":
                    pluginAPI.changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), ProtectType.PROTECT_TYPE_PUBLIC, null);
                    event.getPlayer().sendMessage("[ChestGuard] Set guard type to PUBLIC.");
                    break;
            }
        } else if (event.getFormID() == formAPI.getId("add-chest")) {
            if (event.wasClosed()) return;
            FormResponseCustom responseCustom = (FormResponseCustom) event.getResponse();
            if (responseCustom.getInputResponse(1).equals("")) {
                event.getPlayer().sendMessage("[ChestGuard] Enter owner's name.");
            } else {
                pluginAPI.addChestGuard(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()), responseCustom.getInputResponse(1));
                event.getPlayer().sendMessage("[ChestGuard] Added new chest.");
            }
        } else if (event.getFormID() == formAPI.getId("pass-auth")) {
            if (event.wasClosed()) {
                DataCenter.removeCmdQueue(event.getPlayer());
                return;
            }
            FormResponseCustom responseCustom = (FormResponseCustom) event.getResponse();
            if (responseCustom.getInputResponse(1).equals("")) {
                DataCenter.removeCmdQueue(event.getPlayer());
                event.getPlayer().sendMessage("[ChestGuard] You have to enter password to open the chest.");
            } else {
                if (pluginAPI.getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(event.getPlayer())).get("data").equals(responseCustom.getInputResponse(1))) {
                    event.getPlayer().addWindow(((BlockEntityChest) event.getPlayer().getLevel().getBlockEntity(DataCenter.getCmdQueueRegisteredChest(event.getPlayer()).getLocation())).getInventory());
                    DataCenter.removeCmdQueue(event.getPlayer());
                    event.getPlayer().sendMessage("[ChestGuard] Certified.");
                } else {
                    DataCenter.removeCmdQueue(event.getPlayer());
                    event.getPlayer().sendMessage("[ChestGuard] Password is not correct.");
                }
            }
        }
    }

    private FormWindowSimple getHomeWindow(Player player) {
        List<ElementButton> buttons = new LinkedList<>();
        buttons.add(new ElementButton("See information"));
        buttons.add(new ElementButton("Open setting editor"));
        if (player.isOp()) buttons.add(new ElementButton("Add new chest"));
        return new FormWindowSimple("ChestGuard", "What are you want to do?", buttons);
    }
    private FormWindowModal getStatusWindow(BlockChest chest) {
        return new FormWindowModal("Info - ChestGuard",
                "owner: "+pluginAPI.getRegisteredDataMap(chest).get("owner")+br+"Location: "+pluginAPI.getRegisteredDataMap(chest).get("location")+br+"Type: "+pluginAPI.getRegisteredDataMap(chest).get("typeString")+br+"Optional data: "+pluginAPI.getRegisteredDataMap(chest).getOrDefault("data", "null"),
                "Edit option",
                "close");
    }
    private FormWindowCustom getChangeGuardOptionWindow() {
        List<Element> elements = new LinkedList<Element>() {
            {
                add(new ElementLabel("Select guard option to change. If you selected PASSWORD or SHARE, you have to enter additional data below drop-down."));
                add(new ElementDropdown("type", new LinkedList<String>() {
                    {
                        add("DEFAULT");
                        add("PASSWORD");
                        add("SHARE");
                        add("PUBLIC");
                    }
                }));
                add(new ElementLabel("If you selected PASSWORD or SHARE, please enter password or friends' name.(ex. Steve,Alex)"));
                add(new ElementInput("optional data", "enter data..."));
            }
        };
        return new FormWindowCustom("Editor - ChestGuard", elements);
    }
    private FormWindowCustom getAddChestWindow() {
        List<Element> elements = new LinkedList<Element>() {
            {
                add(new ElementLabel("Enter owner's name and push submit."));
                add(new ElementInput("Owner", "enter name..."));
            }
        };
        return new FormWindowCustom("Add - ChestGuard", elements);
    }
    private FormWindowCustom getPasswordAuthenticationWindow() {
        List<Element> elements = new LinkedList<Element>() {
            {
                add(new ElementLabel("Enter password and push submit."));
                add(new ElementInput("Password", "enter password..."));
            }
        };
        return new FormWindowCustom("Authentication - ChestGuard", elements);
    }
}
