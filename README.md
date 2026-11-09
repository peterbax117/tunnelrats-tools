# TunnelRat's Tools

A NeoForge 1.21.1 utility mod for modpacks: useful blocks found in large mods, available on their own without the
large mod and its dependencies. One jar; each tool can be turned off in the config.

**Status: in development (0.1.0).** The Weather Vane and the Rain Detector are done; the storage crate is next.

## Weather Vane

A block that changes the time of day or the weather.

- **Effects:** Sunrise, Noon, Sunset, Midnight; Clear, Rain, Thunder. Right-click opens the GUI: pick an effect
  (the block remembers it), then press Activate.
- **Redstone:** a redstone pulse fires the selected effect once, on the rising edge. Holding the signal on does not
  fire again; turn it off and on for the next use.
- **Sequence of a use:**
  1. **Activate** (button or redstone pulse): the cost is paid, the server-wide cooldown starts, and one chat
     message announces it ("TunnelRat117 used a Weather Vane: changing the weather to Clear").
  2. **Charge** for `chargeSeconds` (default 3): particles spiral up from the vane, more and tighter as the charge
     completes, with a rising chime.
  3. **Release:** a column of particles shoots from the vane up to 64 blocks into the sky for about a second, with
     a beacon sound (thunder for Thunder). The effect applies now.
  4. **Sweep** (time effects): the time fast-forwards over `animationSeconds` (default 3) so the sun, moon and stars
     visibly cross the sky. The server sends the time every tick during the sweep.

  Colors: warm gold for time, white and light blue for Clear, blue for Rain, purple with electric sparks for
  Thunder. Particles and sounds are vanilla and sent by the server to players within normal particle range, so
  clients need nothing extra; `showEffects = false` turns them off.
- **One at a time:** while any vane on the server is charging or sweeping, every vane refuses to activate and its GUI
  says "Charging" or "Changing the sky".
- **Breaking the vane mid-charge** does not cancel the use: it was paid for, so the effect still applies when the
  charge ends (only the particles stop). The same holds if its chunk unloads. The pending charge and sweep are saved
  with the world; if the server stops during either, the effect is finished at once (no charge, no sweep) on the
  first tick after the world loads, so the world never stays half changed.
- **Time only moves forward.** The vane moves to the next sunrise (tick 23000 of the day, when the sun reaches the
  horizon), noon (6000), sunset (12000) or midnight (18000). The day count never goes back, unlike `/time set`.
  `animateTime = false` jumps there at once instead of sweeping.
- **Weather** is set the way `/weather` does it, with the same random durations; rain fades in and out as usual.
- **Cost per use:** Forge Energy if the internal buffer holds enough, otherwise one **Sky Shard** from its slot,
  otherwise it refuses. Hoppers and pipes can insert Sky Shards (they cannot take them out) and energy cables can
  charge it. Vanilla has no energy source, so Sky Shards are the standalone path. `requireCatalyst = false` makes
  it free.
- **Cooldown:** one cooldown for the whole server, shared by every Weather Vane (time and weather are server-wide,
  so more vanes must not mean more changes). It is saved with the world and counts game ticks.
- **Announcement:** a chat message to everyone names who used it and what changed. For redstone it names the
  player who placed the vane and says it was a redstone trigger.
- **Where it works:** time changes are refused in dimensions with a fixed time (the Nether, the End); weather
  changes are refused in dimensions without weather. The GUI shows the reason.
- **Server authority:** the GUI sends only button clicks. The server checks every click: the player is in reach,
  the block is still there, and the effect is allowed.

## Rain Detector

The weather counterpart of the daylight detector, a slab-height block that outputs a redstone signal. Right-click
cycles three modes (shown on its top and in an action bar message):

| Mode | Output |
|---|---|
| Rain | 15 while it rains or thunders, else 0 |
| Thunder | 15 only while it thunders, else 0 |
| Clear | 15 while it does not rain, else 0 |

It reads the dimension's weather (the same rain and thunder the sky shows), not the biome or the sky above the
block, so it also works in deserts, in snowy biomes and under a roof. It checks every second and updates its
neighbors only when its output changes. In dimensions without weather (the Nether, the End) it outputs 0 in every
mode.

**Automatic clear weather:** put a Rain Detector (Rain mode) next to a Weather Vane set to Clear. When rain
starts, the detector turns on and the vane charges (the rain keeps falling meanwhile), then releases and clears the
sky. When the rain fades, the detector turns off, which does not fire the vane.

## Recipes

All vanilla items; tags (`c:`) accept modded equivalents. Each recipe is a data file under
`data/tunnelrats_tools/recipe/` and is disabled with its tool in the config.

Weather Vane:

```
Gold Ingot    Daylight Detector  Gold Ingot
Lapis Block   Clock              Lapis Block
Obsidian      Diamond            Obsidian
```

Sky Shard (makes 2):

