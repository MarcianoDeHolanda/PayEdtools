package ve.nottabaker.payedtools.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.models.PendingTransaction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages transaction confirmations for large amounts
 */
public class ConfirmationManager {
    
    private final PayEdtools plugin;
    private final Map<UUID, PendingTransaction> pendingConfirmations;
    
    public ConfirmationManager(PayEdtools plugin) {
        this.plugin = plugin;
        this.pendingConfirmations = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if amount requires confirmation
     */
    public boolean requiresConfirmation(double amount) {
        if (!plugin.getConfigManager().isConfirmationEnabled()) {
            return false;
        }
        
        return amount >= plugin.getConfigManager().getConfirmationThreshold();
    }
    
    /**
     * Create a pending confirmation
     */
    public void createConfirmation(Player sender, UUID receiver, String currency, double amount) {
        UUID senderId = sender.getUniqueId();
        
        PendingTransaction pending = new PendingTransaction(
            senderId,
            receiver,
            currency,
            amount,
            System.currentTimeMillis()
        );
        
        pendingConfirmations.put(senderId, pending);
        
        // Schedule timeout
        int timeout = plugin.getConfigManager().getConfirmationTimeout();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingConfirmations.containsKey(senderId)) {
                pendingConfirmations.remove(senderId);
                
                Player player = Bukkit.getPlayer(senderId);
                if (player != null && player.isOnline()) {
                    plugin.getMessageManager().send(player, "confirmation-expired");
                }
            }
        }, timeout * 20L);
    }
    
    /**
     * Get pending confirmation for player
     */
    public PendingTransaction getPendingConfirmation(UUID uuid) {
        return pendingConfirmations.get(uuid);
    }
    
    /**
     * Confirm a transaction
     */
    public PendingTransaction confirm(UUID uuid) {
        return pendingConfirmations.remove(uuid);
    }
    
    /**
     * Cancel a transaction
     */
    public boolean cancel(UUID uuid) {
        return pendingConfirmations.remove(uuid) != null;
    }
    
    /**
     * Check if player has pending confirmation
     */
    public boolean hasPendingConfirmation(UUID uuid) {
        return pendingConfirmations.containsKey(uuid);
    }
    
    /**
     * Cleanup all pending confirmations
     */
    public void cleanup() {
        pendingConfirmations.clear();
    }
}
