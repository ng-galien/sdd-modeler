package io.statemodeler.core;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SddModelTest {

    @Test
    void shouldCreateValidSddModel() {
        // Given
        var database = new DatabaseConfig("postgres", "public");
        var entityDef = createSampleEntityDef();
        var entities = Map.of("order", entityDef);

        // When
        var model = new SddModel("0.1", "test-model", database, entities);

        // Then
        assertThat(model.version()).isEqualTo("0.1");
        assertThat(model.name()).isEqualTo("test-model");
        assertThat(model.database()).isEqualTo(database);
        assertThat(model.entities()).hasSize(1);
        assertThat(model.entities().get("order")).isEqualTo(entityDef);
    }

    @Test
    void shouldRequireNonNullFields() {
        var database = new DatabaseConfig("postgres", "public");
        var entities = Map.<String, EntityDef>of();

        assertThatThrownBy(() -> new SddModel(null, "test", database, entities))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version cannot be null");

        assertThatThrownBy(() -> new SddModel("0.1", null, database, entities))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null");

        assertThatThrownBy(() -> new SddModel("0.1", "test", null, entities))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("database cannot be null");

        assertThatThrownBy(() -> new SddModel("0.1", "test", database, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entities cannot be null");
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
