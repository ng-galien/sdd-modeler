package io.statemodeler.core;

import static org.assertj.core.api.Assertions.*;

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
        assertThat(extension.name()).isEqualTo("paid_extensions");
        assertThat(extension.table()).isEqualTo("order_paid_extensions");
        assertThat(extension.targetState()).isEqualTo("paid");
        assertThat(extension.attributes()).hasSize(2);
        assertThat(extension.attributes()).containsKey("additional_notes");
        assertThat(extension.attributes()).containsKey("updated_at");
    }

    @Test
    void shouldCreateExtensionWithEmptyAttributes() {
        // Given & When
        var extension = new ExtensionDef("minimal_ext", "minimal_table", "target", Map.of());

        // Then
        assertThat(extension.attributes()).isEmpty();
        assertThat(extension.name()).isEqualTo("minimal_ext");
        assertThat(extension.table()).isEqualTo("minimal_table");
        assertThat(extension.targetState()).isEqualTo("target");
    }

    @Test
    void shouldRejectNullName() {
        assertThatThrownBy(() -> new ExtensionDef(null, "table", "target", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null");
    }

    @Test
    void shouldRejectNullTable() {
        assertThatThrownBy(() -> new ExtensionDef("extension", null, "target", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("table cannot be null");
    }

    @Test
    void shouldRejectNullTargetState() {
        assertThatThrownBy(() -> new ExtensionDef("extension", "table", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetState cannot be null");
    }

    @Test
    void shouldRejectNullAttributes() {
        assertThatThrownBy(() -> new ExtensionDef("extension", "table", "target", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attributes cannot be null");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var attributes = Map.of("notes", new AttributeDef("notes", "text", true, false, null, null));
        var ext1 = new ExtensionDef("paid_ext", "order_paid_ext", "paid", attributes);
        var ext2 = new ExtensionDef("paid_ext", "order_paid_ext", "paid", attributes);
        var ext3 = new ExtensionDef("cancelled_ext", "order_paid_ext", "paid", attributes);

        // Then
        assertThat(ext1).isEqualTo(ext2);
        assertThat(ext1).isNotEqualTo(ext3);
        assertThat(ext1.hashCode()).isEqualTo(ext2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var extension = new ExtensionDef("paid_ext", "order_paid_ext", "paid", Map.of());

        // When
        var result = extension.toString();

        // Then
        assertThat(result)
                .contains("ExtensionDef")
                .contains("name=paid_ext")
                .contains("table=order_paid_ext")
                .contains("targetState=paid");
    }

    @Test
    void shouldCreateImmutableAttributesMap() {
        // Given
        var mutableAttributes = Map.of("attr", new AttributeDef("attr", "text", false, false, null, null));

        // When
        var extension = new ExtensionDef("ext", "table", "target", mutableAttributes);

        // Then - should be immutable copy
        assertThat(extension.attributes()).isInstanceOf(Map.class);
        assertThat(extension.attributes()).containsKey("attr");
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
        assertThat(auditExt.attributes()).hasSize(3);
        assertThat(notesExt.attributes()).hasSize(2);
        assertThat(auditExt.targetState()).isEqualTo("paid");
        assertThat(notesExt.targetState()).isEqualTo("completed");
    }
}
