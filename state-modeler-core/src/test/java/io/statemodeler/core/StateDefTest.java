package io.statemodeler.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StateDefTest {

    @Test
    void shouldCreateInitialState() {
        // Given
        var attributes = Map.of("pending_reason", new AttributeDef("pending_reason", "text", false, false, null, null));

        // When
        var state = new StateDef("pending", "order_pending", true, (String) null, List.of(), attributes);

        // Then
        assertEquals("pending", state.name());
        assertEquals("order_pending", state.table());
        assertTrue(state.initial());
        assertNull(state.from());
        assertTrue(state.fromAnyOf().isEmpty());
        assertEquals(1, state.attributes().size());
        assertFalse(state.hasSimpleTransitions());
        assertFalse(state.hasOrTransitions());
    }

    @Test
    void shouldCreateStateWithSimpleTransition() {
        // Given
        var attributes =
                Map.of("paid_amount", new AttributeDef("paid_amount", "decimal(10,2)", false, false, null, null));

        // When
        var state = new StateDef("paid", "order_paid", false, "pending", List.of(), attributes);

        // Then
        assertEquals("paid", state.name());
        assertFalse(state.initial());
        assertEquals("pending", state.from());
        assertTrue(state.fromAnyOf().isEmpty());
        assertTrue(state.hasSimpleTransitions());
        assertFalse(state.hasOrTransitions());
    }

    @Test
    void shouldCreateStateWithOrTransitions() {
        // Given
        var attributes = Map.of("cancel_reason", new AttributeDef("cancel_reason", "text", false, false, null, null));

        // When
        var state =
                new StateDef("cancelled", "order_cancelled", false, (String) null, List.of("pending", "paid"), attributes);

        // Then
        assertEquals("cancelled", state.name());
        assertFalse(state.initial());
        assertNull(state.from());
        assertEquals(List.of("pending", "paid"), state.fromAnyOf());
        assertFalse(state.hasSimpleTransitions());
        assertTrue(state.hasOrTransitions());
    }

    @Test
    void shouldRejectMultipleSimpleTransitions() {
        var attributes = Map.<String, AttributeDef>of();

        assertThrows(
                IllegalArgumentException.class,
                () -> new StateDef("finalized", "order_finalized", false, List.of("paid", "refunded"), List.of(), attributes));
    }

    @Test
    void shouldRejectNullName() {
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StateDef(null, "table", false, (String) null, List.of(), Map.of()));
        assertTrue(ex.getMessage().contains("name cannot be null"));
    }

    @Test
    void shouldRejectNullTable() {
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StateDef("state", null, false, (String) null, List.of(), Map.of()));
        assertTrue(ex.getMessage().contains("table cannot be null"));
    }

    @Test
    void shouldRejectNullFrom() {
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StateDef("state", "table", false, (String) null, null, Map.of()));
        assertTrue(ex.getMessage().contains("fromAnyOf cannot be null"));
    }

    @Test
    void shouldRejectNullFromAnyOf() {
        var ex = assertThrows(
                IllegalArgumentException.class, () -> new StateDef("state", "table", false, (String) null, null, Map.of()));
        assertTrue(ex.getMessage().contains("fromAnyOf cannot be null"));
    }

    @Test
    void shouldRejectNullAttributes() {
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StateDef("state", "table", false, (String) null, List.of(), null));
        assertTrue(ex.getMessage().contains("attributes cannot be null"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var attributes = Map.of("attr", new AttributeDef("attr", "text", false, false, null, null));
        var state1 = new StateDef("pending", "order_pending", true, (String) null, List.of(), attributes);
        var state2 = new StateDef("pending", "order_pending", true, (String) null, List.of(), attributes);
        var state3 = new StateDef("paid", "order_pending", true, (String) null, List.of(), attributes);

        // Then
        assertEquals(state2, state1);
        assertNotEquals(state3, state1);
        assertEquals(state2.hashCode(), state1.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var state = new StateDef("pending", "order_pending", true, (String) null, List.of(), Map.of());

        // When
        var result = state.toString();

        // Then
        assertTrue(result.contains("StateDef"));
        assertTrue(result.contains("name=pending"));
        assertTrue(result.contains("table=order_pending"));
        assertTrue(result.contains("initial=true"));
    }

    @Test
    void shouldCreateImmutableCollections() {
        // Given
        var mutableFrom = List.of("pending");
        var mutableFromAnyOf = List.of("paid");
        var mutableAttributes = Map.of("attr", new AttributeDef("attr", "text", false, false, null, null));

        // When
        var state =
                new StateDef("cancelled", "order_cancelled", false, "pending", mutableFromAnyOf, mutableAttributes);

        // Then - collections should be immutable copies
        assertInstanceOf(String.class, state.from());
        assertInstanceOf(List.class, state.fromAnyOf());
        assertInstanceOf(Map.class, state.attributes());

        // Verify they contain the expected data
        assertEquals("pending", state.from());
        assertEquals(List.of("paid"), state.fromAnyOf());
        assertEquals(1, state.attributes().size());
    }

    @Test
    void shouldHandleEmptyCollections() {
        // Given & When
        var state = new StateDef("isolated", "isolated_table", true, (String) null, List.of(), Map.of());

        // Then
        assertNull(state.from());
        assertTrue(state.fromAnyOf().isEmpty());
        assertTrue(state.attributes().isEmpty());
        assertFalse(state.hasSimpleTransitions());
        assertFalse(state.hasOrTransitions());
    }
}
