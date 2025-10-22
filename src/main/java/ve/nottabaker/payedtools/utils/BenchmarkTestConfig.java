package ve.nottabaker.payedtools.utils;

import java.util.List;

/**
 * Configuration for benchmark tests
 */
public class BenchmarkTestConfig extends TestConfig {
    private List<BenchmarkScenario> scenarios;
    
    public BenchmarkTestConfig(int playerCount, int concurrentThreads, int transactionsPerThread,
                              double minAmount, double maxAmount, long delayBetweenTransactions,
                              List<BenchmarkScenario> scenarios) {
        super(playerCount, concurrentThreads, transactionsPerThread, minAmount, maxAmount, delayBetweenTransactions);
        this.scenarios = scenarios;
    }
    
    public List<BenchmarkScenario> getScenarios() { return scenarios; }
}

