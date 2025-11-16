package io.statemodeler.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProjectionDefTest {

    @Test
    void shouldCreateValidProjectionDef() {
        // Given & When
        var projection =
                new ProjectionDef("current_state", "current_order_states", ProjectionDef.ProjectionKind.CURRENT_STATE);

        // Then
        assertThat(projection.name()).isEqualTo("current_state");
        assertThat(projection.viewName()).isEqualTo("current_order_states");
        assertThat(projection.kind()).isEqualTo(ProjectionDef.ProjectionKind.CURRENT_STATE);
    }

    @Test
    void shouldCreateIntervalsProjection() {
        // Given & When
        var projection =
                new ProjectionDef("state_intervals", "order_state_intervals", ProjectionDef.ProjectionKind.INTERVALS);

        // Then
        assertThat(projection.name()).isEqualTo("state_intervals");
        assertThat(projection.viewName()).isEqualTo("order_state_intervals");
        assertThat(projection.kind()).isEqualTo(ProjectionDef.ProjectionKind.INTERVALS);
    }

    @Test
    void shouldRejectNullName() {
        assertThatThrownBy(() -> new ProjectionDef(null, "view_name", ProjectionDef.ProjectionKind.CURRENT_STATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null");
    }

    @Test
    void shouldRejectNullViewName() {
        assertThatThrownBy(() -> new ProjectionDef("projection", null, ProjectionDef.ProjectionKind.INTERVALS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("viewName cannot be null");
    }

    @Test
    void shouldRejectNullKind() {
        assertThatThrownBy(() -> new ProjectionDef("projection", "view_name", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kind cannot be null");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var projection1 = new ProjectionDef("current", "current_states", ProjectionDef.ProjectionKind.CURRENT_STATE);
        var projection2 = new ProjectionDef("current", "current_states", ProjectionDef.ProjectionKind.CURRENT_STATE);
        var projection3 = new ProjectionDef("intervals", "current_states", ProjectionDef.ProjectionKind.CURRENT_STATE);

        // Then
        assertThat(projection1).isEqualTo(projection2);
        assertThat(projection1).isNotEqualTo(projection3);
        assertThat(projection1.hashCode()).isEqualTo(projection2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var projection = new ProjectionDef("current", "current_states", ProjectionDef.ProjectionKind.CURRENT_STATE);

        // When
        var result = projection.toString();

        // Then
        assertThat(result)
                .contains("ProjectionDef")
                .contains("name=current")
                .contains("viewName=current_states")
                .contains("kind=CURRENT_STATE");
    }

    @Test
    void shouldTestProjectionKindEnum() {
        // Test enum values
        assertThat(ProjectionDef.ProjectionKind.values())
                .hasSize(2)
                .contains(ProjectionDef.ProjectionKind.INTERVALS, ProjectionDef.ProjectionKind.CURRENT_STATE);

        // Test valueOf
        assertThat(ProjectionDef.ProjectionKind.valueOf("INTERVALS")).isEqualTo(ProjectionDef.ProjectionKind.INTERVALS);
        assertThat(ProjectionDef.ProjectionKind.valueOf("CURRENT_STATE"))
                .isEqualTo(ProjectionDef.ProjectionKind.CURRENT_STATE);
    }

    @Test
    void shouldHandleEdgeCases() {
        // Test with minimal valid data
        var minimalProjection = new ProjectionDef("a", "b", ProjectionDef.ProjectionKind.INTERVALS);

        assertThat(minimalProjection.name()).hasSize(1);
        assertThat(minimalProjection.viewName()).hasSize(1);

        // Test different naming conventions
        var camelCase =
                new ProjectionDef("currentState", "currentOrderStates", ProjectionDef.ProjectionKind.CURRENT_STATE);
        var snakeCase =
                new ProjectionDef("current_state", "current_order_states", ProjectionDef.ProjectionKind.CURRENT_STATE);

        assertThat(camelCase.name()).contains("currentState");
        assertThat(snakeCase.name()).contains("current_state");
    }
}
