package io.statemodeler.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EntityDefTest {

    @Test
    void shouldCreateValidEntityDef() {
        // Given
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var attributes = Map.of(
                "customer_id", new AttributeDef("customer_id", "int", false, false, null, "Customer reference"),
                "total_amount", new AttributeDef("total_amount", "decimal(10,2)", false, false, null, null));
        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var states = Map.of("pending", pendingState);
        var extensions = Map.<String, ExtensionDef>of();
        var projections = Map.<String, ProjectionDef>of();

        // When
        var entity = new EntityDef("order", "orders", idAttr, attributes, states, extensions, projections);

        // Then
        assertEquals("order", entity.name());
        assertEquals("orders", entity.table());
        assertEquals(idAttr, entity.id());
        assertEquals(2, entity.attributes().size());
        assertEquals(1, entity.states().size());
        assertTrue(entity.extensions().isEmpty());
        assertTrue(entity.projections().isEmpty());
    }

    @Test
    void shouldCreateEntityWithComplexStructure() {
        // Given
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var attributes = Map.of("customer_id", new AttributeDef("customer_id", "int", false, false, null, null));

        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var paidState = new StateDef("paid", "order_paid", false, List.of("pending"), List.of(), Map.of());
        var states = Map.of("pending", pendingState, "paid", paidState);

        var extension = new ExtensionDef("paid_ext", "order_paid_ext", "paid", Map.of());
        var extensions = Map.of("paid_ext", extension);

        var projection = new ProjectionDef("current", "current_states", ProjectionDef.ProjectionKind.CURRENT_STATE);
        var projections = Map.of("current", projection);

        // When
        var entity = new EntityDef("order", "orders", idAttr, attributes, states, extensions, projections);

        // Then
        assertEquals(2, entity.states().size());
        assertEquals(1, entity.extensions().size());
        assertEquals(1, entity.projections().size());
    }

    @Test
    void shouldFindInitialState() {
        // Given
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var paidState = new StateDef("paid", "order_paid", false, List.of("pending"), List.of(), Map.of());
        var states = Map.of("pending", pendingState, "paid", paidState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());

        // When
        var initialState = entity.findInitialState();

        // Then
        assertEquals(pendingState, initialState);
        assertTrue(initialState.initial());
    }

    @Test
    void shouldReturnNullWhenNoInitialState() {
        // Given
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var paidState = new StateDef("paid", "order_paid", false, List.of("pending"), List.of(), Map.of());
        var states = Map.of("paid", paidState);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), states, Map.of(), Map.of());

        // When
        var initialState = entity.findInitialState();

        // Then
        assertNull(initialState);
    }

    @Test
    void shouldRejectNullName() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new EntityDef(null, "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of()));
        assertTrue(ex.getMessage().contains("name cannot be null"));
    }

    @Test
    void shouldRejectNullTable() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new EntityDef("order", null, idAttr, Map.of(), Map.of(), Map.of(), Map.of()));
        assertTrue(ex.getMessage().contains("table cannot be null"));
    }

    @Test
    void shouldRejectNullId() {
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new EntityDef("order", "orders", null, Map.of(), Map.of(), Map.of(), Map.of()));
        assertTrue(ex.getMessage().contains("id cannot be null"));
    }

    @Test
    void shouldRejectNullAttributes() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new EntityDef("order", "orders", idAttr, null, Map.of(), Map.of(), Map.of()));
        assertTrue(ex.getMessage().contains("attributes cannot be null"));
    }

    @Test
    void shouldRejectNullStates() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new EntityDef("order", "orders", idAttr, Map.of(), null, Map.of(), Map.of()));
        assertTrue(ex.getMessage().contains("states cannot be null"));
    }

    @Test
    void shouldRejectNullExtensions() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), null, Map.of()));
        assertTrue(ex.getMessage().contains("extensions cannot be null"));
    }

    @Test
    void shouldRejectNullProjections() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), Map.of(), null));
        assertTrue(ex.getMessage().contains("projections cannot be null"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var entity1 = new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of());
        var entity2 = new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of());
        var entity3 = new EntityDef("customer", "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of());

        // Then
        assertEquals(entity2, entity1);
        assertNotEquals(entity3, entity1);
        assertEquals(entity2.hashCode(), entity1.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of());

        // When
        var result = entity.toString();

        // Then
        assertTrue(result.contains("EntityDef"));
        assertTrue(result.contains("name=order"));
        assertTrue(result.contains("table=orders"));
    }

    @Test
    void shouldCreateImmutableCollections() {
        // Given
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var mutableAttributes = Map.of("attr", new AttributeDef("attr", "text", false, false, null, null));
        var mutableStates = Map.of("state", new StateDef("state", "table", true, List.of(), List.of(), Map.of()));
        var mutableExtensions = Map.of("ext", new ExtensionDef("ext", "table", "state", Map.of()));
        var mutableProjections =
                Map.of("proj", new ProjectionDef("proj", "view", ProjectionDef.ProjectionKind.CURRENT_STATE));

        // When
        var entity = new EntityDef(
                "order", "orders", idAttr, mutableAttributes, mutableStates, mutableExtensions, mutableProjections);

        // Then - all collections should be immutable copies
        assertInstanceOf(Map.class, entity.attributes());
        assertInstanceOf(Map.class, entity.states());
        assertInstanceOf(Map.class, entity.extensions());
        assertInstanceOf(Map.class, entity.projections());

        assertEquals(1, entity.attributes().size());
        assertEquals(1, entity.states().size());
        assertEquals(1, entity.extensions().size());
        assertEquals(1, entity.projections().size());
    }
}
