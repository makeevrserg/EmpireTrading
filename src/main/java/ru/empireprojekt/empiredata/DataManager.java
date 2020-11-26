package ru.empireprojekt.empiredata;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DataManager {
    private EmpireData plugin;
    private File configFiles = null;
    private File playerDataFiles = null;
    private FileConfiguration dataConfig = null;
    private FileConfiguration playerData = null;


    public DataManager(EmpireData plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
    }

    public void reloadConfig() {
        if (this.configFiles == null)
            this.configFiles = new File(this.plugin.getDataFolder(),"config.yml");

        if (this.playerData == null)
            this.playerDataFiles = new File(this.plugin.getDataFolder(),"playerData.yml");

        dataConfig = YamlConfiguration.loadConfiguration(configFiles);
        playerData = YamlConfiguration.loadConfiguration(playerDataFiles);

        InputStream defaultStream = this.plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.dataConfig.setDefaults(defaultConfig);
        }
    }


    public FileConfiguration getConfig() {
        if (this.dataConfig == null)
            reloadConfig();
        return this.dataConfig;
    }

    public FileConfiguration getPlayerData() {
        if (this.playerData == null)
            reloadConfig();
        return this.playerData;
    }

    private void LoadFiles() {

        configFiles = new File(this.plugin.getDataFolder() + "\\config.yml");
        playerDataFiles = new File(this.plugin.getDataFolder(),"\\playerData.yml");

    }

    public void updateConfig(FileConfiguration conf) {
        this.dataConfig = conf;
    }

    public void saveConfig() {
        if (this.configFiles == null || this.dataConfig == null)
            return;
        try {
            this.getConfig().save(this.configFiles);
        } catch (IOException e) {
            System.out.println("[EmpireData]"+ChatColor.RED + "Не удалось сохранить файл");
        }
    }

    public void saveDefaultConfig() {
        if (this.configFiles == null)
            this.configFiles = new File(this.plugin.getDataFolder(), "config.yml");
        if (!this.configFiles.exists())
            this.plugin.saveResource("config.yml", false);
    }
}
