package ve.nottabaker.payedtools.utils;

/**
 * Base configuration for performance tests
 */
public abstract class TestConfig {
    protected int playerCount;
    protected int concurrentThreads;
    protected int transactionsPerThread;
    protected double minAmount;
    protected double maxAmount;
    protected long delayBetweenTransactions;
    
    public TestConfig(int playerCount, int concurrentThreads, int transactionsPerThread, 
                     double minAmount, double maxAmount, long delayBetweenTransactions) {
        this.playerCount = playerCount;
        this.concurrentThreads = concurrentThreads;
        this.transactionsPerThread = transactionsPerThread;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.delayBetweenTransactions = delayBetweenTransactions;
    }
    
    // Getters
    public int getPlayerCount() { return playerCount; }
    public int getConcurrentThreads() { return concurrentThreads; }
    public int getTransactionsPerThread() { return transactionsPerThread; }
    public double getMinAmount() { return minAmount; }
    public double getMaxAmount() { return maxAmount; }
    public long getDelayBetweenTransactions() { return delayBetweenTransactions; }
}