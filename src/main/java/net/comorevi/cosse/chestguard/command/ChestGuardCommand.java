package net.comorevi.cosse.chestguard.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import net.comorevi.cosse.chestguard.util.DataCenter;

public class ChestGuardCommand extends Command {
    public ChestGuardCommand() {
        super("chestgd", "Enable/Disable ChestGuard editor.", "/chestgd");
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] strings) {
        if (!sender.isPlayer()) {
            sender.sendMessage("You can't execute this command by console.");
            return false;
        }
        if (!DataCenter.existCmdQueue((Player) sender)) {
            sender.sendMessage("[ChestGuard] Hit target chest.");
            DataCenter.addCmdQueue((Player) sender);
        } else {
            sender.sendMessage("[ChestGuard] Editor disabled.");
            DataCenter.removeCmdQueue((Player) sender);
        }
        return true;
    }
}
