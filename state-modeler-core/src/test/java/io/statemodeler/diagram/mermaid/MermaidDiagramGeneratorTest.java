package io.statemodeler.diagram.mermaid;

import static org.junit.jupiter.api.Assertions.*;

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

        var model = new SddModel(
                "0.1",
                "test-model",
                new DatabaseConfig("postgres", null, null, java.util.Map.of()),
                Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertFalse(diagram.isEmpty());
        assertTrue(diagram.contains("stateDiagram-v2"));
        assertTrue(diagram.contains("[*] --> pending"));
        assertTrue(diagram.contains("pending --> paid"));
        assertTrue(diagram.contains("title: order State Diagram"));
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

        var model = new SddModel(
                "0.1",
                "test-model",
                new DatabaseConfig("postgres", null, null, java.util.Map.of()),
                Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertTrue(diagram.contains("pending --> cancelled : OR"));
        assertTrue(diagram.contains("paid --> cancelled : OR"));
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
                new DatabaseConfig("postgres", null, null, java.util.Map.of()),
                Map.of("entity1", entity1, "entity2", entity2));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model, "entity1");

        // Then
        assertTrue(diagram.contains("title: entity1 State Diagram"));
        assertFalse(diagram.contains("entity2"));
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
                "0.1",
                "test-model",
                new DatabaseConfig("postgres", null, null, java.util.Map.of()),
                Map.of("entity1", entity));

        var generator = new MermaidDiagramGenerator();

        // When/Then
        var exception =
                assertThrows(IllegalArgumentException.class, () -> generator.generateDiagram(model, "nonexistent"));
        assertTrue(exception.getMessage().contains("Entity not found"));
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

        var model = new SddModel(
                "0.1",
                "test-model",
                new DatabaseConfig("postgres", null, null, java.util.Map.of()),
                Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertTrue(diagram.contains("pending :"));
        assertTrue(diagram.contains("reason") || diagram.contains("timestamp") || diagram.contains("user_id"));
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

        var model = new SddModel(
                "0.1",
                "test-model",
                new DatabaseConfig("postgres", null, null, java.util.Map.of()),
                Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertTrue(diagram.contains("note right of paid"));
        assertTrue(diagram.contains("Extensions:"));
        assertTrue(diagram.contains("paid_ext"));
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

        var model = new SddModel(
                "0.1",
                "test-model",
                new DatabaseConfig("postgres", null, null, java.util.Map.of()),
                Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertTrue(diagram.contains("note right of pending"));
        assertTrue(diagram.contains("note right of paid"));
        assertTrue(diagram.contains("pending_ext"));
        assertTrue(diagram.contains("paid_ext1"));
        assertTrue(diagram.contains("paid_ext2"));
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

        var model = new SddModel(
                "0.1",
                "test-model",
                new DatabaseConfig("postgres", null, null, java.util.Map.of()),
                Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertFalse(diagram.contains("note right of"));
        assertFalse(diagram.contains("Extensions:"));
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

        var model = new SddModel(
                "0.1",
                "test-model",
                new DatabaseConfig("postgres", null, null, java.util.Map.of()),
                Map.of("order", entity));

        var generator = new MermaidDiagramGenerator();

        // When
        var diagram = generator.generateDiagram(model);

        // Then
        assertTrue(diagram.contains("stateDiagram-v2"));
        assertTrue(diagram.contains("[*] --> pending"));
        assertFalse(diagram.contains("pending :"));
    }

    @Test
    void shouldReturnMermaidFormat() {
        // Given
        var generator = new MermaidDiagramGenerator();

        // When/Then
        assertEquals("mermaid", generator.getFormat());
        assertTrue(generator.supports("mermaid"));
        assertTrue(generator.supports("MERMAID"));
        assertFalse(generator.supports("plantuml"));
    }
}
