package net.comorevi.cosse.chestguard.util;

import cn.nukkit.Player;
import cn.nukkit.block.BlockChest;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.response.FormResponseSimple;
import net.comorevi.cosse.chestguard.Main;
import net.comorevi.cosse.chestguard.api.ChestGuardAPI;
import ru.nukkitx.forms.elements.CustomForm;
import ru.nukkitx.forms.elements.ModalForm;
import ru.nukkitx.forms.elements.SimpleForm;

import java.util.LinkedList;

public class FormManager {

    private Main plugin;

    public FormManager(Main mainClass) {
        this.plugin = mainClass;
    }

    public void sendHomeWindow(Player player) {
        getHomeWindow().send(player, (targetPlayer, targetForm, data) -> {
            if(data == -1) return;
            FormResponseSimple response = targetForm.getResponse();
            if (response.getClickedButton().getText().equals(plugin.translateString("button-see-information"))) {
                sendStatusWindow(DataCenter.getCmdQueueRegisteredChest(player), player);
            } else if (response.getClickedButton().getText().equals(plugin.translateString("button-open-editor"))) {
                sendChangeGuardOptionWindow(player);
            }
        });
    }

    public void sendStatusWindow(BlockChest chest, Player player) {
        getStatusWindow(chest, player).send(player, (targetPlayer, targetForm, data) -> {
            if(data == -1) return;
            FormResponseModal response = targetForm.getResponse();
            if (response.getClickedButtonText().equals(plugin.translateString("button-open-editor"))) {
                if (ChestGuardAPI.getInstance().isOwner(DataCenter.getCmdQueueRegisteredChest(player), player) || player.isOp()) {
                    sendChangeGuardOptionWindow(player);
                } else {
                    player.sendMessage(MessageType.ALERT + plugin.translateString("error-non-permission"));
                }
            }
        });
    }

