package ve.nottabaker.payedtools.managers;

import org.bukkit.configuration.file.FileConfiguration;
import ve.nottabaker.payedtools.PayEdtools;

import java.util.*;

/**
 * Manages plugin configuration with validation and caching
 */
public class ConfigManager {
    
    private final PayEdtools plugin;
    private FileConfiguration config;
    
    // Cached values for performance
    private boolean debug;
    private boolean saveTransactionHistory;
    private int maxHistorySize;
    private int cleanupAfterDays;
    private boolean asyncOperations;
    
    // Currency formats
    private boolean currencyFormatsEnabled;
    private Map<String, Double> currencyFormats;
    private boolean allowDecimals;
    
    // Cooldown
    private boolean cooldownEnabled;
    private int cooldownTime;
    
    // Limits
    private boolean limitsEnabled;
    private double minimumAmount;
    private double maximumAmount;
    
    // Rate limiting
    private boolean rateLimitEnabled;
    private int maxTransactions;
    private int timeWindow;
    private String rateLimitAction;
    
    // Confirmation
    private boolean confirmationEnabled;
    private double confirmationThreshold;
    private int confirmationTimeout;
    
    // Blocked/Allowed currencies
    private Set<String> blockedCurrencies;
    private Set<String> allowedCurrencies;
    
    // General settings
    private boolean allowSelfTransfer;
    private boolean allowOfflineTransfers;
    
    // Tax
    private boolean taxEnabled;
    private double taxPercentage;
    private double taxFixed;
    private double minimumForTax;
    
    // Logging
    private boolean consoleLogging;
    private boolean fileLogging;
    private String logFilePath;
    private String logFormat;
    
    // Performance
    private boolean cacheCurrencyValidation;
    private int cacheDuration;
    private boolean balanceCacheEnabled;
    private int balanceCacheDuration;
    private int balanceCacheCleanupInterval;
    private boolean batchDatabaseOperations;
    private int batchSize;
    private int threadPoolSize;
    
    // PayAll settings
    private int payallBatchSize;
    private int payallMaxPlayers;
    private int payallCacheUpdateInterval;
    
    // Advanced
    private boolean checkUpdates;
    private boolean metrics;
    private boolean backupOnReload;
    private int decimalPlaces;
    private String numberFormatLocale;
    
    // Integration
    private boolean placeholderAPIEnabled;
    private boolean discordEnabled;
    private String discordWebhook;
    private double discordLogThreshold;
    
    // Database
    private String databaseType;
    private String sqliteFilename;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private int mysqlMaxPoolSize;
    private int mysqlMinIdle;
    private int mysqlConnectionTimeout;
    
    public ConfigManager(PayEdtools plugin) {
        this.plugin = plugin;
        loadConfiguration();
    }
    
    /**
     * Get the ConfigManager instance from the plugin
     */
    public static ConfigManager getInstance() {
        return PayEdtools.getInstance().getConfigManager();
    }
    
    /**
     * Load and cache configuration values
     */
    public void loadConfiguration() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // General settings
        debug = config.getBoolean("settings.debug", false);
        saveTransactionHistory = config.getBoolean("settings.save-transaction-history", true);
        maxHistorySize = config.getInt("settings.max-history-size", 100);
        cleanupAfterDays = config.getInt("settings.cleanup-after-days", 30);
        asyncOperations = config.getBoolean("settings.async-operations", true);
        
        // Currency formats
        currencyFormatsEnabled = config.getBoolean("currency-formats.enabled", true);
        currencyFormats = new HashMap<>();
        if (config.isConfigurationSection("currency-formats.formats")) {
            for (String key : config.getConfigurationSection("currency-formats.formats").getKeys(false)) {
                double value = config.getDouble("currency-formats.formats." + key);
                currencyFormats.put(key.toLowerCase(), value);
            }
        } else {
            // Default formats
            currencyFormats.put("k", 1000.0);
            currencyFormats.put("m", 1000000.0);
            currencyFormats.put("b", 1000000000.0);
            currencyFormats.put("t", 1000000000000.0);
        }
        allowDecimals = config.getBoolean("currency-formats.allow-decimals", true);
        
        // Cooldown
        cooldownEnabled = config.getBoolean("cooldown.enabled", true);
        cooldownTime = config.getInt("cooldown.time", 5);
        
        // Limits
        limitsEnabled = config.getBoolean("limits.enabled", true);
        minimumAmount = config.getDouble("limits.minimum", 1.0);
        maximumAmount = config.getDouble("limits.maximum", 0);
        
        // Rate limiting
        rateLimitEnabled = config.getBoolean("rate-limit.enabled", true);
        maxTransactions = config.getInt("rate-limit.max-transactions", 10);
        timeWindow = config.getInt("rate-limit.time-window", 60);
        rateLimitAction = config.getString("rate-limit.action", "DENY");
        
