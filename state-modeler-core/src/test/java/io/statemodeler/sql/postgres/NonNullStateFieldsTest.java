package io.statemodeler.sql.postgres;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.statemodeler.core.AttributeDef;
import io.statemodeler.core.DatabaseConfig;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import io.statemodeler.core.StateDef;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NonNullStateFieldsTest {

    @Test
    void shouldEnforceNotNullOnStateFields() {
        // Given a model with a nullable state attribute
        var database = new DatabaseConfig("postgres", "public", null, Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        // "optional_field" is explicitly nullable=true
        var optionalAttr = new AttributeDef("optional_field", "text", true, false, null, null);

        var state = new StateDef(
                "draft", "doc_draft", true, (String) null, List.of(), Map.of("optional_field", optionalAttr));

        var entity =
                new EntityDef("document", "documents", idAttr, Map.of(), Map.of("draft", state), Map.of(), Map.of());

        var model = new SddModel("1.0.0", "test", database, Map.of("document", entity));
        var generator = new PebblePostgresDdlGenerator();

        // When
        var ddl = generator.generateDdl(model);

        // Then
        // The optional_field should be NOT NULL despite being defined as nullable in
        // the model
        assertTrue(
                ddl.contains("optional_field text NOT NULL"),
                "State field should be NOT NULL even if defined as nullable. Actual DDL:\n" + ddl);
    }
}
