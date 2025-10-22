package ve.nottabaker.payedtools.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ve.nottabaker.payedtools.PayEdtools;

/**
 * /payreload command implementation
 */
public class PayReloadCommand implements CommandExecutor {
    
    private final PayEdtools plugin;
    
    public PayReloadCommand(PayEdtools plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("payedtools.admin")) {
            if (sender instanceof Player) {
                plugin.getMessageManager().send((Player) sender, "no-permission");
            } else {
                sender.sendMessage("You don't have permission to use this command!");
            }
            return true;
        }
        
        // Reload plugin
        try {
            plugin.reload();
            
            if (sender instanceof Player) {
                plugin.getMessageManager().send((Player) sender, "reload-success");
            } else {
                sender.sendMessage("Configuration reloaded successfully!");
            }
        } catch (Exception e) {
            if (sender instanceof Player) {
                plugin.getMessageManager().send((Player) sender, "reload-error");
            } else {
                sender.sendMessage("Error reloading configuration! Check console for details.");
            }
        }
        
        return true;
    }
}
