package net.teamuni.economy.database;

import net.teamuni.economy.Uconomy;
import net.teamuni.economy.config.PlayerFileManager;
import net.teamuni.economy.data.MoneyUpdater;
import net.teamuni.economy.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class YMLDatabase implements MoneyUpdater {
    private final Uconomy main;
    private final PlayerFileManager playerFileManager;
    private final List<String> economyIDs = new ArrayList<>();

    public YMLDatabase(Uconomy instance) {
        this.main = instance;
        this.playerFileManager = instance.getPlayerFileManager();
        this.economyIDs.addAll(instance.getConfig().getStringList("EconomyID"));
    }

    @Override
    public void updatePlayerStats(PlayerData stats) {
        for (Map.Entry<String, Long> entry : stats.getMoneyMap().entrySet()) {
            this.playerFileManager.get(stats.getUuid()).set(entry.getKey(), entry.getValue());
        }
        this.playerFileManager.save(stats.getUuid());
        main.getPlayerFileManager().remove(stats.getUuid());
    }

    @Override
    public PlayerData loadPlayerStats(UUID uuid) {
        if (hasAccount(uuid)) {
            Map<String, Long> moneyMap = new HashMap<>();
            for (String economyID : this.economyIDs) {
                long money = this.playerFileManager.get(uuid).getLong(economyID);
                moneyMap.put(economyID, money);
            }
            return new PlayerData(uuid, moneyMap);
        }
        return defaultPlayerStats(uuid);
    }

    @Override
    public PlayerData defaultPlayerStats(UUID uuid) {
        Map<String, Long> map = new HashMap<>();
        for (String economyID : this.economyIDs) {
            map.put(economyID, 0L);
        }
        return new PlayerData(uuid, map);
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return this.playerFileManager.isExist(uuid);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        this.playerFileManager.createPlayerFile(player.getUniqueId());
        Bukkit.getLogger().info("[Uconomy] " + player.getName() + "님의 돈 정보를 생성하였습니다.");
        return true;
    }
}
