package net.comorevi.cosse.chestguard;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;
import net.comorevi.cosse.chestguard.api.ChestGuardAPI;
import net.comorevi.cosse.chestguard.command.ChestGuardCommand;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Main extends PluginBase {

    private Config translateFile;
    private Config conf;
    private Map<String, Object> configData = new HashMap<>();
    protected List<String> ignoreWorlds = new ArrayList<>();

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        initMessageConfig();
        initChestProtectConfig();
        getServer().getCommandMap().register("chestgd", new ChestGuardCommand());
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
    }

    @Override
    public void onDisable() {
        ChestGuardAPI.getSingletonInstance().disconnect();
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
    }

    private void initChestProtectConfig(){
        List<String> list = new LinkedList<String>(){
            {
                add("enter_ignore_level_name_NOT_folder_name");
            }
        };
        ConfigSection cs = new ConfigSection(){
            {
                put("IgnoreWorlds", list);
            }
        };
        conf = new Config(new File(getDataFolder(), "Config.yml"), Config.YAML, cs);
        ignoreWorlds = conf.getStringList("IgnoreWorlds");
    }
}
