# Xshards Plugin

A comprehensive Minecraft plugin for managing a shard-based economy system with multiple earning methods and a customizable shop interface.

## Features

- 💎 **Shard Economy System**
  - Multiple ways to earn shards
  - Configurable earning rates
  - PlaceholderAPI integration
  - Database storage (SQLite or MySQL)

- 🏪 **Customizable Shop System**
  - Support for custom items with lore and model data
  - Adjustable shop size (9-54 slots)
  - Price editing system
  - Purchase confirmation interface

- 🔄 **Shard Transfer System**
  - Send shards to other players
  - GUI confirmation dialog
  - Automatic validation and balance checking

- 🏆 **Leaderboard System**
  - View top players by shard count
  - GUI with player heads and rankings
  - Pagination support (7 players per page)
  - Database sorting and tracking

- 📅 **Daily Rewards with Streaks**
  - Daily login bonuses
  - Streak multiplier system (Easy/Mid/Hard)
  - Auto-reset on missed days
  - Fully configurable multipliers

- 💤 **Advanced AFK System**
  - Persistent location storage
  - Automatic return to previous location
  - 5-second countdown with movement detection
  - Free movement while in AFK mode
  - Safe teleportation system
  - Configurable earning rates
  - Protection from damage while AFK
  - Dedicated world support (recommended)
  - Seamless server restart handling
  - Anti-abuse measures

## Earning Methods

1. **Playtime Rewards**
   - Earn shards by staying online
   - Configurable interval and amount

2. **PvP Rewards**
   - Earn shards from player kills
   - 24-hour cooldown per player killed
   - Configurable amount

3. **AFK Rewards**
   - Earn shards while in AFK mode
   - Configurable interval and amount
   - 5-second countdown with movement detection
   - Safe teleportation system
   - Persistent location storage
   - Dedicated world support for optimal performance

## Commands

### General Commands
- `/shards` - Check your shard balance
- `/store` - Open the shard shop
- `/afk` - Enter AFK mode
- `/quitafk` - Exit AFK mode
- `/transfer <player> <amount>` - Transfer shards to another player
- `/leaderboard [page]` - View the shards leaderboard

### Admin Commands
- `/store edit <slot> <price>` - Edit item price in shop
- `/store add <slot> <price>` - Add item to shop
- `/store remove <slot>` - Remove item from shop
- `/setafk` - Set AFK location
- `/afkremove` - Remove AFK location
- `/xshards reload` - Reload plugin configuration
- `/xshards help` - Show help menu

## Permissions

- `xshards.use` - Access to basic commands (default: true)
- `xshards.admin` - Access to admin commands (default: op)
- `xshards.transfer` - Access to shard transfer command (default: true)
- `xshards.leaderboard` - Access to leaderboard (default: true)

## Configuration

```yaml
# Storage configuration
storage:
  type: sqlite # Storage type: 'mysql' or 'sqlite'
  # MySQL Configuration
  mysql:
    host: localhost
    port: 3306
    database: xshards
    user: root
    password: password
  # SQLite Configuration
  sqlite:
    file: plugins/XShards/storage/xshards.db

# Store settings
store:
  size: 54  # GUI size (9, 18, 27, 36, 45, 54)

# Shard earning methods
earning:
  playtime:
    enabled: true
    interval: 3600000  # 1 hour in milliseconds
    amount: 3

  kills:
    enabled: true
    amount: 10

  afk:
    enabled: true
    interval: 30  # seconds
    amount: 1
    countdown: 5  # seconds to wait before teleporting
    # We highly recommend setting up a dedicated world for AFK using MultiVerse-Core

# PlaceholderAPI Integration
placeholderapi: true
```

## PlaceholderAPI Integration

Available placeholders:
- `%xshards_playershards%` - Shows player's current shard balance

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart the server
4. Configure the plugin in `plugins/Xshards/config.yml`

## Support

For support, please open an issue on our GitHub repository.

## License

This project is licensed under the GNUV3 License - see the LICENSE file for details.