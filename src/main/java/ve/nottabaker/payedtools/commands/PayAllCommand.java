package ve.nottabaker.payedtools.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.managers.TransactionManager;
import ve.nottabaker.payedtools.utils.AmountParser;
import ve.nottabaker.payedtools.utils.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized /pay * command for paying all online players
 */
public class PayAllCommand implements CommandExecutor, TabCompleter {
    
    private final PayEdtools plugin;
    private final AmountParser amountParser;
    
    // Cache for online players to optimize performance
    private final Set<UUID> cachedOnlinePlayers = ConcurrentHashMap.newKeySet();
    private long lastPlayerCacheUpdate = 0;
    private static final long PLAYER_CACHE_UPDATE_INTERVAL = 2000; // 2 seconds for payall
    
    public PayAllCommand(PayEdtools plugin) {
        this.plugin = plugin;
        this.amountParser = new AmountParser(plugin);
        updateOnlinePlayerCache();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        // Check permission
        if (!player.hasPermission("payedtools.payall")) {
            plugin.getMessageManager().send(player, "no-permission");
            return true;
        }
        
        // Check arguments
        if (args.length < 2) {
            player.sendMessage("§cUsage: /payall <currency> <amount>");
            return true;
        }
        
        String currency = args[0];
        String amountStr = args[1];
        
        // Validate currency
        if (!plugin.getCurrencyManager().validateCurrency(currency)) {
            if (!plugin.getCurrencyManager().isCurrency(currency)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("currency", currency);
                plugin.getMessageManager().send(player, "invalid-currency", placeholders);
            } else if (plugin.getCurrencyManager().isBlocked(currency)) {
                plugin.getMessageManager().send(player, "currency-blocked");
            } else if (!plugin.getCurrencyManager().isAllowed(currency)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("currency", currency);
                plugin.getMessageManager().send(player, "currency-not-allowed", placeholders);
            }
            return true;
        }
        
        // Parse amount
        double amount;
        try {
            amount = amountParser.parse(amountStr);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().send(player, "invalid-amount");
            return true;
        }
        
        // Validate amount
        boolean bypassLimits = player.hasPermission("payedtools.bypass.limits");
        AmountParser.ValidationResult validation = amountParser.validate(amount, bypassLimits);
        
        if (!validation.isValid()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("minimum", String.valueOf(plugin.getConfigManager().getMinimumAmount()));
            placeholders.put("maximum", String.valueOf(plugin.getConfigManager().getMaximumAmount()));
            plugin.getMessageManager().send(player, validation.getErrorKey(), placeholders);
            return true;
        }
        
        // Get online players (excluding sender)
        updateOnlinePlayerCache();
        List<UUID> targetPlayers = new ArrayList<>(cachedOnlinePlayers);
        targetPlayers.remove(player.getUniqueId()); // Remove sender
        
        if (targetPlayers.isEmpty()) {
            player.sendMessage("§cNo other players online to pay!");
            return true;
        }
        
        // Calculate total cost
        double totalCost = amount * targetPlayers.size();
        double tax = calculateTax(amount) * targetPlayers.size();
        double totalWithTax = totalCost + tax;
        
        // Check if sender has enough balance
        if (!plugin.getCurrencyManager().hasEnough(player.getUniqueId(), currency, totalWithTax)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("currency", currency);
            placeholders.put("balance", amountParser.format(plugin.getCurrencyManager().getBalance(player.getUniqueId(), currency)));
            plugin.getMessageManager().send(player, "insufficient-funds", placeholders);
            return true;
        }
        
        // Check cooldown (optional for payall)
        if (plugin.getCooldownManager().isOnCooldown(player)) {
            int remaining = plugin.getCooldownManager().getRemainingCooldown(player);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("cooldown", String.valueOf(remaining));
            plugin.getMessageManager().send(player, "cooldown-active", placeholders);
            return true;
        }
        
        // Process bulk payment
        processBulkPayment(player, targetPlayers, currency, amount, tax);
        
        return true;
    }
    
    /**
     * Process bulk payment to multiple players
     */
    private void processBulkPayment(Player sender, List<UUID> targetPlayers, String currency, double amount, double taxPerPlayer) {
        long startTime = System.currentTimeMillis();
        
        // Send initial message
        sender.sendMessage("§eProcessing payment to " + targetPlayers.size() + " players...");
        
        // Process transactions in parallel batches
        int batchSize = Math.min(10, targetPlayers.size()); // Process max 10 at a time
        List<CompletableFuture<TransactionResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < targetPlayers.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, targetPlayers.size());
            List<UUID> batch = targetPlayers.subList(i, endIndex);
            
