package ve.nottabaker.payedtools.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.utils.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Command for running performance tests
 */
public class PayTestCommand implements CommandExecutor, TabCompleter {
    
    private final PayEdtools plugin;
    
    public PayTestCommand(PayEdtools plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("payedtools.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "stress":
                return handleStressTest(sender, args);
            case "load":
                return handleLoadTest(sender, args);
            case "benchmark":
                return handleBenchmarkTest(sender, args);
            case "stop":
                return handleStopTest(sender);
            case "results":
                return handleShowResults(sender, args);
            case "clear":
                return handleClearResults(sender);
            case "status":
                return handleStatus(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }
    
    /**
     * Handle stress test command
     */
    private boolean handleStressTest(CommandSender sender, String[] args) {
        if (plugin.getPerformanceTester().isRunning()) {
            sender.sendMessage("§cA test is already running! Use /paytest stop to stop it first.");
            return true;
        }
        
        // Parse arguments with defaults
        int players = parseInt(args, 1, 50);
        int threads = parseInt(args, 2, 10);
        int transactionsPerThread = parseInt(args, 3, 100);
        double minAmount = parseDouble(args, 4, 100.0);
        double maxAmount = parseDouble(args, 5, 10000.0);
        long delay = parseLong(args, 6, 0);
        
        sender.sendMessage("§eStarting stress test...");
        sender.sendMessage("§7Players: " + players + " | Threads: " + threads + " | Transactions/Thread: " + transactionsPerThread);
        
        TestConfig config = new StressTestConfig(players, threads, transactionsPerThread, minAmount, maxAmount, delay);
        
        CompletableFuture<TestResult> future = plugin.getPerformanceTester().startStressTest((StressTestConfig) config);
        
        future.thenAccept(result -> {
            sender.sendMessage("§aStress test completed!");
            sender.sendMessage("§e" + result.getSummary());
        }).exceptionally(throwable -> {
            sender.sendMessage("§cStress test failed: " + throwable.getMessage());
            return null;
        });
        
        return true;
    }
    
    /**
     * Handle load test command
     */
    private boolean handleLoadTest(CommandSender sender, String[] args) {
        if (plugin.getPerformanceTester().isRunning()) {
            sender.sendMessage("§cA test is already running! Use /paytest stop to stop it first.");
            return true;
        }
        
        // Parse arguments with defaults
        int players = parseInt(args, 1, 100);
        int initialLoad = parseInt(args, 2, 5);
        int maxLoad = parseInt(args, 3, 50);
        int loadIncrement = parseInt(args, 4, 5);
        long loadIncreaseDelay = parseLong(args, 5, 2000);
        int transactionsPerThread = parseInt(args, 6, 50);
        
        sender.sendMessage("§eStarting load test...");
        sender.sendMessage("§7Players: " + players + " | Load: " + initialLoad + " → " + maxLoad + " (+" + loadIncrement + ")");
        
        LoadTestConfig config = new LoadTestConfig(players, players, initialLoad, maxLoad, 
                                                 loadIncrement, loadIncreaseDelay, transactionsPerThread,
                                                 100.0, 10000.0, 0);
        
        CompletableFuture<TestResult> future = plugin.getPerformanceTester().startLoadTest(config);
        
        future.thenAccept(result -> {
            sender.sendMessage("§aLoad test completed!");
            sender.sendMessage("§e" + result.getSummary());
        }).exceptionally(throwable -> {
            sender.sendMessage("§cLoad test failed: " + throwable.getMessage());
            return null;
        });
        
        return true;
    }
    
    /**
     * Handle benchmark test command
     */
    private boolean handleBenchmarkTest(CommandSender sender, String[] args) {
        if (plugin.getPerformanceTester().isRunning()) {
            sender.sendMessage("§cA test is already running! Use /paytest stop to stop it first.");
            return true;
        }
        
        sender.sendMessage("§eStarting benchmark test...");
        
        // Create benchmark scenarios
        List<BenchmarkScenario> scenarios = Arrays.asList(
            new BenchmarkScenario("Small Transactions", 20, 50, 100.0, 0, 1000),
            new BenchmarkScenario("Medium Transactions", 15, 30, 1000.0, 0, 1000),
            new BenchmarkScenario("Large Transactions", 10, 20, 10000.0, 0, 1000),
            new BenchmarkScenario("High Frequency", 30, 100, 500.0, 10, 1000),
            new BenchmarkScenario("Low Frequency", 5, 10, 5000.0, 100, 1000)
        );
        
        BenchmarkTestConfig config = new BenchmarkTestConfig(100, 20, 50, 100.0, 10000.0, 0, scenarios);
        
        CompletableFuture<TestResult> future = plugin.getPerformanceTester().startBenchmarkTest(config);
        
        future.thenAccept(result -> {
            sender.sendMessage("§aBenchmark test completed!");
            sender.sendMessage("§e" + result.getSummary());
        }).exceptionally(throwable -> {
            sender.sendMessage("§cBenchmark test failed: " + throwable.getMessage());
            return null;
        });
        
        return true;
    }
    
    /**
     * Handle stop test command
     */
    private boolean handleStopTest(CommandSender sender) {
        if (!plugin.getPerformanceTester().isRunning()) {
            sender.sendMessage("§cNo tests are currently running.");
            return true;
        }
        
        plugin.getPerformanceTester().stopAllTests();
        sender.sendMessage("§eStopping all performance tests...");
        return true;
    }
    
    /**
     * Handle show results command
     */
    private boolean handleShowResults(CommandSender sender, String[] args) {
        Map<String, TestResult> results = plugin.getPerformanceTester().getTestResults();
        
        if (results.isEmpty()) {
            sender.sendMessage("§cNo test results available.");
            return true;
        }
        
        sender.sendMessage("§6=== Performance Test Results ===");
        
        if (args.length > 1 && args[1].equals("detailed")) {
            // Show detailed results
            for (TestResult result : results.values()) {
                sender.sendMessage("§e" + result.getTestId() + ":");
                sender.sendMessage("§7  Total Time: " + result.getTotalTime() + "ms");
                sender.sendMessage("§7  Transactions: " + result.getTotalTransactions());
                sender.sendMessage("§7  Successful: " + result.getSuccessfulTransactions());
                sender.sendMessage("§7  Failed: " + result.getFailedTransactions());
                sender.sendMessage("§7  Success Rate: " + String.format("%.2f%%", result.getSuccessRate() * 100));
                sender.sendMessage("§7  TPS: " + String.format("%.2f", result.getTransactionsPerSecond()));
                sender.sendMessage("§7  Avg Processing Time: " + String.format("%.2fms", result.getAverageProcessingTime()));
                sender.sendMessage("");
            }
        } else {
            // Show summary
            for (TestResult result : results.values()) {
                sender.sendMessage("§e" + result.getSummary());
            }
        }
        
        return true;
    }
    
    /**
     * Handle clear results command
     */
    private boolean handleClearResults(CommandSender sender) {
        plugin.getPerformanceTester().clearResults();
        sender.sendMessage("§aTest results cleared.");
        return true;
    }
    
    /**
     * Handle status command
     */
    private boolean handleStatus(CommandSender sender) {
        boolean isRunning = plugin.getPerformanceTester().isRunning();
        int activeTests = plugin.getPerformanceTester().getActiveTests();
        Map<String, TestResult> results = plugin.getPerformanceTester().getTestResults();
        
        sender.sendMessage("§6=== Performance Tester Status ===");
        sender.sendMessage("§7Running: " + (isRunning ? "§aYes" : "§cNo"));
        sender.sendMessage("§7Active Tests: " + activeTests);
        sender.sendMessage("§7Total Results: " + results.size());
        
        if (isRunning) {
            sender.sendMessage("§eUse /paytest stop to stop running tests.");
        }
        
        return true;
    }
    
    /**
     * Send usage information
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6=== PayEdtools Performance Testing ===");
        sender.sendMessage("§e/paytest stress [players] [threads] [transactions] [min] [max] [delay]");
        sender.sendMessage("§7  Run a stress test with simulated players");
        sender.sendMessage("");
        sender.sendMessage("§e/paytest load [players] [initial] [max] [increment] [delay] [transactions]");
        sender.sendMessage("§7  Run a load test with gradual increase");
        sender.sendMessage("");
        sender.sendMessage("§e/paytest benchmark");
        sender.sendMessage("§7  Run a comprehensive benchmark test");
        sender.sendMessage("");
        sender.sendMessage("§e/paytest stop");
        sender.sendMessage("§7  Stop all running tests");
        sender.sendMessage("");
        sender.sendMessage("§e/paytest results [detailed]");
        sender.sendMessage("§7  Show test results");
        sender.sendMessage("");
        sender.sendMessage("§e/paytest clear");
        sender.sendMessage("§7  Clear all test results");
        sender.sendMessage("");
        sender.sendMessage("§e/paytest status");
        sender.sendMessage("§7  Show tester status");
    }
    
    /**
     * Parse integer argument with default
     */
    private int parseInt(String[] args, int index, int defaultValue) {
        if (index >= args.length) return defaultValue;
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Parse double argument with default
     */
    private double parseDouble(String[] args, int index, double defaultValue) {
        if (index >= args.length) return defaultValue;
        try {
            return Double.parseDouble(args[index]);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Parse long argument with default
     */
    private long parseLong(String[] args, int index, long defaultValue) {
        if (index >= args.length) return defaultValue;
        try {
            return Long.parseLong(args[index]);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            String[] subCommands = {"stress", "load", "benchmark", "stop", "results", "clear", "status"};
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("results")) {
            String input = args[1].toLowerCase();
            if ("detailed".startsWith(input)) {
                completions.add("detailed");
            }
        }
        
        return completions;
    }
}
