package net.comorevi.cosse.chestguard.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import net.comorevi.cosse.chestguard.Main;
import net.comorevi.cosse.chestguard.util.DataCenter;
import net.comorevi.cosse.chestguard.util.MessageType;

public class ChestGuardCommand extends Command {
    private Main plugin;
    public ChestGuardCommand(Main plugin) {
        super("chest", "Enable/Disable ChestGuard editor.", "/chest");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] strings) {
        if (!sender.isPlayer()) {
            sender.sendMessage(MessageType.ERROR + plugin.translateString("error-execute-command-console"));
            return false;
        }
        if (!DataCenter.existCmdQueue((Player) sender)) {
            sender.sendMessage(MessageType.INFO + plugin.translateString("player-execute-command-enable"));
            DataCenter.addCmdQueue((Player) sender);
        } else {
            sender.sendMessage(MessageType.INFO + plugin.translateString("player-execute-command-disable"));
            DataCenter.removeCmdQueue((Player) sender);
        }
        return true;
    }
}
