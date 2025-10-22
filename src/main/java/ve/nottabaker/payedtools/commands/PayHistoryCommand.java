package ve.nottabaker.payedtools.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.models.Transaction;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * /payhistory command implementation
 */
public class PayHistoryCommand implements CommandExecutor, TabCompleter {
    
    private final PayEdtools plugin;
    private final SimpleDateFormat dateFormat;
    
    public PayHistoryCommand(PayEdtools plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("MM/dd HH:mm");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        // Check permission
        if (!player.hasPermission("payedtools.history")) {
            plugin.getMessageManager().send(player, "no-permission");
            return true;
        }
        
        // Determine target player
        UUID targetUUID;
        String targetName;
        
        if (args.length > 0) {
            // Check permission to view others' history
            if (!player.hasPermission("payedtools.history.others")) {
                plugin.getMessageManager().send(player, "no-permission");
                return true;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[0]);
                plugin.getMessageManager().send(player, "invalid-player", placeholders);
                return true;
            }
            
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            targetUUID = player.getUniqueId();
            targetName = player.getName();
        }
        
        // Get transaction history
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Transaction> history = plugin.getTransactionManager().getTransactionHistory(
                targetUUID, 
                plugin.getConfigManager().getMaxHistorySize()
            );
            
            // Display on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                displayHistory(player, targetName, history);
            });
        });
        
        return true;
    }
    
    /**
     * Display transaction history to player
     */
    private void displayHistory(Player player, String targetName, List<Transaction> history) {
        if (history.isEmpty()) {
            plugin.getMessageManager().send(player, "history-empty");
            return;
        }
        
        // Header
        plugin.getMessageManager().sendRaw(player, "history-header", null);
        
        // Entries
        for (Transaction transaction : history) {
            UUID senderUUID = transaction.getSender();
            boolean isSender = senderUUID != null && senderUUID.equals(player.getUniqueId());
            
            String type = isSender ? "§c↑ Sent" : "§a↓ Received";
            String otherPlayer;
            
            if (isSender) {
                OfflinePlayer receiver = Bukkit.getOfflinePlayer(transaction.getReceiver());
                otherPlayer = receiver.getName();
            } else {
                if (senderUUID != null) {
                    OfflinePlayer sender = Bukkit.getOfflinePlayer(senderUUID);
                    otherPlayer = sender.getName();
                } else {
                    otherPlayer = "§6§lCONSOLE§r";
                }
            }
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("timestamp", dateFormat.format(new Date(transaction.getTimestamp())));
            placeholders.put("type", type);
            placeholders.put("amount", String.valueOf(transaction.getAmount()));
            placeholders.put("currency", transaction.getCurrency());
            placeholders.put("player", otherPlayer);
            
            plugin.getMessageManager().sendRaw(player, "history-entry", placeholders);
        }
        
        // Footer
        Map<String, String> footerPlaceholders = new HashMap<>();
        footerPlaceholders.put("page", "1");
        footerPlaceholders.put("maxpage", "1");
        plugin.getMessageManager().sendRaw(player, "history-footer", footerPlaceholders);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender.hasPermission("payedtools.history.others")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}
