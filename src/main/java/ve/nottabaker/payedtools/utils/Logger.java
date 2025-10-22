package ve.nottabaker.payedtools.utils;

import ve.nottabaker.payedtools.PayEdtools;

import java.util.logging.Level;

/**
 * Centralized logging utility with debug support
 */
public class Logger {
    
    private static PayEdtools plugin;
    
    public static void setPlugin(PayEdtools p) {
        plugin = p;
    }
    
    /**
     * Log info message
     */
    public static void info(String message) {
        if (plugin != null) {
            plugin.getLogger().info(message);
        }
    }
    
    /**
     * Log warning message
     */
    public static void warning(String message) {
        if (plugin != null) {
            plugin.getLogger().warning(message);
        }
    }
    
    /**
     * Log error message
     */
    public static void error(String message) {
        if (plugin != null) {
            plugin.getLogger().severe(message);
        }
    }
    
    /**
     * Log error message with exception
     */
    public static void error(String message, Throwable throwable) {
        if (plugin != null) {
            plugin.getLogger().log(Level.SEVERE, message, throwable);
        }
    }
    
    /**
     * Log debug message (only if debug is enabled)
     */
    public static void debug(String message) {
        if (plugin != null && plugin.getConfigManager() != null && plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}
