package ve.nottabaker.payedtools.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.models.Transaction;
import ve.nottabaker.payedtools.utils.AmountParser;
import ve.nottabaker.payedtools.utils.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Handles player-related events
 */
public class PlayerListener implements Listener {
    
    private final PayEdtools plugin;
    private final AmountParser amountParser;
    private final File lastLoginFile;
    
    public PlayerListener(PayEdtools plugin) {
        this.plugin = plugin;
        this.amountParser = new AmountParser(plugin);
        this.lastLoginFile = new File(plugin.getDataFolder(), "last-logins.properties");
    }
    
    /**
     * Handle player join - process pending transactions and notify about offline transactions
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Process pending transactions first
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Process any pending transactions for this player
                plugin.getTransactionManager().processPendingTransactions(event.getPlayer().getUniqueId());
                
                // Check for transactions since last login
                long lastLoginTime = getLastLoginTime(event.getPlayer().getUniqueId());
                List<Transaction> recentTransactions = getRecentTransactions(event.getPlayer().getUniqueId(), lastLoginTime);
                
                if (!recentTransactions.isEmpty()) {
                    // Notify player about offline transactions
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        notifyOfflineTransactions(event.getPlayer(), recentTransactions);
                    });
                }
                
                // Update last login time
                updateLastLoginTime(event.getPlayer().getUniqueId());
            } catch (Exception e) {
                Logger.error("Error processing pending transactions for " + event.getPlayer().getName(), e);
            }
        });
    }
    
    /**
     * Clean up player data on quit
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cancel any pending confirmations
        plugin.getConfirmationManager().cancel(event.getPlayer().getUniqueId());
    }
    
    /**
     * Get recent transactions for a player
     */
    private List<Transaction> getRecentTransactions(java.util.UUID playerUUID, long sinceTime) {
        // Get transactions since the specified time
        return plugin.getTransactionManager().getTransactionHistorySince(playerUUID, sinceTime, 50);
    }
    
    /**
     * Notify player about offline transactions
     */
    private void notifyOfflineTransactions(org.bukkit.entity.Player player, List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return;
        }
        
        // Filter only received transactions (where player is receiver)
        List<Transaction> receivedTransactions = transactions.stream()
                .filter(t -> t.getReceiver().equals(player.getUniqueId()))
                .toList();
        
        if (receivedTransactions.isEmpty()) {
            return;
        }
        
        // Group transactions by currency
        Map<String, Double> currencyTotals = new HashMap<>();
        int transactionCount = 0;
        
        for (Transaction transaction : receivedTransactions) {
            String currency = transaction.getCurrency();
            double amount = transaction.getAmount();
            
            currencyTotals.merge(currency, amount, Double::sum);
            transactionCount++;
        }
        
        // Send notification
        player.sendMessage("§8§m----------&r §6Offline Transactions &8§m----------");
        player.sendMessage("§eYou received money while offline:");
        
        for (Map.Entry<String, Double> entry : currencyTotals.entrySet()) {
            String currency = entry.getKey();
            double total = entry.getValue();
            player.sendMessage("§a+ " + amountParser.format(total) + " " + currency);
        }
        
        player.sendMessage("§7Total: " + transactionCount + " transactions");
        player.sendMessage("§8§m----------------------------------------");
    }
    
    /**
     * Get the last login time for a player
     */
    private long getLastLoginTime(UUID playerUUID) {
        Properties props = new Properties();
        try {
            if (lastLoginFile.exists()) {
                try (FileReader reader = new FileReader(lastLoginFile)) {
                    props.load(reader);
                }
            }
            
            String lastLoginStr = props.getProperty(playerUUID.toString());
            if (lastLoginStr != null) {
                return Long.parseLong(lastLoginStr);
            }
        } catch (IOException | NumberFormatException e) {
            Logger.warning("Error reading last login time for " + playerUUID + ": " + e.getMessage());
        }
        
        // If no last login time found, return 24 hours ago as fallback
        return System.currentTimeMillis() - (24 * 60 * 60 * 1000);
    }
    
    /**
     * Update the last login time for a player
     */
    private void updateLastLoginTime(UUID playerUUID) {
        Properties props = new Properties();
        try {
            // Load existing properties
            if (lastLoginFile.exists()) {
                try (FileReader reader = new FileReader(lastLoginFile)) {
                    props.load(reader);
                }
            }
            
            // Update the last login time
            props.setProperty(playerUUID.toString(), String.valueOf(System.currentTimeMillis()));
            
            // Save back to file
            try (FileWriter writer = new FileWriter(lastLoginFile)) {
                props.store(writer, "Last login times for players");
            }
            
            Logger.debug("Updated last login time for " + playerUUID);
        } catch (IOException e) {
            Logger.warning("Error updating last login time for " + playerUUID + ": " + e.getMessage());
        }
    }
}
