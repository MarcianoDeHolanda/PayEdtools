package ve.nottabaker.payedtools.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.managers.TransactionManager;
import ve.nottabaker.payedtools.models.PendingTransaction;
import ve.nottabaker.payedtools.utils.AmountParser;
import ve.nottabaker.payedtools.utils.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main /pay command implementation
 */
public class PayCommand implements CommandExecutor, TabCompleter {
    
    private final PayEdtools plugin;
    private final AmountParser amountParser;
    
    // Cache for player names to optimize tab completion
    private final Set<String> cachedPlayerNames = ConcurrentHashMap.newKeySet();
    private long lastPlayerCacheUpdate = 0;
    private static final long PLAYER_CACHE_UPDATE_INTERVAL = 5000; // 5 seconds
    
    public PayCommand(PayEdtools plugin) {
        this.plugin = plugin;
        this.amountParser = new AmountParser(plugin);
        updatePlayerCache(); // Initialize cache
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        // Check permission
        if (!player.hasPermission("payedtools.use")) {
            plugin.getMessageManager().send(player, "no-permission");
            return true;
        }
        
        // Handle subcommands for confirmation
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("confirm")) {
                return handleConfirm(player);
            } else if (args[0].equalsIgnoreCase("cancel")) {
                return handleCancel(player);
            }
        }
        
        // Check arguments
        if (args.length < 3) {
            plugin.getMessageManager().send(player, "usage-pay");
            return true;
        }
        
        String targetName = args[0];
        String currency = args[1];
        String amountStr = args[2];
        
        // Find target player
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            plugin.getMessageManager().send(player, "invalid-player", placeholders);
            return true;
        }
        
        // Check if offline transfers are allowed
        if (!target.isOnline() && !plugin.getConfigManager().isAllowOfflineTransfers()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            plugin.getMessageManager().send(player, "invalid-player", placeholders);
            return true;
        }
        
        // Check self-transfer
        if (player.getUniqueId().equals(target.getUniqueId()) && !plugin.getConfigManager().isAllowSelfTransfer()) {
            plugin.getMessageManager().send(player, "self-transfer");
            return true;
        }
        
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
        
        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(player)) {
            int remaining = plugin.getCooldownManager().getRemainingCooldown(player);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("cooldown", String.valueOf(remaining));
            plugin.getMessageManager().send(player, "cooldown-active", placeholders);
            return true;
        }
        
        // Check rate limit
        if (plugin.getRateLimitManager().isRateLimited(player)) {
            plugin.getMessageManager().send(player, "rate-limit-exceeded");
            return true;
        }
        
        // Check balance (including tax)
        double tax = calculateTax(amount);
        double total = amount + tax;
        
        if (!plugin.getCurrencyManager().hasEnough(player.getUniqueId(), currency, total)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("currency", currency);
            placeholders.put("balance", amountParser.format(plugin.getCurrencyManager().getBalance(player.getUniqueId(), currency)));
            plugin.getMessageManager().send(player, "insufficient-funds", placeholders);
            return true;
        }
        
        // Check if confirmation is required
        if (plugin.getConfirmationManager().requiresConfirmation(amount)) {
            plugin.getConfirmationManager().createConfirmation(player, target.getUniqueId(), currency, amount);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", amountParser.format(amount));
            placeholders.put("currency", currency);
            placeholders.put("receiver", target.getName());
            plugin.getMessageManager().send(player, "confirmation-required", placeholders);
            plugin.getMessageManager().sendRaw(player, "confirmation-prompt", null);
            return true;
        }
        
        // Process transaction
        processPayment(player, target.getUniqueId(), currency, amount);
        
        return true;
    }
    
    /**
     * Handle confirm subcommand
     */
    private boolean handleConfirm(Player player) {
        PendingTransaction pending = plugin.getConfirmationManager().confirm(player.getUniqueId());
        
        if (pending == null) {
            plugin.getMessageManager().send(player, "confirmation-expired");
            return true;
        }
        
        // Process the transaction
        processPayment(player, pending.getReceiver(), pending.getCurrency(), pending.getAmount());
        
        plugin.getMessageManager().send(player, "confirmation-sent");
        return true;
    }
    
    /**
     * Handle cancel subcommand
     */
    private boolean handleCancel(Player player) {
        boolean cancelled = plugin.getConfirmationManager().cancel(player.getUniqueId());
        
        if (cancelled) {
            plugin.getMessageManager().send(player, "confirmation-cancelled");
        } else {
            plugin.getMessageManager().send(player, "confirmation-expired");
        }
        
        return true;
    }
    
    /**
     * Process the actual payment
     */
    private void processPayment(Player sender, UUID receiverUUID, String currency, double amount) {
        // Process transaction first
        plugin.getTransactionManager().processTransaction(
            sender.getUniqueId(),
            receiverUUID,
            currency,
            amount,
            true
        ).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    // Only set cooldown and rate limit on SUCCESS
                    plugin.getCooldownManager().setCooldown(sender);
                    plugin.getRateLimitManager().recordTransaction(sender);
                    
                    // Send success messages
                    OfflinePlayer receiver = Bukkit.getOfflinePlayer(receiverUUID);
                    
                    Map<String, String> senderPlaceholders = new HashMap<>();
                    senderPlaceholders.put("amount", amountParser.format(amount));
                    senderPlaceholders.put("currency", currency);
                    senderPlaceholders.put("receiver", receiver.getName());
                    plugin.getMessageManager().send(sender, "payment-sent", senderPlaceholders);
                    
                    // Show tax if applicable
                    if (result.getTransaction().getTax() > 0) {
                        Map<String, String> taxPlaceholders = new HashMap<>();
                        taxPlaceholders.put("tax", amountParser.format(result.getTransaction().getTax()));
                        taxPlaceholders.put("currency", currency);
                        plugin.getMessageManager().send(sender, "tax-applied", taxPlaceholders);
                    }
                    
                    // Notify receiver if online
                    if (receiver.isOnline()) {
                        Player receiverPlayer = receiver.getPlayer();
                        Map<String, String> receiverPlaceholders = new HashMap<>();
                        receiverPlaceholders.put("amount", amountParser.format(amount));
                        receiverPlaceholders.put("currency", currency);
                        receiverPlaceholders.put("sender", sender.getName());
                        plugin.getMessageManager().send(receiverPlayer, "payment-received", receiverPlaceholders);
                    }
                } else {
                    // Handle error - NO cooldown or rate limit on failure
                    handleTransactionError(sender, result.getErrorCode(), currency);
                }
            });
        }).exceptionally(throwable -> {
            // Handle async execution errors
            Bukkit.getScheduler().runTask(plugin, () -> {
                Logger.error("Async transaction processing failed", throwable);
                sender.sendMessage("§cTransaction failed due to an internal error. Please try again.");
            });
            return null;
        });
    }
    
    /**
     * Handle transaction errors with appropriate messages
     */
    private void handleTransactionError(Player sender, String errorCode, String currency) {
        switch (errorCode) {
            case "insufficient_funds":
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("currency", currency);
                placeholders.put("balance", amountParser.format(plugin.getCurrencyManager().getBalance(sender.getUniqueId(), currency)));
                plugin.getMessageManager().send(sender, "insufficient-funds", placeholders);
                break;
            case "transaction_failed":
                sender.sendMessage("§cTransaction failed due to a technical error. Your currency has been refunded.");
                break;
            case "unknown_error":
                sender.sendMessage("§cAn unknown error occurred. Please contact an administrator.");
                break;
            default:
                sender.sendMessage("§cTransaction failed: " + errorCode);
                break;
        }
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
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            
            // Check if it's a subcommand first
            if ("confirm".startsWith(input)) {
                completions.add("confirm");
            } else if ("cancel".startsWith(input)) {
                completions.add("cancel");
            } else {
                // Player names (exclude sender if it's a player) - using cached names
                for (String playerName : getCachedPlayerNames()) {
                    if (playerName.toLowerCase().startsWith(input)) {
                        // Don't suggest self if sender is a player
                        if (!(sender instanceof Player) || !playerName.equalsIgnoreCase(sender.getName())) {
                            completions.add(playerName);
                        }
                    }
                }
                
                // Add subcommands if they match
                if ("confirm".startsWith(input)) {
                    completions.add("confirm");
                }
                if ("cancel".startsWith(input)) {
                    completions.add("cancel");
                }
            }
            
        } else if (args.length == 2) {
            // Currency names (only if first arg is a player name)
            String firstArg = args[0];
            if (!firstArg.equalsIgnoreCase("confirm") && !firstArg.equalsIgnoreCase("cancel")) {
                List<String> currencies = plugin.getCurrencyManager().getAvailableCurrencies();
                String input = args[1].toLowerCase();
                
                for (String currency : currencies) {
                    if (currency.toLowerCase().startsWith(input)) {
                        // Only suggest currencies the sender has access to
                        if (sender.hasPermission("payedtools.currency." + currency) || 
                            sender.hasPermission("payedtools.currency.*")) {
                            completions.add(currency);
                        }
                    }
                }
                
                // If no input, show all available currencies
                if (input.isEmpty()) {
                    for (String currency : currencies) {
                        if (sender.hasPermission("payedtools.currency." + currency) || 
                            sender.hasPermission("payedtools.currency.*")) {
                            completions.add(currency);
                        }
                    }
                }
            }
            
        } else if (args.length == 3) {
            // Amount suggestions (only if we have player and currency)
            String firstArg = args[0];
            String secondArg = args[1];
            
            if (!firstArg.equalsIgnoreCase("confirm") && !firstArg.equalsIgnoreCase("cancel") &&
                !secondArg.isEmpty() && plugin.getCurrencyManager().isCurrency(secondArg)) {
                
                String input = args[2].toLowerCase();
                
                // Get player's balance for smart suggestions
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), secondArg);
                    
                    // Suggest common amounts based on balance
                    List<String> amountSuggestions = getSmartAmountSuggestions(balance, input);
                    completions.addAll(amountSuggestions);
                } else {
                    // Console suggestions
                    List<String> amountSuggestions = getBasicAmountSuggestions(input);
                    completions.addAll(amountSuggestions);
                }
            }
        }
        
        // Sort and limit completions
        completions.sort(String.CASE_INSENSITIVE_ORDER);
        return completions.size() > 10 ? completions.subList(0, 10) : completions;
    }
    
    /**
     * Get smart amount suggestions based on player's balance
     */
    private List<String> getSmartAmountSuggestions(double balance, String input) {
        List<String> suggestions = new ArrayList<>();
        
        // Common amounts
        String[] commonAmounts = {"100", "500", "1000", "5000", "10000"};
        
        for (String amount : commonAmounts) {
            if (amount.startsWith(input) && Double.parseDouble(amount) <= balance) {
                suggestions.add(amount);
            }
        }
        
        // Percentage suggestions (10%, 25%, 50%, 75%, 100%)
        double[] percentages = {0.1, 0.25, 0.5, 0.75, 1.0};
        String[] percentLabels = {"10%", "25%", "50%", "75%", "all"};
        
        for (int i = 0; i < percentages.length; i++) {
            double amount = balance * percentages[i];
            if (amount >= 1) { // Only suggest if amount is meaningful
                String formattedAmount = amountParser.format(Math.floor(amount));
                if (formattedAmount.startsWith(input)) {
                    suggestions.add(formattedAmount);
                }
                // Also add percentage format
                if (percentLabels[i].startsWith(input)) {
                    suggestions.add(percentLabels[i]);
                }
            }
        }
        
        // Format suggestions (k, M, B)
        if (balance >= 1000) {
            String[] formats = {"1k", "5k", "10k", "50k", "100k"};
            for (String format : formats) {
                if (format.startsWith(input)) {
                    suggestions.add(format);
                }
            }
        }
        
        if (balance >= 1000000) {
            String[] formats = {"1M", "5M", "10M"};
            for (String format : formats) {
                if (format.startsWith(input)) {
                    suggestions.add(format);
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * Get basic amount suggestions for console
     */
    private List<String> getBasicAmountSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();
        
        String[] amounts = {"100", "500", "1000", "5000", "10000", "100k", "1M", "10M"};
        
        for (String amount : amounts) {
            if (amount.startsWith(input)) {
                suggestions.add(amount);
            }
        }
        
        return suggestions;
    }
    
    /**
     * Update player name cache for tab completion optimization
     */
    private void updatePlayerCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlayerCacheUpdate > PLAYER_CACHE_UPDATE_INTERVAL) {
            cachedPlayerNames.clear();
            Bukkit.getOnlinePlayers().forEach(p -> cachedPlayerNames.add(p.getName()));
            lastPlayerCacheUpdate = currentTime;
        }
    }
    
    /**
     * Get cached player names for tab completion
     */
    private Set<String> getCachedPlayerNames() {
        updatePlayerCache();
        return cachedPlayerNames;
    }
}
