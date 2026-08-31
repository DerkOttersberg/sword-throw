# Porting Sword Throw

Sword Throw uses Architectury Loom as build tooling only. Architectury API is not a runtime dependency.

## Update order

1. Change Minecraft, Java, loaders, Fabric API, Loom, and Seamless API only in `gradle/libs.versions.toml`.
2. Compile `common` first and adapt shared code using Minecraft’s official names.
3. Compile Fabric, Forge, and NeoForge independently. Loader imports are forbidden in `common`.
4. Keep payload behavior and `swordthrow:thrown_sword` stable unless a migration is explicitly supplied.
5. Run `clean check build`, the Fabric GameTests, and one real client per loader.
6. Test all suite mods together with the matching loader jars before release.

## Architecture boundaries

- Gameplay, projectile physics, persistence, charge validation, poses, trail math, screens, and resources belong in `common`.
- Registration, networking transport, lifecycle events, config-directory lookup, renderer hookup, and config-screen exposure belong in loader modules.
- Loader services are passed explicitly into `SwordThrow` and `SwordThrowClient`; do not add reflection or `ServiceLoader` discovery.
- Reuse Seamless API visual contracts. Never copy, shade, or embed those classes into a loader jar.
- Keep client classes out of server entrypoints so dedicated servers can load the mod without initializing rendering code.

## Compatibility invariants

- Mod ID: `swordthrow`
- Projectile entity: `swordthrow:thrown_sword`
- Client config: `swordthrow-client.json`
- Network payload: `swordthrow:throw_action`

Archive an existing branch tip before retiring it. Permanent work happens on `main`; use `port/mc-<version>`, `feat/<name>`, `fix/<name>`, and `release/<version>` for short-lived work.
