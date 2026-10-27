# Changelog

All notable changes to Cobblemon Ultra-Beasts are listed here.

## [4.1] - 2026-10-27

This release moves the mod to Cobblemon 1.8.4, gives the configuration its own folder, and cuts down the work the server does around wormholes.

### Changed

- **Requires Cobblemon 1.8.4 or later**, on both Fabric and NeoForge.
- **The configuration moved to `config/cobblemon_ultrabeast/main.json`.** It used to be `config/ultrabeasts.json`.
  Your settings are carried over automatically on first launch, and the old file is renamed to `ultrabeasts.json.migrated`. You can delete it once you have checked the new file.
- On Fabric, the mod now asks for Fabric Loader 0.17.2 and Fabric API 0.116.6 or later, the same minimums as Cobblemon 1.8.4.
- **Wormholes cost far less network traffic.** The swirl of particles around both portals is now drawn by each player's game instead of being sent by the server, one packet per particle. That was 360 packets per second for every player near a portal. The funnel shape is unchanged. If the particle textures are missing or broken (for example because of a resource pack), the swirl is simply turned off and a warning is written to the log, instead of crashing the game.
- **Leaving Ultra-Space causes less lag.** Removing the structure skips the empty parts of the area instead of checking every block one by one. For the largest structures, that was close to 900 000 checks on the server, all at once.

### Fixed

- The debris of the wormhole's arrival animation now spins and shrinks smoothly. It used to jump from one position to the next every few ticks, because the game was never told to blend between them.
- Removing a structure no longer makes torches, carpets and other attached blocks drop as items into the void.
- Seeing a wild Ultra Beast whose species you had already caught could throw an error on the server. The "Ultra-Space Master" advancement now looks up the player who owns the Pokédex, not the owner of the Pokémon.
- On dedicated servers, command messages showed `command.ultrabeasts.generic.server_name` instead of "The Server" when a command was run from the console. `/ultrabeasts wormhole clear` also always said "Server" in English. Both now show the translated name, or the player's name.
- Wormhole sound subtitles showed their raw translation keys. They now read "Wormhole opens" and "Wormhole hums", in English and French.

### Technical

- Built against Cobblemon `1.8.4+1.21.1`. The mod version is now plain `4.1`, without the Cobblemon suffix.
- NeoForge development environment updated to 21.1.265. Cobblemon 1.8.3 and later need MixinExtras 0.5 or newer, which older NeoForge builds do not provide, so the NeoForge dev client would crash on startup.
- The config file is read and written as UTF-8 on every platform.
- The server-side task scheduler no longer crashes when a scheduled task schedules another one.
- A failed structure lookup no longer leaves Ultra-Space claimed by a player who never entered it.
- The two portals now share one entity base class and one renderer; the code is documented throughout.
