package io.statemodeler.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class YamlModelLoaderTest {

    @Test
    void shouldLoadOrdersSddModelFromYaml() throws Exception {
        // Given
        var yamlLoader = new YamlModelLoader();
        var ordersModelUrl = getClass().getClassLoader().getResource("orders-sdd-model.yaml");
        assertNotNull(ordersModelUrl);
        var ordersModelPath = Paths.get(ordersModelUrl.toURI());

        // When
        var result = yamlLoader.loadFromFile(ordersModelPath);

        // Then
        assertTrue(
                result.isSuccess(),
                "YAML parsing failed: "
                        + (result.isFailure() ? result.getCause().getMessage() : "Unknown error"));

        var model = result.get();

        assertNotNull(model);
        assertEquals("orders-sdd-example", model.name());
        assertTrue(model.entities().containsKey("order"));

        var orderEntity = model.entities().get("order");
        assertTrue(orderEntity.states().containsKey("pending"));
        assertTrue(orderEntity.states().containsKey("paid"));
        assertTrue(orderEntity.states().containsKey("cancelled"));
        assertTrue(orderEntity.states().containsKey("refunded"));
    }

    @Test
    void shouldLoadSimpleModel() {
        var yamlLoader = new YamlModelLoader();
        var simpleYaml = """
                version: "0.1"
                name: "test-model"
                database:
                  dialect: "postgres"
                entities: {}
                """;

        var result = yamlLoader.loadFromString(simpleYaml);

        assertTrue(result.isSuccess());
        var model = result.get();
        assertNotNull(model);
        assertEquals("0.1", model.version());
        assertEquals("test-model", model.name());
        assertEquals("postgres", model.database().dialect());
        assertTrue(model.entities().isEmpty());
    }

    @Test
    void shouldRejectEmptyContent() {
        var yamlLoader = new YamlModelLoader();

        var result1 = yamlLoader.loadFromString("");
        assertTrue(result1.isFailure());
        assertInstanceOf(IllegalArgumentException.class, result1.getCause());
        assertTrue(result1.getCause().getMessage().contains("Model content cannot be empty"));

        var result2 = yamlLoader.loadFromString(null);
        assertTrue(result2.isFailure());
        assertInstanceOf(IllegalArgumentException.class, result2.getCause());
        assertTrue(result2.getCause().getMessage().contains("Model content cannot be empty"));
    }

    @Test
    void shouldSupportYamlExtensions() {
        var yamlLoader = new YamlModelLoader();

        assertTrue(yamlLoader.supports("yaml"));
        assertTrue(yamlLoader.supports("yml"));
        assertTrue(yamlLoader.supports("YAML"));
        assertFalse(yamlLoader.supports("json"));
        assertFalse(yamlLoader.supports(null));
    }
}
