# Changelog

## 2.0.0+mc26.2

- Ported Sword Throw to Minecraft 26.2 and Java 25.
- Added first-class Fabric, Forge, and NeoForge modules from one shared codebase.
- Merged the latest Fabric configuration/overlay work with the richer NeoForge projectile, animation, embedding, and trail implementation.
- Made charging server-authoritative and added held-stack, cooldown, lifetime, and spawn-success validation.
- Fixed partial embedded-stack pickup duplication and persisted prior block impacts across saves.
- Replaced reflective renderer/model access with mapped Mixins and a typed invoker.
- Reused Seamless API 2.x visual helpers without shading the API.
- Preserved the `swordthrow` mod ID, `swordthrow:thrown_sword` registry ID, and existing client config filename.
