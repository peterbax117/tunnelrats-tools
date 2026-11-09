# TunnelRat's Tools

A NeoForge 1.21.1 utility mod for modpacks: useful blocks found in large mods, available on their own without the
large mod and its dependencies. One jar; each tool can be turned off in the config.

**Status: in development.** This repository is the scaffold (registries, config, tests, CI). No tools are
implemented yet.

## Planned v1 tools

### Time and weather control block

- Sets the time (sunrise, noon, sunset, midnight) or the weather (clear, rain, thunder).
- A GUI selects the effect; a GUI button or a redstone pulse triggers it.
- Each use costs a crafted catalyst item, or Forge Energy when the block is powered. The config can make it free.
- Server controls: a cooldown between uses, a chat message naming who changed the time or weather, and separate
  toggles for time and weather.

### Storage crate

- Three tiers (54, 81 and 108 slots), upgraded in place with contents kept. Larger tiers scroll.
- Keeps its contents when broken, as a shulker box does.
- An upgrade item turns a placed vanilla chest into a crate and keeps its contents.
- Crates and shulker boxes cannot be stored inside a crate.
- Hoppers and pipe mods can insert and extract items.

Concept inspired by, not copied from: Draconic Evolution Celestial Manipulator, Actually Additions Storage Crates.
All code, art, names and wording in this mod are original.

## Configuration

`config/tunnelrats_tools-common.toml`, also editable in game from the Mods list:

| Key | Default | Meaning |
|---|---|---|
| `timeWeather.enabled` | `true` | Enable the time and weather control block |
| `timeWeather.allowTime` | `true` | Allow time changes |
| `timeWeather.allowWeather` | `true` | Allow weather changes |
| `timeWeather.cooldownSeconds` | `60` | Seconds between uses (0 = no cooldown) |
| `timeWeather.announceInChat` | `true` | Chat message naming the player who changed time or weather |
| `timeWeather.requireCatalyst` | `true` | Each use costs a catalyst item (or Forge Energy); `false` makes it free |
| `crate.enabled` | `true` | Enable the storage crate |

Disabling a tool does not remove its blocks or items from the game, so existing worlds keep loading. Its recipes
and its use are disabled instead.

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
