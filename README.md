# PayEdtools - Production-Ready EdTools Currency Transfer Addon

**Author:** nottabaker  
**Version:** 1.0.0  
**License:** All Rights Reserved

## Description

PayEdtools is a comprehensive, production-ready addon for EdTools that implements a powerful `/pay` command system for transferring currencies between players. Built with enterprise-grade features including transaction logging, rate limiting, confirmations, and complete error handling.

## Features

### Core Functionality
- **Currency Transfer:** Send any EdTools currency to other players
- **Amount Formatting:** Support for k, M, B, T suffixes (e.g., 1.5M = 1,500,000)
- **Async Operations:** Non-blocking transactions for optimal server performance
- **Transaction Rollback:** Automatic rollback on failed transactions

### Security & Protection
- **Rate Limiting:** Prevents spam and abuse
- **Cooldown System:** Configurable cooldown between payments
- **Transaction Limits:** Min/max amount restrictions
- **Confirmation System:** Requires confirmation for large amounts
- **Permission System:** Granular permission control
- **Input Validation:** Comprehensive validation and sanitization

### Data & Logging
- **Transaction History:** Complete history with database storage
- **Statistics:** View sent/received transaction stats
- **Console Logging:** Detailed transaction logs
- **Database Support:** SQLite (default) and MySQL support
- **Batch Operations:** Efficient batch database writes

### Advanced Features
- **Tax System:** Optional transaction fees/taxes
- **Currency Whitelist/Blacklist:** Control which currencies can be transferred
- **Offline Transfers:** Send currency to offline players
- **PlaceholderAPI Integration:** Full placeholder support
- **Multi-language Ready:** Fully customizable messages
- **Admin Tools:** Reload command and bypass permissions

## Installation

1. **Requirements:**
   - Minecraft Server 1.16 - 1.21.4 (Paper/Spigot)
   - Java 21
   - EdTools plugin installed

2. **Installation Steps:**
   ```
   1. Stop your server
   2. Place PayEdtools.jar in your plugins folder
   3. Ensure EdTools is installed and working
   4. Start your server
   5. Configure config.yml to your needs
   6. Reload with /payreload
   ```

## Commands

| Command | Description | Permission | Aliases |
|---------|-------------|------------|---------|
| `/pay <player> <currency> <amount>` | Transfer currency to a player | `payedtools.use` | /transfer, /send |
| `/pay confirm` | Confirm a pending large transaction | `payedtools.use` | - |
| `/pay cancel` | Cancel a pending transaction | `payedtools.use` | - |
| `/payall <currency> <amount>` | Transfer currency to all online players | `payedtools.payall` | /payeveryone, /payonline |
| `/payreload` | Reload configuration | `payedtools.admin` | - |
| `/payhistory [player]` | View transaction history | `payedtools.history` | /transactions, /history |
| `/paystats [player]` | View payment statistics | `payedtools.stats` | /stats |
| `/paymetrics` | View performance metrics | `payedtools.admin` | - |
| `/paytest` | Run performance tests | `payedtools.admin` | - |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `payedtools.*` | Access to all features | op |
| `payedtools.use` | Use /pay command | true |
| `payedtools.payall` | Use /payall command | op |
| `payedtools.admin` | Administrative commands | op |
| `payedtools.history` | View own transaction history | true |
| `payedtools.history.others` | View others' transaction history | op |
| `payedtools.stats` | View own payment statistics | true |
| `payedtools.stats.others` | View others' payment statistics | op |
| `payedtools.bypass.cooldown` | Bypass payment cooldowns | op |
| `payedtools.bypass.limits` | Bypass payment limits | op |

## Configuration

The plugin comes with a comprehensive `config.yml` with over 50 configurable options:

### Currency Formats
```yaml
currency-formats:
  enabled: true
  formats:
    k: 1000
    m: 1000000
    b: 1000000000
    t: 1000000000000
  allow-decimals: true
```

### Cooldown & Limits
```yaml
cooldown:
  enabled: true
  time: 5  # seconds

limits:
  enabled: true
  minimum: 1.0
  maximum: 0  # 0 = unlimited
```

### Rate Limiting
```yaml
rate-limit:
  enabled: true
  max-transactions: 10
  time-window: 60  # seconds
```

### Transaction Tax
```yaml
tax:
  enabled: false
  percentage: 5  # 5%
  fixed: 0
  minimum-for-tax: 1000
```

## Usage Examples

