package io.statemodeler.diagram.mermaid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.statemodeler.core.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MermaidDiagramGenerator}.
 */
class MermaidDiagramGeneratorTest {

    @Test
    void shouldGenerateDiagramForSimpleEntity() {
        // Given
        var pending = new StateDef(
                "pending",
                "order_pending",
                true,
                List.of(),
                List.of(),
                Map.of("pending_reason", new AttributeDef("pending_reason", "text", false, false, null, null)));

        var paid = new StateDef(
                "paid",
                "order_paid",
                false,
                List.of("pending"),
                List.of(),
                Map.of("payment_method", new AttributeDef("payment_method", "text", false, false, null, null)));

        var entity = new EntityDef(
                "order",
                "orders",
                new AttributeDef("id", "serial", false, true, null, null),
                Map.of(),
                Map.of("pending", pending, "paid", paid),
                Map.of(),
                Map.of());

        var model =
                new SddModel("0.1", "test-model", new DatabaseConfig("postgres", null, null), Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertThat(diagram).isNotEmpty();
        assertThat(diagram).contains("stateDiagram-v2");
        assertThat(diagram).contains("[*] --> pending");
        assertThat(diagram).contains("pending --> paid");
        assertThat(diagram).contains("title: order State Diagram");
    }

    @Test
    void shouldGenerateDiagramWithOrTransitions() {
        // Given
        var pending = new StateDef(
                "pending",
                "order_pending",
                true,
                List.of(),
                List.of(),
                Map.of("reason", new AttributeDef("reason", "text", false, false, null, null)));

        var paid = new StateDef(
                "paid",
                "order_paid",
                false,
                List.of("pending"),
                List.of(),
                Map.of("amount", new AttributeDef("amount", "numeric", false, false, null, null)));

        var cancelled = new StateDef(
                "cancelled",
                "order_cancelled",
                false,
                List.of(),
                List.of("pending", "paid"),
                Map.of("cancel_reason", new AttributeDef("cancel_reason", "text", false, false, null, null)));

        var entity = new EntityDef(
                "order",
                "orders",
                new AttributeDef("id", "serial", false, true, null, null),
                Map.of(),
                Map.of("pending", pending, "paid", paid, "cancelled", cancelled),
                Map.of(),
                Map.of());

        var model =
                new SddModel("0.1", "test-model", new DatabaseConfig("postgres", null, null), Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertThat(diagram).contains("pending --> cancelled : OR");
        assertThat(diagram).contains("paid --> cancelled : OR");
    }

    @Test
    void shouldGenerateDiagramForSpecificEntity() {
        // Given
        var state = new StateDef("active", "entity1_active", true, List.of(), List.of(), Map.of());
        var entity1 = new EntityDef(
                "entity1",
                "entity1_table",
                new AttributeDef("id", "serial", false, true, null, null),
                Map.of(),
                Map.of("active", state),
                Map.of(),
                Map.of());

        var entity2 = new EntityDef(
                "entity2",
                "entity2_table",
                new AttributeDef("id", "serial", false, true, null, null),
                Map.of(),
                Map.of("active", state),
                Map.of(),
                Map.of());

        var model = new SddModel(
                "0.1",
                "test-model",
                new DatabaseConfig("postgres", null, null),
                Map.of("entity1", entity1, "entity2", entity2));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model, "entity1");

        // Then
        assertThat(diagram).contains("title: entity1 State Diagram");
        assertThat(diagram).doesNotContain("entity2");
    }

    @Test
    void shouldRejectNonExistentEntity() {
        // Given
        var state = new StateDef("active", "entity1_active", true, List.of(), List.of(), Map.of());
        var entity = new EntityDef(
                "entity1",
                "entity1_table",
                new AttributeDef("id", "serial", false, true, null, null),
                Map.of(),
                Map.of("active", state),
                Map.of(),
                Map.of());

        var model = new SddModel(
                "0.1", "test-model", new DatabaseConfig("postgres", null, null), Map.of("entity1", entity));

        var generator = new MermaidDiagramGenerator();

        // When/Then
        assertThatThrownBy(() -> generator.generateDiagram(model, "nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Entity not found");
    }

    @Test
    void shouldIncludeStateAttributes() {
        // Given
        var state = new StateDef(
                "pending",
                "order_pending",
                true,
                List.of(),
                List.of(),
                Map.of(
                        "reason", new AttributeDef("reason", "text", false, false, null, null),
                        "timestamp", new AttributeDef("timestamp", "timestamptz", false, false, null, null),
                        "user_id", new AttributeDef("user_id", "int", false, false, null, null)));

        var entity = new EntityDef(
                "order",
                "orders",
                new AttributeDef("id", "serial", false, true, null, null),
                Map.of(),
                Map.of("pending", state),
                Map.of(),
                Map.of());

        var model =
                new SddModel("0.1", "test-model", new DatabaseConfig("postgres", null, null), Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertThat(diagram).contains("pending :");
        assertThat(diagram).containsAnyOf("reason", "timestamp", "user_id");
    }

    @Test
    void shouldIncludeExtensionsNote() {
        // Given
        var state = new StateDef("paid", "order_paid", true, List.of(), List.of(), Map.of());

        var extension = new ExtensionDef(
                "paid_ext",
                "order_paid_ext",
                "paid",
                Map.of("notes", new AttributeDef("notes", "text", true, false, null, null)));

        var entity = new EntityDef(
                "order",
                "orders",
                new AttributeDef("id", "serial", false, true, null, null),
                Map.of(),
                Map.of("paid", state),
                Map.of("paid_ext", extension),
                Map.of());

        var model =
                new SddModel("0.1", "test-model", new DatabaseConfig("postgres", null, null), Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertThat(diagram).contains("note right of paid");
        assertThat(diagram).contains("Extensions:");
        assertThat(diagram).contains("paid_ext");
    }

    @Test
    void shouldGroupExtensionsByTargetState() {
        // Given
        var pending = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());
        var paid = new StateDef("paid", "order_paid", false, List.of("pending"), List.of(), Map.of());

        var pendingExt = new ExtensionDef(
                "pending_ext",
                "order_pending_ext",
                "pending",
                Map.of("notes", new AttributeDef("notes", "text", true, false, null, null)));

        var paidExt1 = new ExtensionDef(
                "paid_ext1",
                "order_paid_ext1",
                "paid",
                Map.of("notes", new AttributeDef("notes", "text", true, false, null, null)));

        var paidExt2 = new ExtensionDef(
                "paid_ext2",
                "order_paid_ext2",
                "paid",
                Map.of("metadata", new AttributeDef("metadata", "jsonb", true, false, null, null)));

        var entity = new EntityDef(
                "order",
                "orders",
                new AttributeDef("id", "serial", false, true, null, null),
                Map.of(),
                Map.of("pending", pending, "paid", paid),
                Map.of("pending_ext", pendingExt, "paid_ext1", paidExt1, "paid_ext2", paidExt2),
                Map.of());

        var model =
                new SddModel("0.1", "test-model", new DatabaseConfig("postgres", null, null), Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertThat(diagram).contains("note right of pending");
        assertThat(diagram).contains("note right of paid");
        assertThat(diagram).contains("pending_ext");
        assertThat(diagram).contains("paid_ext1");
        assertThat(diagram).contains("paid_ext2");
    }

    @Test
    void shouldHandleEmptyExtensions() {
        // Given
        var state = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());

        var entity = new EntityDef(
                "order",
                "orders",
                new AttributeDef("id", "serial", false, true, null, null),
                Map.of(),
                Map.of("pending", state),
                Map.of(),
                Map.of());

        var model =
                new SddModel("0.1", "test-model", new DatabaseConfig("postgres", null, null), Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertThat(diagram).doesNotContain("note right of");
        assertThat(diagram).doesNotContain("Extensions:");
    }

    @Test
    void shouldHandleStateWithNoAttributes() {
        // Given
        var state = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());

        var entity = new EntityDef(
                "order",
                "orders",
                new AttributeDef("id", "serial", false, true, null, null),
                Map.of(),
                Map.of("pending", state),
                Map.of(),
                Map.of());

        var model =
                new SddModel("0.1", "test-model", new DatabaseConfig("postgres", null, null), Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertThat(diagram).contains("stateDiagram-v2");
        assertThat(diagram).contains("[*] --> pending");
        assertThat(diagram).doesNotContain("pending :");
    }

    @Test
    void shouldReturnMermaidFormat() {
        // Given
        var generator = new MermaidDiagramGenerator();

        // When/Then
        assertThat(generator.getFormat()).isEqualTo("mermaid");
        assertThat(generator.supports("mermaid")).isTrue();
        assertThat(generator.supports("MERMAID")).isTrue();
        assertThat(generator.supports("plantuml")).isFalse();
    }
}
