package ve.nottabaker.payedtools.utils;

/**
 * Benchmark scenario configuration
 */
public class BenchmarkScenario {
    private String name;
    private int concurrentTransactions;
    private int transactionsPerThread;
    private double amount;
    private long delayBetweenTransactions;
    private long delayAfterScenario;
    
    public BenchmarkScenario(String name, int concurrentTransactions, int transactionsPerThread,
                           double amount, long delayBetweenTransactions, long delayAfterScenario) {
        this.name = name;
        this.concurrentTransactions = concurrentTransactions;
        this.transactionsPerThread = transactionsPerThread;
        this.amount = amount;
        this.delayBetweenTransactions = delayBetweenTransactions;
        this.delayAfterScenario = delayAfterScenario;
    }
    
    // Getters
    public String getName() { return name; }
    public int getConcurrentTransactions() { return concurrentTransactions; }
    public int getTransactionsPerThread() { return transactionsPerThread; }
    public double getAmount() { return amount; }
    public long getDelayBetweenTransactions() { return delayBetweenTransactions; }
    public long getDelayAfterScenario() { return delayAfterScenario; }
}

