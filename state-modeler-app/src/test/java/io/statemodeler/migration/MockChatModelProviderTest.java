package io.statemodeler.migration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MockChatModelProvider}.
 *
 * <p>Verifies that the mock provider creates working mock models for testing purposes.
 */
class MockChatModelProviderTest {

    @Test
    void shouldCreateMockModelWithDefaultResponse() {
        // Given
        var provider = new MockChatModelProvider();

        // When
        var model = provider.createModel("test-model", 0.0);

        // Then
        assertNotNull(model);
        String response = model.generate("test prompt");
        assertTrue(response.contains("Mock migration script"));
        assertTrue(response.contains("BEGIN"));
        assertTrue(response.contains("COMMIT"));
    }

    @Test
    void shouldCreateMockModelWithCustomResponse() {
        // Given
        String customResponse = "-- Custom migration\nALTER TABLE test ADD COLUMN new_col TEXT;";
        var provider = new MockChatModelProvider(customResponse);

        // When
        var model = provider.createModel("test-model", 0.0);

        // Then
        assertNotNull(model);
        assertEquals(customResponse, model.generate("any prompt"));
    }

    @Test
    void shouldReturnSameResponseForAllModelNames() {
        // Given
        String expectedResponse = "-- Deterministic response";
        var provider = new MockChatModelProvider(expectedResponse);
        var model = provider.createModel("test", 0.5);

        // When/Then - all prompts return same response
        assertEquals(expectedResponse, model.generate("prompt 1"));
        assertEquals(expectedResponse, model.generate("prompt 2"));
        assertEquals(expectedResponse, model.generate("different prompt"));
    }

    @Test
    void shouldCreateModelWithFullParameters() {
        // Given
        var provider = new MockChatModelProvider("test response");

        // When - use full parameter version
        var model = provider.createModel("model-name", 0.7, "http://localhost:8080", 120);

        // Then
        assertNotNull(model);
        assertEquals("test response", model.generate("prompt"));
    }

    @Test
    void shouldIgnoreModelNameAndTemperatureParameters() {
        // Given - mock provider ignores provider/model name
        var provider = new MockChatModelProvider("fixed response");

        // When - different providers and models all work
        var model1 = provider.createModel("some-model", 0.0);
        var model2 = provider.createModel("other-model", 1.0);
        var model3 = provider.createModel("test", 0.5);

        // Then - all return same mock response
        assertEquals("fixed response", model1.generate("test"));
        assertEquals("fixed response", model2.generate("test"));
        assertEquals("fixed response", model3.generate("test"));
    }
}
