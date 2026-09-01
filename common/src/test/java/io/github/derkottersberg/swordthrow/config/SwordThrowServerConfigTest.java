package io.github.derkottersberg.swordthrow.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwordThrowServerConfigTest {
    private static final List<ConfigField> CONFIG_FIELDS = List.of(
        damage("baseHandDamage"),
        multiplier("velocityDamageBase"),
        multiplier("velocityDamageFactor"),
        multiplier("cleanFlightDamageMultiplier"),
        multiplier("standardDamageMultiplier"),
        multiplier("spearDamageMultiplier"),
        damage("spearMinimumAttackDamage"),
        multiplier("spearBaseDamageMultiplier"),
        damage("spearFlatDamageBonus"),
        multiplier("swordDamageMultiplier"),
        multiplier("axeDamageMultiplier"),
        damage("axeMinimumDamage"),
        multiplier("pickaxeDamageMultiplier"),
        damage("pickaxeMinimumDamage"),
        multiplier("shovelAndHoeDamageMultiplier"),
        damage("shovelAndHoeMinimumDamage"),
        multiplier("damageableItemDamageMultiplier"),
        damage("damageableItemMinimumDamage"),
        damage("blockItemBaseDamage"),
        damage("miscItemBaseDamage")
    );

    @AfterEach
    void restoreDefaults() {
        SwordThrowServerConfig.replaceForTests(new SwordThrowServerConfig.ConfigData());
    }

    @Test
    void defaultsPreserveExistingDamageBalance() {
        SwordThrowServerConfig.ConfigData defaults = SwordThrowServerConfig.sanitize(null);
        assertEquals(SwordThrowServerConfig.CURRENT_SCHEMA_VERSION, defaults.schemaVersion());
        assertEquals(1.0F, defaults.baseHandDamage(), 0.0001F);
        assertEquals(0.65F, defaults.velocityDamageBase(), 0.0001F);
        assertEquals(0.7F, defaults.velocityDamageFactor(), 0.0001F);
        assertEquals(1.35F, defaults.cleanFlightDamageMultiplier(), 0.0001F);
        assertEquals(1.0F, defaults.standardDamageMultiplier(), 0.0001F);
        assertEquals(1.0F, defaults.spearDamageMultiplier(), 0.0001F);
        assertEquals(4.0F, defaults.spearMinimumAttackDamage(), 0.0001F);
        assertEquals(1.45F, defaults.spearBaseDamageMultiplier(), 0.0001F);
        assertEquals(2.5F, defaults.spearFlatDamageBonus(), 0.0001F);
        assertEquals(1.0F, defaults.swordDamageMultiplier(), 0.0001F);
        assertEquals(0.75F, defaults.axeDamageMultiplier(), 0.0001F);
        assertEquals(3.5F, defaults.axeMinimumDamage(), 0.0001F);
        assertEquals(0.6F, defaults.pickaxeDamageMultiplier(), 0.0001F);
        assertEquals(2.0F, defaults.pickaxeMinimumDamage(), 0.0001F);
        assertEquals(0.5F, defaults.shovelAndHoeDamageMultiplier(), 0.0001F);
        assertEquals(1.25F, defaults.shovelAndHoeMinimumDamage(), 0.0001F);
        assertEquals(0.4F, defaults.damageableItemDamageMultiplier(), 0.0001F);
        assertEquals(1.0F, defaults.damageableItemMinimumDamage(), 0.0001F);
        assertEquals(0.35F, defaults.blockItemBaseDamage(), 0.0001F);
        assertEquals(0.6F, defaults.miscItemBaseDamage(), 0.0001F);
    }

    @Test
    void everyInvalidFieldValueFallsBackToThatFieldsDefault() throws ReflectiveOperationException {
        float[] invalidValues = {
            -0.001F,
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        };
        SwordThrowServerConfig.ConfigData defaults = new SwordThrowServerConfig.ConfigData();

        for (ConfigField configField : CONFIG_FIELDS) {
            Field field = field(configField.name());
            float expectedDefault = field.getFloat(defaults);
            for (float invalidValue : invalidValues) {
                assertFallsBack(field, configField.name(), invalidValue, expectedDefault);
            }
            assertFallsBack(
                field,
                configField.name(),
                configField.maximum() + 0.001F,
                expectedDefault
            );
        }
    }

    @Test
    void validInclusiveBoundariesArePreservedForEveryField() throws ReflectiveOperationException {
        for (ConfigField configField : CONFIG_FIELDS) {
            Field field = field(configField.name());
            assertPreserved(field, configField.name(), SwordThrowServerConfig.MIN_VALUE);
            assertPreserved(field, configField.name(), configField.maximum());
        }
    }

    @Test
    void everyTypeInvalidFieldFallsBackWithoutDiscardingValidSiblings() throws ReflectiveOperationException {
        SwordThrowServerConfig.ConfigData defaults = new SwordThrowServerConfig.ConfigData();
        for (ConfigField configField : CONFIG_FIELDS) {
            String companion = configField.name().equals("miscItemBaseDamage")
                ? "baseHandDamage"
                : "miscItemBaseDamage";
            SwordThrowServerConfig.ConfigData parsed = SwordThrowServerConfig.deserializeForTests(
                "{\"" + configField.name() + "\":{\"invalid\":true},\"" + companion + "\":7.0}"
            );

            assertEquals(
                field(configField.name()).getFloat(defaults),
                field(configField.name()).getFloat(parsed),
                0.0001F,
                () -> configField.name() + " did not use its own default after a type-invalid value"
            );
            assertEquals(
                7.0F,
                field(companion).getFloat(parsed),
                0.0001F,
                () -> configField.name() + " caused a valid sibling field to be discarded"
            );
        }
    }

    @Test
    void selectsIndependentStandardAndSpearMultipliers() {
        SwordThrowServerConfig.replaceForTests(new SwordThrowServerConfig.ConfigData(0.5F, 0.2F));
        assertEquals(0.5F, SwordThrowServerConfig.get().standardDamageMultiplier(), 0.0001F);
        assertEquals(0.2F, SwordThrowServerConfig.get().spearDamageMultiplier(), 0.0001F);
    }

    private static void assertFallsBack(
        Field field,
        String fieldName,
        float invalidValue,
        float expectedDefault
    ) throws IllegalAccessException {
        SwordThrowServerConfig.ConfigData candidate = new SwordThrowServerConfig.ConfigData();
        field.setFloat(candidate, invalidValue);
        SwordThrowServerConfig.ConfigData sanitized = SwordThrowServerConfig.sanitize(candidate);
        assertEquals(
            expectedDefault,
            field.getFloat(sanitized),
            0.0001F,
            () -> fieldName + " did not fall back from " + invalidValue + " to its own default"
        );
    }

    private static void assertPreserved(Field field, String fieldName, float value) throws IllegalAccessException {
        SwordThrowServerConfig.ConfigData candidate = new SwordThrowServerConfig.ConfigData();
        field.setFloat(candidate, value);
        SwordThrowServerConfig.ConfigData sanitized = SwordThrowServerConfig.sanitize(candidate);
        assertEquals(
            value,
            field.getFloat(sanitized),
            0.0001F,
            () -> fieldName + " rejected valid inclusive boundary " + value
        );
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = SwordThrowServerConfig.ConfigData.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static ConfigField damage(String name) {
        return new ConfigField(name, SwordThrowServerConfig.MAX_DAMAGE_VALUE);
    }

    private static ConfigField multiplier(String name) {
        return new ConfigField(name, SwordThrowServerConfig.MAX_MULTIPLIER);
    }

    private record ConfigField(String name, float maximum) {
    }
}
