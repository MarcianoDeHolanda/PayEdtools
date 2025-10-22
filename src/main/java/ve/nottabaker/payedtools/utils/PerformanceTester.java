package ve.nottabaker.payedtools.utils;

import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.managers.TransactionManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance testing system for PayEdtools
 * Simulates multiple players executing transactions to test performance
 */
public class PerformanceTester {
    
    private final PayEdtools plugin;
    private final ExecutorService testExecutor;
    private final Random random = new Random();
    
    // Test configuration
    private volatile boolean isRunning = false;
    private volatile int activeTests = 0;
    private final AtomicInteger totalTransactions = new AtomicInteger(0);
    private final AtomicInteger successfulTransactions = new AtomicInteger(0);
    private final AtomicInteger failedTransactions = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    // Test results
    private final Map<String, TestResult> testResults = new ConcurrentHashMap<>();
    
    public PerformanceTester(PayEdtools plugin) {
        this.plugin = plugin;
        this.testExecutor = Executors.newCachedThreadPool();
    }
    
    /**
     * Start a stress test with simulated players
     */
    public CompletableFuture<TestResult> startStressTest(StressTestConfig config) {
        if (isRunning) {
            throw new IllegalStateException("A test is already running");
        }
        
        isRunning = true;
        activeTests++;
        
        return CompletableFuture.supplyAsync(() -> {
            String testId = "stress_" + System.currentTimeMillis();
            Logger.info("Starting stress test: " + testId);
            
            long startTime = System.currentTimeMillis();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            // Create simulated players
            List<SimulatedPlayer> players = createSimulatedPlayers(config.getPlayerCount());
            
            // Start transaction simulation
            for (int i = 0; i < config.getConcurrentThreads(); i++) {
                CompletableFuture<Void> future = simulateTransactions(players, config);
                futures.add(future);
            }
            
            // Wait for all threads to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            // Calculate results
            TestResult result = new TestResult(
                testId,
                config,
                totalTime,
                totalTransactions.get(),
                successfulTransactions.get(),
                failedTransactions.get(),
                totalProcessingTime.get()
            );
            
            testResults.put(testId, result);
            isRunning = false;
            activeTests--;
            
            Logger.info("Stress test completed: " + testId + " in " + totalTime + "ms");
            return result;
        }, testExecutor);
    }
    
