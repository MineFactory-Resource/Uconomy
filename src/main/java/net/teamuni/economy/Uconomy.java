package net.teamuni.economy;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.teamuni.economy.command.CommandTabCompleter;
import net.teamuni.economy.command.UconomyCmd;
import net.teamuni.economy.config.MessageManager;
import net.teamuni.economy.data.MoneyManager;
import net.teamuni.economy.event.JoinEvent;
import net.teamuni.economy.hooks.HookIntoVault;
import net.teamuni.economy.data.EconomyManager;
import net.teamuni.economy.hooks.UconomyPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Uconomy extends JavaPlugin {
    private HookIntoVault hookIntoVault;
    private EconomyManager economyManager;
    private MessageManager messageManager;
    private MoneyManager moneyManager;

    @Override
    public void onEnable() {
        this.economyManager = new EconomyManager(this);
        this.hookIntoVault = new HookIntoVault(this);
        this.messageManager = new MessageManager(this);
        this.moneyManager = new MoneyManager(this);
        this.hookIntoVault.hook();
        this.messageManager.createMessagesYml();
        this.moneyManager.createMoneyDataYml();
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(new JoinEvent(this), this);
        getCommand("돈").setExecutor(new UconomyCmd(this));
        getCommand("uconomy").setExecutor(new UconomyCmd(this));
        getCommand("돈").setTabCompleter(new CommandTabCompleter());
        getCommand("uconomy").setTabCompleter(new CommandTabCompleter());
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new UconomyPlaceholders().register();
        }
        if (!setupEconomy()) {
            getLogger().info("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        this.moneyManager.save();
        this.hookIntoVault.unhook();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        return rsp != null;
    }
}