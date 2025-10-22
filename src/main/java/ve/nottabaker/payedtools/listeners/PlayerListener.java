package ve.nottabaker.payedtools.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ve.nottabaker.payedtools.PayEdtools;

/**
 * Handles player-related events
 */
public class PlayerListener implements Listener {
    
    private final PayEdtools plugin;
    
    public PlayerListener(PayEdtools plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Clean up player data on quit
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cancel any pending confirmations
        plugin.getConfirmationManager().cancel(event.getPlayer().getUniqueId());
    }
}
