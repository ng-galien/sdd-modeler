package io.statemodeler.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProjectionDefTest {

    @Test
    void shouldCreateValidProjectionDef() {
        // Given & When
        var projection =
                new ProjectionDef("current_state", "current_order_states", ProjectionDef.ProjectionKind.CURRENT_STATE);

        // Then
        assertEquals("current_state", projection.name());
        assertEquals("current_order_states", projection.viewName());
        assertEquals(ProjectionDef.ProjectionKind.CURRENT_STATE, projection.kind());
    }

    @Test
    void shouldCreateIntervalsProjection() {
        // Given & When
        var projection =
                new ProjectionDef("state_intervals", "order_state_intervals", ProjectionDef.ProjectionKind.INTERVALS);

        // Then
        assertEquals("state_intervals", projection.name());
        assertEquals("order_state_intervals", projection.viewName());
        assertEquals(ProjectionDef.ProjectionKind.INTERVALS, projection.kind());
    }

    @Test
    void shouldRejectNullName() {
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectionDef(null, "view_name", ProjectionDef.ProjectionKind.CURRENT_STATE));
        assertTrue(ex.getMessage().contains("name cannot be null"));
    }

    @Test
    void shouldRejectNullViewName() {
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectionDef("projection", null, ProjectionDef.ProjectionKind.INTERVALS));
        assertTrue(ex.getMessage().contains("viewName cannot be null"));
    }

    @Test
    void shouldRejectNullKind() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new ProjectionDef("projection", "view_name", null));
        assertTrue(ex.getMessage().contains("kind cannot be null"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var projection1 = new ProjectionDef("current", "current_states", ProjectionDef.ProjectionKind.CURRENT_STATE);
        var projection2 = new ProjectionDef("current", "current_states", ProjectionDef.ProjectionKind.CURRENT_STATE);
        var projection3 = new ProjectionDef("intervals", "current_states", ProjectionDef.ProjectionKind.CURRENT_STATE);

        // Then
        assertEquals(projection2, projection1);
        assertNotEquals(projection3, projection1);
        assertEquals(projection2.hashCode(), projection1.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var projection = new ProjectionDef("current", "current_states", ProjectionDef.ProjectionKind.CURRENT_STATE);

        // When
        var result = projection.toString();

        // Then
        assertTrue(result.contains("ProjectionDef"));
        assertTrue(result.contains("name=current"));
        assertTrue(result.contains("viewName=current_states"));
        assertTrue(result.contains("kind=CURRENT_STATE"));
    }

    @Test
    void shouldTestProjectionKindEnum() {
        // Test enum values
        assertEquals(2, ProjectionDef.ProjectionKind.values().length);
        assertTrue(java.util.Arrays.asList(ProjectionDef.ProjectionKind.values())
                .containsAll(java.util.List.of(
                        ProjectionDef.ProjectionKind.INTERVALS, ProjectionDef.ProjectionKind.CURRENT_STATE)));

        // Test valueOf
        assertEquals(ProjectionDef.ProjectionKind.INTERVALS, ProjectionDef.ProjectionKind.valueOf("INTERVALS"));
        assertEquals(ProjectionDef.ProjectionKind.CURRENT_STATE, ProjectionDef.ProjectionKind.valueOf("CURRENT_STATE"));
    }

    @Test
    void shouldHandleEdgeCases() {
        // Test with minimal valid data
        var minimalProjection = new ProjectionDef("a", "b", ProjectionDef.ProjectionKind.INTERVALS);

        assertEquals(1, minimalProjection.name().length());
        assertEquals(1, minimalProjection.viewName().length());

        // Test different naming conventions
        var camelCase =
                new ProjectionDef("currentState", "currentOrderStates", ProjectionDef.ProjectionKind.CURRENT_STATE);
        var snakeCase =
                new ProjectionDef("current_state", "current_order_states", ProjectionDef.ProjectionKind.CURRENT_STATE);

        assertTrue(camelCase.name().contains("currentState"));
        assertTrue(snakeCase.name().contains("current_state"));
    }
}
