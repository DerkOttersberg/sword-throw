# Changelog

## 2.1.0+mc26.2

- Added server-authoritative charge, release, and cancellation pose synchronization for the thrower and remote players, with immediate late-tracker snapshots, heartbeats, and lifecycle cleanup.
- Replaced global client pose state with independent per-player timelines and added two-player release/cancel, timeout, and unloaded-entity tests.
- Added `throwable`, `cannot_throw`, `spears`, and `embeddable` item tags. Explicit denial wins, broad untagged-item compatibility remains, and spear-name heuristics were removed.
- Added a validated, atomically written server damage configuration covering the full existing damage formula while preserving all default values.
- Preserved the existing Drop Item behavior while adding a separately configurable Throw Item binding and one-time binding migration.
- Added five live Fabric, Forge, and NeoForge GameTest scenarios for authoritative timing, changed-stack rejection, duplicate-start/late-tracker behavior, configured impact damage, tag precedence, embedding/bounce/pickup, and component/enchantment/NBT conservation.
- Tightened Fabric API metadata to the supported 0.158.x/0.159.x range and updated the Seamless API development pin to `2.0.1+mc26.2` while keeping the runtime dependency on compatible 2.x releases.

## 2.0.0+mc26.2

- Ported Sword Throw to Minecraft 26.2 and Java 25.
- Added first-class Fabric, Forge, and NeoForge modules from one shared codebase.
- Merged the latest Fabric configuration/overlay work with the richer NeoForge projectile, animation, embedding, and trail implementation.
- Made charging server-authoritative and added held-stack, cooldown, lifetime, and spawn-success validation.
- Fixed partial embedded-stack pickup duplication and persisted prior block impacts across saves.
- Replaced reflective renderer/model access with mapped Mixins and a typed invoker.
- Reused Seamless API 2.x visual helpers without shading the API.
- Preserved the `swordthrow` mod ID, `swordthrow:thrown_sword` registry ID, and existing client config filename.
