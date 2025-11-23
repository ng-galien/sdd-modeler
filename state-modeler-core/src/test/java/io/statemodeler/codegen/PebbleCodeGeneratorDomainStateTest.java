package io.statemodeler.codegen;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.core.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PebbleCodeGeneratorDomainStateTest {

    @Test
    void shouldGenerateDomainStateComponents() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var createdAtAttr = new AttributeDef("created_at", "timestamptz", false, false, null, null);
        var created =
                new StateDef("created", "order_created", true, java.util.List.of(), java.util.List.of(), Map.of());
        var states = Map.of("created", created);
        var entity = new EntityDef(
                "order_item", "orders", idAttr, Map.of("created_at", createdAtAttr), states, Map.of(), Map.of());
        var database = new DatabaseConfig("postgres", "public", null, Map.of("packageName", "com.example"));
        var model = new SddModel("1.0.0", "test", database, Map.of(entity.name(), entity));

        CodeGenerator generator = CodeGenerators.forLanguage("java");
        var generated = generator.generate(model);

        // 1. Verify Domain State Entity
        var domainStateFilename = "com/example/OrderItemDomainState.java";
        assertTrue(
                generated.containsKey(domainStateFilename),
                "Expected generated Domain State Entity: " + domainStateFilename);
        var domainStateContent = generated.get(domainStateFilename);
        assertTrue(domainStateContent.contains("public record OrderItemDomainState("));
        assertTrue(domainStateContent.contains("@Table(\"order_item_state\")"));
        assertTrue(domainStateContent.contains("String stateJson"));

        // 2. Verify Domain State Repository
        var domainRepoFilename = "com/example/OrderItemDomainStateRepository.java";
        assertTrue(
                generated.containsKey(domainRepoFilename),
                "Expected generated Domain State Repository: " + domainRepoFilename);
        var domainRepoContent = generated.get(domainRepoFilename);
        assertTrue(domainRepoContent.contains(
                "interface OrderItemDomainStateRepository extends CrudRepository<OrderItemDomainState, OrderItemId>"));

        // 3. Verify Service updates
        var serviceFilename = "com/example/OrderItemService.java";
        assertTrue(generated.containsKey(serviceFilename));
        var serviceContent = generated.get(serviceFilename);
        assertTrue(serviceContent.contains("private final OrderItemDomainStateRepository domainStateRepository"));
        assertTrue(serviceContent.contains("private final com.fasterxml.jackson.databind.ObjectMapper objectMapper"));
        assertTrue(serviceContent.contains("public java.util.List<OrderItemState> findAll()"));
        assertTrue(serviceContent.contains("objectMapper.readValue(ds.stateJson(), stateClass)"));

        // 4. Verify Controller updates
        var controllerFilename = "com/example/OrderItemController.java";
        assertTrue(generated.containsKey(controllerFilename));
        var controllerContent = generated.get(controllerFilename);
        assertTrue(controllerContent.contains("@GetMapping"));
        assertTrue(controllerContent.contains("public java.util.List<OrderItemState> findAll()"));
        assertTrue(controllerContent.contains("return service.findAll();"));
    }
}
