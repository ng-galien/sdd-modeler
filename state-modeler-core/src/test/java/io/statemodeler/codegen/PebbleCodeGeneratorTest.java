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

        var repoFilename = "com/example/pendingRepository.java";
        assertTrue(generated.containsKey(repoFilename), "Expected generated repository: " + repoFilename);
        var repoContent = generated.get(repoFilename);
        assertTrue(
                repoContent.contains("interface pendingRepository extends CrudRepository<OrderState.pending, UUID>"));

        // Verify ID generation
        var idFilename = "com/example/OrderId.java";
        assertTrue(generated.containsKey(idFilename), "Expected generated ID: " + idFilename);
        assertTrue(generated.get(idFilename).contains("public record OrderId(UUID value)"));

        // Verify Converters generation
        var convertersFilename = "com/example/OrderConverters.java";
        assertTrue(generated.containsKey(convertersFilename), "Expected generated Converters: " + convertersFilename);
        assertTrue(generated.get(convertersFilename).contains("class UuidToOrderIdConverter"));
        assertTrue(generated.get(convertersFilename).contains("class OrderIdToUuidConverter"));

        // Verify Config generation
        var configFilename = "com/example/SddConfig.java";
        assertTrue(generated.containsKey(configFilename), "Expected generated Config: " + configFilename);
        assertTrue(generated.get(configFilename).contains("new OrderConverters.UuidToOrderIdConverter()"));

        // Verify Entity annotations
        assertTrue(content.contains("@Table(\"orders\")"));
        assertTrue(content.contains("@Id OrderId id"));
    }
}
