package ru.empireprojekt.empiredata;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Placeholders extends PlaceholderExpansion {

    private EmpireData plugin;
    Placeholders(EmpireData plugin){
        this.plugin=plugin;
    }
    @Override
    public @NotNull String getIdentifier() {
        return "empiredata";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Ne Ratushev Roman";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (plugin.isItem(params))
            return plugin.GetItemValue(params);

        if (player==null)
            return  "";
        if (params.contains("player"))
            return plugin.GetPlayerItemCount(player.getUniqueId().toString(),params.replace("_player",""));
        return "null";
    }
}
