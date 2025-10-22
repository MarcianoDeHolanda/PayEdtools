package ve.nottabaker.payedtools.utils;

/**
 * Test result data
 */
public class TestResult {
    private String testId;
    private TestConfig config;
    private long totalTime;
    private int totalTransactions;
    private int successfulTransactions;
    private int failedTransactions;
    private long totalProcessingTime;
    
    public TestResult(String testId, TestConfig config, long totalTime, int totalTransactions,
                     int successfulTransactions, int failedTransactions, long totalProcessingTime) {
        this.testId = testId;
        this.config = config;
        this.totalTime = totalTime;
        this.totalTransactions = totalTransactions;
        this.successfulTransactions = successfulTransactions;
        this.failedTransactions = failedTransactions;
        this.totalProcessingTime = totalProcessingTime;
    }
    
    // Getters
    public String getTestId() { return testId; }
    public TestConfig getConfig() { return config; }
    public long getTotalTime() { return totalTime; }
    public int getTotalTransactions() { return totalTransactions; }
    public int getSuccessfulTransactions() { return successfulTransactions; }
    public int getFailedTransactions() { return failedTransactions; }
    public long getTotalProcessingTime() { return totalProcessingTime; }
    
    // Calculated metrics
    public double getSuccessRate() {
        return totalTransactions > 0 ? (double) successfulTransactions / totalTransactions : 0.0;
    }
    
    public double getTransactionsPerSecond() {
        return totalTime > 0 ? (double) totalTransactions / (totalTime / 1000.0) : 0.0;
    }
    
    public double getAverageProcessingTime() {
        return totalTransactions > 0 ? (double) totalProcessingTime / totalTransactions : 0.0;
    }
    
    public String getSummary() {
        return String.format(
            "Test: %s | Time: %dms | Transactions: %d | Success: %.2f%% | TPS: %.2f | Avg Time: %.2fms",
            testId, totalTime, totalTransactions, getSuccessRate() * 100, 
            getTransactionsPerSecond(), getAverageProcessingTime()
        );
    }
}

