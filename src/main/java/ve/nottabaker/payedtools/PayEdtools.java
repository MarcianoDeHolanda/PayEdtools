package ve.nottabaker.payedtools;

import es.edwardbelt.edgens.iapi.EdToolsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ve.nottabaker.payedtools.commands.PayAllCommand;
import ve.nottabaker.payedtools.commands.PayCommand;
import ve.nottabaker.payedtools.commands.PayHistoryCommand;
import ve.nottabaker.payedtools.commands.PayMetricsCommand;
import ve.nottabaker.payedtools.commands.PayReloadCommand;
import ve.nottabaker.payedtools.commands.PayStatsCommand;
import ve.nottabaker.payedtools.commands.PayTestCommand;
import ve.nottabaker.payedtools.database.DatabaseManager;
import ve.nottabaker.payedtools.listeners.PlayerListener;
import ve.nottabaker.payedtools.managers.*;
import ve.nottabaker.payedtools.utils.Logger;
import ve.nottabaker.payedtools.utils.MetricsUtil;
import ve.nottabaker.payedtools.utils.PerformanceMetrics;
import ve.nottabaker.payedtools.utils.PerformanceTester;
import ve.nottabaker.payedtools.utils.UpdateChecker;

/**
 * PayEdtools - Production-ready EdTools currency transfer addon
 * 
 * @author nottabaker
 * @version 1.0.0
 */
public class PayEdtools extends JavaPlugin {
    
    private static PayEdtools instance;
    private EdToolsAPI edToolsAPI;
    
    // Managers
    private ConfigManager configManager;
    private CurrencyManager currencyManager;
    private TransactionManager transactionManager;
    private CooldownManager cooldownManager;
    private RateLimitManager rateLimitManager;
    private ConfirmationManager confirmationManager;
    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    
    // Performance monitoring
    private PerformanceMetrics performanceMetrics;
    private PerformanceTester performanceTester;
    
    // Static instance getter - moved to top as recommended
    public static PayEdtools getInstance() {
        return instance;
    }
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize logger
        Logger.setPlugin(this);
        
        // Check for EdTools dependency
        if (!checkDependencies()) {
            getLogger().severe("EdTools plugin not found! Disabling PayEdtools...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize EdTools API
        edToolsAPI = EdToolsAPI.getInstance();
        if (edToolsAPI == null) {
            getLogger().severe("Failed to initialize EdTools API! Disabling PayEdtools...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        Logger.info("Initializing PayEdtools v" + getDescription().getVersion() + "...");
        
        // Initialize managers
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Initialize database
        if (configManager.isSaveTransactionHistory()) {
            databaseManager.initialize();
        }
        
        // Check for updates
        if (configManager.isCheckUpdates()) {
            checkForUpdates();
        }
        
        // Initialize metrics
        if (configManager.isMetricsEnabled()) {
            MetricsUtil.initialize(this);
        }
        
        Logger.info("PayEdtools has been enabled successfully!");
        Logger.info("Author: nottabaker");
        Logger.info("Loaded " + currencyManager.getAvailableCurrencies().size() + " currencies from EdTools");
    }
    
    @Override
    public void onDisable() {
        Logger.info("Disabling PayEdtools...");
        
        // Cancel all pending confirmations
        if (confirmationManager != null) {
            confirmationManager.cleanup();
        }
        
        // Close database connections
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        // Cleanup managers
        if (transactionManager != null) {
            transactionManager.shutdown();
        }
        
        // Shutdown performance tester
        if (performanceTester != null) {
            performanceTester.shutdown();
        }
        
        Logger.info("PayEdtools has been disabled successfully!");
    }
    
    /**
     * Check for required dependencies
     */
    private boolean checkDependencies() {
        return getServer().getPluginManager().getPlugin("EdTools") != null;
    }
    
    /**
     * Initialize all managers
     */
    private void initializeManagers() {
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        currencyManager = new CurrencyManager(this);
        transactionManager = new TransactionManager(this);
        cooldownManager = new CooldownManager(this);
        rateLimitManager = new RateLimitManager(this);
        confirmationManager = new ConfirmationManager(this);
        databaseManager = new DatabaseManager(this);
        performanceMetrics = new PerformanceMetrics(this);
        performanceTester = new PerformanceTester(this);
        
        Logger.debug("All managers initialized successfully");
    }
    
    /**
     * Register plugin commands
     */
    private void registerCommands() {
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("payall").setExecutor(new PayAllCommand(this));
        getCommand("payreload").setExecutor(new PayReloadCommand(this));
        getCommand("payhistory").setExecutor(new PayHistoryCommand(this));
        getCommand("paystats").setExecutor(new PayStatsCommand(this));
        getCommand("paymetrics").setExecutor(new PayMetricsCommand(this));
        getCommand("paytest").setExecutor(new PayTestCommand(this));
        
        Logger.debug("Commands registered successfully");
    }
    
    /**
     * Register event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        Logger.debug("Listeners registered successfully");
    }
    
    /**
     * Check for plugin updates
     */
    private void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            UpdateChecker checker = new UpdateChecker(this);
            if (checker.checkForUpdates()) {
                Logger.info("A new version is available! Download it from: " + checker.getDownloadUrl());
            }
        });
    }
    
    /**
     * Reload plugin configuration and managers
     */
    public void reload() {
        try {
            // Backup before reload if enabled
            if (configManager.isBackupOnReload()) {
                databaseManager.backup();
            }
            
            // Reload configuration
            configManager.reload();
            messageManager.reload();
            
            // Reload currency cache
            currencyManager.reloadCache();
            
            // Clear cooldowns if needed
            cooldownManager.clear();
            
            Logger.info("Plugin reloaded successfully!");
        } catch (Exception e) {
            Logger.error("Error during reload: " + e.getMessage(), e);
            throw e;
        }
    }
    
    // Getters
    
    public EdToolsAPI getEdToolsAPI() {
        return edToolsAPI;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }
    
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public RateLimitManager getRateLimitManager() {
        return rateLimitManager;
    }
    
    public ConfirmationManager getConfirmationManager() {
        return confirmationManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }
    
    public PerformanceTester getPerformanceTester() {
        return performanceTester;
    }
}
