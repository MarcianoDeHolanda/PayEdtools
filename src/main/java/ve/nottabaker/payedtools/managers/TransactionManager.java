package ve.nottabaker.payedtools.managers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.models.Transaction;
import ve.nottabaker.payedtools.utils.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * Manages currency transactions with async support and error handling
 */
public class TransactionManager {
    
    private final PayEdtools plugin;
    private final CurrencyManager currencyManager;
    private final ExecutorService executorService;
    
    // Transaction queue for batch processing
    private final BlockingQueue<Transaction> transactionQueue;
    private final List<Transaction> pendingTransactions;
    
    public TransactionManager(PayEdtools plugin) {
        this.plugin = plugin;
        this.currencyManager = plugin.getCurrencyManager();
        
        int threadPoolSize = plugin.getConfigManager().getThreadPoolSize();
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        
        this.transactionQueue = new LinkedBlockingQueue<>();
        this.pendingTransactions = Collections.synchronizedList(new ArrayList<>());
        
        // Start batch processor if enabled
        if (plugin.getConfigManager().isBatchDatabaseOperations()) {
            startBatchProcessor();
        }
    }
    
    /**
     * Process a currency transfer transaction
     */
    public CompletableFuture<TransactionResult> processTransaction(
            UUID sender,
            UUID receiver,
            String currency,
            double amount,
            boolean async) {
        
        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            sender,
            receiver,
            currency,
            amount,
            System.currentTimeMillis()
        );
        
