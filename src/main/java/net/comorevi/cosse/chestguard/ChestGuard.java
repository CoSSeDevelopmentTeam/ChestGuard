package net.comorevi.cosse.chestguard;

import cn.nukkit.level.Location;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ChestGuard extends PluginBase {

    private static ChestGuard instance;

    private Config translateFile;
    private Map<String, Object> configData = new HashMap<String, Object>();

    private SQLite3DataProvider sql;

    // API //
    public static ChestGuard getInstance() {
        return instance;
    }

    public boolean existsChestData(Location loc) {
        if (getSQL().existsChestData(getFormattedPosition(loc))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isOwnChest(String owner, Location location) {
        String chestOwner = getSQL().getChestOwnerName(getFormattedPosition(location));
        if (owner.equals(chestOwner)) {
            return true;
        } else {
            return false;
        }
    }

    public void addGuard(String owner, Location location) {
        getSQL().createChestData(getFormattedPosition(location), owner);
    }

    public void removeGuard(Location location) {
        getSQL().deleteChestData(getFormattedPosition(location));
    }

    public void changeGuardType(Location location, int type, String data) {
        getSQL().changeGuardType(getFormattedPosition(location), type, data);
    }

    public String getFormattedPosition(Location location) {
        return location.getFloorX() + "::" + location.getFloorY() + "::" + location.getFloorZ() + "::" + location.getLevel().getFolderName();
    }

    // CHEST GUARD//
    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        initMessageConfig();
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        this.sql = new SQLite3DataProvider(this);
        this.instance = this;
    }

    @Override
    public void onDisable() {
        getSQL().disConnectSQL();
    }

    public String translateString(String key, String... args){
        if(configData != null || !configData.isEmpty()){
            String src = (String) configData.get(key);
            if(src == null || src.equals("")) return TextFormat.YELLOW + (String) configData.get("error-notFoundKey");
            for(int i=0;i < args.length;i++){
                src = src.replace("{%" + i + "}", args[i]);
            }
            return src;
        }
        return null;
    }

    public String parseMessage(String message) {
        return "";
    }

    private void initMessageConfig() {
        if(!new File(getDataFolder().toString() + "/Message.yml").exists()){
            try {
                FileWriter fw = new FileWriter(new File(getDataFolder().toString() + "/Message.yml"), true);//trueで追加書き込み,falseで上書き
                PrintWriter pw = new PrintWriter(fw);
                pw.println("");
                pw.close();
                Utils.writeFile(new File(getDataFolder().toString() + "/Message.yml"), this.getClass().getClassLoader().getResourceAsStream("Message.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.translateFile = new Config(new File(getDataFolder().toString() + "/Message.yml"), Config.YAML);
        this.translateFile.load(getDataFolder().toString() + "/Message.yml");
        this.configData = this.translateFile.getAll();
        return;
    }

    SQLite3DataProvider getSQL() {
        return this.sql;
    }
}
