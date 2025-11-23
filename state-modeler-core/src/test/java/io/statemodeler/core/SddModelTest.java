package io.statemodeler.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SddModelTest {

    @Test
    void shouldCreateValidSddModel() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var entityDef = createSampleEntityDef();
        var entities = Map.of("order", entityDef);

        // When
        var model = new SddModel("0.1.0", "test-model", database, entities);

        // Then
        assertEquals("0.1.0", model.version());
        assertEquals("test-model", model.name());
        assertEquals(database, model.database());
        assertEquals(1, model.entities().size());
        assertEquals(entityDef, model.entities().get("order"));
    }

    @Test
    void shouldRequireNonNullFields() {
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var entities = Map.<String, EntityDef>of();

        var ex1 = assertThrows(IllegalArgumentException.class, () -> new SddModel(null, "test", database, entities));
        assertTrue(ex1.getMessage().contains("version cannot be null"));

        var ex2 = assertThrows(IllegalArgumentException.class, () -> new SddModel("0.1.0", null, database, entities));
        assertTrue(ex2.getMessage().contains("name cannot be null"));

        var ex3 = assertThrows(IllegalArgumentException.class, () -> new SddModel("0.1.0", "test", null, entities));
        assertTrue(ex3.getMessage().contains("database cannot be null"));

        var ex4 = assertThrows(IllegalArgumentException.class, () -> new SddModel("0.1.0", "test", database, null));
        assertTrue(ex4.getMessage().contains("entities cannot be null"));

        var ex5 = assertThrows(IllegalArgumentException.class,
                () -> new SddModel("invalid-version", "test", database, entities));
        assertTrue(ex5.getMessage().contains("version must be a valid SemVer string"));
    }

    private EntityDef createSampleEntityDef() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var attributes = Map.<String, AttributeDef>of();
        var states = Map.<String, StateDef>of();
        var extensions = Map.<String, ExtensionDef>of();
        var projections = Map.<String, ProjectionDef>of();

        return new EntityDef("order", "orders", idAttr, attributes, states, extensions, projections);
    }
}
