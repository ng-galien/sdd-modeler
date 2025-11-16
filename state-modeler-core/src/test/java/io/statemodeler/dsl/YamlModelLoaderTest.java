package io.statemodeler.dsl;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class YamlModelLoaderTest {

    @Test
    void shouldLoadOrdersSddModelFromYaml() throws Exception {
        // Given
        var yamlLoader = new YamlModelLoader();
        var ordersModelUrl = getClass().getClassLoader().getResource("orders-sdd-model.yaml");
        assertThat(ordersModelUrl).isNotNull();
        var ordersModelPath = Paths.get(ordersModelUrl.toURI());

        // When
        var result = yamlLoader.loadFromFile(ordersModelPath);

        // Then
        assertThat(result.isSuccess())
                .withFailMessage(
                        "YAML parsing failed: %s",
                        result.isFailure() ? result.getCause().getMessage() : "Unknown error")
                .isTrue();

        var model = result.get();

        assertThat(model).isNotNull();
        assertThat(model.name()).isEqualTo("orders-sdd-example");
        assertThat(model.entities()).containsKey("order");

        var orderEntity = model.entities().get("order");
        assertThat(orderEntity.states()).containsKey("pending");
        assertThat(orderEntity.states()).containsKey("paid");
        assertThat(orderEntity.states()).containsKey("cancelled");
        assertThat(orderEntity.states()).containsKey("refunded");
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

        assertThat(result.isSuccess()).isTrue();
        var model = result.get();
        assertThat(model).isNotNull();
        assertThat(model.version()).isEqualTo("0.1");
        assertThat(model.name()).isEqualTo("test-model");
        assertThat(model.database().dialect()).isEqualTo("postgres");
        assertThat(model.entities()).isEmpty();
    }

    @Test
    void shouldRejectEmptyContent() {
        var yamlLoader = new YamlModelLoader();

        var result1 = yamlLoader.loadFromString("");
        assertThat(result1.isFailure()).isTrue();
        assertThat(result1.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(result1.getCause().getMessage()).contains("Model content cannot be empty");

        var result2 = yamlLoader.loadFromString(null);
        assertThat(result2.isFailure()).isTrue();
        assertThat(result2.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(result2.getCause().getMessage()).contains("Model content cannot be empty");
    }

    @Test
    void shouldSupportYamlExtensions() {
        var yamlLoader = new YamlModelLoader();

        assertThat(yamlLoader.supports("yaml")).isTrue();
        assertThat(yamlLoader.supports("yml")).isTrue();
        assertThat(yamlLoader.supports("YAML")).isTrue();
        assertThat(yamlLoader.supports("json")).isFalse();
        assertThat(yamlLoader.supports(null)).isFalse();
    }
}
