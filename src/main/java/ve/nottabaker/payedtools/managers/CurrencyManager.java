package ve.nottabaker.payedtools.managers;

import es.edwardbelt.edgens.iapi.EdToolsCurrencyAPI;
import org.bukkit.Bukkit;
import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.utils.BalanceCache;
import ve.nottabaker.payedtools.utils.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages currency operations and validation with caching
 */
public class CurrencyManager {
    
    private final PayEdtools plugin;
    private final EdToolsCurrencyAPI currencyAPI;
    private final BalanceCache balanceCache;
    
    // Cache for currency validation
    private final Map<String, Boolean> currencyValidationCache;
    private final Map<String, Long> cacheTimestamps;
    
    public CurrencyManager(PayEdtools plugin) {
        this.plugin = plugin;
        this.currencyAPI = plugin.getEdToolsAPI().getCurrencyAPI();
        this.balanceCache = new BalanceCache(plugin);
        this.currencyValidationCache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if a currency exists in EdTools
     */
    public boolean isCurrency(String currency) {
        if (currency == null || currency.isEmpty()) {
            return false;
        }
        
        // Check cache first if enabled
        if (plugin.getConfigManager().isCacheCurrencyValidation()) {
            Long timestamp = cacheTimestamps.get(currency);
            if (timestamp != null) {
                long cacheDuration = plugin.getConfigManager().getCacheDuration() * 1000L;
                if (System.currentTimeMillis() - timestamp < cacheDuration) {
                    Boolean cached = currencyValidationCache.get(currency);
                    if (cached != null) {
                        return cached;
                    }
                }
            }
        }
        
        // Validate with EdTools API
        try {
            currencyAPI.isCurrency(currency);
            
            // Cache the result
            if (plugin.getConfigManager().isCacheCurrencyValidation()) {
                currencyValidationCache.put(currency, true);
                cacheTimestamps.put(currency, System.currentTimeMillis());
            }
            
            return true;
        } catch (Exception e) {
            // Currency doesn't exist
            if (plugin.getConfigManager().isCacheCurrencyValidation()) {
                currencyValidationCache.put(currency, false);
                cacheTimestamps.put(currency, System.currentTimeMillis());
            }
            return false;
        }
    }
    
    /**
     * Check if a currency is blocked
     */
    public boolean isBlocked(String currency) {
        return plugin.getConfigManager().getBlockedCurrencies().contains(currency);
    }
    
    /**
     * Check if a currency is allowed (whitelist check)
     */
    public boolean isAllowed(String currency) {
        Set<String> allowed = plugin.getConfigManager().getAllowedCurrencies();
        // If whitelist is empty, all currencies are allowed
        return allowed.isEmpty() || allowed.contains(currency);
    }
    
    /**
     * Validate currency for transactions
     */
    public boolean validateCurrency(String currency) {
        if (!isCurrency(currency)) {
            Logger.debug("Currency validation failed: " + currency + " does not exist");
            return false;
        }
        
        if (isBlocked(currency)) {
            Logger.debug("Currency validation failed: " + currency + " is blocked");
            return false;
        }
        
        if (!isAllowed(currency)) {
            Logger.debug("Currency validation failed: " + currency + " is not in whitelist");
            return false;
        }
        
        return true;
    }
    
    /**
     * Get player's currency balance (with cache)
     */
    public double getBalance(UUID uuid, String currency) {
        return balanceCache.getBalance(uuid, currency);
    }
    
    /**
     * Get player's currency balance directly from API (bypass cache)
     */
    public double getBalanceDirect(UUID uuid, String currency) {
        try {
            return currencyAPI.getCurrency(uuid, currency);
        } catch (Exception e) {
            Logger.error("Error getting balance for " + uuid + " currency " + currency, e);
            return 0;
        }
    }
    
    /**
     * Add currency to player (for transactions - boosters should NOT affect this)
     */
    public void addCurrency(UUID uuid, String currency, double amount) {
        try {
            // For transactions, we want to avoid boosters affecting the amount
            // So we'll get the current balance and set the new balance directly
            double currentBalance = getBalanceDirect(uuid, currency);
            double newBalance = currentBalance + amount;
            
            currencyAPI.setCurrency(uuid, currency, newBalance);
            
            // Invalidate cache for this player and currency
            balanceCache.invalidateBalance(uuid, currency);
            
            Logger.debug("Added " + amount + " " + currency + " to " + uuid + " (total: " + newBalance + ")");
        } catch (Exception e) {
            Logger.error("Error adding currency to " + uuid, e);
            throw new RuntimeException("Failed to add currency", e);
        }
    }
    
    /**
     * Add currency to player with booster support (for other uses)
     */
    public void addCurrencyWithBoosters(UUID uuid, String currency, double amount) {
        try {
            // Try the 4-parameter version first (with affectBoosters)
            try {
                currencyAPI.addCurrency(uuid, currency, amount, true);
            } catch (NoSuchMethodError e) {
                // Fallback to 3-parameter version if the method doesn't exist
                currencyAPI.addCurrency(uuid, currency, amount);
            }
            Logger.debug("Added " + amount + " " + currency + " to " + uuid + " (with boosters)");
        } catch (Exception e) {
            Logger.error("Error adding currency to " + uuid, e);
            throw new RuntimeException("Failed to add currency", e);
        }
    }
    
    /**
     * Remove currency from player
     */
    public void removeCurrency(UUID uuid, String currency, double amount) {
        try {
            currencyAPI.removeCurrency(uuid, currency, amount);
            
            // Invalidate cache for this player and currency
            balanceCache.invalidateBalance(uuid, currency);
            
            Logger.debug("Removed " + amount + " " + currency + " from " + uuid);
        } catch (Exception e) {
            Logger.error("Error removing currency from " + uuid, e);
            throw new RuntimeException("Failed to remove currency", e);
        }
    }
    
    /**
     * Check if player has enough currency
     */
    public boolean hasEnough(UUID uuid, String currency, double amount) {
        return getBalance(uuid, currency) >= amount;
    }
    
    /**
     * Get currency display name
     */
    public String getCurrencyName(String currency) {
        try {
            return currencyAPI.getCurrencyName(currency);
        } catch (Exception e) {
            return currency;
        }
    }
    
    /**
     * Get all available currencies for autocompletion (uses config for speed)
     */
    public List<String> getAvailableCurrencies() {
        List<String> currencies = new ArrayList<>();
        
        // For autocompletion, use config list for speed, but still validate with EdTools
        Set<String> allowedCurrencies = plugin.getConfigManager().getAllowedCurrencies();
        
        if (allowedCurrencies != null && !allowedCurrencies.isEmpty()) {
            // Use the whitelist from config for autocompletion
            for (String currency : allowedCurrencies) {
                // Still validate with EdTools to ensure currency exists
                if (isCurrency(currency)) {
                    currencies.add(currency);
                }
            }
            Logger.debug("Using allowed currencies from config for autocompletion: " + currencies);
        } else {
            // If no whitelist, try common currency names
            String[] commonCurrencies = {
                "savia", "farm-coins", "mining-coins", "tokens", "gems", 
                "credits", "points", "money", "coins", "dollars", "gold",
                "silver", "bronze", "crystals", "essence", "souls"
            };
            
            for (String currency : commonCurrencies) {
                if (isCurrency(currency)) {
                    currencies.add(currency);
                }
            }
            Logger.debug("Using common currencies for autocompletion: " + currencies);
        }
        
        // If still no currencies found, add some defaults for testing
        if (currencies.isEmpty()) {
            currencies.add("savia");
            currencies.add("farm-coins");
            currencies.add("tokens");
            Logger.debug("No currencies found, using defaults for autocompletion");
        }
        
        return currencies;
    }
    
    /**
     * Get all currencies that actually exist in EdTools (for validation purposes)
     */
    public List<String> getAllEdToolsCurrencies() {
        List<String> currencies = new ArrayList<>();
        
        // Try common currency names and validate with EdTools
        String[] commonCurrencies = {
            "savia", "farm-coins", "mining-coins", "tokens", "gems", 
            "credits", "points", "money", "coins", "dollars", "gold",
            "silver", "bronze", "crystals", "essence", "souls"
        };
        
        for (String currency : commonCurrencies) {
            if (isCurrency(currency)) {
                currencies.add(currency);
            }
        }
        
        Logger.debug("All EdTools currencies found: " + currencies);
        return currencies;
    }
    
    /**
     * Clear validation cache
     */
    public void clearCache() {
        currencyValidationCache.clear();
        cacheTimestamps.clear();
        balanceCache.clear();
        Logger.debug("Currency validation cache cleared");
    }
    
    /**
     * Get balance cache instance
     */
    public BalanceCache getBalanceCache() {
        return balanceCache;
    }
    
    /**
     * Reload cache
     */
    public void reloadCache() {
        clearCache();
        // Pre-warm cache with common currencies
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            getAvailableCurrencies();
        });
    }
}
