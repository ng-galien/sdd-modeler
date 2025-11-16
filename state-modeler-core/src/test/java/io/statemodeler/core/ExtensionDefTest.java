package io.statemodeler.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ExtensionDefTest {

    @Test
    void shouldCreateValidExtensionDef() {
        // Given
        var attributes = Map.of(
                "additional_notes", new AttributeDef("additional_notes", "text", true, false, null, null),
                "updated_at", new AttributeDef("updated_at", "timestamptz", false, false, "NOW()", null));

        // When
        var extension = new ExtensionDef("paid_extensions", "order_paid_extensions", "paid", attributes);

        // Then
        assertEquals("paid_extensions", extension.name());
        assertEquals("order_paid_extensions", extension.table());
        assertEquals("paid", extension.targetState());
        assertEquals(2, extension.attributes().size());
        assertTrue(extension.attributes().containsKey("additional_notes"));
        assertTrue(extension.attributes().containsKey("updated_at"));
    }

    @Test
    void shouldCreateExtensionWithEmptyAttributes() {
        // Given & When
        var extension = new ExtensionDef("minimal_ext", "minimal_table", "target", Map.of());

        // Then
        assertTrue(extension.attributes().isEmpty());
        assertEquals("minimal_ext", extension.name());
        assertEquals("minimal_table", extension.table());
        assertEquals("target", extension.targetState());
    }

    @Test
    void shouldRejectNullName() {
        var ex =
                assertThrows(IllegalArgumentException.class, () -> new ExtensionDef(null, "table", "target", Map.of()));
        assertTrue(ex.getMessage().contains("name cannot be null"));
    }

    @Test
    void shouldRejectNullTable() {
        var ex = assertThrows(
                IllegalArgumentException.class, () -> new ExtensionDef("extension", null, "target", Map.of()));
        assertTrue(ex.getMessage().contains("table cannot be null"));
    }

    @Test
    void shouldRejectNullTargetState() {
        var ex = assertThrows(
                IllegalArgumentException.class, () -> new ExtensionDef("extension", "table", null, Map.of()));
        assertTrue(ex.getMessage().contains("targetState cannot be null"));
    }

    @Test
    void shouldRejectNullAttributes() {
        var ex = assertThrows(
                IllegalArgumentException.class, () -> new ExtensionDef("extension", "table", "target", null));
        assertTrue(ex.getMessage().contains("attributes cannot be null"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var attributes = Map.of("notes", new AttributeDef("notes", "text", true, false, null, null));
        var ext1 = new ExtensionDef("paid_ext", "order_paid_ext", "paid", attributes);
        var ext2 = new ExtensionDef("paid_ext", "order_paid_ext", "paid", attributes);
        var ext3 = new ExtensionDef("cancelled_ext", "order_paid_ext", "paid", attributes);

        // Then
        assertEquals(ext2, ext1);
        assertNotEquals(ext3, ext1);
        assertEquals(ext2.hashCode(), ext1.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var extension = new ExtensionDef("paid_ext", "order_paid_ext", "paid", Map.of());

        // When
        var result = extension.toString();

        // Then
        assertTrue(result.contains("ExtensionDef"));
        assertTrue(result.contains("name=paid_ext"));
        assertTrue(result.contains("table=order_paid_ext"));
        assertTrue(result.contains("targetState=paid"));
    }

    @Test
    void shouldCreateImmutableAttributesMap() {
        // Given
        var mutableAttributes = Map.of("attr", new AttributeDef("attr", "text", false, false, null, null));

        // When
        var extension = new ExtensionDef("ext", "table", "target", mutableAttributes);

        // Then - should be immutable copy
        assertInstanceOf(Map.class, extension.attributes());
        assertTrue(extension.attributes().containsKey("attr"));
    }

    @Test
    void shouldHandleCommonExtensionPatterns() {
        // Test typical extension patterns

        // Audit extension
        var auditAttributes = Map.of(
                "created_by", new AttributeDef("created_by", "varchar(255)", true, false, null, "User who created"),
                "updated_by", new AttributeDef("updated_by", "varchar(255)", true, false, null, "User who updated"),
                "updated_at", new AttributeDef("updated_at", "timestamptz", false, false, "NOW()", "Last update time"));
        var auditExt = new ExtensionDef("audit", "order_paid_audit", "paid", auditAttributes);

        // Notes extension
        var notesAttributes = Map.of(
                "admin_notes", new AttributeDef("admin_notes", "text", true, false, null, "Admin comments"),
                "customer_notes", new AttributeDef("customer_notes", "text", true, false, null, "Customer feedback"));
        var notesExt = new ExtensionDef("notes", "order_notes", "completed", notesAttributes);

        // Verification
        assertEquals(3, auditExt.attributes().size());
        assertEquals(2, notesExt.attributes().size());
        assertEquals("paid", auditExt.targetState());
        assertEquals("completed", notesExt.targetState());
    }
}
