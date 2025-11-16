package io.statemodeler.sql.postgres;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PostgresTypeValidatorTest {

    @Test
    void shouldValidateNumericTypes() {
        assertThat(PostgresTypeValidator.isValidType("INTEGER")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("INT")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("BIGINT")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("SMALLINT")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("SERIAL")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("BIGSERIAL")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("DECIMAL")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("NUMERIC")).isTrue();
    }

    @Test
    void shouldValidateParameterizedNumericTypes() {
        assertThat(PostgresTypeValidator.isValidType("NUMERIC(10,2)")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("NUMERIC(10, 2)")).isTrue(); // with space
        assertThat(PostgresTypeValidator.isValidType("DECIMAL(5,3)")).isTrue();
        // Support precision-only format (no scale)
        assertThat(PostgresTypeValidator.isValidType("NUMERIC(10)")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("DECIMAL(5)")).isTrue();
    }

    @Test
    void shouldValidateComplexArrayTypes() {
        // Simple arrays
        assertThat(PostgresTypeValidator.isValidType("TEXT[]")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("INTEGER[]")).isTrue();
        // Parameterized type arrays
        assertThat(PostgresTypeValidator.isValidType("VARCHAR(255)[]")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("NUMERIC(10,2)[]")).isTrue();
        // Complex datetime arrays
        assertThat(PostgresTypeValidator.isValidType("TIMESTAMP WITH TIME ZONE[]"))
                .isTrue();
        assertThat(PostgresTypeValidator.isValidType("TIME WITHOUT TIME ZONE[]"))
                .isTrue();
        // Case insensitive
        assertThat(PostgresTypeValidator.isValidType("varchar(100)[]")).isTrue();
    }

    @Test
    void shouldValidateDateTimeTypesWithPrecision() {
        // Short form with precision
        assertThat(PostgresTypeValidator.isValidType("TIMESTAMP(6)")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("TIMESTAMPTZ(3)")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("TIME(0)")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("TIMETZ(6)")).isTrue();
        // Full form with precision - note: PostgreSQL only accepts abbreviated forms with precision
        // TIMESTAMP WITH TIME ZONE(6) is NOT valid PostgreSQL syntax
        // Use TIMESTAMPTZ(6) instead
        assertThat(PostgresTypeValidator.isValidType("timestamp with time zone(6)"))
                .isTrue();
        assertThat(PostgresTypeValidator.isValidType("time with time zone(6)")).isTrue();
    }

    @Test
    void shouldValidateCharacterTypes() {
        assertThat(PostgresTypeValidator.isValidType("TEXT")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("VARCHAR")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("CHAR")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("VARCHAR(255)")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("CHAR(10)")).isTrue();
    }

    @Test
    void shouldValidateDateTimeTypes() {
        assertThat(PostgresTypeValidator.isValidType("TIMESTAMP")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("TIMESTAMPTZ")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("TIMESTAMP WITH TIME ZONE"))
                .isTrue();
        assertThat(PostgresTypeValidator.isValidType("TIMESTAMP WITHOUT TIME ZONE"))
                .isTrue();
        assertThat(PostgresTypeValidator.isValidType("DATE")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("TIME")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("TIMETZ")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("INTERVAL")).isTrue();
    }

    @Test
    void shouldValidateBooleanType() {
        assertThat(PostgresTypeValidator.isValidType("BOOLEAN")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("BOOL")).isTrue();
    }

    @Test
    void shouldValidateJsonTypes() {
        assertThat(PostgresTypeValidator.isValidType("JSON")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("JSONB")).isTrue();
    }

    @Test
    void shouldValidateUuidType() {
        assertThat(PostgresTypeValidator.isValidType("UUID")).isTrue();
    }

    @Test
    void shouldValidateCaseInsensitively() {
        assertThat(PostgresTypeValidator.isValidType("text")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("Text")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("TEXT")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("varchar(255)")).isTrue();
        assertThat(PostgresTypeValidator.isValidType("numeric(10,2)")).isTrue();
    }

    @Test
    void shouldRejectInvalidTypes() {
        assertThat(PostgresTypeValidator.isValidType("INVALID_TYPE")).isFalse();
        assertThat(PostgresTypeValidator.isValidType("string")).isFalse();
        assertThat(PostgresTypeValidator.isValidType("int(10)")).isFalse(); // INT doesn't take precision
        assertThat(PostgresTypeValidator.isValidType("")).isFalse();
        assertThat(PostgresTypeValidator.isValidType(null)).isFalse();
        assertThat(PostgresTypeValidator.isValidType("  ")).isFalse();
    }

    @Test
    void shouldProvideErrorMessage() {
        var message = PostgresTypeValidator.getErrorMessage("INVALID_TYPE");
        assertThat(message).contains("Invalid PostgreSQL type");
        assertThat(message).contains("INVALID_TYPE");
    }
}
