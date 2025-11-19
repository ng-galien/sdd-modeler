package io.statemodeler.validation;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.core.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultModelValidatorTest {

    private DefaultModelValidator validator;

    @BeforeEach
    void setUp() {
        validator = ModelValidators.create();
    }

    @Test
    void shouldValidateCorrectModel() {
        // Given
        var model = createValidModel();

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isValid());
        assertEquals(model, result.get());
    }

    @Test
    void shouldRejectModelWithNoEntities() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var model = new SddModel("1.0", "test", database, Map.of());

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isInvalid());
        var errors = result.getError();
        assertEquals(1, errors.size());
        assertEquals("MODEL_NO_ENTITIES", errors.get(0).code());
    }

    @Test
    void shouldRejectEntityWithNoStates() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isInvalid());
        var errors = result.getError();
        assertEquals(1, errors.size());
        assertEquals("ENTITY_NO_STATES", errors.get(0).code());
        assertEquals("order", errors.get(0).entityName());
    }

    @Test
    void shouldRejectEntityWithNoInitialState() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var state = new StateDef("pending", "order_pending", false, List.of(), List.of(), Map.of());
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), Map.of("pending", state), Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isInvalid());
        var errors = result.getError();
        assertTrue(errors.stream()
                .anyMatch(
                        error -> "ENTITY_NO_INITIAL_STATE".equals(error.code()) && "order".equals(error.entityName())));
    }

    @Test
    void shouldRejectEntityWithMultipleInitialStates() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var state1 = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var state2 = new StateDef("processing", "order_processing", true, List.of(), List.of(), Map.of());
        var states = Map.of("pending", state1, "processing", state2);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isInvalid());
        var errors = result.getError();
        assertTrue(errors.stream()
                .anyMatch(error ->
                        "ENTITY_MULTIPLE_INITIAL_STATES".equals(error.code()) && "order".equals(error.entityName())));
    }

    @Test
    void shouldRejectInvalidTransitions() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var paidState = new StateDef("paid", "order_paid", false, List.of("unknown_state"), List.of(), Map.of());
        var states = Map.of("pending", pendingState, "paid", paidState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isInvalid());
        var errors = result.getError();
        assertTrue(errors.stream()
                .anyMatch(error -> "STATE_INVALID_FROM_TRANSITION".equals(error.code())
                        && "order".equals(error.entityName())
                        && "paid".equals(error.stateName())));
    }

    @Test
    void shouldRejectInitialStateWithTransitions() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of("some_state"), List.of(), Map.of());
        var states = Map.of("pending", pendingState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isInvalid());
        var errors = result.getError();
        assertTrue(errors.stream()
                .anyMatch(error -> "STATE_INITIAL_WITH_TRANSITIONS".equals(error.code())
                        && "order".equals(error.entityName())
                        && "pending".equals(error.stateName())));
    }

    @Test
    void shouldRejectConflictingTransitions() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var paidState = new StateDef("paid", "order_paid", false, List.of("pending"), List.of("pending"), Map.of());
        var states = Map.of("pending", pendingState, "paid", paidState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isInvalid());
        var errors = result.getError();
        assertTrue(errors.stream()
                .anyMatch(error -> "STATE_CONFLICTING_TRANSITIONS".equals(error.code())
                        && "order".equals(error.entityName())
                        && "paid".equals(error.stateName())));
    }

    @Test
    void shouldValidateOrTransitionsWithMultipleSources() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var paidState = new StateDef("paid", "order_paid", false, List.of(), List.of(), Map.of());
        var canceledState =
                new StateDef("canceled", "order_canceled", false, List.of(), List.of("pending", "paid"), Map.of());
        var states = Map.of("pending", pendingState, "paid", paidState, "canceled", canceledState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    void shouldUseValidateOrThrowSuccessfully() {
        // Given
        var model = createValidModel();

        // When & Then
        assertDoesNotThrow(() -> validator.validateOrThrow(model));
    }

    @Test
    void shouldThrowOnInvalidModel() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var model = new SddModel("1.0", "test", database, Map.of());

        // When & Then
        var exception = assertThrows(IllegalArgumentException.class, () -> validator.validateOrThrow(model));
        assertTrue(exception.getMessage().contains("Model validation failed"));
    }

    @Test
    void shouldCheckIsValidCorrectly() {
        // Given
        var validModel = createValidModel();
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var invalidModel = new SddModel("1.0", "test", database, Map.of());

        // When & Then
        assertTrue(validator.isValid(validModel));
        assertFalse(validator.isValid(invalidModel));
    }

    @Test
    void shouldRejectInvalidAttributeTypes() {
        // Given - model with invalid PostgreSQL type
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        // Invalid type "string" instead of "TEXT"
        var invalidAttr = new AttributeDef("name", "string", false, false, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var states = Map.of("pending", pendingState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of("name", invalidAttr), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test-model", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isInvalid());
        var errors = result.getError();
        assertTrue(errors.size() >= 1);
        assertTrue(errors.stream().anyMatch(e -> e.code().equals("INVALID_ATTRIBUTE_TYPE")));
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("string")));
    }

    @Test
    void shouldAcceptValidPostgresTypes() {
        // Given - model with valid PostgreSQL types
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var nameAttr = new AttributeDef("name", "TEXT", false, false, null, null);
        var priceAttr = new AttributeDef("price", "NUMERIC(10,2)", false, false, null, null);
        var createdAtAttr = new AttributeDef("created_at", "TIMESTAMPTZ", false, false, null, null);
        var metadataAttr = new AttributeDef("metadata", "JSONB", true, false, null, null);

        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var states = Map.of("pending", pendingState);
        var attributes =
                Map.of("name", nameAttr, "price", priceAttr, "created_at", createdAtAttr, "metadata", metadataAttr);
        var entity = new EntityDef("order", "orders", idAttr, attributes, states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test-model", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    void shouldValidateStateAttributeTypes() {
        // Given - model with invalid type in state attributes
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        // Invalid type in state attributes
        var invalidStateAttr = new AttributeDef("reason", "varchar", false, false, null, null); // missing length
        var pendingState = new StateDef(
                "pending", "order_pending", true, List.of(), List.of(), Map.of("reason", invalidStateAttr));
        var states = Map.of("pending", pendingState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test-model", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then - should accept bare VARCHAR (it's in CHARACTER_TYPES)
        assertTrue(result.isValid());
    }

    @Test
    void shouldValidateExtensionAttributeTypes() {
        // Given - model with invalid type in extension attributes
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var states = Map.of("pending", pendingState);

        // Invalid type in extension attributes
        var invalidExtAttr = new AttributeDef("notes", "longtext", false, false, null, null); // MySQL type
        var extension =
                new ExtensionDef("pending_ext", "order_pending_ext", "pending", Map.of("notes", invalidExtAttr));
        var extensions = Map.of("pending_ext", extension);

        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, extensions, Map.of());
        var model = new SddModel("1.0", "test-model", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertTrue(result.isInvalid());
        var errors = result.getError();
        assertTrue(errors.stream().anyMatch(e -> e.code().equals("INVALID_ATTRIBUTE_TYPE")));
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("longtext")));
    }

    private SddModel createValidModel() {
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var paidState = new StateDef("paid", "order_paid", false, List.of("pending"), List.of(), Map.of());
        var states = Map.of("pending", pendingState, "paid", paidState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        return new SddModel("1.0", "test-model", database, Map.of("order", entity));
    }
}
