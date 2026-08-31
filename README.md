# Sword Throw

Sword Throw lets you charge the drop key and launch nearly any held item as a physical projectile. Swords, axes, and spear-like weapons can embed in blocks; other items bounce, deal item-aware impact damage, and remain recoverable. The client adds first- and third-person throwing poses, a charge indicator, configurable trails, and embedded-item rendering.

## Supported release

| Minecraft | Java | Fabric | Forge | NeoForge |
|---|---:|---:|---:|---:|
| 26.2 | 25 | 0.19.3 + Fabric API 0.158.0 | 65.1.3 | 26.2.0.75 |

[Seamless API](https://github.com/DerkOttersberg/seamless-api) 2.x is required and is not bundled into Sword Throw.

## Controls

Hold the normal drop key (`Q` by default) for at least 15 ticks, then release it to throw the entire main-hand stack. A short press still performs the normal one-item drop. Vanilla tridents retain their vanilla behavior.

The server owns charge timing, validates that the held stack did not change, and removes the item only after the projectile was successfully spawned.

## Development

Clone `sword-throw` and `seamless-api` as sibling directories, then use Java 25 and the included Gradle wrapper:

```text
minecraft-workspace/
├── seamless-api/
└── sword-throw/
```

```bash
./gradlew clean check build
./gradlew :fabric:runClient
./gradlew :forge:runClient
./gradlew :neoforge:runClient
```

Loader jars are written to each loader module’s `build/libs` directory as `sword-throw-2.0.0+mc26.2-<loader>.jar`.

## Project layout

- `common`: loader-neutral gameplay, entity physics, configuration, rendering, mixins, payload contracts, and tests.
- `fabric`, `forge`, `neoforge`: entrypoints, registry/network adapters, client events, metadata, and config-screen integration.
- `gradle/libs.versions.toml`: the only Minecraft, loader, API, and toolchain version source.

See [PORTING.md](PORTING.md) before adding another Minecraft version or loader.

## Compatibility

The mod ID remains `swordthrow`, the projectile registry ID remains `swordthrow:thrown_sword`, and the client config remains `config/swordthrow-client.json`. These names are intentionally stable for existing installations and copied-world upgrades.

## License

Sword Throw retains its existing [CC0 1.0 Universal](LICENSE) license.
