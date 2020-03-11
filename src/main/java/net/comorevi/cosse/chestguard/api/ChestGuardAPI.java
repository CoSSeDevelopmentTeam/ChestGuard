package net.comorevi.cosse.chestguard.api;

import cn.nukkit.Player;
import cn.nukkit.block.BlockChest;
import cn.nukkit.level.Location;
import net.comorevi.cosse.chestguard.util.ProtectType;
import net.comorevi.cosse.chestguard.util.SQLite3DataProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChestGuardAPI {
    private static ChestGuardAPI instance = new ChestGuardAPI();
    private SQLite3DataProvider dataProvider = new SQLite3DataProvider();

    private ChestGuardAPI() {
        instance = this;
    }

    /*
     * Get API instance.
     */
    public static ChestGuardAPI getSingletonInstance() {
        return instance;
    }

    public boolean existsChestData(BlockChest chest) {
        return dataProvider.existsChestData(getFormalizedLocationString(chest.getLocation()));
    }

    public boolean isOwner(BlockChest chest, Player player) {
        return dataProvider.getOwnerName(getFormalizedLocationString(chest.getLocation())).equals(player.getName());
    }

    public void addChestGuard(BlockChest chest, Player player) {
        dataProvider.createChestData(getFormalizedLocationString(chest.getLocation()), player.getName());
    }

    public void addChestGuard(BlockChest chest, String name) {
        dataProvider.createChestData(getFormalizedLocationString(chest.getLocation()), name);
    }

    public void removeChestGuard(BlockChest chest) {
        dataProvider.deleteChestData(getFormalizedLocationString(chest.getLocation()));
    }

    public void changeChestGuardType(BlockChest chest, ProtectType type, String additionalData) {
        dataProvider.changeGuardType(getFormalizedLocationString(chest.getLocation()), type, additionalData);
    }

    public Map<String, Object> getRegisteredDataMap(BlockChest chest) {
        return new LinkedHashMap<String, Object>() {
            {
                put("owner", dataProvider.getOwnerName(getFormalizedLocationString(chest.getLocation())));
                put("location", getFormalizedLocationString(chest.getLocation()));
                put("typeString", ProtectType.getById(dataProvider.getGuardTypeId(getFormalizedLocationString(chest.getLocation()))).toString());
                put("typeId", dataProvider.getGuardTypeId(getFormalizedLocationString(chest.getLocation())));
                put("data", dataProvider.getOptionData(getFormalizedLocationString(chest.getLocation())));
            }
        };
    }

    public void disconnect() {
        if (dataProvider.isConnected()) {
            dataProvider.disConnectSQL();
        } else {
            throw new NullPointerException("Database is not connected.");
        }
    }

    private String getFormalizedLocationString(Location location) {
        return location.getFloorX() + "." + location.getFloorY() + "." + location.getFloorZ() + ":" + location.getLevel().getFolderName();
    }
}
