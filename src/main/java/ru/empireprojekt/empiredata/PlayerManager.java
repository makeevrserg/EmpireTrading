package ru.empireprojekt.empiredata;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class PlayerManager {
    class mPlayer {
        public String UUID;
        public Map<String, Integer> currency;

        public mPlayer(String UUID, String currencyName) {
            this.UUID = UUID;
            this.currency = new HashMap<String, Integer>();
            currency.put(currencyName, 1);
        }

        public mPlayer(String UUID, String currencyName, int currencyValue) {
            this.UUID = UUID;
            this.currency = new HashMap<String, Integer>();
            currency.put(currencyName, currencyValue);
        }

        public mPlayer(String UUID, Map<String, Integer> currency) {
            this.UUID = UUID;
            this.currency = currency;
        }
    }

    Map<String, mPlayer> players;

    PlayerManager() {
        players = new HashMap<String, mPlayer>();
    }

    public void addPlayerCount(String UUID, String currencyName,int currencyCount) {
        if (players.containsKey(UUID))
            if (players.get(UUID).currency.containsKey(currencyName))
                players.get(UUID).currency.put(currencyName, players.get(UUID).currency.get(currencyName) + currencyCount);
            else
                players.get(UUID).currency.put(currencyName, currencyCount);
        else
            players.put(UUID, new mPlayer(UUID, currencyName,currencyCount));
    }

    public void sellPlayer(String UUID, String currencyName,int currencyCount) {
        if (players.containsKey(UUID))
            if (players.get(UUID).currency.containsKey(currencyName))
                players.get(UUID).currency.put(currencyName, players.get(UUID).currency.get(currencyName) - currencyCount);

    }

    public void ClearData(){
        for (String player:players.keySet()){
            players.get(player).currency=new HashMap<String, Integer>();
        }
    }
    public void addPlayer(String UUID, String currency, int currencyValue) {
        if (!players.containsKey(UUID))
            players.put(UUID, new mPlayer(UUID, currency, currencyValue));
        else {
            mPlayer pl = players.get(UUID);
            pl.currency.put(currency, currencyValue);
            players.put(UUID, pl);
        }
    }

    public void LoadPlayers(ConfigurationSection players) {
        if (players != null) {
            this.players = new HashMap<String, mPlayer>();
            for (String pl : players.getKeys(false)) {
                ConfigurationSection player = players.getConfigurationSection(pl);
                for (String currency : player.getConfigurationSection("currency").getKeys(false)) {
                    addPlayer(pl, currency, player.getConfigurationSection("currency").getInt(currency));
                }
            }
        }
    }

    public FileConfiguration SavePlayers(FileConfiguration configs) {
        if (!configs.contains("players"))
            configs.getConfigurationSection("players");
        else
            configs.createSection("players");

        for (String key : this.players.keySet()) {
            configs.createSection("players." + key);//UUID
            configs.createSection("players." + key + ".currency", this.players.get(key).currency);
        }
        return configs;
    }
}
