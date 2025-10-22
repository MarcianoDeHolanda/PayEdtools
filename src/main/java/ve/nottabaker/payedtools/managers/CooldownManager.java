package ve.nottabaker.payedtools.managers;

import org.bukkit.entity.Player;
import ve.nottabaker.payedtools.PayEdtools;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cooldowns between payments
 */
public class CooldownManager {
    
    private final PayEdtools plugin;
    private final Map<UUID, Long> cooldowns;
    
    public CooldownManager(PayEdtools plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if player is on cooldown
     */
    public boolean isOnCooldown(Player player) {
        if (!plugin.getConfigManager().isCooldownEnabled()) {
            return false;
        }
        
        if (player.hasPermission("payedtools.bypass.cooldown")) {
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        Long lastUsed = cooldowns.get(uuid);
        
        if (lastUsed == null) {
            return false;
        }
        
        long cooldownTime = plugin.getConfigManager().getCooldownTime() * 1000L;
        return System.currentTimeMillis() - lastUsed < cooldownTime;
    }
    
    /**
     * Get remaining cooldown time in seconds
     */
    public int getRemainingCooldown(Player player) {
        if (!isOnCooldown(player)) {
            return 0;
        }
        
        UUID uuid = player.getUniqueId();
        Long lastUsed = cooldowns.get(uuid);
        
        if (lastUsed == null) {
            return 0;
        }
        
        long cooldownTime = plugin.getConfigManager().getCooldownTime() * 1000L;
        long elapsed = System.currentTimeMillis() - lastUsed;
        long remaining = cooldownTime - elapsed;
        
        return (int) Math.ceil(remaining / 1000.0);
    }
    
    /**
     * Set cooldown for player
     */
    public void setCooldown(Player player) {
        if (!plugin.getConfigManager().isCooldownEnabled()) {
            return;
        }
        
        if (player.hasPermission("payedtools.bypass.cooldown")) {
            return;
        }
        
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Remove cooldown for player
     */
    public void removeCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
    
    /**
     * Clear all cooldowns
     */
    public void clear() {
        cooldowns.clear();
    }
}
