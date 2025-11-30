# Xshards Minecraft Plugin - Complete

## Overview
Comprehensive shard-based economy plugin with daily rewards, streaks, item shop, AFK farming, player transfers, and leaderboard system.

## Features Implemented ✅

### 1. **Configurable Message System**
- 100+ customizable message keys in `messages.yml`
- Color codes with `&` system (`&a` = green, `&c` = red, etc.)
- Dynamic placeholders: `{amount}`, `{player}`, `{days}`, `{multiplier}`, `{stage}`, etc.
- Full multi-language support via YAML configuration

### 2. **Daily Rewards with Streak Multipliers**
- Automatic shards on first login each day
- Single active difficulty stage (Easy/Mid/Hard)
- **Easy**: +0.1x multiplier every 1 day
- **Mid**: +0.1x multiplier every 3 consecutive days
- **Hard**: +0.1x multiplier every 7 consecutive days
- Auto-reset streak if player misses a day
- Fully customizable increment & intervals

### 3. **Shard Transfer System** (NEW)
- `/transfer <player> <amount>` - Send shards to other players
- GUI confirmation dialog (green/red wool buttons)
- Prevents self-transfers
- Validates sufficient balance
- Custom messages for all scenarios
- Enable/disable via config

### 4. **Leaderboard System with GUI** (NEW)
- `/leaderboard [page]` - View top shards players
- GUI displays:
  - Player heads (skulls) 
  - Rank position (#1, #2, etc.)
  - Player names
  - Shard amounts
- Pagination support (7 players per page)
  - Previous/Next buttons
  - Page indicator
  - Up to 100 players tracked
- Database sorted by shards (descending)

### 5. **Core Systems**
- Earning Methods: Playtime, PvP Kills, AFK Mode
- Shop System: 9-54 slot GUI with prices
- AFK System: Location-based earning with boss bar countdown
- Database: SQLite (default) or MySQL support
- PlaceholderAPI integration

## Configuration

### Daily Rewards
```yaml
daily-rewards:
  enabled: true
  amount: 10
  streak:
    enabled: true
    increment: 0.1
    active_stage: easy  # easy, mid, or hard
    easy:
      interval: 1
    mid:
      interval: 3
    hard:
      interval: 7
```

### Shard Transfer
```yaml
transfer:
  enabled: true
```

### Leaderboard
```yaml
leaderboard:
  enabled: true
```

## Commands

### Player Commands
- `/shards` - Check balance
- `/store` - Open shop
- `/afk` - Enter AFK mode
- `/quitafk` - Exit AFK mode
- `/transfer <player> <amount>` - Send shards to player
- `/leaderboard [page]` - View leaderboard

### Admin Commands
- `/setafk` - Set AFK location
- `/afkremove` - Remove AFK location
- `/store add/edit/remove` - Manage shop
- `/shards give <player> <amount>` - Give shards
- `/xshards reload` - Reload all configs
- `/xshards help` - Show help

## Permissions
- `xshards.use` - Basic commands (default: true)
- `xshards.admin` - Admin commands (default: op)
- `xshards.transfer` - Transfer shards (default: true)
- `xshards.leaderboard` - View leaderboard (default: true)

## Installation
1. Download JAR: `target/Xshards-1.2.2-SNAPSHOT.jar`
2. Place in `plugins/` folder
3. Restart server
4. Edit `config.yml` to configure
5. Edit `messages.yml` to customize messages

## Build Status
✅ **Successfully compiled** - 18MB JAR ready to deploy!

## Database Tables
- `player_shards` - Player balances & daily streak data
- `shop_items` - Shop inventory & prices
- `daily_rewards` - Daily login tracking & multipliers
- `afk_data` - AFK player tracking

## Tech Stack
- Java 17, Maven build
- Spigot 1.17.1+ API
- SQLite/MySQL persistence
- PlaceholderAPI compatible

All features are fully customizable through YAML configuration files!
