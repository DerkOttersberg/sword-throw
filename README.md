# Sword Throw

Sword Throw lets you charge a configurable Throw Item key and launch nearly any held item as a physical projectile. Tagged swords, axes, and spears can embed in blocks; other items bounce, deal item-aware impact damage, and remain recoverable. The client adds synchronized first- and third-person throwing poses, a charge indicator, configurable trails, and embedded-item rendering.

## Supported release

| Minecraft | Java | Fabric | Forge | NeoForge |
|---|---:|---:|---:|---:|
| 26.2 | 25 | 0.19.3 + Fabric API 0.158.x–0.159.x | 65.1.3 | 26.2.0.75 |

[Seamless API](https://github.com/DerkOttersberg/seamless-api) 2.x is required and is not bundled into Sword Throw.

## Controls

Hold the Throw Item key (`Q` by default) for at least 2 ticks (about 0.1 seconds at 20 TPS), then release it to throw the entire main-hand stack. You do not need full charge: longer holds smoothly increase launch speed, range, and velocity-based impact damage, reaching maximum power after 30 ticks (about 1.5 seconds). The charge bar is red before a throw is ready, blue for a ready partial throw, and gold only at full power.

Its binding is separate from Drop Item, but upgrading from an older config copies the existing Drop Item binding once so established controls keep working. When both actions share a key, a shorter tap still drops one item and Ctrl+Drop still performs vanilla whole-stack dropping. A tap on a separately bound Throw Item key cancels without dropping anything. Vanilla tridents retain their vanilla behavior unless a data pack explicitly adds them to `swordthrow:throwable`.

The server owns charge timing, validates that the exact held stack did not change, and removes the item only after the projectile was successfully spawned. Charge, release, and cancellation states are synchronized to the thrower and every tracking client, including players who begin tracking mid-charge.

## Data-pack tags

Four item tags provide stable compatibility hooks:

- `swordthrow:throwable` explicitly opts an item in, including a vanilla trident.
- `swordthrow:cannot_throw` opts an item out and always wins over `throwable`.
- `swordthrow:spears` selects point-first rendering and spear damage; the default inherits Minecraft 26.2's `minecraft:spears` tag and retains `minecraft:trident` as a compatibility fallback.
- `swordthrow:embeddable` selects block embedding independently of damage classification; the default contains Minecraft swords, axes, and `swordthrow:spears`.

Ordinary untagged items remain throwable for backward compatibility. Spear classification is tag-only; registry-name guesses such as `spear` or `javelin` are intentionally not used.

## Server damage configuration

The first server start creates `config/swordthrow-server.json`. It exposes the complete impact formula: base hand damage; velocity base/factor; clean-flight, standard, spear, sword, axe, pickaxe, shovel/hoe, and generic damageable-item multipliers; category minimums; spear base/flat tuning; and block/miscellaneous base damage. Defaults reproduce the 2.0 balance. Invalid, negative, non-finite, or out-of-range fields are logged and individually reset to their defaults without discarding valid fields.

## Development

Clone `sword-throw` and `seamless-api` as sibling directories, then use Java 25 and the included Gradle wrapper:

```text
minecraft-workspace/
├── seamless-api/
└── sword-throw/
```

```bash
./gradlew clean check build
./gradlew :fabric:runGameTest
./gradlew :forge:runGameTestServer
./gradlew :neoforge:runGameTestServer
./gradlew :fabric:runClient
./gradlew :forge:runClient
./gradlew :neoforge:runClient
```

Loader jars are written to each loader module’s `build/libs` directory as `sword-throw-2.1.0+mc26.2-<loader>.jar`.

## Project layout

- `common`: loader-neutral gameplay, entity physics, configuration, rendering, mixins, payload contracts, and tests.
- `fabric`, `forge`, `neoforge`: entrypoints, registry/network adapters, client events, metadata, and config-screen integration.
- `gradle/libs.versions.toml`: the only Minecraft, loader, API, and toolchain version source.

See [PORTING.md](PORTING.md) before adding another Minecraft version or loader.

## Compatibility

The mod ID remains `swordthrow`, the projectile registry ID remains `swordthrow:thrown_sword`, the C2S payload remains `swordthrow:throw_action`, and the client config remains `config/swordthrow-client.json`. Projectile NBT keys remain compatible; item registry identity, data components, enchantments, and represented stack count survive throws and save/reload.

## License

Sword Throw retains its existing [CC0 1.0 Universal](LICENSE) license.
