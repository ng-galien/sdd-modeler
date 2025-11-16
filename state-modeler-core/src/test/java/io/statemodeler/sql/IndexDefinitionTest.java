package io.statemodeler.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class IndexDefinitionTest {

    @Test
    void shouldCreateValidIndexDefinition() {
        // Given & When
        var index = new IndexDefinition("idx_order_paid_order_id", "order_paid", "public", List.of("order_id"), false);

        // Then
        assertEquals("idx_order_paid_order_id", index.name());
        assertEquals("order_paid", index.table());
        assertEquals("public", index.schema());
        assertEquals(List.of("order_id"), index.columns());
        assertFalse(index.unique());
    }

    @Test
    void shouldCreateUniqueIndex() {
        // Given & When
        var index = new IndexDefinition("idx_unique_email", "users", "public", List.of("email"), true);

        // Then
        assertTrue(index.unique());
    }

    @Test
    void shouldCreateIndexWithNullSchema() {
        // Given & When
        var index = new IndexDefinition("idx_name", "table", null, List.of("col"), false);

        // Then
        assertNull(index.schema());
        assertEquals("table", index.fullTableName());
    }

    @Test
    void shouldCreateIndexWithEmptySchema() {
        // Given & When
        var index = new IndexDefinition("idx_name", "table", "", List.of("col"), false);

        // Then
        assertEquals("", index.schema());
        assertEquals("table", index.fullTableName());
    }

    @Test
    void shouldCreateSchemaQualifiedTableName() {
        // Given & When
        var index = new IndexDefinition("idx_name", "table", "myschema", List.of("col"), false);

        // Then
        assertEquals("myschema.table", index.fullTableName());
    }

    @Test
    void shouldCreateMultiColumnIndex() {
        // Given & When
        var index = new IndexDefinition("idx_composite", "table", "public", List.of("col1", "col2", "col3"), false);

        // Then
        assertEquals(List.of("col1", "col2", "col3"), index.columns());
    }

    @Test
    void shouldCreateImmutableColumns() {
        // Given
        var columns = List.of("col1", "col2");
        var index = new IndexDefinition("idx_name", "table", "public", columns, false);

        // When & Then - verify columns are immutable
        assertThrows(UnsupportedOperationException.class, () -> index.columns().add("col3"));
    }

    @Test
    void shouldRejectNullName() {
        // When & Then
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new IndexDefinition(null, "table", "public", List.of("col"), false));
        assertTrue(ex.getMessage().contains("name cannot be null"));
    }

    @Test
    void shouldRejectNullTable() {
        // When & Then
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new IndexDefinition("idx_name", null, "public", List.of("col"), false));
        assertTrue(ex.getMessage().contains("table cannot be null"));
    }

    @Test
    void shouldRejectNullColumns() {
        // When & Then
        var ex = assertThrows(
                NullPointerException.class, () -> new IndexDefinition("idx_name", "table", "public", null, false));
        assertTrue(ex.getMessage().contains("columns cannot be null"));
    }

    @Test
    void shouldRejectEmptyColumns() {
        // When & Then
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new IndexDefinition("idx_name", "table", "public", List.of(), false));
        assertTrue(ex.getMessage().contains("columns cannot be empty"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var index1 = new IndexDefinition("idx_name", "table", "public", List.of("col"), false);
        var index2 = new IndexDefinition("idx_name", "table", "public", List.of("col"), false);
        var index3 = new IndexDefinition("idx_other", "table", "public", List.of("col"), false);

        // Then
        assertEquals(index2, index1);
        assertEquals(index2.hashCode(), index1.hashCode());
        assertNotEquals(index3, index1);
    }

    @Test
    void shouldImplementToString() {
        // Given
        var index = new IndexDefinition("idx_name", "table", "public", List.of("col1", "col2"), true);

        // When
        var toString = index.toString();

        // Then
        assertTrue(toString.contains("idx_name"));
        assertTrue(toString.contains("table"));
        assertTrue(toString.contains("public"));
        assertTrue(toString.contains("col1"));
        assertTrue(toString.contains("col2"));
        assertTrue(toString.contains("true"));
    }
}
