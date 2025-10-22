package ve.nottabaker.payedtools.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ve.nottabaker.payedtools.PayEdtools;
import ve.nottabaker.payedtools.managers.TransactionManager.TransactionStats;
import ve.nottabaker.payedtools.models.Transaction;
import ve.nottabaker.payedtools.utils.Logger;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages database operations for transaction history
 */
public class DatabaseManager {
    
    private final PayEdtools plugin;
    private Connection connection;
    private HikariDataSource dataSource;
    
    public DatabaseManager(PayEdtools plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize database connection and tables
     */
    public void initialize() {
        try {
            String dbType = plugin.getConfigManager().getDatabaseType();
            
                    if (dbType.equalsIgnoreCase("SQLITE")) {
                        initializeSQLite();
                        createTables(connection);
                    } else if (dbType.equalsIgnoreCase("MYSQL")) {
                        initializeMySQL();
                        // Tables are created in initializeMySQL() method
                    } else {
                        Logger.error("Unknown database type: " + dbType);
                        return;
                    }
            Logger.info("Database initialized successfully");
            
        } catch (Exception e) {
            Logger.error("Failed to initialize database", e);
        }
    }
    
    /**
     * Initialize SQLite connection
     */
    private void initializeSQLite() throws SQLException {
        String filename = plugin.getConfigManager().getSqliteFilename();
        File dbFile = new File(plugin.getDataFolder(), filename);
        
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        
        Logger.debug("SQLite database connected: " + dbFile.getAbsolutePath());
    }
    
    /**
     * Initialize MySQL connection with HikariCP
     */
    private void initializeMySQL() throws SQLException {
        String host = plugin.getConfigManager().getMysqlHost();
        int port = plugin.getConfigManager().getMysqlPort();
        String database = plugin.getConfigManager().getMysqlDatabase();
        String username = plugin.getConfigManager().getMysqlUsername();
        String password = plugin.getConfigManager().getMysqlPassword();
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
                         "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Connection pool settings
        config.setMaximumPoolSize(plugin.getConfigManager().getMysqlMaxPoolSize());
        config.setMinimumIdle(plugin.getConfigManager().getMysqlMinIdle());
        config.setConnectionTimeout(plugin.getConfigManager().getMysqlConnectionTimeout());
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setLeakDetectionThreshold(60000); // 1 minute
        
        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        dataSource = new HikariDataSource(config);
        
        // Test connection and create tables
        try (Connection testConnection = dataSource.getConnection()) {
            createTables(testConnection);
        }
        
        Logger.debug("MySQL database connected with HikariCP: " + host + ":" + port);
        Logger.debug("Connection pool size: " + config.getMaximumPoolSize());
    }
    
    /**
     * Create necessary database tables
     */
    private void createTables(Connection conn) throws SQLException {
        // Create the main table first
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS transactions (
                id VARCHAR(36) PRIMARY KEY,
                sender VARCHAR(36) NOT NULL,
                receiver VARCHAR(36) NOT NULL,
                currency VARCHAR(64) NOT NULL,
                amount DOUBLE NOT NULL,
                tax DOUBLE DEFAULT 0,
                timestamp BIGINT NOT NULL
            )
        """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            Logger.debug("Transactions table created/verified");
            
            // Create last logins table
            String createLastLoginsSQL = """
                CREATE TABLE IF NOT EXISTS last_logins (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    last_login_time BIGINT NOT NULL
                )
            """;
            stmt.execute(createLastLoginsSQL);
            Logger.debug("Last logins table created/verified");
            
            // Create indexes separately (SQLite compatible)
            createIndexes(stmt);
        }
    }
    
    /**
     * Create database indexes
     */
    private void createIndexes(Statement stmt) throws SQLException {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_transactions_sender ON transactions (sender)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_receiver ON transactions (receiver)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions (timestamp)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_currency ON transactions (currency)"
        };
        
        for (String indexSQL : indexes) {
            try {
                stmt.execute(indexSQL);
                Logger.debug("Index created: " + indexSQL);
            } catch (SQLException e) {
                Logger.warning("Failed to create index: " + indexSQL + " - " + e.getMessage());
                // Continue with other indexes even if one fails
            }
        }
    }
    
    /**
     * Save a single transaction
     */
    public void saveTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (id, sender, receiver, currency, amount, tax, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transaction.getId().toString());
            // Handle console transactions (sender is null)
            String senderUUID = transaction.getSender() != null ? 
                transaction.getSender().toString() : "CONSOLE";
            stmt.setString(2, senderUUID);
            stmt.setString(3, transaction.getReceiver().toString());
            stmt.setString(4, transaction.getCurrency());
            stmt.setDouble(5, transaction.getAmount());
            stmt.setDouble(6, transaction.getTax());
            stmt.setLong(7, transaction.getTimestamp());
            
            stmt.executeUpdate();
            Logger.debug("Transaction saved: " + transaction.getId());
            
        } catch (SQLException e) {
            Logger.error("Failed to save transaction: " + transaction.getId(), e);
        }
    }
    
    /**
     * Save multiple transactions in a batch
     */
    public void saveTransactionBatch(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return;
        }
        
        String sql = "INSERT INTO transactions (id, sender, receiver, currency, amount, tax, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            
            for (Transaction transaction : transactions) {
                stmt.setString(1, transaction.getId().toString());
                // Handle console transactions (sender is null)
                String senderUUID = transaction.getSender() != null ? 
                    transaction.getSender().toString() : "CONSOLE";
                stmt.setString(2, senderUUID);
                stmt.setString(3, transaction.getReceiver().toString());
                stmt.setString(4, transaction.getCurrency());
                stmt.setDouble(5, transaction.getAmount());
                stmt.setDouble(6, transaction.getTax());
                stmt.setLong(7, transaction.getTimestamp());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
            
            Logger.debug("Batch of " + transactions.size() + " transactions saved");
            
        } catch (SQLException e) {
            Logger.error("Failed to save transaction batch", e);
            try (Connection rollbackConn = getConnection()) {
                rollbackConn.rollback();
            } catch (SQLException ex) {
                Logger.error("Failed to rollback batch", ex);
            }
        }
    }
    
    /**
     * Get transaction history for a player
     */
    public List<Transaction> getTransactionHistory(UUID uuid, int limit) {
        List<Transaction> history = new ArrayList<>();
        
        String sql = "SELECT * FROM transactions WHERE sender = ? OR receiver = ? ORDER BY timestamp DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, uuid.toString());
            stmt.setInt(3, limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                // Handle console transactions (sender stored as "CONSOLE")
                String senderStr = rs.getString("sender");
                UUID senderUUID = "CONSOLE".equals(senderStr) ? null : UUID.fromString(senderStr);
                
                Transaction transaction = new Transaction(
                    UUID.fromString(rs.getString("id")),
                    senderUUID,
                    UUID.fromString(rs.getString("receiver")),
                    rs.getString("currency"),
                    rs.getDouble("amount"),
                    rs.getLong("timestamp")
                );
                transaction.setTax(rs.getDouble("tax"));
                
                history.add(transaction);
            }
            
        } catch (SQLException e) {
            Logger.error("Failed to get transaction history for " + uuid, e);
        }
        
        return history;
    }
    
    /**
     * Get transaction history for a player since a specific time
     */
    public List<Transaction> getTransactionHistorySince(UUID uuid, long sinceTime, int limit) {
        List<Transaction> history = new ArrayList<>();
        
        String sql = "SELECT * FROM transactions WHERE (sender = ? OR receiver = ?) AND timestamp >= ? ORDER BY timestamp DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, uuid.toString());
            stmt.setLong(3, sinceTime);
            stmt.setInt(4, limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                // Handle console transactions (sender stored as "CONSOLE")
                String senderStr = rs.getString("sender");
                UUID senderUUID = "CONSOLE".equals(senderStr) ? null : UUID.fromString(senderStr);
                
                Transaction transaction = new Transaction(
                    UUID.fromString(rs.getString("id")),
                    senderUUID,
                    UUID.fromString(rs.getString("receiver")),
                    rs.getString("currency"),
                    rs.getDouble("amount"),
                    rs.getLong("timestamp")
                );
                transaction.setTax(rs.getDouble("tax"));
                
                history.add(transaction);
            }
            
        } catch (SQLException e) {
            Logger.error("Failed to get transaction history since " + sinceTime + " for " + uuid, e);
        }
        
        return history;
    }
    
    /**
     * Get transaction statistics for a player
     */
    public TransactionStats getTransactionStats(UUID uuid) {
        int sentCount = 0;
        int receivedCount = 0;
        double sentTotal = 0;
        double receivedTotal = 0;
        
        // Get sent transactions
        String sentSQL = "SELECT COUNT(*) as count, SUM(amount) as total FROM transactions WHERE sender = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sentSQL)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                sentCount = rs.getInt("count");
                sentTotal = rs.getDouble("total");
            }
        } catch (SQLException e) {
            Logger.error("Failed to get sent stats for " + uuid, e);
        }
        
        // Get received transactions
        String receivedSQL = "SELECT COUNT(*) as count, SUM(amount) as total FROM transactions WHERE receiver = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(receivedSQL)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                receivedCount = rs.getInt("count");
                receivedTotal = rs.getDouble("total");
            }
        } catch (SQLException e) {
            Logger.error("Failed to get received stats for " + uuid, e);
        }
        
        return new TransactionStats(sentCount, receivedCount, sentTotal, receivedTotal);
    }
    
    /**
     * Cleanup old transactions
     */
    public void cleanupOldTransactions() {
        int days = plugin.getConfigManager().getCleanupAfterDays();
        if (days <= 0) {
            return;
        }
        
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        String sql = "DELETE FROM transactions WHERE timestamp < ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, cutoffTime);
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                Logger.info("Cleaned up " + deleted + " old transactions");
            }
        } catch (SQLException e) {
            Logger.error("Failed to cleanup old transactions", e);
        }
    }
    
    /**
     * Backup database
     */
    public void backup() {
        // Implementation would depend on database type
        Logger.info("Database backup requested (not yet implemented)");
    }
    
    /**
     * Get the last login time for a player from database
     */
    public long getLastLoginTime(UUID playerUUID) {
        String sql = "SELECT last_login_time FROM last_logins WHERE player_uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_login_time");
            }
        } catch (SQLException e) {
            Logger.error("Error getting last login time for " + playerUUID, e);
        }
        
        // If no last login time found, return 24 hours ago as fallback
        return System.currentTimeMillis() - (24 * 60 * 60 * 1000);
    }
    
    /**
     * Update the last login time for a player in database
     */
    public void updateLastLoginTime(UUID playerUUID) {
        // Use UPSERT syntax compatible with both MySQL and SQLite
        String sql;
        if (plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("MYSQL")) {
            sql = "INSERT INTO last_logins (player_uuid, last_login_time) VALUES (?, ?) ON DUPLICATE KEY UPDATE last_login_time = VALUES(last_login_time)";
        } else {
            sql = "INSERT OR REPLACE INTO last_logins (player_uuid, last_login_time) VALUES (?, ?)";
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setLong(2, System.currentTimeMillis());
            
            stmt.executeUpdate();
            Logger.debug("Updated last login time for " + playerUUID);
        } catch (SQLException e) {
            Logger.error("Error updating last login time for " + playerUUID, e);
        }
    }
    
    /**
     * Get a database connection from the pool or direct for SQLite.
     */
    private Connection getConnection() throws SQLException {
        if (plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("MYSQL") && dataSource != null) {
            return dataSource.getConnection();
        }
        // Fallback for SQLite or if MySQL dataSource is not initialized
        return connection;
    }
    
    /**
     * Close database connection
     */
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                Logger.info("Database connection closed");
            }
            
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                Logger.info("HikariCP connection pool closed");
            }
        } catch (SQLException e) {
            Logger.error("Error closing database connection", e);
        }
    }
}