        if (async && plugin.getConfigManager().isAsyncOperations()) {
            return CompletableFuture.supplyAsync(() -> executeTransaction(transaction), executorService);
        } else {
            return CompletableFuture.completedFuture(executeTransaction(transaction));
        }
    }
    
    /**
     * Execute the actual transaction
     */
    private TransactionResult executeTransaction(Transaction transaction) {
        try {
            UUID sender = transaction.getSender();
            UUID receiver = transaction.getReceiver();
            String currency = transaction.getCurrency();
            double amount = transaction.getAmount();
            
            Logger.debug("Processing transaction: " + transaction);
            
            // Calculate tax if enabled
            double tax = calculateTax(amount);
            double totalDeducted = amount + tax;
            
            // Check if sender has enough
            if (!currencyManager.hasEnough(sender, currency, totalDeducted)) {
                return new TransactionResult(false, "insufficient_funds", null);
            }
            
            // Perform the transfer atomically
            try {
                // Remove from sender
                currencyManager.removeCurrency(sender, currency, totalDeducted);
                
                // Add to receiver
                currencyManager.addCurrency(receiver, currency, amount);
                
                // Update transaction with tax
                transaction.setTax(tax);
                
                // Log transaction
                logTransaction(transaction);
                
                // Save to database if enabled
                if (plugin.getConfigManager().isSaveTransactionHistory()) {
                    saveTransaction(transaction);
                }
                
                Logger.debug("Transaction completed successfully: " + transaction.getId());
                return new TransactionResult(true, "success", transaction);
                
            } catch (Exception e) {
                // Attempt rollback
                Logger.error("Transaction failed, attempting rollback: " + transaction.getId(), e);
                rollbackTransaction(transaction, amount, tax);
                return new TransactionResult(false, "transaction_failed", null);
            }
            
        } catch (Exception e) {
            Logger.error("Unexpected error during transaction processing", e);
            return new TransactionResult(false, "unknown_error", null);
        }
    }
    
    /**
     * Calculate transaction tax
     */
    private double calculateTax(double amount) {
        if (!plugin.getConfigManager().isTaxEnabled()) {
            return 0;
        }
        
        if (amount < plugin.getConfigManager().getMinimumForTax()) {
            return 0;
        }
        
        double percentageTax = amount * (plugin.getConfigManager().getTaxPercentage() / 100.0);
        double fixedTax = plugin.getConfigManager().getTaxFixed();
        
        return percentageTax + fixedTax;
    }
    
    /**
     * Rollback a failed transaction
     */
    private void rollbackTransaction(Transaction transaction, double amount, double tax) {
        try {
            // Return currency to sender
            currencyManager.addCurrency(
                transaction.getSender(), 
                transaction.getCurrency(), 
                amount + tax
            );
            
            // Remove from receiver if it was added
            try {
                currencyManager.removeCurrency(
                    transaction.getReceiver(), 
                    transaction.getCurrency(), 
                    amount
                );
            } catch (Exception e) {
                Logger.error("Failed to remove currency from receiver during rollback", e);
            }
            
            Logger.info("Transaction rolled back successfully: " + transaction.getId());
        } catch (Exception e) {
            Logger.error("CRITICAL: Failed to rollback transaction " + transaction.getId(), e);
            // This is a critical error - notify admins
            notifyAdminsOfCriticalError(transaction);
        }
    }
    
    /**
     * Log transaction to console/file
     */
    private void logTransaction(Transaction transaction) {
        if (plugin.getConfigManager().isConsoleLogging()) {
            Logger.info(formatTransactionLog(transaction));
        }
        
        if (plugin.getConfigManager().isFileLogging()) {
            // File logging would be implemented here
            // For now, we'll use the database for persistence
        }
    }
    
    /**
     * Format transaction for logging
     */
    private String formatTransactionLog(Transaction transaction) {
        String format = plugin.getConfigManager().getLogFormat();
        
        OfflinePlayer sender = Bukkit.getOfflinePlayer(transaction.getSender());
        OfflinePlayer receiver = Bukkit.getOfflinePlayer(transaction.getReceiver());
        
        return format
            .replace("%timestamp%", new Date(transaction.getTimestamp()).toString())
            .replace("%sender%", sender.getName())
            .replace("%receiver%", receiver.getName())
            .replace("%amount%", String.valueOf(transaction.getAmount()))
            .replace("%currency%", transaction.getCurrency());
    }
    
    /**
     * Save transaction to database
     */
    private void saveTransaction(Transaction transaction) {
        if (plugin.getConfigManager().isBatchDatabaseOperations()) {
            // Add to queue for batch processing
            transactionQueue.offer(transaction);
        } else {
            // Save immediately
            plugin.getDatabaseManager().saveTransaction(transaction);
        }
    }
    
    /**
     * Start batch processor for database operations
     */
    private void startBatchProcessor() {
        int batchSize = plugin.getConfigManager().getBatchSize();
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (transactionQueue.isEmpty()) {
                return;
            }
            
            List<Transaction> batch = new ArrayList<>();
            transactionQueue.drainTo(batch, batchSize);
            
            if (!batch.isEmpty()) {
                try {
                    plugin.getDatabaseManager().saveTransactionBatch(batch);
                    Logger.debug("Saved batch of " + batch.size() + " transactions");
                } catch (Exception e) {
                    Logger.error("Failed to save transaction batch", e);
                    // Re-queue failed transactions
                    transactionQueue.addAll(batch);
                }
            }
        }, 100L, 100L); // Run every 5 seconds
    }
    
    /**
     * Notify admins of critical errors
     */
    private void notifyAdminsOfCriticalError(Transaction transaction) {
        String message = "§c§lCRITICAL: Failed to rollback transaction " + transaction.getId();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("payedtools.admin")) {
                player.sendMessage(message);
            }
        }
        
        Logger.error(message);
    }
    
    /**
     * Get transaction history for a player
     */
    public List<Transaction> getTransactionHistory(UUID uuid, int limit) {
        return plugin.getDatabaseManager().getTransactionHistory(uuid, limit);
    }
    
    /**
     * Get transaction statistics for a player
     */
    public TransactionStats getTransactionStats(UUID uuid) {
        return plugin.getDatabaseManager().getTransactionStats(uuid);
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        Logger.debug("Shutting down transaction manager...");
        
        // Save any pending transactions
        if (!transactionQueue.isEmpty()) {
            List<Transaction> remaining = new ArrayList<>();
            transactionQueue.drainTo(remaining);
            try {
                plugin.getDatabaseManager().saveTransactionBatch(remaining);
                Logger.info("Saved " + remaining.size() + " pending transactions");
            } catch (Exception e) {
                Logger.error("Failed to save pending transactions on shutdown", e);
            }
        }
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
    
    /**
     * Transaction result class
     */
    public static class TransactionResult {
        private final boolean success;
        private final String errorCode;
        private final Transaction transaction;
        
        public TransactionResult(boolean success, String errorCode, Transaction transaction) {
            this.success = success;
            this.errorCode = errorCode;
            this.transaction = transaction;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public Transaction getTransaction() {
            return transaction;
        }
    }
    
    /**
     * Transaction statistics class
     */
    public static class TransactionStats {
        private int sentCount;
        private int receivedCount;
        private double sentTotal;
        private double receivedTotal;
        
        public TransactionStats(int sentCount, int receivedCount, double sentTotal, double receivedTotal) {
            this.sentCount = sentCount;
            this.receivedCount = receivedCount;
            this.sentTotal = sentTotal;
            this.receivedTotal = receivedTotal;
        }
        
        public int getSentCount() {
            return sentCount;
        }
        
        public int getReceivedCount() {
            return receivedCount;
        }
        
        public double getSentTotal() {
            return sentTotal;
        }
        
        public double getReceivedTotal() {
            return receivedTotal;
        }
    }
}
