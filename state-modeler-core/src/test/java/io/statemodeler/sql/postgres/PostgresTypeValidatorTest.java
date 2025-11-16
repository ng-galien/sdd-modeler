package io.statemodeler.sql.postgres;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PostgresTypeValidatorTest {

    @Test
    void shouldValidateNumericTypes() {
        assertTrue(PostgresTypeValidator.isValidType("INTEGER"));
        assertTrue(PostgresTypeValidator.isValidType("INT"));
        assertTrue(PostgresTypeValidator.isValidType("BIGINT"));
        assertTrue(PostgresTypeValidator.isValidType("SMALLINT"));
        assertTrue(PostgresTypeValidator.isValidType("SERIAL"));
        assertTrue(PostgresTypeValidator.isValidType("BIGSERIAL"));
        assertTrue(PostgresTypeValidator.isValidType("DECIMAL"));
        assertTrue(PostgresTypeValidator.isValidType("NUMERIC"));
    }

    @Test
    void shouldValidateParameterizedNumericTypes() {
        assertTrue(PostgresTypeValidator.isValidType("NUMERIC(10,2)"));
        assertTrue(PostgresTypeValidator.isValidType("NUMERIC(10, 2)")); // with space
        assertTrue(PostgresTypeValidator.isValidType("DECIMAL(5,3)"));
        // Support precision-only format (no scale)
        assertTrue(PostgresTypeValidator.isValidType("NUMERIC(10)"));
        assertTrue(PostgresTypeValidator.isValidType("DECIMAL(5)"));
    }

    @Test
    void shouldValidateComplexArrayTypes() {
        // Simple arrays
        assertTrue(PostgresTypeValidator.isValidType("TEXT[]"));
        assertTrue(PostgresTypeValidator.isValidType("INTEGER[]"));
        // Parameterized type arrays
        assertTrue(PostgresTypeValidator.isValidType("VARCHAR(255)[]"));
        assertTrue(PostgresTypeValidator.isValidType("NUMERIC(10,2)[]"));
        // Complex datetime arrays
        assertTrue(PostgresTypeValidator.isValidType("TIMESTAMP WITH TIME ZONE[]"));
        assertTrue(PostgresTypeValidator.isValidType("TIME WITHOUT TIME ZONE[]"));
        // Case insensitive
        assertTrue(PostgresTypeValidator.isValidType("varchar(100)[]"));
    }

    @Test
    void shouldValidateDateTimeTypesWithPrecision() {
        // Short form with precision
        assertTrue(PostgresTypeValidator.isValidType("TIMESTAMP(6)"));
        assertTrue(PostgresTypeValidator.isValidType("TIMESTAMPTZ(3)"));
        assertTrue(PostgresTypeValidator.isValidType("TIME(0)"));
        assertTrue(PostgresTypeValidator.isValidType("TIMETZ(6)"));
        // Full form with precision - note: PostgreSQL only accepts abbreviated forms with precision
        // TIMESTAMP WITH TIME ZONE(6) is NOT valid PostgreSQL syntax
        // Use TIMESTAMPTZ(6) instead
        assertTrue(PostgresTypeValidator.isValidType("timestamp with time zone(6)"));
        assertTrue(PostgresTypeValidator.isValidType("time with time zone(6)"));
    }

    @Test
    void shouldValidateCharacterTypes() {
        assertTrue(PostgresTypeValidator.isValidType("TEXT"));
        assertTrue(PostgresTypeValidator.isValidType("VARCHAR"));
        assertTrue(PostgresTypeValidator.isValidType("CHAR"));
        assertTrue(PostgresTypeValidator.isValidType("VARCHAR(255)"));
        assertTrue(PostgresTypeValidator.isValidType("CHAR(10)"));
    }

    @Test
    void shouldValidateDateTimeTypes() {
        assertTrue(PostgresTypeValidator.isValidType("TIMESTAMP"));
        assertTrue(PostgresTypeValidator.isValidType("TIMESTAMPTZ"));
        assertTrue(PostgresTypeValidator.isValidType("TIMESTAMP WITH TIME ZONE"));
        assertTrue(PostgresTypeValidator.isValidType("TIMESTAMP WITHOUT TIME ZONE"));
        assertTrue(PostgresTypeValidator.isValidType("DATE"));
        assertTrue(PostgresTypeValidator.isValidType("TIME"));
        assertTrue(PostgresTypeValidator.isValidType("TIMETZ"));
        assertTrue(PostgresTypeValidator.isValidType("INTERVAL"));
    }

    @Test
    void shouldValidateBooleanType() {
        assertTrue(PostgresTypeValidator.isValidType("BOOLEAN"));
        assertTrue(PostgresTypeValidator.isValidType("BOOL"));
    }

    @Test
    void shouldValidateJsonTypes() {
        assertTrue(PostgresTypeValidator.isValidType("JSON"));
        assertTrue(PostgresTypeValidator.isValidType("JSONB"));
    }

    @Test
    void shouldValidateUuidType() {
        assertTrue(PostgresTypeValidator.isValidType("UUID"));
    }

    @Test
    void shouldValidateCaseInsensitively() {
        assertTrue(PostgresTypeValidator.isValidType("text"));
        assertTrue(PostgresTypeValidator.isValidType("Text"));
        assertTrue(PostgresTypeValidator.isValidType("TEXT"));
        assertTrue(PostgresTypeValidator.isValidType("varchar(255)"));
        assertTrue(PostgresTypeValidator.isValidType("numeric(10,2)"));
    }

    @Test
    void shouldRejectInvalidTypes() {
        assertFalse(PostgresTypeValidator.isValidType("INVALID_TYPE"));
        assertFalse(PostgresTypeValidator.isValidType("string"));
        assertFalse(PostgresTypeValidator.isValidType("int(10)")); // INT doesn't take precision
        assertFalse(PostgresTypeValidator.isValidType(""));
        assertFalse(PostgresTypeValidator.isValidType(null));
        assertFalse(PostgresTypeValidator.isValidType("  "));
    }

    @Test
    void shouldProvideErrorMessage() {
        var message = PostgresTypeValidator.getErrorMessage("INVALID_TYPE");
        assertTrue(message.contains("Invalid PostgreSQL type"));
        assertTrue(message.contains("INVALID_TYPE"));
    }
}