```
Glowstone Dust  Lapis Lazuli    Glowstone Dust
Lapis Lazuli    Amethyst Shard  Lapis Lazuli
Glowstone Dust  Lapis Lazuli    Glowstone Dust
```

Rain Detector (the bucket is returned):

```
Glass              Glass              Glass
Redstone Dust      Water Bucket       Redstone Dust
Smooth Stone Slab  Smooth Stone Slab  Smooth Stone Slab
```

## Planned: storage crate

- Three tiers (54, 81 and 108 slots), upgraded in place with contents kept. Larger tiers scroll.
- Keeps its contents when broken, as a shulker box does.
- An upgrade item turns a placed vanilla chest into a crate and keeps its contents.
- Crates and shulker boxes cannot be stored inside a crate.
- Hoppers and pipe mods can insert and extract items.

Concepts inspired by, not copied from: Draconic Evolution (time and weather control) and Actually Additions
(storage crates). All code, art, names and wording in this mod are original; the textures are drawn by
`tools/generate_textures.py`.

## Configuration

`config/tunnelrats_tools-common.toml`, also editable in game from the Mods list:

| Key | Default | Meaning |
|---|---|---|
| `timeWeather.enabled` | `true` | Enable the Weather Vane and Sky Shard |
| `timeWeather.allowTime` | `true` | Allow time changes |
| `timeWeather.allowWeather` | `true` | Allow weather changes |
| `timeWeather.cooldownSeconds` | `60` | Seconds between uses, shared by every vane on the server (0 = no cooldown) |
| `timeWeather.announceInChat` | `true` | Chat message naming who changed the time or weather |
| `timeWeather.chargeSeconds` | `3.0` | Charge before the effect is released, 0.0 to 30.0 (0 = at once) |
| `timeWeather.animateTime` | `true` | Sweep the time forward instead of jumping |
| `timeWeather.animationSeconds` | `3.0` | Length of the sweep, the same for any distance, 0.5 to 20.0 |
| `timeWeather.showEffects` | `true` | Charge and release particles and sounds |
| `timeWeather.requireCatalyst` | `true` | Each use costs a Sky Shard or energy; `false` makes it free |
| `timeWeather.energyPerUse` | `10000` | FE per use when the buffer holds enough (0 = Sky Shards only) |
| `timeWeather.energyCapacity` | `100000` | Size of the energy buffer (ten uses) |
| `rainDetector.enabled` | `true` | Enable the Rain Detector |
| `crate.enabled` | `true` | Enable the storage crate |

Disabling a tool does not remove its blocks or items from the game, so existing worlds keep loading. Its recipes
are removed and the blocks stop working: a disabled Weather Vane refuses to activate, a disabled Rain Detector
outputs 0.

## Integrations

| Mod | Integration |
|---|---|
| Jade | Weather Vane overlay: selected effect and remaining cooldown |
| JEI | Recipes show automatically (they are vanilla crafting recipes) |
| CraftTweaker, KubeJS | Recipes are data files with common tags, so pack makers can replace them |
| Energy mods | Weather Vane accepts Forge Energy from any cable or generator |
| Hoppers, pipe mods | Insert Sky Shards into the Weather Vane |
## Standalone first

Every tool works fully with no other mods installed. Integrations (JEI, Jade, CraftTweaker and KubeJS) only add
behavior; they never replace or restrict it. CI tests the no-integration setup on every push.

## Requirements

Minecraft 1.21.1, NeoForge 21.1.x. No other mods are required.

## Building and testing

```
gradlew build                                 # mod jar in build/libs
gradlew runGameTestServer -Pstandalone        # server tests, no optional mods
gradlew runGameTestServer                     # with JEI and Jade
gradlew runGameTestServer -Pcrafttweaker      # plus CraftTweaker and a sample script
gradlew clientTest -Pstandalone               # real client, no optional mods
gradlew clientTest                            # real client with JEI and Jade
```

Tests use [neoforge-mod-test-harness](https://github.com/peterbax117/neoforge-mod-test-harness), cloned next to
this repository (`C:\code\neoforge-mod-test-harness` beside `C:\code\tunnelrats-tools`). Without it the mod still
builds; only the test tasks are missing. CI runs the three setups as a matrix.

## Releasing

Not set up yet: there is no CurseForge or Modrinth project. `.github/workflows/release.yml` has its publish job
disabled, and the project ids in `gradle.properties` are blank placeholders, so `gradlew publishMods` is a dry run
that writes to `build/publishMods`.

To activate: create the CurseForge and Modrinth projects, set `curseforge_project_id` and `modrinth_project_id` in
`gradle.properties`, add the `CURSEFORGE_TOKEN` and `MODRINTH_TOKEN` repository secrets, and remove the
`if: ${{ false }}` line from the publish job. Then release by bumping `mod_version`, committing, and pushing an
annotated tag whose message is the changelog (`git tag -a v0.1.0 -m "First release"`).

## License

MIT
