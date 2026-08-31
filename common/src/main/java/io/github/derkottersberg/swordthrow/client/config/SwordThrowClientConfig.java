package io.github.derkottersberg.swordthrow.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.derkottersberg.swordthrow.SwordThrow;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SwordThrowClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    private static final TrailColorOption[] TRAIL_COLOR_OPTIONS = new TrailColorOption[] {
        new TrailColorOption("Amber", 0xD4A63A),
        new TrailColorOption("Crimson", 0xCF3B3B),
        new TrailColorOption("Emerald", 0x3BCF78),
        new TrailColorOption("Arcane Blue", 0x4A89FF),
        new TrailColorOption("Violet", 0x9F53FF),
        new TrailColorOption("Frost", 0x86E8FF),
        new TrailColorOption("White", 0xFFFFFF)
    };

    private static ConfigData data = new ConfigData();

    private SwordThrowClientConfig() {
    }

    public static void initialize(Path configDirectory) {
        configPath = configDirectory.resolve("swordthrow-client.json");
        load();
    }

    public static void load() {
        Path path = requireConfigPath();
        if (!Files.exists(path)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                data = loaded;
            }
            sanitize();
        } catch (IOException | RuntimeException ex) {
            SwordThrow.LOGGER.warn("Could not read {}. Defaults will be used.", path, ex);
            sanitize();
        }
    }

    public static void save() {
        sanitize();
        Path path = requireConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ex) {
            SwordThrow.LOGGER.warn("Could not save {}", path, ex);
        }
    }

    public static ConfigData get() {
        sanitize();
        return data;
    }

    public static void set(ConfigData nextData) {
        data = nextData;
        sanitize();
        save();
    }

    public static TrailColorOption[] trailColorOptions() {
        return TRAIL_COLOR_OPTIONS.clone();
    }

    public static int nextTrailColor(int currentColor) {
        int currentIndex = 0;
        for (int i = 0; i < TRAIL_COLOR_OPTIONS.length; i++) {
            if (TRAIL_COLOR_OPTIONS[i].rgb() == currentColor) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % TRAIL_COLOR_OPTIONS.length;
        return TRAIL_COLOR_OPTIONS[nextIndex].rgb();
    }

    public static String colorLabel(int rgb) {
        for (TrailColorOption option : TRAIL_COLOR_OPTIONS) {
            if (option.rgb() == rgb) {
                return option.label();
            }
        }
        return "Custom";
    }

    private static void sanitize() {
        if (data == null) {
            data = new ConfigData();
        }

        boolean colorFound = false;
        for (TrailColorOption option : TRAIL_COLOR_OPTIONS) {
            if (option.rgb() == data.trailColor) {
                colorFound = true;
                break;
            }
        }

        if (!colorFound) {
            data.trailColor = TRAIL_COLOR_OPTIONS[0].rgb();
        }
    }

    private static Path requireConfigPath() {
        if (configPath == null) {
            throw new IllegalStateException("Sword Throw client config used before initialization");
        }
        return configPath;
    }

    public record TrailColorOption(String label, int rgb) {
    }

    public static final class ConfigData {
        private boolean thirdPersonAnimationsEnabled = true;
        private boolean trailEffectEnabled = true;
        private int trailColor = 0xD4A63A;

        public ConfigData() {
        }

        public ConfigData(boolean thirdPersonAnimationsEnabled, boolean trailEffectEnabled, int trailColor) {
            this.thirdPersonAnimationsEnabled = thirdPersonAnimationsEnabled;
            this.trailEffectEnabled = trailEffectEnabled;
            this.trailColor = trailColor;
        }

        public boolean thirdPersonAnimationsEnabled() {
            return thirdPersonAnimationsEnabled;
        }

        public boolean trailEffectEnabled() {
            return trailEffectEnabled;
        }

        public int trailColor() {
            return trailColor;
        }
    }
}
