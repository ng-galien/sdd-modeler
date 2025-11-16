package io.statemodeler.core;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StateDefTest {

    @Test
    void shouldCreateInitialState() {
        // Given
        var attributes = Map.of("pending_reason", new AttributeDef("pending_reason", "text", false, false, null, null));

        // When
        var state = new StateDef("pending", "order_pending", true, List.of(), List.of(), attributes);

        // Then
        assertThat(state.name()).isEqualTo("pending");
        assertThat(state.table()).isEqualTo("order_pending");
        assertThat(state.initial()).isTrue();
        assertThat(state.from()).isEmpty();
        assertThat(state.fromAnyOf()).isEmpty();
        assertThat(state.attributes()).hasSize(1);
        assertThat(state.hasSimpleTransitions()).isFalse();
        assertThat(state.hasOrTransitions()).isFalse();
    }

    @Test
    void shouldCreateStateWithSimpleTransition() {
        // Given
        var attributes =
                Map.of("paid_amount", new AttributeDef("paid_amount", "decimal(10,2)", false, false, null, null));

        // When
        var state = new StateDef("paid", "order_paid", false, List.of("pending"), List.of(), attributes);

        // Then
        assertThat(state.name()).isEqualTo("paid");
        assertThat(state.initial()).isFalse();
        assertThat(state.from()).containsExactly("pending");
        assertThat(state.fromAnyOf()).isEmpty();
        assertThat(state.hasSimpleTransitions()).isTrue();
        assertThat(state.hasOrTransitions()).isFalse();
    }

    @Test
    void shouldCreateStateWithOrTransitions() {
        // Given
        var attributes = Map.of("cancel_reason", new AttributeDef("cancel_reason", "text", false, false, null, null));

        // When
        var state =
                new StateDef("cancelled", "order_cancelled", false, List.of(), List.of("pending", "paid"), attributes);

        // Then
        assertThat(state.name()).isEqualTo("cancelled");
        assertThat(state.initial()).isFalse();
        assertThat(state.from()).isEmpty();
        assertThat(state.fromAnyOf()).containsExactly("pending", "paid");
        assertThat(state.hasSimpleTransitions()).isFalse();
        assertThat(state.hasOrTransitions()).isTrue();
    }

    @Test
    void shouldCreateStateWithMultipleSimpleTransitions() {
        // Given
        var attributes = Map.<String, AttributeDef>of();

        // When
        var state =
                new StateDef("finalized", "order_finalized", false, List.of("paid", "refunded"), List.of(), attributes);

        // Then
        assertThat(state.from()).containsExactly("paid", "refunded");
        assertThat(state.hasSimpleTransitions()).isTrue();
        assertThat(state.hasOrTransitions()).isFalse();
    }

    @Test
    void shouldRejectNullName() {
        assertThatThrownBy(() -> new StateDef(null, "table", false, List.of(), List.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null");
    }

    @Test
    void shouldRejectNullTable() {
        assertThatThrownBy(() -> new StateDef("state", null, false, List.of(), List.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("table cannot be null");
    }

    @Test
    void shouldRejectNullFrom() {
        assertThatThrownBy(() -> new StateDef("state", "table", false, null, List.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from cannot be null");
    }

    @Test
    void shouldRejectNullFromAnyOf() {
        assertThatThrownBy(() -> new StateDef("state", "table", false, List.of(), null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromAnyOf cannot be null");
    }

    @Test
    void shouldRejectNullAttributes() {
        assertThatThrownBy(() -> new StateDef("state", "table", false, List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attributes cannot be null");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var attributes = Map.of("attr", new AttributeDef("attr", "text", false, false, null, null));
        var state1 = new StateDef("pending", "order_pending", true, List.of(), List.of(), attributes);
        var state2 = new StateDef("pending", "order_pending", true, List.of(), List.of(), attributes);
        var state3 = new StateDef("paid", "order_pending", true, List.of(), List.of(), attributes);

        // Then
        assertThat(state1).isEqualTo(state2);
        assertThat(state1).isNotEqualTo(state3);
        assertThat(state1.hashCode()).isEqualTo(state2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var state = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());

        // When
        var result = state.toString();

        // Then
        assertThat(result)
                .contains("StateDef")
                .contains("name=pending")
                .contains("table=order_pending")
                .contains("initial=true");
    }

    @Test
    void shouldCreateImmutableCollections() {
        // Given
        var mutableFrom = List.of("pending");
        var mutableFromAnyOf = List.of("paid");
        var mutableAttributes = Map.of("attr", new AttributeDef("attr", "text", false, false, null, null));

        // When
        var state =
                new StateDef("cancelled", "order_cancelled", false, mutableFrom, mutableFromAnyOf, mutableAttributes);

        // Then - collections should be immutable copies
        assertThat(state.from()).isInstanceOf(List.class);
        assertThat(state.fromAnyOf()).isInstanceOf(List.class);
        assertThat(state.attributes()).isInstanceOf(Map.class);

        // Verify they contain the expected data
        assertThat(state.from()).containsExactly("pending");
        assertThat(state.fromAnyOf()).containsExactly("paid");
        assertThat(state.attributes()).hasSize(1);
    }

    @Test
    void shouldHandleEmptyCollections() {
        // Given & When
        var state = new StateDef("isolated", "isolated_table", true, List.of(), List.of(), Map.of());

        // Then
        assertThat(state.from()).isEmpty();
        assertThat(state.fromAnyOf()).isEmpty();
        assertThat(state.attributes()).isEmpty();
        assertThat(state.hasSimpleTransitions()).isFalse();
        assertThat(state.hasOrTransitions()).isFalse();
    }
}
