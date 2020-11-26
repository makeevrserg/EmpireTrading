package ru.empireprojekt.empiredata;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class EmpireData extends JavaPlugin {

    class TradingItem {
        public String name;
        public double division;
        public String[] dataList;
        public int updateTime;
        public boolean isStopped = false;
        public Double currentValue;
        public Thread thread;
        public int lastItem = 0;
        public int maxAmount, curAmount;
        public int burnDelay, burnAmount;
    }

    private Map<String, TradingItem> tradingItems;
    //ImageMapManager imageMapManager;

    public String GetItemValue(String item) {
        if (!isItem(item))
            return "None";
        else return Double.toString(tradingItems.get(item).currentValue);
    }

    public String GetPlayerItemCount(String UUID, String item) {
        if (!isItem(item) || !isPlayerHas(UUID, item))
            return "0";
        else
            return Integer.toString(playerManager.players.get(UUID).currency.get(item));

    }

    public boolean isItem(String item) {
        return tradingItems.containsKey(item);
    }

    public boolean isPlayer(String UUID) {
        return playerManager.players.containsKey(UUID);
    }

    public boolean isPlayerHas(String UUID, String item) {
        return (isPlayer(UUID) && playerManager.players.get(UUID).currency.containsKey(item));

    }


    private DataManager dataManager;
    private Connection connection;
    PlayerManager playerManager;


    int autosavePeriod = 30;
    boolean autosaveAlive = true;
    Thread autosaveThread;

    private void Autosave() {
        while (autosaveAlive) {
            Save(null);
            System.out.println("[EmpireData]" + ChatColor.RED + "EmpireData Saved!");
            try {
                autosaveThread.sleep(autosavePeriod * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
    }

    @Override
    public void onEnable() {
        System.out.println("[EmpireData]" + "-----------------------------------------------------------");
        System.out.println("[EmpireData]" + "Plugin EmpireItems has been Enabled!");
        System.out.println("[EmpireData]" + "-----------------------------------------------------------");
        dataManager = new DataManager(this);
        playerManager = new PlayerManager();

        LoadData();
    }

    private boolean CheckDB() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("[EmpireData]" + ChatColor.RED + "Соединение с базой данных утеряно");

                System.out.println("[EmpireData]" + ChatColor.RED + "Пытаемся переподключиться");
                connectToDB();

                return false;
            } else return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void DataGeneration(String position) {
        TradingItem item = tradingItems.get(position);
        System.out.println("[EmpireData]" + ChatColor.GREEN + "Тред запустился для " + item.name);
        int sign = 1;
        int i = item.lastItem;
        while (!item.isStopped) {
            item = tradingItems.get(position);
            try {
                item.currentValue = Float.parseFloat(item.dataList[i]) / item.division;
                item.currentValue = Math.round(item.currentValue * 1000.0) / 1000.0;
                synchronized (this) {
                    if (!CheckDB())
                        return;
                    @SuppressWarnings("SqlResolve") PreparedStatement ps = connection.prepareStatement("INSERT INTO " + item.name + " (curr_time,curr_value) VALUES ('" + System.currentTimeMillis() / 1000L + "', '" + Float.parseFloat(item.dataList[i]) / item.division + "')");
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                System.out.println("[EmpireData]" + ChatColor.RED + "Не удалось записать в базу данных");
                e.printStackTrace();
            }
            try {
                item.thread.sleep(item.updateTime * 1000);
            } catch (InterruptedException e) {
                System.out.println("[EmpireData]" + ChatColor.RED + "Тред даты прервался");
            }

            i += sign;
            item.lastItem = i;
            if (i + 1 >= item.dataList.length || i <= 0)
                sign *= -1;
            if (i == 0)
                ClearTable(null);
        }
    }

    private String host, user, passwd;
    private int port;

    private void LoadData() {
        host = dataManager.getConfig().getString("database.host", "localhost");
        port = dataManager.getConfig().getInt("database.port", 3306);
        user = dataManager.getConfig().getString("database.user", "localhost");
        passwd = dataManager.getConfig().getString("database.password", "password");
        autosavePeriod = dataManager.getConfig().getInt("database.autosave", 30);
        tradingItems = new HashMap<String, TradingItem>();
        connectToDB();
//        imageMapManager = new ImageMapManager(this);
//        if (dataManager.getConfig().contains("database.blacklisted_images"))
//            imageMapManager.setBlacklistedImages(dataManager.getConfig().getStringList("database.blacklisted_images"));

        ConfigurationSection items = dataManager.getConfig().getConfigurationSection("items");
        for (String item : items.getKeys(false)) {
            TradingItem tradingItem = new TradingItem();
            tradingItem.division = items.getConfigurationSection(item).getDouble("division");
            tradingItem.updateTime = items.getConfigurationSection(item).getInt("update");
            tradingItem.lastItem = items.getConfigurationSection(item).getInt("lastitem", 0);
            tradingItem.name = item;
            tradingItem.dataList = items.getConfigurationSection(item).getString("default_trade_data").split(",");
            tradingItem.currentValue = Double.parseDouble(tradingItem.dataList[0]);
            tradingItem.thread = CreateThread(tradingItem.name);

            tradingItem.maxAmount = items.getConfigurationSection(item).getInt("max_amount", 0);
            tradingItem.curAmount = items.getConfigurationSection(item).getInt("cur_amount", 0);
            tradingItem.burnDelay = items.getConfigurationSection(item).getInt("burn_delay", 0);
            tradingItem.burnAmount = items.getConfigurationSection(item).getInt("burn_amount", 0);

            if (createDB(tradingItem.name)) {
                tradingItems.put(tradingItem.name, tradingItem);
                tradingItem.thread.start();
            }
        }
        playerManager.LoadPlayers(dataManager.getConfig().getConfigurationSection("players"));
        autosaveAlive = true;
        autosaveThread = RunAutosave();
        autosaveThread.start();
        new Placeholders(this).register();
    }

    private Thread RunAutosave() {
        return new Thread(new Runnable() {
            public void run() {
                Autosave();
            }
        });
    }


    private void connectToDB() {
        try {
            if (connection != null)
                if (!connection.isClosed())
                    connection.close();
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + "empiredata", user, passwd);
            System.out.println("[EmpireData]" + ChatColor.GREEN + "TRADING DATABASE CONNECTED");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private boolean createDB(String name) {
        try {
            synchronized (this) {
                PreparedStatement ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + name + "(id int NOT NULL AUTO_INCREMENT,curr_time int(255) NOT NULL, curr_value FLOAT(10,4), PRIMARY KEY(id))");
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }

    private Thread CreateThread(final String currencyName) {
        return new Thread(new Runnable() {
            public void run() {
                DataGeneration(currencyName);
            }
        });
    }

    private boolean ClearTable(CommandSender sender) {
        if (sender != null && !sender.hasPermission("empiredata.cleardb")) {
            sender.sendMessage(ChatColor.RED + "У вас нет разрешения использовать cleardb");
            return true;
        }
        System.out.println("[EmpireData]" + ChatColor.GREEN + "Чистим базу данных");

        if (sender != null)
            sender.sendMessage(ChatColor.GREEN + "Чистим базу данных");
        try {
            for (String key : tradingItems.keySet()) {
                TradingItem tradingItem = tradingItems.get(key);
                PreparedStatement ps = connection.prepareStatement("TRUNCATE " + tradingItem.name);
                ps.execute();
            }
            System.out.println("[EmpireData]" + ChatColor.GREEN + "Датабазы очищены");
            if (sender != null)
                sender.sendMessage(ChatColor.GREEN + "Базы данных очищены");
            return true;
        } catch (SQLException e) {
            System.out.println("[EmpireData]" + ChatColor.RED + "Неу удалось очистить базу данных");
            if (sender != null)
                sender.sendMessage(ChatColor.RED + "Не удалось очистить базу данных");
            e.printStackTrace();
        }
        return false;
    }

    private void stopThreads() {
        System.out.println("[EmpireData]" + ChatColor.RED + "Останавливаем треды");
        autosaveAlive = false;
        for (String key : tradingItems.keySet()) {
            TradingItem tradingItem = tradingItems.get(key);
            tradingItem.isStopped = true;
            tradingItem.thread.interrupt();
        }
        autosaveThread.interrupt();
        System.out.println("[EmpireData]" + ChatColor.RED + "Треды остановлены");
    }


    private boolean SaveConfig() {
        for (String item : tradingItems.keySet()) {
            if (!dataManager.getConfig().contains("items." + item + ".lastitem"))
                dataManager.getConfig().createSection("items." + item + ".lastitem");
            dataManager.getConfig().set("items." + item + ".lastitem", tradingItems.get(item).lastItem);
        }
        return true;
    }

    private boolean Save(CommandSender sender) {
        if (sender != null && !sender.hasPermission("empiredata.save")) {
            sender.sendMessage(ChatColor.RED + "У вас нет разрешения использовать эту команду");
            return true;
        }
        SaveConfig();
        dataManager.updateConfig(playerManager.SavePlayers(dataManager.getPlayerData()));
        dataManager.saveConfig();
        if (sender != null)
            sender.sendMessage(ChatColor.GREEN + "Сохранено");
        return true;

    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("empiredata.reload")) {
            sender.sendMessage(ChatColor.RED + "У вас нет разрешения использовать эту команду");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "Перезагружаем EmpireItems");
        SaveConfig();
        dataManager.updateConfig(playerManager.SavePlayers(dataManager.getPlayerData()));
        dataManager.saveConfig();
        stopThreads();
        disconnectDatabase();
        dataManager.reloadConfig();
        LoadData();

        sender.sendMessage(ChatColor.GREEN + "Плагин успешно перезагружен!");
        return true;
    }

    private boolean load(CommandSender sender) {
        if (!sender.hasPermission("empiredata.load")) {
            sender.sendMessage(ChatColor.RED + "У вас нет разрешения использовать эту команду");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "Загружаем EmpireItems");
        stopThreads();
        disconnectDatabase();
        dataManager.reloadConfig();
        LoadData();
        sender.sendMessage(ChatColor.GREEN + "Плагин успешно загружен!");
        return true;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("emdreload"))
            return reload(sender);
        if (label.equalsIgnoreCase("empiredata") || label.equalsIgnoreCase("emd")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload"))
                    return reload(sender);
                if (args[0].equalsIgnoreCase("load"))
                    return load(sender);
                if (args[0].equalsIgnoreCase("save"))
                    return Save(sender);
                if (args[0].equalsIgnoreCase("cleardb"))
                    return ClearTable(sender);
//                if (args[0].equalsIgnoreCase("imgload") && sender.hasPermission("empiredata.imgload"))
//                    imageMapManager.LoadImages();
            }
        }
        if (label.equalsIgnoreCase("trading")) {
            if (!sender.hasPermission("empiredata.trading")) {
                sender.sendMessage(ChatColor.RED + "Вы не" + ChatColor.DARK_PURPLE + " трейдер");
                return true;
            }
            RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            Economy econ = economyProvider.getProvider();
            OfflinePlayer offlinePlayer = ((OfflinePlayer) sender);
            if (args.length > 0)
                if (args[0].equalsIgnoreCase("buy")) {
                    int count = 1;
                    if (args[2] != null)
                        try {

                            count = Integer.parseInt(args[2]);
                        } catch (Exception ignored) {
                        }
                    if ((econ.getBalance(offlinePlayer)) >= tradingItems.get(args[1]).currentValue * count) {
                        playerManager.addPlayerCount(offlinePlayer.getUniqueId().toString(), args[1], count);
                        econ.withdrawPlayer(offlinePlayer, tradingItems.get(args[1]).currentValue * count);
                        sender.sendMessage(ChatColor.GREEN + "Куплено " + count + " " + args[1] + " за " + tradingItems.get(args[1]).currentValue * count);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Недостаточно денег");
                    }

                } else if (args[0].equalsIgnoreCase("sell")) {

                    int count = 1;
                    if (args[2] != null)
                        try {

                            count = Integer.parseInt(args[2]);
                        } catch (Exception ignored) {
                        }
                    if (playerManager.players.containsKey(offlinePlayer.getUniqueId().toString()) &&
                            playerManager.players.get(offlinePlayer.getUniqueId().toString()).currency.containsKey(args[1]) &&
                            playerManager.players.get(offlinePlayer.getUniqueId().toString()).currency.get(args[1]) >= count) {
                        playerManager.sellPlayer(offlinePlayer.getUniqueId().toString(), args[1], count);
                        econ.depositPlayer(offlinePlayer, tradingItems.get(args[1]).currentValue * count);
                        sender.sendMessage(ChatColor.GREEN + "Продано " + count + " " + args[1] + " за " + tradingItems.get(args[1]).currentValue * count);
                    } else
                        sender.sendMessage(ChatColor.RED + "Недостаточно валюты");
                }
        }
        return false;
    }


    private void disconnectDatabase() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println("[EmpireData]" + ChatColor.RED + "Не удалось закрыть доступ к базе данных");
        }
    }

    @Override
    public void onDisable() {
        disconnectDatabase();
        stopThreads();

        Save(null);
        super.onDisable();
    }
}
