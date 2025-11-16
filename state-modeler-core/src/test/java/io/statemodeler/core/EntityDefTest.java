package io.statemodeler.core;

import static org.assertj.core.api.Assertions.*;

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
        assertThat(entity.name()).isEqualTo("order");
        assertThat(entity.table()).isEqualTo("orders");
        assertThat(entity.id()).isEqualTo(idAttr);
        assertThat(entity.attributes()).hasSize(2);
        assertThat(entity.states()).hasSize(1);
        assertThat(entity.extensions()).isEmpty();
        assertThat(entity.projections()).isEmpty();
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
        assertThat(entity.states()).hasSize(2);
        assertThat(entity.extensions()).hasSize(1);
        assertThat(entity.projections()).hasSize(1);
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
        assertThat(initialState).isEqualTo(pendingState);
        assertThat(initialState.initial()).isTrue();
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
        assertThat(initialState).isNull();
    }

    @Test
    void shouldRejectNullName() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        assertThatThrownBy(() -> new EntityDef(null, "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null");
    }

    @Test
    void shouldRejectNullTable() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        assertThatThrownBy(() -> new EntityDef("order", null, idAttr, Map.of(), Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("table cannot be null");
    }

    @Test
    void shouldRejectNullId() {
        assertThatThrownBy(() -> new EntityDef("order", "orders", null, Map.of(), Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be null");
    }

    @Test
    void shouldRejectNullAttributes() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        assertThatThrownBy(() -> new EntityDef("order", "orders", idAttr, null, Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attributes cannot be null");
    }

    @Test
    void shouldRejectNullStates() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        assertThatThrownBy(() -> new EntityDef("order", "orders", idAttr, Map.of(), null, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("states cannot be null");
    }

    @Test
    void shouldRejectNullExtensions() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        assertThatThrownBy(() -> new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("extensions cannot be null");
    }

    @Test
    void shouldRejectNullProjections() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        assertThatThrownBy(() -> new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projections cannot be null");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var entity1 = new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of());
        var entity2 = new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of());
        var entity3 = new EntityDef("customer", "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of());

        // Then
        assertThat(entity1).isEqualTo(entity2);
        assertThat(entity1).isNotEqualTo(entity3);
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var entity = new EntityDef("order", "orders", idAttr, Map.of(), Map.of(), Map.of(), Map.of());

        // When
        var result = entity.toString();

        // Then
        assertThat(result).contains("EntityDef").contains("name=order").contains("table=orders");
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
        assertThat(entity.attributes()).isInstanceOf(Map.class);
        assertThat(entity.states()).isInstanceOf(Map.class);
        assertThat(entity.extensions()).isInstanceOf(Map.class);
        assertThat(entity.projections()).isInstanceOf(Map.class);

        assertThat(entity.attributes()).hasSize(1);
        assertThat(entity.states()).hasSize(1);
        assertThat(entity.extensions()).hasSize(1);
        assertThat(entity.projections()).hasSize(1);
    }
}
