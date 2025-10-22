package ve.nottabaker.payedtools.utils;

import ve.nottabaker.payedtools.PayEdtools;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for player balances to reduce API calls
 */
public class BalanceCache {
    
    private final PayEdtools plugin;
    private final Map<UUID, Map<String, CachedBalance>> balanceCache;
    private final long cacheDuration;
    
    public BalanceCache(PayEdtools plugin) {
        this.plugin = plugin;
        this.balanceCache = new ConcurrentHashMap<>();
        this.cacheDuration = plugin.getConfigManager().getBalanceCacheDuration() * 1000L; // Convert to milliseconds
    }
    
    /**
     * Get cached balance or fetch from API
     */
    public double getBalance(UUID uuid, String currency) {
        // Check if balance cache is enabled
        if (!plugin.getConfigManager().isBalanceCacheEnabled()) {
            return plugin.getCurrencyManager().getBalanceDirect(uuid, currency);
        }
        
        Map<String, CachedBalance> playerCache = balanceCache.get(uuid);
        
        if (playerCache != null) {
            CachedBalance cached = playerCache.get(currency);
            if (cached != null && !cached.isExpired()) {
                plugin.getPerformanceMetrics().recordCacheHit();
                return cached.getBalance();
            }
        }
        
        // Cache miss - fetch from API
        plugin.getPerformanceMetrics().recordCacheMiss();
        double balance = plugin.getCurrencyManager().getBalanceDirect(uuid, currency);
        cacheBalance(uuid, currency, balance);
        
        return balance;
    }
    
    /**
     * Cache a balance value
     */
    public void cacheBalance(UUID uuid, String currency, double balance) {
        balanceCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                   .put(currency, new CachedBalance(balance, System.currentTimeMillis()));
    }
    
    /**
     * Invalidate cache for a specific player and currency
     */
    public void invalidateBalance(UUID uuid, String currency) {
        Map<String, CachedBalance> playerCache = balanceCache.get(uuid);
        if (playerCache != null) {
            playerCache.remove(currency);
        }
    }
    
    /**
     * Invalidate all cache for a player
     */
    public void invalidatePlayer(UUID uuid) {
        balanceCache.remove(uuid);
    }
    
    /**
     * Clear all expired cache entries
     */
    public void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        
        balanceCache.entrySet().removeIf(entry -> {
            Map<String, CachedBalance> playerCache = entry.getValue();
            playerCache.entrySet().removeIf(cacheEntry -> cacheEntry.getValue().isExpired(currentTime));
            return playerCache.isEmpty();
        });
    }
    
    /**
     * Clear cache for a specific player
     */
    public void clearPlayerCache(UUID playerUUID) {
        balanceCache.remove(playerUUID);
        Logger.debug("Cleared balance cache for player " + playerUUID);
    }
    
    /**
     * Clear all cache
     */
    public void clear() {
        balanceCache.clear();
    }
    
    /**
     * Get cache size
     */
    public int getCacheSize() {
        return balanceCache.size();
    }
    
    /**
     * Cached balance entry
     */
    private class CachedBalance {
        private final double balance;
        private final long timestamp;
        
        public CachedBalance(double balance, long timestamp) {
            this.balance = balance;
            this.timestamp = timestamp;
        }
        
        public double getBalance() {
            return balance;
        }
        
        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }
        
        public boolean isExpired(long currentTime) {
            return currentTime - timestamp > cacheDuration;
        }
    }
}
