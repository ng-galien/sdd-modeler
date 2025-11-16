package io.statemodeler.validation;

import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import io.statemodeler.core.StateDef;
import io.vavr.control.Validation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ModelValidator using Vavr Validation to validate SDD models.
 * Validates SDD models according to business rules and structural constraints.
 */
public final class DefaultModelValidator {

    public Validation<List<ValidationError>, SddModel> validate(SddModel model) {
        var errors = new ArrayList<ValidationError>();

        // Validate model-level constraints
        validateModelStructure(model, errors);

        // Validate each entity
        for (var entry : model.entities().entrySet()) {
            var entityName = entry.getKey();
            var entity = entry.getValue();
            validateEntity(entityName, entity, errors);
        }

        // Return validation result
        if (errors.isEmpty()) {
            return Validation.valid(model);
        } else {
            return Validation.invalid(errors);
        }
    }

    private void validateModelStructure(SddModel model, List<ValidationError> errors) {
        // Validate that model has at least one entity
        if (model.entities().isEmpty()) {
            errors.add(ValidationError.global("MODEL_NO_ENTITIES", "Model must contain at least one entity"));
        }

        // Validate entity names are not empty and valid
        for (var entityName : model.entities().keySet()) {
            if (entityName == null || entityName.trim().isEmpty()) {
                errors.add(ValidationError.global("MODEL_INVALID_ENTITY_NAME", "Entity name cannot be null or empty"));
            }
        }
    }

    private void validateEntity(String entityName, EntityDef entity, List<ValidationError> errors) {
        // Validate entity has at least one state
        if (entity.states().isEmpty()) {
            errors.add(ValidationError.entity("ENTITY_NO_STATES", "Entity must have at least one state", entityName));
            return; // Skip further state validation if no states
        }

        // Validate exactly one initial state
        validateInitialState(entityName, entity, errors);

        // Validate each state
        for (var entry : entity.states().entrySet()) {
            var stateName = entry.getKey();
            var state = entry.getValue();
            validateState(entityName, stateName, state, entity, errors);
        }

        // Validate state transitions are valid
        validateStateTransitions(entityName, entity, errors);

        // Validate extensions reference valid states
        validateExtensions(entityName, entity, errors);

        // Validate projections reference valid states
        validateProjections(entityName, entity, errors);
    }

    private void validateInitialState(String entityName, EntityDef entity, List<ValidationError> errors) {
        var initialStates =
                entity.states().values().stream().filter(StateDef::initial).toList();

        if (initialStates.isEmpty()) {
            errors.add(ValidationError.entity(
                    "ENTITY_NO_INITIAL_STATE", "Entity must have exactly one initial state", entityName));
        } else if (initialStates.size() > 1) {
            errors.add(ValidationError.entity(
                    "ENTITY_MULTIPLE_INITIAL_STATES",
                    String.format("Entity has %d initial states but must have exactly one", initialStates.size()),
                    entityName));
        }
    }

    private void validateState(
            String entityName, String stateName, StateDef state, EntityDef entity, List<ValidationError> errors) {

        // Validate state name matches key
        if (!stateName.equals(state.name())) {
            errors.add(ValidationError.state(
                    "STATE_NAME_MISMATCH",
                    String.format("State key '%s' does not match state name '%s'", stateName, state.name()),
                    entityName,
                    stateName));
        }

        // Validate table name is not empty
        if (state.table() == null || state.table().trim().isEmpty()) {
            errors.add(ValidationError.state(
                    "STATE_EMPTY_TABLE", "State table name cannot be null or empty", entityName, stateName));
        }

        // Validate from and from_any_of are not both specified
        if (!state.from().isEmpty() && !state.fromAnyOf().isEmpty()) {
            errors.add(ValidationError.state(
                    "STATE_CONFLICTING_TRANSITIONS",
                    "State cannot have both 'from' and 'from_any_of' transitions",
                    entityName,
                    stateName));
        }

        // Initial state should not have incoming transitions
        if (state.initial() && (!state.from().isEmpty() || !state.fromAnyOf().isEmpty())) {
            errors.add(ValidationError.state(
                    "STATE_INITIAL_WITH_TRANSITIONS",
                    "Initial state cannot have incoming transitions (from/from_any_of)",
                    entityName,
                    stateName));
        }
    }