    public void sendChangeGuardOptionWindow(Player player) {
        getChangeGuardOptionWindow().send(player, (targetPlayer, targetForm, data) -> {
            if(data == null) return;
            FormResponseCustom response = targetForm.getResponse();
            switch (response.getDropdownResponse(1).getElementContent()) {
                case "DEFAULT":
                    ChestGuardAPI.getInstance().changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(player), ProtectType.PROTECT_TYPE_DEFAULT, null);
                    player.sendMessage(MessageType.INFO + plugin.translateString("player-set-protect-type1", "DEFAULT"));
                    break;
                case "PASSWORD":
                    if (response.getInputResponse(3).equals("")) {
                        player.sendMessage(MessageType.ERROR + plugin.translateString("error-lack-of-data"));
                        break;
                    }
                    ChestGuardAPI.getInstance().changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(player), ProtectType.PROTECT_TYPE_PASSWORD, response.getInputResponse(3));
                    player.sendMessage(MessageType.INFO + plugin.translateString("player-set-protect-type2", (String) ChestGuardAPI.getInstance().getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(player)).get("data")));
                    break;
                case "SHARE":
                    if (response.getInputResponse(3).equals("")) {
                        player.sendMessage(MessageType.ERROR + plugin.translateString("error-lack-of-data"));
                        break;
                    }
                    ChestGuardAPI.getInstance().changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(player), ProtectType.PROTECT_TYPE_SHARE, response.getInputResponse(3));
                    player.sendMessage(MessageType.INFO + plugin.translateString("player-set-protect-type3", (String) ChestGuardAPI.getInstance().getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(player)).get("data")));
                    break;
                case "PUBLIC":
                    ChestGuardAPI.getInstance().changeChestGuardType(DataCenter.getCmdQueueRegisteredChest(player), ProtectType.PROTECT_TYPE_PUBLIC, null);
                    player.sendMessage(MessageType.INFO + plugin.translateString("player-set-protect-type1", "PUBLIC"));
                    break;
            }
        });
    }

    public void sendPassAuthenticationWindow(Player player) {
        getPasswordAuthenticationWindow().send(player, (targetPlayer, targetForm, data) -> {
            FormResponseCustom response = targetForm.getResponse();
            if(data == null) {
                DataCenter.removeCmdQueue(player);
                return;
            }
            if (response.getInputResponse(1).equals("")) {
                DataCenter.removeCmdQueue(player);
                player.sendMessage(MessageType.ERROR + plugin.translateString("error-password-not-entered"));
            } else {
                if (ChestGuardAPI.getInstance().getRegisteredDataMap(DataCenter.getCmdQueueRegisteredChest(player)).get("data").equals(response.getInputResponse(1))) {
                    player.addWindow(((BlockEntityChest) player.getLevel().getBlockEntity(DataCenter.getCmdQueueRegisteredChest(player).getLocation())).getInventory());
                    DataCenter.removeCmdQueue(player);
                    player.sendMessage(MessageType.INFO + plugin.translateString("player-certified"));
                } else {
                    DataCenter.removeCmdQueue(player);
                    player.sendMessage(MessageType.ERROR + plugin.translateString("error-missed-password"));
                }
            }
        });
    }

    public void sendAddChestWindow(Player player) {
        getAddChestWindow().send(player, (targetPlayer, targetForm, data) -> {
            if(data == null) return;
            FormResponseCustom response = targetForm.getResponse();
            if (response.getInputResponse(1).equals("")) {
                player.sendMessage(MessageType.ERROR + plugin.translateString("error-lack-of-data"));
            } else {
                ChestGuardAPI.getInstance().addChestGuard(DataCenter.getCmdQueueRegisteredChest(player), response.getInputResponse(1));
                player.sendMessage(MessageType.INFO + plugin.translateString("player-chest-added"));
            }
        });
    }

    public SimpleForm getHomeWindow() {
        SimpleForm simpleForm = new SimpleForm(plugin.translateString("main-window-title"))
                .addButton(plugin.translateString("button-see-information"))
                .addButton(plugin.translateString("button-open-editor"));
        return simpleForm;
    }

    public ModalForm getStatusWindow(BlockChest chest, Player player) {
        String status;
        if (ChestGuardAPI.getInstance().isOwner(chest, player) || player.isOp()) {
            status = (String) ChestGuardAPI.getInstance().getRegisteredDataMap(chest).getOrDefault("data", "null");
        } else {
            status = "*****";
        }
        ModalForm modalForm = new ModalForm(plugin.translateString("status-window-title"))
                .setContent("owner: "+ChestGuardAPI.getInstance().getRegisteredDataMap(chest).get("owner")+"\nLocation: "+ChestGuardAPI.getInstance().getRegisteredDataMap(chest).get("location")+"\nType: "+ChestGuardAPI.getInstance().getRegisteredDataMap(chest).get("typeString")+"\nOptional data: "+status)
                .setButton1(plugin.translateString("button-open-editor2"))
                .setButton2(plugin.translateString("button-close"));
        return modalForm;
    }

    public CustomForm getChangeGuardOptionWindow() {
        CustomForm customForm = new CustomForm(plugin.translateString("edit-guard-type-window-title"))
                .addLabel(plugin.translateString("label-editor1"))
                .addDropDown("Type", new LinkedList<String>() {
                    {
                        add("DEFAULT");
                        add("PASSWORD");
                        add("SHARE");
                        add("PUBLIC");
                    }
                })
                .addLabel(plugin.translateString("label-editor2"))
                .addInput("Additional data", "enter data...");
        return customForm;
    }

    public CustomForm getAddChestWindow() {
        CustomForm customForm = new CustomForm(plugin.translateString("add-chest-window-title"))
                .addLabel(plugin.translateString("label-add"))
                .addInput("Owner", "enter name...");
        return customForm;
    }

    public CustomForm getPasswordAuthenticationWindow() {
        CustomForm customForm = new CustomForm(plugin.translateString("pass-auth-window-title"))
                .addLabel(plugin.translateString("label-pass"))
                .addInput("Password", "enter password...");
        return customForm;
    }
}
