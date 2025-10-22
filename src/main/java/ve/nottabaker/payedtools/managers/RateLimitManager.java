package ve.nottabaker.payedtools.managers;

import org.bukkit.entity.Player;
import ve.nottabaker.payedtools.PayEdtools;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages rate limiting to prevent spam and abuse
 */
public class RateLimitManager {
    
    private final PayEdtools plugin;
    private final Map<UUID, Queue<Long>> transactionTimes;
    
    public RateLimitManager(PayEdtools plugin) {
        this.plugin = plugin;
        this.transactionTimes = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if player has exceeded rate limit
     */
    public boolean isRateLimited(Player player) {
        if (!plugin.getConfigManager().isRateLimitEnabled()) {
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        Queue<Long> times = transactionTimes.computeIfAbsent(uuid, k -> new LinkedList<>());
        
        // Remove old entries outside the time window
        long timeWindow = plugin.getConfigManager().getTimeWindow() * 1000L;
        long currentTime = System.currentTimeMillis();
        
        times.removeIf(time -> currentTime - time > timeWindow);
        
        // Check if exceeded max transactions
        int maxTransactions = plugin.getConfigManager().getMaxTransactions();
        return times.size() >= maxTransactions;
    }
    
    /**
     * Record a transaction for rate limiting
     */
    public void recordTransaction(Player player) {
        if (!plugin.getConfigManager().isRateLimitEnabled()) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        Queue<Long> times = transactionTimes.computeIfAbsent(uuid, k -> new LinkedList<>());
        times.offer(System.currentTimeMillis());
    }
    
    /**
     * Get remaining transactions in current window
     */
    public int getRemainingTransactions(Player player) {
        if (!plugin.getConfigManager().isRateLimitEnabled()) {
            return Integer.MAX_VALUE;
        }
        
        UUID uuid = player.getUniqueId();
        Queue<Long> times = transactionTimes.get(uuid);
        
        if (times == null) {
            return plugin.getConfigManager().getMaxTransactions();
        }
        
        // Clean old entries
        long timeWindow = plugin.getConfigManager().getTimeWindow() * 1000L;
        long currentTime = System.currentTimeMillis();
        times.removeIf(time -> currentTime - time > timeWindow);
        
        return plugin.getConfigManager().getMaxTransactions() - times.size();
    }
    
    /**
     * Clear rate limit data for a player
     */
    public void clearPlayer(Player player) {
        transactionTimes.remove(player.getUniqueId());
    }
    
    /**
     * Clear all rate limit data
     */
    public void clear() {
        transactionTimes.clear();
    }
}
