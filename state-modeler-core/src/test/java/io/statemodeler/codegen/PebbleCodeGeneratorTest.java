package io.statemodeler.codegen;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.core.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PebbleCodeGeneratorTest {

    @Test
    void shouldGenerateJavaFromModel() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pending =
                new StateDef("pending", "order_pending", true, java.util.List.of(), java.util.List.of(), Map.of());
        var states = Map.of("pending", pending);
        var entity = new EntityDef("Order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var database = new DatabaseConfig("postgres", "public", null, Map.of("packageName", "com.example"));
        var model = new SddModel("1.0", "test", database, Map.of("Order", entity));

        CodeGenerator generator = CodeGenerators.forLanguage("java");
        var generated = generator.generate(model);

        assertNotNull(generated);
        assertTrue(generated.size() >= 1);
        var filename = "com/example/Order.java";
        assertTrue(generated.containsKey(filename), "Expected generated file: " + filename);
        var content = generated.get(filename);
        assertTrue(content.contains("interface OrderState"));
        assertTrue(content.contains("record pending("));
    }
}
