# Changelog

All notable changes to Cobblemon Ultra-Beasts are listed here.

## [4.1] - 2026-10-27

This release moves the mod to Cobblemon 1.8.4, gives the configuration its own folder, adds an arrival cinematic, and cuts down the work the server does around wormholes.

### Added

- **Encounter cinematic.** When you go through a wormhole, the camera now presents the Ultra-Space structure, then cuts to a close-up of its Ultra Beast, between black cinema bands. At the end the beast lets out a roar and the camera zooms on its head as its name appears. You cannot move during it, and you are safe as everywhere in Ultra-Space. Press Space, Enter or Escape to skip it.
  Open structures are shown from the sky. Nihilego's cave is shown from the inside instead, the camera gliding from the entrance toward the beast.
  Each player can turn it off with `ENCOUNTER_CINEMATIC` in their own `config/cobblemon_ultrabeast/client.json`, created on first launch. Turned off, the arrival is a short fade from black, which replaces the old blindness.
- **New wormhole arrival.** Space now tears open: a crackling line of light splits the air, opens into a rift onto the void while shards of tinted glass fly out, then turns on itself and dissolves into the wormhole's swirl. It takes 7.5 seconds instead of 9, and replaces the falling smoke trail and the ring of floating blocks.
- Ultra Beasts are now visible from farther away in Ultra-Space. On the Celesteela and Xurkitree structures, the beast was too far from the arrival point to be seen until you walked toward it.
- **11 new languages:** German, Spanish (Spain and Mexico), Italian, Brazilian Portuguese, Polish, Russian, Simplified and Traditional Chinese, Japanese and Korean.

### Changed

- **Requires Cobblemon 1.8.4 or later**, on both Fabric and NeoForge.
- **The configuration moved to `config/cobblemon_ultrabeast/main.json`.** It used to be `config/ultrabeasts.json`.
  Your settings are carried over automatically on first launch, and the old file is renamed to `ultrabeasts.json.migrated`. You can delete it once you have checked the new file.
- **Wormholes are a bit more common:** the default `WORMHOLE_SPAWN_CHANCE` goes from 2500 to 2000, about one wormhole per 11 hours of play instead of 14. Existing configs keep their value; set it to 2000 by hand to get the new rate.
- On Fabric, the mod now asks for Fabric Loader 0.17.2 and Fabric API 0.116.6 or later, the same minimums as Cobblemon 1.8.4.
- **Wormholes cost far less network traffic.** The swirl of particles around both portals is now drawn by each player's game instead of being sent by the server, one packet per particle. That was 360 packets per second for every player near a portal. The funnel shape is unchanged. If the particle textures are missing or broken (for example because of a resource pack), the swirl is simply turned off and a warning is written to the log, instead of crashing the game.
- **Leaving Ultra-Space causes less lag.** Removing the structure skips the empty parts of the area instead of checking every block one by one. For the largest structures, that was close to 900 000 checks on the server, all at once.

### Fixed

- Ultra Beasts now stand on the floor of their structure. They used to float up to a block and a half above it.
- Wormhole particles no longer cover the screen with large pink discs when you look at a portal from the front. Particles shrink as they get very close to your view.

- The debris of the wormhole's arrival animation now spins and shrinks smoothly. It used to jump from one position to the next every few ticks, because the game was never told to blend between them.
- Removing a structure no longer makes torches, carpets and other attached blocks drop as items into the void.
- Seeing a wild Ultra Beast whose species you had already caught could throw an error on the server. The "Ultra-Space Master" advancement now looks up the player who owns the Pokédex, not the owner of the Pokémon.
- On dedicated servers, command messages showed `command.ultrabeasts.generic.server_name` instead of "The Server" when a command was run from the console. `/ultrabeasts wormhole clear` also always said "Server" in English. Both now show the translated name, or the player's name.
- In French, the "Into the Unknown" advancement showed its description in English: its translation key was misspelled.
- Wormhole sound subtitles showed their raw translation keys. They now read "Wormhole opens" and "Wormhole hums", in English and French.

### Technical

- Built against Cobblemon `1.8.4+1.21.1`. The mod version is now plain `4.1`, without the Cobblemon suffix.
- NeoForge development environment updated to 21.1.265. Cobblemon 1.8.3 and later need MixinExtras 0.5 or newer, which older NeoForge builds do not provide, so the NeoForge dev client would crash on startup.
- The config file is read and written as UTF-8 on every platform.
- The server-side task scheduler no longer crashes when a scheduled task schedules another one.
- A failed structure lookup no longer leaves Ultra-Space claimed by a player who never entered it.
- The two portals now share one entity base class and one renderer; the code is documented throughout.
- The arrival animation is drawn by each player's game. The old one sent particle packets every tick and moved 80 block display entities for 9 seconds; the new one sends a single start time. The access widener and access transformer entries it needed are gone.
- The mod now uses Mixin, for the cinematic camera and the Ultra Beast's view range. On NeoForge the Fabric-only refmap is left out of the jar.
- `main.json` and `client.json` are rewritten on every load, so settings added by a new version show up in an existing file.
