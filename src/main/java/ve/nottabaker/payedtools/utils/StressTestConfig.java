package ve.nottabaker.payedtools.utils;

/**
 * Configuration for stress tests
 */
public class StressTestConfig extends TestConfig {
    public StressTestConfig(int playerCount, int concurrentThreads, int transactionsPerThread, 
                           double minAmount, double maxAmount, long delayBetweenTransactions) {
        super(playerCount, concurrentThreads, transactionsPerThread, minAmount, maxAmount, delayBetweenTransactions);
    }
}

