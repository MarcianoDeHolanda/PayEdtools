# PayEdtools - Production-Ready EdTools Currency Transfer Addon

**Author:** nottabaker  
**Version:** 1.0.0  
**License:** Free to Use - Open Source  
**🤖 Developed 100% with [Codella AI](https://codella.ai/)** - This entire plugin was created using Codella's artificial intelligence platform

## 🤝 Open Source & Collaborations

This plugin is **completely free to use** and I'm **open to collaborations**! Feel free to:
- Use this plugin on your server
- Fork and modify the code
- Submit pull requests with improvements
- Report bugs or suggest new features
- Contribute to the development

**Contact:** If you want to collaborate or have questions, feel free to reach out!

## Description

PayEdtools is a comprehensive, production-ready addon for EdTools that implements a powerful `/pay` command system for transferring currencies between players. Built with enterprise-grade features including transaction logging, rate limiting, confirmations, and complete error handling.

**🚀 Codella AI-Powered Development:** This plugin represents a complete end-to-end AI development project using [Codella AI](https://codella.ai/), showcasing how artificial intelligence can create production-ready software with advanced optimizations, comprehensive testing systems, and enterprise-grade features.

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
- **Console Support:** `/payall` command works from console with unlimited power! 😄

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

# Console usage (TODAPODEROSA! 😄)
payall savia 1000                # From console - bypasses ALL restrictions!
payall farm-coins 500            # No balance check, no cooldowns, no limits!

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
  - **Console Support:** Can be executed from console (console bypasses all limits - TODAPODEROSA! 😄)
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

## 🤖 Codella AI Development Showcase

This project demonstrates the capabilities of [Codella AI](https://codella.ai/) in software development:

### **What AI Accomplished:**
- ✅ **Complete Plugin Architecture** - Designed from scratch with proper separation of concerns
- ✅ **Advanced Performance Optimizations** - HikariCP, caching systems, connection pooling
- ✅ **Comprehensive Testing Framework** - Stress tests, load tests, benchmark scenarios
- ✅ **Production-Ready Code** - Error handling, logging, configuration management
- ✅ **Database Integration** - SQLite and MySQL support with optimized queries
- ✅ **Real-time Monitoring** - Performance metrics and analytics
- ✅ **Documentation** - Complete README, code comments, and usage examples

### **Codella AI Development Process:**
1. **Requirements Analysis** - Understanding EdTools integration needs
2. **Architecture Design** - Creating scalable, maintainable structure
3. **Feature Implementation** - Core functionality with advanced optimizations
4. **Performance Engineering** - Caching, connection pooling, async operations
5. **Testing System** - Comprehensive performance testing framework
6. **Documentation** - Complete user and developer documentation
7. **Build & Deployment** - Maven configuration and GitHub integration

### **Technical Achievements:**
- **5,970+ lines of code** written entirely by Codella AI
- **39 files** with proper Java architecture
- **Enterprise-grade optimizations** for 200+ concurrent players
- **Advanced testing capabilities** with simulated load scenarios
- **Production-ready features** including rollback, error handling, and monitoring

## Support

For support, bug reports, or feature requests:
- **Author:** nottabaker
- **Email:** support@nottabaker.ve
- **Website:** https://nottabaker.ve

## 🤖 Powered by Codella AI

This project was entirely developed using [Codella AI](https://codella.ai/), a powerful AI platform for creating Minecraft plugins without coding knowledge.

### **About Codella AI:**
- **No Coding Required** - Create astounding plugins with natural language
- **Production-Ready Code** - Generates enterprise-grade Java code
- **Complete Solutions** - From concept to deployment
- **Advanced Features** - Performance optimizations, testing frameworks, and more

### **Why Codella AI:**
- ✅ **Intelligent Architecture** - Designs proper software patterns
- ✅ **Performance Focus** - Implements advanced optimizations
- ✅ **Testing Integration** - Creates comprehensive test systems
- ✅ **Documentation** - Generates complete documentation
- ✅ **Production Ready** - Enterprise-grade code quality

**Visit [Codella AI](https://codella.ai/) to create your own plugins with AI!**

## License

Copyright © 2024 nottabaker. All Rights Reserved.

This software is proprietary and confidential. Unauthorized copying, distribution, or modification is strictly prohibited.

## Changelog

### Version 1.0.0 (Initial Release) - 🤖 AI-Generated
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

**🤖 Codella AI Development Note:** This entire plugin (5,970+ lines of code across 39 files) was developed from concept to production-ready release using [Codella AI](https://codella.ai/), demonstrating the current capabilities of AI in software development.

---

## 🚀 Contributing & Development

### How to Contribute
1. **Fork the repository** on GitHub
2. **Create a feature branch** for your changes
3. **Make your improvements** and test thoroughly
4. **Submit a pull request** with a clear description

### Areas for Contribution
- **New Features:** Additional commands, integrations, or functionality
- **Performance:** Further optimizations for large servers
- **Bug Fixes:** Report and fix any issues you encounter
- **Documentation:** Improve README, code comments, or wiki
- **Testing:** Add more comprehensive test cases
- **Translations:** Add support for multiple languages

### Development Setup
```bash
git clone https://github.com/MarcianoDeHolanda/PayEdtools.git
cd PayEdtools

# Compile the plugin
mvn clean package
```

**Note:** The plugin requires EdTools API JARs to compile. Make sure you have:
1. `EdTools-API.jar` and `EdLib-API.jar` in the `lib/` folder
2. These JARs are obtained from your EdTools installation

### Code Style
- Follow Java conventions
- Add comprehensive comments for complex logic
- Include error handling for all operations
- Test thoroughly before submitting

**🎯 Goal:** Make this the best EdTools payment plugin available for the community!

---

## 📞 Contact & Support

- **GitHub Issues:** For bug reports and feature requests
- **Pull Requests:** For code contributions
- **Discussions:** For general questions and ideas

**Made with ❤️ by nottabaker using [Codella AI](https://codella.ai/)**
