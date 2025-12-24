package io.statemodeler.codegen;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.core.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for domain state code generation using golden file (snapshot) testing.
 * Expected files are stored in src/test/resources/expected/simple-entity/
 *
 * To update expected files when templates change, run:
 * ./gradlew :state-modeler-core:test --tests PebbleCodeGeneratorDomainStateTest
 * -DupdateExpected=true
 */
class PebbleCodeGeneratorDomainStateTest {

    @Test
    void shouldGenerateDomainStateComponents() {
        // Create a simple test model with one state
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var createdAtAttr = new AttributeDef("created_at", "timestamptz", false, false, null, null);
        var created =
                new StateDef("created", "order_created", true, java.util.List.of(), java.util.List.of(), Map.of());
        var states = Map.of("created", created);
        var entity = new EntityDef(
                "order_item", "orders", idAttr, Map.of("created_at", createdAtAttr), states, Map.of(), Map.of());
        var database = new DatabaseConfig("postgres", "public", null, Map.of("packageName", "com.example"));
        var model = new SddModel("1.0.0", "test", database, Map.of(entity.name(), entity));

        // Generate code
        CodeGenerator generator = CodeGenerators.forLanguage("java");
        var generated = generator.generate(model);

        assertNotNull(generated);
        assertTrue(generated.size() >= 1, "Should generate multiple files");

        // Compare against golden files
        GoldenFileTest.assertGeneratedMatchesExpected("simple-entity", generated);
    }
}
