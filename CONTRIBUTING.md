# Contributing

For this branch, base changes on `1.20.1`, not `main` or a 26.x branch.
Java 25 hosts Gradle; Java 17 compiles and runs Minecraft. Supported loaders
are Fabric and Forge only. Keep a matching `1.20.1` API sibling checkout.


Use Java 25 and keep `seamless-api` checked out beside this repository. Before opening a pull request, run:

```bash
./gradlew clean check build
```

Keep loader-specific imports out of `common`, preserve registered IDs, and add a focused unit or GameTest for gameplay changes. Do not introduce a runtime Architectury API dependency or shade Seamless API. Changes that alter item conservation, damage, persistence, networking, or world compatibility should explain the migration and include a copied-world test.
