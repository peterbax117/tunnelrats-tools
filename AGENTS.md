# AGENTS.md

Instructions for coding agents working in this repository.

## What this is

TunnelRat's Tools, a NeoForge 1.21.1 mod. Mod id `tunnelrats_tools`, package
`io.github.peterbax117.tunnelratstools`, GitHub `peterbax117/tunnelrats-tools` (public, MIT). Build layout, test
harness and CI follow `peterbax117/angelring-pb`.

## Rules

- **Original work only.** The tools are inspired by Draconic Evolution (Celestial Manipulator) and Actually
  Additions (Storage Crates). Never copy code, textures, names or wording from those mods or any other.
- **Standalone first.** Every feature works with no other mods installed. Integrations (JEI, Jade, CraftTweaker,
  KubeJS) only add behavior. Code that touches an optional mod lives in its own class, loaded only when that mod is
  present (`ModList.get().isLoaded(...)`, or a plugin class the other mod discovers itself).
- **Registry entries are permanent.** A tool disabled in `TunnelRatsConfig` stays registered so worlds keep
  loading; disable its recipes and its use instead. Keep ids stable between versions (packs and quests use them).
- **Recipes are data files** under `data/tunnelrats_tools/recipe/`, using common `c:` tags, so pack makers can
  change them.
- **No em dashes or en dashes** anywhere: code, comments, docs, commit messages. Use commas, colons or parentheses.
- Commit messages end with `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`.
- Do not create tags or releases unless asked. Publishing is disabled until CurseForge and Modrinth projects exist
  (see README "Releasing").

## Build and test

The test harness must be cloned next to this repository: `C:\code\neoforge-mod-test-harness`.

```
gradlew build
gradlew runGameTestServer -Pstandalone
gradlew runGameTestServer
gradlew runGameTestServer -Pcrafttweaker
gradlew clientTest -Pstandalone
gradlew clientTest
```

- GameTests go in `src/gametest`, client tests in `src/clienttest`. Both must pass in every setup; read the setup
  with `ModList.get().isLoaded(...)` when an expectation differs.
- Every new feature gets GameTests (the crate no-nesting rule is required). Prove each new test can fail once.
- CI (`.github/workflows/build.yml`) runs the `standalone`, `integrations` and `crafttweaker` setups. Keep it green.
- See the harness `README.md` and `docs/integrations.md` for fixtures, client test APIs and integration guidance.
