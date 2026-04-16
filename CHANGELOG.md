# Changelog

All notable changes to the Xshards plugin are documented in this file.

## [1.2.9] - 2026-04-16

### Fixed

- **Shop: items could be received without paying / lost shards on purchase.**
  The shop listener now uses `getRawSlot()` and only reacts to clicks inside
  the shop's top inventory, so clicks in the player's own inventory no longer
  trigger purchase flows. Shards are now deducted **before** the item is
  delivered, the player's balance is re-verified at confirm time, and any
  items that don't fit in a full inventory are dropped at the player's feet
  so they never lose what they paid for. The pending-purchase state is also
  cleared when the confirm GUI is closed without a choice. Fixes the
  "I can take items from the shard shop without paying" report.

- **Database path mismatch (`XShards` vs `Xshards`).**
  The default SQLite path in `config.yml` was `plugins/XShards/storage/xshards.db`
  (capital S) while the plugin's data folder is `Xshards`. The default has
  been corrected to `plugins/Xshards/storage/xshards.db`.

  > **Upgrade note:** if you were running 1.2.8 or earlier, you'll have
  > existing data in `plugins/XShards/storage/xshards.db`. Either move that
  > file to `plugins/Xshards/storage/xshards.db`, or set
  > `storage.sqlite.file: plugins/XShards/storage/xshards.db` in your
  > `config.yml` to keep the old path.

- **SQLite errors: "stmt pointer is closed" / "database has been closed" /
  "Database connection lost. Reconnecting..." spam.**
  The previous `DatabaseManager` returned a single shared JDBC connection
  that callers would then close via `try-with-resources`, which closed the
  shared connection out from under any other thread mid-query (e.g. the AFK
  task and PlaceholderAPI/DecentHolograms refreshes hitting the leaderboard
  and AFK status). `DatabaseManager.getConnection()` now returns a fresh
  connection per call, so each query is fully isolated. SQLite connections
  also enable WAL journal mode and a 5s busy timeout for better concurrent
  read/write behaviour.

- **`/shards remove <player> <amount>` now works from the console.**
  Previously the console branch only accepted `give`. Console, command
  blocks, and RCON can now use `remove` as well, enabling custom commands
  and admin scripts to debit a player's balance without the
  `give <player> -N` workaround.

### Changed

- Startup banner now reads the version from `plugin.yml` instead of a
  hard-coded string (was stuck at `v1.2.4`).
- `ShardCommand` now rejects non-positive amounts up front with the
  existing "invalid amount" message, so admins can't accidentally invert a
  give/remove with a negative number.

## [1.2.8]

- Previous release. See git history for details.