        // Confirmation
        confirmationEnabled = config.getBoolean("confirmation.enabled", true);
        confirmationThreshold = config.getDouble("confirmation.threshold", 1000000);
        confirmationTimeout = config.getInt("confirmation.timeout", 30);
        
        // Blocked/Allowed currencies
        blockedCurrencies = new HashSet<>(config.getStringList("blocked-currencies"));
        allowedCurrencies = new HashSet<>(config.getStringList("allowed-currencies"));
        
        // General
        allowSelfTransfer = config.getBoolean("allow-self-transfer", false);
        allowOfflineTransfers = config.getBoolean("allow-offline-transfers", true);
        
        // Tax
        taxEnabled = config.getBoolean("tax.enabled", false);
        taxPercentage = config.getDouble("tax.percentage", 0);
        taxFixed = config.getDouble("tax.fixed", 0);
        minimumForTax = config.getDouble("tax.minimum-for-tax", 0);
        
        // Logging
        consoleLogging = config.getBoolean("logging.console", true);
        fileLogging = config.getBoolean("logging.file", true);
        logFilePath = config.getString("logging.file-path", "transactions.log");
        logFormat = config.getString("logging.format", "[%timestamp%] %sender% -> %receiver%: %amount% %currency%");
        
        // Performance
        cacheCurrencyValidation = config.getBoolean("performance.cache-currency-validation", true);
        cacheDuration = config.getInt("performance.cache-duration", 300);
        
        // Balance cache
        balanceCacheEnabled = config.getBoolean("performance.balance-cache.enabled", true);
        balanceCacheDuration = config.getInt("performance.balance-cache.duration", 30);
        balanceCacheCleanupInterval = config.getInt("performance.balance-cache.cleanup-interval", 300);
        
        batchDatabaseOperations = config.getBoolean("performance.batch-database-operations", true);
        batchSize = config.getInt("performance.batch-size", 200);
        threadPoolSize = config.getInt("performance.thread-pool-size", 12);
        
        // PayAll settings
        payallBatchSize = config.getInt("performance.payall.batch-size", 10);
        payallMaxPlayers = config.getInt("performance.payall.max-players", 100);
        payallCacheUpdateInterval = config.getInt("performance.payall.cache-update-interval", 2000);
        
        // Advanced
        checkUpdates = config.getBoolean("advanced.check-updates", true);
        metrics = config.getBoolean("advanced.metrics", true);
        backupOnReload = config.getBoolean("advanced.backup-on-reload", true);
        decimalPlaces = config.getInt("advanced.decimal-places", 2);
        numberFormatLocale = config.getString("advanced.number-format-locale", "en_US");
        
        // Integration
        placeholderAPIEnabled = config.getBoolean("integrations.placeholderapi.enabled", true);
        discordEnabled = config.getBoolean("integrations.discord.enabled", false);
        discordWebhook = config.getString("integrations.discord.webhook-url", "");
        discordLogThreshold = config.getDouble("integrations.discord.log-threshold", 1000000);
        
        // Database
        databaseType = config.getString("database.type", "SQLITE");
        sqliteFilename = config.getString("database.sqlite.filename", "transactions.db");
        mysqlHost = config.getString("database.mysql.host", "localhost");
        mysqlPort = config.getInt("database.mysql.port", 3306);
        mysqlDatabase = config.getString("database.mysql.database", "payedtools");
        mysqlUsername = config.getString("database.mysql.username", "root");
        mysqlPassword = config.getString("database.mysql.password", "password");
        mysqlMaxPoolSize = config.getInt("database.mysql.pool.maximum-pool-size", 10);
        mysqlMinIdle = config.getInt("database.mysql.pool.minimum-idle", 2);
        mysqlConnectionTimeout = config.getInt("database.mysql.pool.connection-timeout", 30000);
        
