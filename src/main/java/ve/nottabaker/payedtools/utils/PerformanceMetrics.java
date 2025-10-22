package ve.nottabaker.payedtools.utils;

import ve.nottabaker.payedtools.PayEdtools;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance metrics and monitoring for PayEdtools
 */
public class PerformanceMetrics {
    
    @SuppressWarnings("unused")
    private final PayEdtools plugin;
    
    // Transaction metrics
    private final AtomicLong totalTransactions = new AtomicLong();
    private final AtomicLong successfulTransactions = new AtomicLong();
    private final AtomicLong failedTransactions = new AtomicLong();
    private final AtomicLong totalProcessingTime = new AtomicLong();
    private final AtomicLong averageProcessingTime = new AtomicLong();
    
    // Currency statistics
    private final Map<String, AtomicLong> currencyTransactionCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> currencyTotalAmount = new ConcurrentHashMap<>();
    
    // Player statistics
    private final Map<UUID, AtomicLong> playerTransactionCount = new ConcurrentHashMap<>();
    
    // Performance counters
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong databaseOperations = new AtomicLong();
    private final AtomicLong batchOperations = new AtomicLong();
    
    // Rate limiting statistics
    private final AtomicLong rateLimitHits = new AtomicLong();
    private final AtomicLong cooldownHits = new AtomicLong();
    
    public PerformanceMetrics(PayEdtools plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Record a transaction processing
     */
    public void recordTransaction(String currency, UUID sender, UUID receiver, 
                                double amount, boolean success, long processingTime) {
        totalTransactions.incrementAndGet();
        totalProcessingTime.addAndGet(processingTime);
        
        if (success) {
            successfulTransactions.incrementAndGet();
            
            // Update currency stats
            currencyTransactionCount.computeIfAbsent(currency, k -> new AtomicLong()).incrementAndGet();
            currencyTotalAmount.computeIfAbsent(currency, k -> new AtomicLong()).addAndGet((long) amount);
            
            // Update player stats
            playerTransactionCount.computeIfAbsent(sender, k -> new AtomicLong()).incrementAndGet();
            playerTransactionCount.computeIfAbsent(receiver, k -> new AtomicLong()).incrementAndGet();
        } else {
            failedTransactions.incrementAndGet();
        }
        
        // Update average processing time
        long currentTotal = totalTransactions.get();
        averageProcessingTime.set(totalProcessingTime.get() / currentTotal);
    }
    
    /**
     * Record cache hit
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }
    
    /**
     * Record cache miss
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }
    
    /**
     * Record database operation
     */
    public void recordDatabaseOperation() {
        databaseOperations.incrementAndGet();
    }
    
    /**
     * Record batch operation
     */
    public void recordBatchOperation(int batchSize) {
        batchOperations.addAndGet(batchSize);
    }
    
    /**
     * Record rate limit hit
     */
    public void recordRateLimitHit() {
        rateLimitHits.incrementAndGet();
    }
    
    /**
     * Record cooldown hit
     */
    public void recordCooldownHit() {
        cooldownHits.incrementAndGet();
    }
    
    /**
     * Get cache hit ratio
     */
    public double getCacheHitRatio() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    /**
     * Get average processing time in milliseconds
     */
    public long getAverageProcessingTime() {
        return averageProcessingTime.get();
    }
    
    /**
     * Get total transactions processed
     */
    public long getTotalTransactions() {
        return totalTransactions.get();
    }
    
    /**
     * Get success rate
     */
    public double getSuccessRate() {
        long total = totalTransactions.get();
        return total > 0 ? (double) successfulTransactions.get() / total : 0.0;
    }
    
    /**
     * Get top currencies by transaction count
     */
    public Map<String, Long> getTopCurrencies(int limit) {
        return currencyTransactionCount.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get(),
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }
    
    /**
     * Get top currencies by total amount
     */
    public Map<String, Long> getTopCurrenciesByAmount(int limit) {
        return currencyTotalAmount.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get(),
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }
    
    /**
     * Get most active players
     */
    public Map<UUID, Long> getMostActivePlayers(int limit) {
        return playerTransactionCount.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get(),
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }
    
    /**
     * Reset all metrics
     */
    public void reset() {
        totalTransactions.set(0);
        successfulTransactions.set(0);
        failedTransactions.set(0);
        totalProcessingTime.set(0);
        averageProcessingTime.set(0);
        
        currencyTransactionCount.clear();
        currencyTotalAmount.clear();
        playerTransactionCount.clear();
        
        cacheHits.set(0);
        cacheMisses.set(0);
        databaseOperations.set(0);
        batchOperations.set(0);
        rateLimitHits.set(0);
        cooldownHits.set(0);
    }
    
    /**
     * Get performance summary
     */
    public String getPerformanceSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("§6=== PayEdtools Performance Metrics ===\n");
        summary.append("§eTotal Transactions: §a").append(getTotalTransactions()).append("\n");
        summary.append("§eSuccess Rate: §a").append(String.format("%.2f%%", getSuccessRate() * 100)).append("\n");
        summary.append("§eAvg Processing Time: §a").append(getAverageProcessingTime()).append("ms\n");
        summary.append("§eCache Hit Ratio: §a").append(String.format("%.2f%%", getCacheHitRatio() * 100)).append("\n");
        summary.append("§eDatabase Operations: §a").append(databaseOperations.get()).append("\n");
        summary.append("§eBatch Operations: §a").append(batchOperations.get()).append("\n");
        summary.append("§eRate Limit Hits: §c").append(rateLimitHits.get()).append("\n");
        summary.append("§eCooldown Hits: §c").append(cooldownHits.get());
        
        return summary.toString();
    }
}
