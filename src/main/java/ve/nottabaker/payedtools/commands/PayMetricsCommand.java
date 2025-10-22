package ve.nottabaker.payedtools.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ve.nottabaker.payedtools.PayEdtools;

/**
 * Command to view performance metrics
 */
public class PayMetricsCommand implements CommandExecutor {
    
    private final PayEdtools plugin;
    
    public PayMetricsCommand(PayEdtools plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("payedtools.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        // Send performance summary
        sender.sendMessage(plugin.getPerformanceMetrics().getPerformanceSummary());
        
        return true;
    }
}
