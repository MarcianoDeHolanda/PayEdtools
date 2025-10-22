package ve.nottabaker.payedtools.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.managers.TransactionManager.TransactionStats;

import java.util.*;

/**
 * /paystats command implementation
 */
public class PayStatsCommand implements CommandExecutor, TabCompleter {
    
    private final PayEdtools plugin;
    
    public PayStatsCommand(PayEdtools plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        // Check permission
        if (!player.hasPermission("payedtools.stats")) {
            plugin.getMessageManager().send(player, "no-permission");
            return true;
        }
        
        // Determine target player
        UUID targetUUID;
        String targetName;
        
        if (args.length > 0) {
            // Check permission to view others' stats
            if (!player.hasPermission("payedtools.stats.others")) {
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
        
        // Get stats
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TransactionStats stats = plugin.getTransactionManager().getTransactionStats(targetUUID);
            
            // Display on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                displayStats(player, targetName, stats);
            });
        });
        
        return true;
    }
    
    /**
     * Display transaction statistics to player
     */
    private void displayStats(Player player, String targetName, TransactionStats stats) {
        // Header
        plugin.getMessageManager().sendRaw(player, "stats-header", null);
        
        // Sent stats
        Map<String, String> sentPlaceholders = new HashMap<>();
        sentPlaceholders.put("sent", String.valueOf(stats.getSentCount()));
        sentPlaceholders.put("sent-amount", String.valueOf(stats.getSentTotal()));
        plugin.getMessageManager().sendRaw(player, "stats-sent", sentPlaceholders);
        
        // Received stats
        Map<String, String> receivedPlaceholders = new HashMap<>();
        receivedPlaceholders.put("received", String.valueOf(stats.getReceivedCount()));
        receivedPlaceholders.put("received-amount", String.valueOf(stats.getReceivedTotal()));
        plugin.getMessageManager().sendRaw(player, "stats-received", receivedPlaceholders);
        
        // Footer
        plugin.getMessageManager().sendRaw(player, "stats-footer", null);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender.hasPermission("payedtools.stats.others")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}
