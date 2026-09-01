package io.github.derkottersberg.swordthrow.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SwordThrowClientConfigTest {
    @Test
    void oldConfigRequestsExactlyOneDropKeyMigration(@TempDir Path directory) throws IOException {
        Path config = directory.resolve("swordthrow-client.json");
        Files.writeString(
            config,
            """
            {
              "thirdPersonAnimationsEnabled": false,
              "trailEffectEnabled": true,
              "trailColor": 13936186
            }
            """,
            StandardCharsets.UTF_8
        );

        SwordThrowClientConfig.initialize(directory);
        assertTrue(SwordThrowClientConfig.consumeDropKeyMigration());
        assertFalse(SwordThrowClientConfig.consumeDropKeyMigration());
        assertEquals(
            SwordThrowClientConfig.CURRENT_SCHEMA_VERSION,
            JsonParser.parseString(Files.readString(config, StandardCharsets.UTF_8))
                .getAsJsonObject()
                .get("schemaVersion")
                .getAsInt()
        );
    }

    @Test
    void currentConfigDoesNotOverwriteTheUsersThrowBinding(@TempDir Path directory) throws IOException {
        Files.writeString(
            directory.resolve("swordthrow-client.json"),
            """
            {
              "schemaVersion": 2,
              "thirdPersonAnimationsEnabled": true,
              "trailEffectEnabled": true,
              "trailColor": 13936186
            }
            """,
            StandardCharsets.UTF_8
        );

        SwordThrowClientConfig.initialize(directory);
        assertFalse(SwordThrowClientConfig.consumeDropKeyMigration());
    }

    @Test
    void freshInstallStartsAtTheCurrentSchemaWithoutMigration(@TempDir Path directory) {
        SwordThrowClientConfig.initialize(directory);
        assertFalse(SwordThrowClientConfig.consumeDropKeyMigration());
        assertEquals(
            SwordThrowClientConfig.CURRENT_SCHEMA_VERSION,
            SwordThrowClientConfig.get().schemaVersion()
        );
    }
}
