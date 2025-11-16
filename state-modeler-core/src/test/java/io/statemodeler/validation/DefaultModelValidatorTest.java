package io.statemodeler.validation;

import static org.assertj.core.api.Assertions.*;

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
        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isEqualTo(model);
    }

    @Test
    void shouldRejectModelWithNoEntities() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null);
        var model = new SddModel("1.0", "test", database, Map.of());

        // When
        var result = validator.validate(model);

        // Then
        assertThat(result.isInvalid()).isTrue();
        var errors = result.getError();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).code()).isEqualTo("MODEL_NO_ENTITIES");
    }

    @Test
    void shouldRejectEntityWithNoStates() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null);
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertThat(result.isInvalid()).isTrue();
        var errors = result.getError();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).code()).isEqualTo("ENTITY_NO_STATES");
        assertThat(errors.get(0).entityName()).isEqualTo("order");
    }

    @Test
    void shouldRejectEntityWithNoInitialState() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null);
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var state = new StateDef("pending", "order_pending", false, List.of(), List.of(), Map.of());
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), Map.of("pending", state), Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertThat(result.isInvalid()).isTrue();
        var errors = result.getError();
        assertThat(errors).anySatisfy(error -> {
            assertThat(error.code()).isEqualTo("ENTITY_NO_INITIAL_STATE");
            assertThat(error.entityName()).isEqualTo("order");
        });
    }

    @Test
    void shouldRejectEntityWithMultipleInitialStates() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null);
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var state1 = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var state2 = new StateDef("processing", "order_processing", true, List.of(), List.of(), Map.of());
        var states = Map.of("pending", state1, "processing", state2);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertThat(result.isInvalid()).isTrue();
        var errors = result.getError();
        assertThat(errors).anySatisfy(error -> {
            assertThat(error.code()).isEqualTo("ENTITY_MULTIPLE_INITIAL_STATES");
            assertThat(error.entityName()).isEqualTo("order");
        });
    }

    @Test
    void shouldRejectInvalidTransitions() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null);
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var paidState = new StateDef("paid", "order_paid", false, List.of("unknown_state"), List.of(), Map.of());
        var states = Map.of("pending", pendingState, "paid", paidState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertThat(result.isInvalid()).isTrue();
        var errors = result.getError();
        assertThat(errors).anySatisfy(error -> {
            assertThat(error.code()).isEqualTo("STATE_INVALID_FROM_TRANSITION");
            assertThat(error.entityName()).isEqualTo("order");
            assertThat(error.stateName()).isEqualTo("paid");
        });
    }

    @Test
    void shouldRejectInitialStateWithTransitions() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null);
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of("some_state"), List.of(), Map.of());
        var states = Map.of("pending", pendingState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertThat(result.isInvalid()).isTrue();
        var errors = result.getError();
        assertThat(errors).anySatisfy(error -> {
            assertThat(error.code()).isEqualTo("STATE_INITIAL_WITH_TRANSITIONS");
            assertThat(error.entityName()).isEqualTo("order");
            assertThat(error.stateName()).isEqualTo("pending");
        });
    }

    @Test
    void shouldRejectConflictingTransitions() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null);
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var paidState = new StateDef("paid", "order_paid", false, List.of("pending"), List.of("pending"), Map.of());
        var states = Map.of("pending", pendingState, "paid", paidState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        var model = new SddModel("1.0", "test", database, Map.of("order", entity));

        // When
        var result = validator.validate(model);

        // Then
        assertThat(result.isInvalid()).isTrue();
        var errors = result.getError();
        assertThat(errors).anySatisfy(error -> {
            assertThat(error.code()).isEqualTo("STATE_CONFLICTING_TRANSITIONS");
            assertThat(error.entityName()).isEqualTo("order");
            assertThat(error.stateName()).isEqualTo("paid");
        });
    }

    @Test
    void shouldValidateOrTransitionsWithMultipleSources() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null);
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
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldUseValidateOrThrowSuccessfully() {
        // Given
        var model = createValidModel();

        // When & Then
        assertThatCode(() -> validator.validateOrThrow(model)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowOnInvalidModel() {
        // Given
        var database = new DatabaseConfig("postgres", "public", null);
        var model = new SddModel("1.0", "test", database, Map.of());

        // When & Then
        assertThatThrownBy(() -> validator.validateOrThrow(model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model validation failed");
    }

    @Test
    void shouldCheckIsValidCorrectly() {
        // Given
        var validModel = createValidModel();
        var database = new DatabaseConfig("postgres", "public", null);
        var invalidModel = new SddModel("1.0", "test", database, Map.of());

        // When & Then
        assertThat(validator.isValid(validModel)).isTrue();
        assertThat(validator.isValid(invalidModel)).isFalse();
    }

    private SddModel createValidModel() {
        var database = new DatabaseConfig("postgres", "public", null);
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var paidState = new StateDef("paid", "order_paid", false, List.of("pending"), List.of(), Map.of());
        var states = Map.of("pending", pendingState, "paid", paidState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());
        return new SddModel("1.0", "test-model", database, Map.of("order", entity));
    }
}