    /**
     * Start a load test with gradual increase
     */
    public CompletableFuture<TestResult> startLoadTest(LoadTestConfig config) {
        if (isRunning) {
            throw new IllegalStateException("A test is already running");
        }
        
        isRunning = true;
        activeTests++;
        
        return CompletableFuture.supplyAsync(() -> {
            String testId = "load_" + System.currentTimeMillis();
            Logger.info("Starting load test: " + testId);
            
            long startTime = System.currentTimeMillis();
            List<SimulatedPlayer> players = createSimulatedPlayers(config.getPlayerCount());
            
            // Gradually increase load
            for (int currentLoad = config.getInitialLoad(); currentLoad <= config.getMaxLoad(); currentLoad += config.getLoadIncrement()) {
                Logger.info("Testing with " + currentLoad + " concurrent transactions");
                
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int i = 0; i < currentLoad; i++) {
                    CompletableFuture<Void> future = simulateTransactions(players, config);
                    futures.add(future);
                }
                
                // Wait for current load to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                // Wait between load increases
                try {
                    Thread.sleep(config.getLoadIncreaseDelay());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            TestResult result = new TestResult(
                testId,
                config,
                totalTime,
                totalTransactions.get(),
                successfulTransactions.get(),
                failedTransactions.get(),
                totalProcessingTime.get()
            );
            
            testResults.put(testId, result);
            isRunning = false;
            activeTests--;
            
            Logger.info("Load test completed: " + testId + " in " + totalTime + "ms");
            return result;
        }, testExecutor);
    }
    
    /**
     * Start a benchmark test with specific scenarios
     */
    public CompletableFuture<TestResult> startBenchmarkTest(BenchmarkTestConfig config) {
        if (isRunning) {
            throw new IllegalStateException("A test is already running");
        }
        
        isRunning = true;
        activeTests++;
        
        return CompletableFuture.supplyAsync(() -> {
            String testId = "benchmark_" + System.currentTimeMillis();
            Logger.info("Starting benchmark test: " + testId);
            
            long startTime = System.currentTimeMillis();
            List<SimulatedPlayer> players = createSimulatedPlayers(config.getPlayerCount());
            
            // Run different scenarios
            for (BenchmarkScenario scenario : config.getScenarios()) {
                Logger.info("Running scenario: " + scenario.getName());
                
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int i = 0; i < scenario.getConcurrentTransactions(); i++) {
                    CompletableFuture<Void> future = simulateScenario(players, scenario);
                    futures.add(future);
                }
                
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                // Wait between scenarios
                try {
                    Thread.sleep(scenario.getDelayAfterScenario());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            TestResult result = new TestResult(
                testId,
                config,
                totalTime,
                totalTransactions.get(),
                successfulTransactions.get(),
                failedTransactions.get(),
                totalProcessingTime.get()
            );
            
            testResults.put(testId, result);
            isRunning = false;
            activeTests--;
            
            Logger.info("Benchmark test completed: " + testId + " in " + totalTime + "ms");
            return result;
        }, testExecutor);
    }
    
    /**
     * Create simulated players
     */
    private List<SimulatedPlayer> createSimulatedPlayers(int count) {
        List<SimulatedPlayer> players = new ArrayList<>();
        List<String> currencies = plugin.getCurrencyManager().getAvailableCurrencies();
        
        for (int i = 0; i < count; i++) {
            UUID uuid = UUID.randomUUID();
            String name = "TestPlayer" + i;
            String currency = currencies.get(random.nextInt(currencies.size()));
            double balance = 1000000 + random.nextDouble() * 9000000; // 1M to 10M
            
            players.add(new SimulatedPlayer(uuid, name, currency, balance));
        }
        
        return players;
    }
    
    /**
     * Simulate transactions for stress/load tests
     */
    private CompletableFuture<Void> simulateTransactions(List<SimulatedPlayer> players, TestConfig config) {
        return CompletableFuture.runAsync(() -> {
            int transactionsPerThread = config.getTransactionsPerThread();
            
            for (int i = 0; i < transactionsPerThread; i++) {
                if (!isRunning) break;
                
                try {
                    // Select random sender and receiver
                    SimulatedPlayer sender = players.get(random.nextInt(players.size()));
                    SimulatedPlayer receiver = players.get(random.nextInt(players.size()));
                    
                    // Don't send to self
                    if (sender.equals(receiver)) {
                        receiver = players.get(random.nextInt(players.size()));
                    }
                    
                    // Random amount
                    double amount = config.getMinAmount() + random.nextDouble() * (config.getMaxAmount() - config.getMinAmount());
                    
                    // Simulate transaction
                    simulateTransaction(sender, receiver, amount);
                    
                    // Random delay between transactions
                    if (config.getDelayBetweenTransactions() > 0) {
                        Thread.sleep(config.getDelayBetweenTransactions());
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Logger.error("Error in transaction simulation", e);
                }
            }
        });
    }
    
    /**
     * Simulate a specific scenario
     */
    private CompletableFuture<Void> simulateScenario(List<SimulatedPlayer> players, BenchmarkScenario scenario) {
        return CompletableFuture.runAsync(() -> {
            for (int i = 0; i < scenario.getTransactionsPerThread(); i++) {
                if (!isRunning) break;
                
                try {
                    SimulatedPlayer sender = players.get(random.nextInt(players.size()));
                    SimulatedPlayer receiver = players.get(random.nextInt(players.size()));
                    
                    if (sender.equals(receiver)) {
                        receiver = players.get(random.nextInt(players.size()));
                    }
                    
                    double amount = scenario.getAmount();
                    simulateTransaction(sender, receiver, amount);
                    
                    if (scenario.getDelayBetweenTransactions() > 0) {
                        Thread.sleep(scenario.getDelayBetweenTransactions());
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Logger.error("Error in scenario simulation", e);
                }
            }
        });
    }
    
    /**
     * Simulate a single transaction
     */
    private void simulateTransaction(SimulatedPlayer sender, SimulatedPlayer receiver, double amount) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Create a mock transaction result
            TransactionManager.TransactionResult result = plugin.getTransactionManager()
                .processTransaction(sender.getUuid(), receiver.getUuid(), sender.getCurrency(), amount, true)
                .get(5, TimeUnit.SECONDS); // 5 second timeout
            
            long processingTime = System.currentTimeMillis() - startTime;
            totalProcessingTime.addAndGet(processingTime);
            totalTransactions.incrementAndGet();
            
            if (result.isSuccess()) {
                successfulTransactions.incrementAndGet();
                // Update simulated balances
                sender.addBalance(-amount);
                receiver.addBalance(amount);
            } else {
                failedTransactions.incrementAndGet();
            }
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            totalProcessingTime.addAndGet(processingTime);
            totalTransactions.incrementAndGet();
            failedTransactions.incrementAndGet();
            Logger.debug("Simulated transaction failed: " + e.getMessage());
        }
    }
    
    /**
     * Stop all running tests
     */
    public void stopAllTests() {
        isRunning = false;
        Logger.info("Stopping all performance tests");
    }
    
    /**
     * Get test results
     */
    public Map<String, TestResult> getTestResults() {
        return new HashMap<>(testResults);
    }
    
    /**
     * Clear test results
     */
    public void clearResults() {
        testResults.clear();
        totalTransactions.set(0);
        successfulTransactions.set(0);
        failedTransactions.set(0);
        totalProcessingTime.set(0);
    }
    
    /**
     * Check if tests are running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Get active test count
     */
    public int getActiveTests() {
        return activeTests;
    }
    
    /**
     * Shutdown the tester
     */
    public void shutdown() {
        stopAllTests();
        testExecutor.shutdown();
        try {
            if (!testExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                testExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            testExecutor.shutdownNow();
        }
    }
    
    /**
     * Simulated player class
     */
    private static class SimulatedPlayer {
        private final UUID uuid;
        private final String name;
        private final String currency;
        private volatile double balance;
        
        public SimulatedPlayer(UUID uuid, String name, String currency, double balance) {
            this.uuid = uuid;
            this.name = name;
            this.currency = currency;
            this.balance = balance;
        }
        
        public UUID getUuid() { return uuid; }
        @SuppressWarnings("unused")
        public String getName() { return name; }
        public String getCurrency() { return currency; }
        @SuppressWarnings("unused")
        public double getBalance() { return balance; }
        
        public void addBalance(double amount) {
            this.balance += amount;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SimulatedPlayer that = (SimulatedPlayer) obj;
            return Objects.equals(uuid, that.uuid);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }
    }
}
