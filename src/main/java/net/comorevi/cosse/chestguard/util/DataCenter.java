package net.comorevi.cosse.chestguard.util;

import cn.nukkit.Player;
import cn.nukkit.block.BlockChest;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataCenter {
    private static Map<Player, BlockChest> cmdQueue = new LinkedHashMap<>();

    public static void addCmdQueue(Player player) {
        cmdQueue.put(player, null);
    }

    public static void removeCmdQueue(Player player) {
        cmdQueue.remove(player);
    }

    public static BlockChest getRegisteredChest(Player player) {
        return cmdQueue.get(player);
    }

    public static void registerChestToCmdQueue(Player player, BlockChest chest) {
        cmdQueue.put(player, chest);
    }

    public static boolean existCmdQueue(Player player) {
        return cmdQueue.containsKey(player);
    }
}