    private void validateStateTransitions(String entityName, EntityDef entity, List<ValidationError> errors) {
        var stateNames = entity.states().keySet();

        for (var entry : entity.states().entrySet()) {
            var stateName = entry.getKey();
            var state = entry.getValue();

            // Validate 'from' transitions reference existing states
            for (var fromState : state.from()) {
                if (!stateNames.contains(fromState)) {
                    errors.add(ValidationError.state(
                            "STATE_INVALID_FROM_TRANSITION",
                            String.format("State references unknown 'from' state: %s", fromState),
                            entityName,
                            stateName));
                }
            }

            // Validate 'from_any_of' transitions reference existing states
            for (var fromState : state.fromAnyOf()) {
                if (!stateNames.contains(fromState)) {
                    errors.add(ValidationError.state(
                            "STATE_INVALID_FROM_ANY_OF_TRANSITION",
                            String.format("State references unknown 'from_any_of' state: %s", fromState),
                            entityName,
                            stateName));
                }
            }

            // Validate 'from_any_of' has at least 2 states (OR semantics)
            if (state.fromAnyOf().size() == 1) {
                errors.add(ValidationError.state(
                        "STATE_SINGLE_FROM_ANY_OF",
                        "State 'from_any_of' should have at least 2 source states for OR semantics",
                        entityName,
                        stateName));
            }
        }

        // Validate no circular dependencies (simplified check)
        validateNoCycles(entityName, entity, errors);
    }

    private void validateNoCycles(String entityName, EntityDef entity, List<ValidationError> errors) {
        // Simple cycle detection - check if any state can reach itself through transitions
        for (var stateName : entity.states().keySet()) {
            if (canReach(stateName, stateName, entity.states(), new HashSet<>())) {
                errors.add(ValidationError.entity(
                        "ENTITY_CYCLIC_TRANSITIONS",
                        String.format("Detected circular transition path involving state: %s", stateName),
                        entityName));
                break; // Report only first cycle found
            }
        }
    }

    private boolean canReach(String from, String target, java.util.Map<String, StateDef> states, Set<String> visited) {
        if (visited.contains(from)) {
            return false; // Already visited, avoid infinite recursion
        }
        visited.add(from);

        var state = states.get(from);
        if (state == null) {
            return false;
        }

        // Check direct transitions
        for (var nextState : state.from()) {
            if (nextState.equals(target) || canReach(nextState, target, states, visited)) {
                return true;
            }
        }

        for (var nextState : state.fromAnyOf()) {
            if (nextState.equals(target) || canReach(nextState, target, states, visited)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a model is valid without returning detailed errors.
     *
     * @param model the SDD model to check
     * @return true if the model is valid, false otherwise
     */
    public boolean isValid(SddModel model) {
        return validate(model).isValid();
    }

    /**
     * Validate a model and throw an exception if invalid.
     *
     * @param model the SDD model to validate
     * @return the model if valid
     * @throws IllegalArgumentException if the model is invalid
     */
    public SddModel validateOrThrow(SddModel model) {
        var validation = validate(model);
        if (validation.isInvalid()) {
            var errors = validation.getError();
            var errorMessage = errors.stream()
                    .map(ValidationError::getFormattedMessage)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown validation error");
            throw new IllegalArgumentException("Model validation failed: " + errorMessage);
        }
        return validation.get();
    }

    private void validateExtensions(String entityName, EntityDef entity, List<ValidationError> errors) {
        for (var entry : entity.extensions().entrySet()) {
            var extensionName = entry.getKey();
            var extension = entry.getValue();

            // Validate target_state exists
            if (!entity.states().containsKey(extension.targetState())) {
                errors.add(ValidationError.field(
                        "EXTENSION_INVALID_TARGET_STATE",
                        String.format(
                                "Extension '%s' references unknown target state: %s",
                                extensionName, extension.targetState()),
                        entityName,
                        null,
                        "targetState"));
            }
        }
    }

    private void validateProjections(String entityName, EntityDef entity, List<ValidationError> errors) {
        for (var entry : entity.projections().entrySet()) {
            var projectionName = entry.getKey();
            var projection = entry.getValue();

            // Validate projection kind is supported
            if (projection.kind() == null) {
                errors.add(ValidationError.field(
                        "PROJECTION_MISSING_KIND",
                        String.format("Projection '%s' must specify a kind", projectionName),
                        entityName,
                        null,
                        "kind"));
            }

            // Validate view name is not empty
            if (projection.viewName() == null || projection.viewName().trim().isEmpty()) {
                errors.add(ValidationError.field(
                        "PROJECTION_EMPTY_VIEW_NAME",
                        String.format("Projection '%s' must have a non-empty view name", projectionName),
                        entityName,
                        null,
                        "viewName"));
            }
        }
    }
}
