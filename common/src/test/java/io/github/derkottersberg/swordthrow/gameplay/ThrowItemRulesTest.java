package io.github.derkottersberg.swordthrow.gameplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class ThrowItemRulesTest {
    @Test
    void cannotThrowAlwaysWinsOverThrowable() {
        assertFalse(ThrowItemRules.decideThrowable(true, true, true, false));
        assertFalse(ThrowItemRules.decideThrowable(true, true, true, true));
    }

    @Test
    void throwableCanOptVanillaTridentsIntoSwordThrow() {
        assertFalse(ThrowItemRules.decideThrowable(true, false, false, true));
        assertTrue(ThrowItemRules.decideThrowable(true, false, true, true));
    }

    @Test
    void broadDefaultStillAllowsOrdinaryUntaggedItems() {
        assertTrue(ThrowItemRules.decideThrowable(true, false, false, false));
        assertFalse(ThrowItemRules.decideThrowable(false, false, true, false));
    }

    @Test
    void defaultSpearTagExtendsNativeSpearsAndKeepsTridentCompatibility() throws IOException {
        try (InputStream input = ThrowItemRulesTest.class.getResourceAsStream(
            "/data/swordthrow/tags/item/spears.json"
        )) {
            assertNotNull(input, "The default swordthrow:spears tag is missing");
            JsonObject tag = JsonParser.parseReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
            ).getAsJsonObject();
            assertFalse(tag.get("replace").getAsBoolean(), "Data packs must be able to extend the default spear tag");

            JsonArray values = tag.getAsJsonArray("values");
            Set<String> entries = StreamSupport.stream(values.spliterator(), false)
                .map(element -> element.getAsString())
                .collect(Collectors.toSet());
            assertTrue(entries.contains("#minecraft:spears"), "The native 26.2 spear tag is not inherited");
            assertTrue(entries.contains("minecraft:trident"), "The trident compatibility fallback is missing");
        }
    }
}
