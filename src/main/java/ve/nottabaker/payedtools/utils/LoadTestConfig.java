package ve.nottabaker.payedtools.utils;

/**
 * Configuration for load tests
 */
public class LoadTestConfig extends TestConfig {
    private int initialLoad;
    private int maxLoad;
    private int loadIncrement;
    private long loadIncreaseDelay;
    
    public LoadTestConfig(int playerCount, int concurrentThreads, int initialLoad, int maxLoad, 
                         int loadIncrement, long loadIncreaseDelay, int transactionsPerThread,
                         double minAmount, double maxAmount, long delayBetweenTransactions) {
        super(playerCount, concurrentThreads, transactionsPerThread, minAmount, maxAmount, delayBetweenTransactions);
        this.initialLoad = initialLoad;
        this.maxLoad = maxLoad;
        this.loadIncrement = loadIncrement;
        this.loadIncreaseDelay = loadIncreaseDelay;
    }
    
    public int getInitialLoad() { return initialLoad; }
    public int getMaxLoad() { return maxLoad; }
    public int getLoadIncrement() { return loadIncrement; }
    public long getLoadIncreaseDelay() { return loadIncreaseDelay; }
}

