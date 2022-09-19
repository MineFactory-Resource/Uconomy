package net.teamuni.economy.hooks;

import net.milkbowl.vault.economy.Economy;
import net.teamuni.economy.Uconomy;
import net.teamuni.economy.data.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;

public class HookIntoVault {
    private final Uconomy plugin = Uconomy.getInstance();
    private final EconomyManager manager = Uconomy.getEconomyManager();
    private Economy provider;

    public void hook() {
        provider = manager;
        Bukkit.getServicesManager().register(Economy.class, provider, this.plugin, ServicePriority.High);
        Bukkit.getLogger().info("[Uconomy] Vault is hooked into " + plugin.getName() + " sucessfully!");
    }

    public void unhook() {
        Bukkit.getServicesManager().unregister(Economy.class, this.provider);
        Bukkit.getLogger().info("[Uconomy] Vault is unhooked from " + plugin.getName() + " sucessfully!");
    }
}