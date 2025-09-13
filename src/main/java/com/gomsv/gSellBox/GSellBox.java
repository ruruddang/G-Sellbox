package com.gomsv.gSellBox;

import com.gomsv.gSellBox.command.SellRootCommand;
import com.gomsv.gSellBox.core.PriceCacheManager;
import com.gomsv.gSellBox.core.SellManager;
import com.gomsv.gSellBox.data.PriceDAO;
import com.gomsv.gSellBox.data.SellCacheStore;
import com.gomsv.gSellBox.gui.GuiHandler;
import com.gomsv.gSellBox.gui.SellBoxInfoListener;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GSellBox extends JavaPlugin {
    private static GSellBox instance;
    private static Economy economy;
    private static PriceCacheManager priceCacheManager;
    private DataSource dataSource;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.dataSource = createDataSource(getConfig());
        if (this.dataSource == null) {
            getLogger().severe("데이터베이스 연결 실패. 플러그인을 비활성화합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupEconomy()) {
            getLogger().severe("Vault 연동 실패! 플러그인 비활성화.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PriceDAO priceDAO = new PriceDAO(dataSource);
        priceDAO.createTableIfNotExists();
        priceCacheManager = new PriceCacheManager(priceDAO);

        Bukkit.getPluginManager().registerEvents(new GuiHandler(), this);
        Bukkit.getPluginManager().registerEvents(new SellBoxInfoListener(), this);

        SellRootCommand root = new SellRootCommand(this);
        getCommand("판매상자").setExecutor(root);
        getCommand("판매상자").setTabCompleter(root);

        SellManager.setAllCache(SellCacheStore.loadAllFromDisk());

        getLogger().info("G-SellBox 활성화 완료.");
    }

    @Override
    public void onDisable() {
        SellCacheStore.saveAllToDisk(SellManager.getAllCache());
        SellManager.clearCache();

        // (1) Hikari 풀 정리
        if (this.dataSource instanceof HikariDataSource ds) {
            try {
                ds.close();
            } catch (Exception ignored) {}
        }
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    private DataSource createDataSource(FileConfiguration cfg) {
        try {
            HikariConfig hc = new HikariConfig();
            hc.setJdbcUrl(cfg.getString("database.jdbc-url"));
            hc.setUsername(cfg.getString("database.username"));
            hc.setPassword(cfg.getString("database.password"));
            hc.setMaximumPoolSize(cfg.getInt("database.pool.maximumPoolSize", 10));
            hc.setMinimumIdle(cfg.getInt("database.pool.minimumIdle", 2));
            hc.setIdleTimeout(cfg.getLong("database.pool.idleTimeout", 30000));
            hc.setMaxLifetime(cfg.getLong("database.pool.maxLifetime", 1800000));
            hc.setConnectionTimeout(cfg.getLong("database.pool.connectionTimeout", 3000));
            hc.setDriverClassName("com.mysql.cj.jdbc.Driver");

            Logger.getLogger("com.zaxxer.hikari").setLevel(Level.SEVERE);
            return new HikariDataSource(hc);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "HikariCP DataSource 생성 실패", e);
            return null;
        }
    }

    public static GSellBox getInstance() { return instance; }
    public DataSource getDataSource() { return this.dataSource; }
    public static Economy getEconomy() { return economy; }
    public static PriceCacheManager getPriceCacheManager() { return priceCacheManager; }
}