        validateConfiguration();
    }
    
    /**
     * Validate configuration values
     */
    private void validateConfiguration() {
        if (cooldownTime < 0) cooldownTime = 0;
        if (minimumAmount < 0) minimumAmount = 0;
        if (maxTransactions < 1) maxTransactions = 1;
        if (timeWindow < 1) timeWindow = 1;
        if (confirmationTimeout < 1) confirmationTimeout = 30;
        if (cacheDuration < 0) cacheDuration = 300;
        if (threadPoolSize < 1) threadPoolSize = 1;
        if (decimalPlaces < 0) decimalPlaces = 2;
    }
    
    /**
     * Reload configuration
     */
    public void reload() {
        plugin.reloadConfig();
        loadConfiguration();
    }
    
    // Getters
    
    public boolean isDebug() {
        return debug;
    }
    
    public boolean isSaveTransactionHistory() {
        return saveTransactionHistory;
    }
    
    public int getMaxHistorySize() {
        return maxHistorySize;
    }
    
    public int getCleanupAfterDays() {
        return cleanupAfterDays;
    }
    
    public boolean isAsyncOperations() {
        return asyncOperations;
    }
    
    public boolean isCurrencyFormatsEnabled() {
        return currencyFormatsEnabled;
    }
    
    public Map<String, Double> getCurrencyFormats() {
        return currencyFormats;
    }
    
    public boolean isAllowDecimals() {
        return allowDecimals;
    }
    
    public boolean isCooldownEnabled() {
        return cooldownEnabled;
    }
    
    public int getCooldownTime() {
        return cooldownTime;
    }
    
    public boolean isLimitsEnabled() {
        return limitsEnabled;
    }
    
    public double getMinimumAmount() {
        return minimumAmount;
    }
    
    public double getMaximumAmount() {
        return maximumAmount;
    }
    
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }
    
    public int getMaxTransactions() {
        return maxTransactions;
    }
    
    public int getTimeWindow() {
        return timeWindow;
    }
    
    public String getRateLimitAction() {
        return rateLimitAction;
    }
    
    public boolean isConfirmationEnabled() {
        return confirmationEnabled;
    }
    
    public double getConfirmationThreshold() {
        return confirmationThreshold;
    }
    
    public int getConfirmationTimeout() {
        return confirmationTimeout;
    }
    
    public Set<String> getBlockedCurrencies() {
        return blockedCurrencies;
    }
    
    public Set<String> getAllowedCurrencies() {
        return allowedCurrencies;
    }
    
    public boolean isAllowSelfTransfer() {
        return allowSelfTransfer;
    }
    
    public boolean isAllowOfflineTransfers() {
        return allowOfflineTransfers;
    }
    
    public boolean isTaxEnabled() {
        return taxEnabled;
    }
    
    public double getTaxPercentage() {
        return taxPercentage;
    }
    
    public double getTaxFixed() {
        return taxFixed;
    }
    
    public double getMinimumForTax() {
        return minimumForTax;
    }
    
    public boolean isConsoleLogging() {
        return consoleLogging;
    }
    
    public boolean isFileLogging() {
        return fileLogging;
    }
    
    public String getLogFilePath() {
        return logFilePath;
    }
    
    public String getLogFormat() {
        return logFormat;
    }
    
    public boolean isCacheCurrencyValidation() {
        return cacheCurrencyValidation;
    }
    
    public int getCacheDuration() {
        return cacheDuration;
    }
    
    public boolean isBatchDatabaseOperations() {
        return batchDatabaseOperations;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    public boolean isCheckUpdates() {
        return checkUpdates;
    }
    
    public boolean isMetricsEnabled() {
        return metrics;
    }
    
    public boolean isBackupOnReload() {
        return backupOnReload;
    }
    
    public int getDecimalPlaces() {
        return decimalPlaces;
    }
    
    public String getNumberFormatLocale() {
        return numberFormatLocale;
    }
    
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled && plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }
    
    public boolean isDiscordEnabled() {
        return discordEnabled;
    }
    
    public String getDiscordWebhook() {
        return discordWebhook;
    }
    
    public double getDiscordLogThreshold() {
        return discordLogThreshold;
    }
    
    public String getDatabaseType() {
        return databaseType;
    }
    
    public String getSqliteFilename() {
        return sqliteFilename;
    }
    
    public String getMysqlHost() {
        return mysqlHost;
    }
    
    public int getMysqlPort() {
        return mysqlPort;
    }
    
    public String getMysqlDatabase() {
        return mysqlDatabase;
    }
    
    public String getMysqlUsername() {
        return mysqlUsername;
    }
    
    public String getMysqlPassword() {
        return mysqlPassword;
    }
    
    public int getMysqlMaxPoolSize() {
        return mysqlMaxPoolSize;
    }
    
    public int getMysqlMinIdle() {
        return mysqlMinIdle;
    }
    
    public int getMysqlConnectionTimeout() {
        return mysqlConnectionTimeout;
    }
    
    public boolean isBalanceCacheEnabled() {
        return balanceCacheEnabled;
    }
    
    public int getBalanceCacheDuration() {
        return balanceCacheDuration;
    }
    
    public int getBalanceCacheCleanupInterval() {
        return balanceCacheCleanupInterval;
    }
    
    public int getPayallBatchSize() {
        return payallBatchSize;
    }
    
    public int getPayallMaxPlayers() {
        return payallMaxPlayers;
    }
    
    public int getPayallCacheUpdateInterval() {
        return payallCacheUpdateInterval;
    }
}