            CompletableFuture<TransactionResult> batchFuture = processBatch(sender, batch, currency, amount, taxPerPlayer);
            futures.add(batchFuture);
        }
        
        // Wait for all batches to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenAccept(v -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    long processingTime = System.currentTimeMillis() - startTime;
                    
                    // Count results
                    AtomicInteger successCount = new AtomicInteger(0);
                    AtomicInteger failureCount = new AtomicInteger(0);
                    
                    futures.forEach(future -> {
                        try {
                            TransactionResult result = future.get();
                            if (result.isSuccess()) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            Logger.error("Error processing batch", e);
                        }
                    });
                    
                    // Send results
                    int totalSuccess = successCount.get();
                    int totalFailure = failureCount.get();
                    
                    if (totalSuccess > 0) {
                        sender.sendMessage("§aSuccessfully paid " + totalSuccess + " players!");
                        
                        // Show tax if applicable
                        if (taxPerPlayer > 0) {
                            double totalTax = taxPerPlayer * totalSuccess;
                            Map<String, String> taxPlaceholders = new HashMap<>();
                            taxPlaceholders.put("tax", amountParser.format(totalTax));
                            taxPlaceholders.put("currency", currency);
                            plugin.getMessageManager().send(sender, "tax-applied", taxPlaceholders);
                        }
                    }
                    
                    if (totalFailure > 0) {
                        sender.sendMessage("§cFailed to pay " + totalFailure + " players. Check console for details.");
                    }
                    
                    sender.sendMessage("§7Processing completed in " + processingTime + "ms");
                    
                    // Set cooldown and record metrics
                    plugin.getCooldownManager().setCooldown(sender);
                    plugin.getPerformanceMetrics().recordTransaction(
                        currency, sender.getUniqueId(), null, amount * totalSuccess, 
                        totalFailure == 0, processingTime
                    );
                });
            })
            .exceptionally(throwable -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Logger.error("Bulk payment processing failed", throwable);
                    sender.sendMessage("§cBulk payment failed due to an internal error. Please try again.");
                });
                return null;
            });
    }
    
    /**
     * Process a batch of transactions
     */
    private CompletableFuture<TransactionResult> processBatch(Player sender, List<UUID> batch, String currency, double amount, double taxPerPlayer) {
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            int failureCount = 0;
            
            for (UUID targetUUID : batch) {
                try {
                    // Process individual transaction
                    TransactionManager.TransactionResult result = plugin.getTransactionManager()
                        .processTransaction(sender.getUniqueId(), targetUUID, currency, amount, false)
                        .get(); // Wait for completion
                    
                    if (result.isSuccess()) {
                        successCount++;
                        
                        // Notify target player if online
                        Player targetPlayer = Bukkit.getPlayer(targetUUID);
                        if (targetPlayer != null) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("amount", amountParser.format(amount));
                            placeholders.put("currency", currency);
                            placeholders.put("sender", sender.getName());
                            plugin.getMessageManager().send(targetPlayer, "payment-received", placeholders);
                        }
                    } else {
                        failureCount++;
                        Logger.warning("Failed to pay " + targetUUID + ": " + result.getErrorCode());
                    }
                } catch (Exception e) {
                    failureCount++;
                    Logger.error("Error processing payment to " + targetUUID, e);
                }
            }
            
            return new TransactionResult(successCount, failureCount);
        });
    }
    
    /**
     * Calculate tax for amount
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
     * Update online player cache
     */
    private void updateOnlinePlayerCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlayerCacheUpdate > PLAYER_CACHE_UPDATE_INTERVAL) {
            cachedOnlinePlayers.clear();
            Bukkit.getOnlinePlayers().forEach(p -> cachedOnlinePlayers.add(p.getUniqueId()));
            lastPlayerCacheUpdate = currentTime;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Currency names
            List<String> currencies = plugin.getCurrencyManager().getAvailableCurrencies();
            String input = args[0].toLowerCase();
            
            for (String currency : currencies) {
                if (currency.toLowerCase().startsWith(input)) {
                    if (sender.hasPermission("payedtools.currency." + currency) || 
                        sender.hasPermission("payedtools.currency.*")) {
                        completions.add(currency);
                    }
                }
            }
            
        } else if (args.length == 2) {
            // Amount suggestions
            String input = args[1].toLowerCase();
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String currency = args[0];
                
                if (plugin.getCurrencyManager().isCurrency(currency)) {
                    double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), currency);
                    int onlineCount = Bukkit.getOnlinePlayers().size() - 1; // Exclude sender
                    
                    if (onlineCount > 0) {
                        double maxPerPlayer = balance / onlineCount;
                        
                        // Suggest amounts based on available balance per player
                        String[] suggestions = {"1", "10", "100", "1000"};
                        for (String suggestion : suggestions) {
                            double amount = Double.parseDouble(suggestion);
                            if (amount <= maxPerPlayer && suggestion.startsWith(input)) {
                                completions.add(suggestion);
                            }
                        }
                    }
                }
            }
        }
        
        completions.sort(String.CASE_INSENSITIVE_ORDER);
        return completions.size() > 10 ? completions.subList(0, 10) : completions;
    }
    
    /**
     * Transaction result for batch processing
     */
    private static class TransactionResult {
        private final int successCount;
        private final int failureCount;
        
        public TransactionResult(int successCount, int failureCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
        
        public boolean isSuccess() {
            return failureCount == 0;
        }
        
        @SuppressWarnings("unused")
        public int getSuccessCount() {
            return successCount;
        }
        
        @SuppressWarnings("unused")
        public int getFailureCount() {
            return failureCount;
        }
    }
}
