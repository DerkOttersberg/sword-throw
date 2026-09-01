package io.github.derkottersberg.swordthrow.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.derkottersberg.swordthrow.SwordThrow;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Server-authoritative gameplay tuning shared by dedicated and integrated servers. */
public final class SwordThrowServerConfig {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final float MIN_VALUE = 0.0F;
    public static final float MAX_DAMAGE_VALUE = 2048.0F;
    public static final float MAX_MULTIPLIER = 100.0F;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    private static ConfigData data = new ConfigData();

    private SwordThrowServerConfig() {
    }

    public static void initialize(Path configDirectory) {
        configPath = configDirectory.resolve("swordthrow-server.json");
        load();
    }

    public static void load() {
        Path path = requireConfigPath();
        if (!Files.exists(path)) {
            data = new ConfigData();
            save();
            return;
        }

        boolean saveNormalized = false;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            data = deserialize(JsonParser.parseReader(reader));
            saveNormalized = true;
        } catch (IOException | RuntimeException exception) {
            SwordThrow.LOGGER.warn("Could not read {}. Safe defaults will be used.", path, exception);
            data = new ConfigData();
        }
        if (saveNormalized) {
            // Fill newly introduced keys and persist repaired values in a deterministic form.
            save();
        }
    }

    public static void save() {
        data = sanitize(data);
        Path path = requireConfigPath();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(
                    temporary,
                    path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            SwordThrow.LOGGER.warn("Could not save {}", path, exception);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // The original config is still intact; a stale temporary file is harmless.
            }
        }
    }

    public static ConfigData get() {
        data = sanitize(data);
        return data;
    }

    static ConfigData sanitize(ConfigData candidate) {
        ConfigData sanitized = candidate == null ? new ConfigData() : candidate;
        sanitized.schemaVersion = CURRENT_SCHEMA_VERSION;
        sanitized.baseHandDamage = sanitizeDamage("baseHandDamage", sanitized.baseHandDamage, 1.0F);
        sanitized.velocityDamageBase = sanitizeMultiplier("velocityDamageBase", sanitized.velocityDamageBase, 0.65F);
        sanitized.velocityDamageFactor = sanitizeMultiplier("velocityDamageFactor", sanitized.velocityDamageFactor, 0.7F);
        sanitized.cleanFlightDamageMultiplier = sanitizeMultiplier(
            "cleanFlightDamageMultiplier",
            sanitized.cleanFlightDamageMultiplier,
            1.35F
        );
        sanitized.standardDamageMultiplier = sanitizeMultiplier("standardDamageMultiplier", sanitized.standardDamageMultiplier, 1.0F);
        sanitized.spearDamageMultiplier = sanitizeMultiplier("spearDamageMultiplier", sanitized.spearDamageMultiplier, 1.0F);
        sanitized.spearMinimumAttackDamage = sanitizeDamage("spearMinimumAttackDamage", sanitized.spearMinimumAttackDamage, 4.0F);
        sanitized.spearBaseDamageMultiplier = sanitizeMultiplier(
            "spearBaseDamageMultiplier",
            sanitized.spearBaseDamageMultiplier,
            1.45F
        );
        sanitized.spearFlatDamageBonus = sanitizeDamage("spearFlatDamageBonus", sanitized.spearFlatDamageBonus, 2.5F);
        sanitized.swordDamageMultiplier = sanitizeMultiplier("swordDamageMultiplier", sanitized.swordDamageMultiplier, 1.0F);
        sanitized.axeDamageMultiplier = sanitizeMultiplier("axeDamageMultiplier", sanitized.axeDamageMultiplier, 0.75F);
        sanitized.axeMinimumDamage = sanitizeDamage("axeMinimumDamage", sanitized.axeMinimumDamage, 3.5F);
        sanitized.pickaxeDamageMultiplier = sanitizeMultiplier("pickaxeDamageMultiplier", sanitized.pickaxeDamageMultiplier, 0.6F);
        sanitized.pickaxeMinimumDamage = sanitizeDamage("pickaxeMinimumDamage", sanitized.pickaxeMinimumDamage, 2.0F);
        sanitized.shovelAndHoeDamageMultiplier = sanitizeMultiplier(
            "shovelAndHoeDamageMultiplier",
            sanitized.shovelAndHoeDamageMultiplier,
            0.5F
        );
        sanitized.shovelAndHoeMinimumDamage = sanitizeDamage(
            "shovelAndHoeMinimumDamage",
            sanitized.shovelAndHoeMinimumDamage,
            1.25F
        );
        sanitized.damageableItemDamageMultiplier = sanitizeMultiplier(
            "damageableItemDamageMultiplier",
            sanitized.damageableItemDamageMultiplier,
            0.4F
        );
        sanitized.damageableItemMinimumDamage = sanitizeDamage(
            "damageableItemMinimumDamage",
            sanitized.damageableItemMinimumDamage,
            1.0F
        );
        sanitized.blockItemBaseDamage = sanitizeDamage("blockItemBaseDamage", sanitized.blockItemBaseDamage, 0.35F);
        sanitized.miscItemBaseDamage = sanitizeDamage("miscItemBaseDamage", sanitized.miscItemBaseDamage, 0.6F);
        return sanitized;
    }

    static void replaceForTests(ConfigData replacement) {
        data = sanitize(replacement);
    }

    static ConfigData deserializeForTests(String json) {
        return deserialize(JsonParser.parseString(json));
    }

    private static ConfigData deserialize(JsonElement root) {
        if (root == null || !root.isJsonObject()) {
            throw new JsonParseException("Sword Throw server config root must be a JSON object");
        }

        JsonObject object = root.getAsJsonObject();
        ConfigData loaded = new ConfigData();
        loaded.baseHandDamage = readFloat(object, "baseHandDamage", loaded.baseHandDamage);
        loaded.velocityDamageBase = readFloat(object, "velocityDamageBase", loaded.velocityDamageBase);
        loaded.velocityDamageFactor = readFloat(object, "velocityDamageFactor", loaded.velocityDamageFactor);
        loaded.cleanFlightDamageMultiplier = readFloat(
            object,
            "cleanFlightDamageMultiplier",
            loaded.cleanFlightDamageMultiplier
        );
        loaded.standardDamageMultiplier = readFloat(
            object,
            "standardDamageMultiplier",
            loaded.standardDamageMultiplier
        );
        loaded.spearDamageMultiplier = readFloat(object, "spearDamageMultiplier", loaded.spearDamageMultiplier);
        loaded.spearMinimumAttackDamage = readFloat(
            object,
            "spearMinimumAttackDamage",
            loaded.spearMinimumAttackDamage
        );
        loaded.spearBaseDamageMultiplier = readFloat(
            object,
            "spearBaseDamageMultiplier",
            loaded.spearBaseDamageMultiplier
        );
        loaded.spearFlatDamageBonus = readFloat(object, "spearFlatDamageBonus", loaded.spearFlatDamageBonus);
        loaded.swordDamageMultiplier = readFloat(object, "swordDamageMultiplier", loaded.swordDamageMultiplier);
        loaded.axeDamageMultiplier = readFloat(object, "axeDamageMultiplier", loaded.axeDamageMultiplier);
        loaded.axeMinimumDamage = readFloat(object, "axeMinimumDamage", loaded.axeMinimumDamage);
        loaded.pickaxeDamageMultiplier = readFloat(
            object,
            "pickaxeDamageMultiplier",
            loaded.pickaxeDamageMultiplier
        );
        loaded.pickaxeMinimumDamage = readFloat(object, "pickaxeMinimumDamage", loaded.pickaxeMinimumDamage);
        loaded.shovelAndHoeDamageMultiplier = readFloat(
            object,
            "shovelAndHoeDamageMultiplier",
            loaded.shovelAndHoeDamageMultiplier
        );
        loaded.shovelAndHoeMinimumDamage = readFloat(
            object,
            "shovelAndHoeMinimumDamage",
            loaded.shovelAndHoeMinimumDamage
        );
        loaded.damageableItemDamageMultiplier = readFloat(
            object,
            "damageableItemDamageMultiplier",
            loaded.damageableItemDamageMultiplier
        );
        loaded.damageableItemMinimumDamage = readFloat(
            object,
            "damageableItemMinimumDamage",
            loaded.damageableItemMinimumDamage
        );
        loaded.blockItemBaseDamage = readFloat(object, "blockItemBaseDamage", loaded.blockItemBaseDamage);
        loaded.miscItemBaseDamage = readFloat(object, "miscItemBaseDamage", loaded.miscItemBaseDamage);
        return sanitize(loaded);
    }

    private static float readFloat(JsonObject object, String fieldName, float fallback) {
        JsonElement element = object.get(fieldName);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            SwordThrow.LOGGER.warn(
                "Invalid Sword Throw server config value {}={}; expected a JSON number. Using default {}.",
                fieldName,
                element,
                fallback
            );
            return fallback;
        }

        try {
            return element.getAsFloat();
        } catch (RuntimeException exception) {
            SwordThrow.LOGGER.warn(
                "Invalid Sword Throw server config value {}={}; expected a JSON number. Using default {}.",
                fieldName,
                element,
                fallback,
                exception
            );
            return fallback;
        }
    }

    private static float sanitizeDamage(String fieldName, float value, float fallback) {
        return sanitizeValue(fieldName, value, fallback, MAX_DAMAGE_VALUE);
    }

    private static float sanitizeMultiplier(String fieldName, float value, float fallback) {
        return sanitizeValue(fieldName, value, fallback, MAX_MULTIPLIER);
    }

    private static float sanitizeValue(String fieldName, float value, float fallback, float maximum) {
        if (!Float.isFinite(value) || value < MIN_VALUE || value > maximum) {
            SwordThrow.LOGGER.warn(
                "Invalid Sword Throw server config value {}={}; expected {}..{}. Using default {}.",
                fieldName,
                value,
                MIN_VALUE,
                maximum,
                fallback
            );
            return fallback;
        }
        return value;
    }

    private static Path requireConfigPath() {
        if (configPath == null) {
            throw new IllegalStateException("Sword Throw server config used before initialization");
        }
        return configPath;
    }

    public static final class ConfigData {
        private int schemaVersion = CURRENT_SCHEMA_VERSION;
        private float baseHandDamage = 1.0F;
        private float velocityDamageBase = 0.65F;
        private float velocityDamageFactor = 0.7F;
        private float cleanFlightDamageMultiplier = 1.35F;
        private float standardDamageMultiplier = 1.0F;
        private float spearDamageMultiplier = 1.0F;
        private float spearMinimumAttackDamage = 4.0F;
        private float spearBaseDamageMultiplier = 1.45F;
        private float spearFlatDamageBonus = 2.5F;
        private float swordDamageMultiplier = 1.0F;
        private float axeDamageMultiplier = 0.75F;
        private float axeMinimumDamage = 3.5F;
        private float pickaxeDamageMultiplier = 0.6F;
        private float pickaxeMinimumDamage = 2.0F;
        private float shovelAndHoeDamageMultiplier = 0.5F;
        private float shovelAndHoeMinimumDamage = 1.25F;
        private float damageableItemDamageMultiplier = 0.4F;
        private float damageableItemMinimumDamage = 1.0F;
        private float blockItemBaseDamage = 0.35F;
        private float miscItemBaseDamage = 0.6F;

        public ConfigData() {
        }

        public ConfigData(float standardDamageMultiplier, float spearDamageMultiplier) {
            this.schemaVersion = CURRENT_SCHEMA_VERSION;
            this.standardDamageMultiplier = standardDamageMultiplier;
            this.spearDamageMultiplier = spearDamageMultiplier;
        }

        public int schemaVersion() {
            return schemaVersion;
        }

        public float baseHandDamage() {
            return baseHandDamage;
        }

        public float velocityDamageBase() {
            return velocityDamageBase;
        }

        public float velocityDamageFactor() {
            return velocityDamageFactor;
        }

        public float cleanFlightDamageMultiplier() {
            return cleanFlightDamageMultiplier;
        }

        public float standardDamageMultiplier() {
            return standardDamageMultiplier;
        }

        public float spearDamageMultiplier() {
            return spearDamageMultiplier;
        }

        public float spearMinimumAttackDamage() {
            return spearMinimumAttackDamage;
        }

        public float spearBaseDamageMultiplier() {
            return spearBaseDamageMultiplier;
        }

        public float spearFlatDamageBonus() {
            return spearFlatDamageBonus;
        }

        public float swordDamageMultiplier() {
            return swordDamageMultiplier;
        }

        public float axeDamageMultiplier() {
            return axeDamageMultiplier;
        }

        public float axeMinimumDamage() {
            return axeMinimumDamage;
        }

        public float pickaxeDamageMultiplier() {
            return pickaxeDamageMultiplier;
        }

        public float pickaxeMinimumDamage() {
            return pickaxeMinimumDamage;
        }

        public float shovelAndHoeDamageMultiplier() {
            return shovelAndHoeDamageMultiplier;
        }

        public float shovelAndHoeMinimumDamage() {
            return shovelAndHoeMinimumDamage;
        }

        public float damageableItemDamageMultiplier() {
            return damageableItemDamageMultiplier;
        }

        public float damageableItemMinimumDamage() {
            return damageableItemMinimumDamage;
        }

        public float blockItemBaseDamage() {
            return blockItemBaseDamage;
        }

        public float miscItemBaseDamage() {
            return miscItemBaseDamage;
        }
    }
}
