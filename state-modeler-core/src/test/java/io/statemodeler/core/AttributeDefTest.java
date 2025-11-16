package io.statemodeler.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AttributeDefTest {

    @Test
    void shouldCreateValidAttributeDef() {
        // Given & When
        var attr = new AttributeDef("id", "serial", false, true, "DEFAULT 1", "Primary key");

        // Then
        assertEquals("id", attr.name());
        assertEquals("serial", attr.type());
        assertFalse(attr.nullable());
        assertTrue(attr.primaryKey());
        assertEquals("DEFAULT 1", attr.defaultValue());
        assertEquals("Primary key", attr.description());
    }

    @Test
    void shouldCreateAttributeDefWithNullOptionals() {
        // Given & When
        var attr = new AttributeDef("customer_id", "int", true, false, null, null);

        // Then
        assertEquals("customer_id", attr.name());
        assertEquals("int", attr.type());
        assertTrue(attr.nullable());
        assertFalse(attr.primaryKey());
        assertNull(attr.defaultValue());
        assertNull(attr.description());
    }

    @Test
    void shouldRejectNullName() {
        var ex = assertThrows(
                IllegalArgumentException.class, () -> new AttributeDef(null, "int", false, false, null, null));
        assertTrue(ex.getMessage().contains("name cannot be null"));
    }

    @Test
    void shouldRejectNullType() {
        var ex = assertThrows(
                IllegalArgumentException.class, () -> new AttributeDef("id", null, false, false, null, null));
        assertTrue(ex.getMessage().contains("type cannot be null"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var attr1 = new AttributeDef("id", "serial", false, true, "DEFAULT 1", "Primary key");
        var attr2 = new AttributeDef("id", "serial", false, true, "DEFAULT 1", "Primary key");
        var attr3 = new AttributeDef("other_id", "serial", false, true, "DEFAULT 1", "Primary key");

        // Then
        assertEquals(attr2, attr1);
        assertNotEquals(attr3, attr1);
        assertEquals(attr2.hashCode(), attr1.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var attr = new AttributeDef("id", "serial", false, true, "DEFAULT 1", "Primary key");

        // When
        var result = attr.toString();

        // Then
        assertTrue(result.contains("AttributeDef"));
        assertTrue(result.contains("name=id"));
        assertTrue(result.contains("type=serial"));
        assertTrue(result.contains("nullable=false"));
        assertTrue(result.contains("primaryKey=true"));
    }

    @Test
    void shouldHandleEdgeCases() {
        // Empty string values (allowed)
        var attrEmptyDefaults = new AttributeDef("test", "varchar", true, false, "", "");

        assertTrue(attrEmptyDefaults.defaultValue().isEmpty());
        assertTrue(attrEmptyDefaults.description().isEmpty());

        // Different boolean combinations
        var attrNullablePrimary = new AttributeDef("weird", "int", true, true, null, null);
        assertTrue(attrNullablePrimary.nullable());
        assertTrue(attrNullablePrimary.primaryKey());
    }

    @Test
    void shouldCreateCommonDatabaseTypes() {
        // Test various common database types
        var stringAttr = new AttributeDef("name", "varchar(255)", true, false, null, "User name");
        var intAttr = new AttributeDef("count", "integer", false, false, "0", null);
        var decimalAttr = new AttributeDef("price", "decimal(10,2)", false, false, "0.00", "Price in euros");
        var timestampAttr = new AttributeDef("created_at", "timestamptz", false, false, "NOW()", null);

        assertEquals("varchar(255)", stringAttr.type());
        assertEquals("integer", intAttr.type());
        assertEquals("decimal(10,2)", decimalAttr.type());
        assertEquals("timestamptz", timestampAttr.type());
    }
}
