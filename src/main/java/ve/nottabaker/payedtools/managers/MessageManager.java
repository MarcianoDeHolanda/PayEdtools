package ve.nottabaker.payedtools.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ve.nottabaker.payedtools.PayEdtools;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all plugin messages with PlaceholderAPI support
 */
public class MessageManager {
    
    private final PayEdtools plugin;
    private final Map<String, String> messages;
    private String prefix;
    
    public MessageManager(PayEdtools plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        loadMessages();
    }
    
    /**
     * Load all messages from config
     */
    public void loadMessages() {
        FileConfiguration config = plugin.getConfig();
        messages.clear();
        
        prefix = color(config.getString("messages.prefix", "&8[&6PayEdtools&8]&r "));
        
        // Load all message keys
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                if (!key.equals("prefix")) {
                    messages.put(key, color(config.getString("messages." + key)));
                }
            }
        }
    }
    
    /**
     * Reload messages
     */
    public void reload() {
        loadMessages();
    }
    
    /**
     * Get a message with placeholders replaced
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = messages.getOrDefault(key, "&cMessage not found: " + key);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        
        return message;
    }
    
    /**
     * Get a message without placeholders
     */
    public String getMessage(String key) {
        return getMessage(key, null);
    }
    
    /**
     * Send a message to a player with prefix
     */
    public void send(Player player, String key, Map<String, String> placeholders) {
        String message = prefix + getMessage(key, placeholders);
        
        // Apply PlaceholderAPI if available
        if (plugin.getConfigManager().isPlaceholderAPIEnabled()) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        
        player.sendMessage(message);
    }
    
    /**
     * Send a message to a player without prefix
     */
    public void sendRaw(Player player, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        
        // Apply PlaceholderAPI if available
        if (plugin.getConfigManager().isPlaceholderAPIEnabled()) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        
        player.sendMessage(message);
    }
    
    /**
     * Send a message with prefix
     */
    public void send(Player player, String key) {
        send(player, key, null);
    }
    
    /**
     * Color code translator
     */
    private String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * Get prefix
     */
    public String getPrefix() {
        return prefix;
    }
}