```
# Basic transfer
/pay nottabaker farm-coins 1000

# Using format shortcuts
/pay player mining-coins 10k     # 10,000
/pay player farm-coins 1.5M      # 1,500,000
/pay player tokens 2B            # 2,000,000,000

# Bulk payment to all online players
/payall farm-coins 1000          # Pay 1000 farm-coins to everyone online

# View history
/payhistory
/payhistory nottabaker

# View statistics
/paystats
/paystats nottabaker

# View performance metrics
/paymetrics

# Performance testing
/paytest stress 50 10 100 100 10000 0    # Stress test with 50 players, 10 threads, 100 transactions each
/paytest load 100 5 50 5 2000 50         # Load test with gradual increase
/paytest benchmark                        # Comprehensive benchmark test
/paytest results detailed                 # Show detailed test results
/paytest status                           # Show tester status

# Admin commands
/payreload
```

## Database

PayEdtools supports two database types:

### SQLite (Default)
- Automatically created in plugin folder
- No additional setup required
- Perfect for small to medium servers

### MySQL
```yaml
database:
  type: "MYSQL"
  mysql:
    host: "localhost"
    port: 3306
    database: "payedtools"
    username: "root"
    password: "password"
```

## Performance

PayEdtools is optimized for production use and high-load servers:

### Core Optimizations
- **Async Operations:** All database operations are async
- **Connection Pooling:** HikariCP for MySQL with optimized settings
- **Batch Processing:** Process up to 200 transactions per batch
- **Balance Caching:** 30-second cache for player balances
- **Thread Pool:** 12 threads for async operations
- **Memory Management:** Automatic cleanup and cache management

### High-Load Optimizations
- **PayAll Command:** Optimized bulk payments with batch processing
- **Tab Completion Cache:** Cached player names for faster suggestions
- **Performance Metrics:** Real-time monitoring of system performance
- **Database Indexes:** Optimized indexes for fast queries
- **Connection Pool:** 20 max connections, 5 minimum idle for MySQL

### Performance Monitoring
- **Real-time Metrics:** Track transaction success rates and processing times
- **Cache Performance:** Monitor cache hit ratios
- **Database Stats:** Track operations and batch processing efficiency
- **Error Tracking:** Monitor failed transactions and rollbacks

### Performance Testing System
- **Stress Tests:** Simulate high-load scenarios with multiple concurrent players
- **Load Tests:** Gradually increase load to find breaking points
- **Benchmark Tests:** Comprehensive testing with different transaction scenarios
- **Simulated Players:** Create virtual players for realistic testing
- **Detailed Reports:** Track TPS, success rates, and processing times
- **Configurable Tests:** Customize test parameters for different scenarios

## Integration

### PlaceholderAPI
Full PlaceholderAPI support in all messages. Configure in config.yml:
```yaml
integrations:
  placeholderapi:
    enabled: true
```

### Discord Webhooks
Log large transactions to Discord:
```yaml
integrations:
  discord:
    enabled: true
    webhook-url: "your-webhook-url"
    log-threshold: 1000000
```

## Troubleshooting

### Enable Debug Mode
```yaml
settings:
  debug: true
```

### Common Issues

**Currency not found:**
- Ensure the currency exists in EdTools
- Check spelling (case-sensitive)
- Verify currency is not blocked in config

**Insufficient funds:**
- Remember tax is added to the amount
- Check actual balance with EdTools commands

**Rate limit exceeded:**
- Wait for the time window to reset
- Adjust rate-limit settings in config

## API for Developers

PayEdtools can be used as a dependency in your own plugins:

```java
PayEdtools api = (PayEdtools) Bukkit.getPluginManager().getPlugin("PayEdtools");

// Process a transaction programmatically
api.getTransactionManager().processTransaction(
    senderUUID,
    receiverUUID,
    "farm-coins",
    1000.0,
    true
).thenAccept(result -> {
    if (result.isSuccess()) {
        // Transaction successful
    }
});
```

## Support

For support, bug reports, or feature requests:
- **Author:** nottabaker
- **Email:** support@nottabaker.ve
- **Website:** https://nottabaker.ve

## License

Copyright © 2024 nottabaker. All Rights Reserved.

This software is proprietary and confidential. Unauthorized copying, distribution, or modification is strictly prohibited.

## Changelog

### Version 1.0.0 (Initial Release)
- Complete /pay command implementation
- **NEW:** /payall command for bulk payments
- **NEW:** Performance optimizations for 200+ players
- **NEW:** HikariCP connection pooling for MySQL
- **NEW:** Balance caching system
- **NEW:** Performance metrics monitoring
- **NEW:** Optimized tab completion
- **NEW:** Performance testing system with simulated players
- **NEW:** Stress, load, and benchmark testing capabilities
- Transaction history and statistics
- Rate limiting and cooldown system
- Confirmation system for large amounts
- Database storage (SQLite/MySQL)
- Tax system
- PlaceholderAPI integration
- Comprehensive error handling
- Full configuration system
- Admin tools and bypass permissions
