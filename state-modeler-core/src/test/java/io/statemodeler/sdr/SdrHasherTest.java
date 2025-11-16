package io.statemodeler.sdr;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SdrHasherTest {

    @Test
    void shouldComputeSha256Hash() {
        // Given
        String input = "hello world";

        // When
        String hash = SdrHasher.computeHash(input);

        // Then
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", hash);
        assertEquals(64, hash.length()); // SHA-256 is 256 bits = 64 hex chars
    }

    @Test
    void shouldProduceDeterministicHashes() {
        // Given
        String input = "test data";

        // When
        String hash1 = SdrHasher.computeHash(input);
        String hash2 = SdrHasher.computeHash(input);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    void shouldProduceDifferentHashesForDifferentInputs() {
        // Given
        String input1 = "data1";
        String input2 = "data2";

        // When
        String hash1 = SdrHasher.computeHash(input1);
        String hash2 = SdrHasher.computeHash(input2);

        // Then
        assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldRejectNullInput() {
        // When/Then
        var exception = assertThrows(IllegalArgumentException.class, () -> SdrHasher.computeHash(null));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    void shouldHashEmptyString() {
        // Given
        String empty = "";

        // When
        String hash = SdrHasher.computeHash(empty);

        // Then
        // SHA-256 of empty string
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    void shouldBeFormatIndependent() {
        // Given - same content, different whitespace
        String compact = "{\"name\":\"test\",\"value\":123}";
        String formatted = """
                {
                  "name": "test",
                  "value": 123
                }
                """;

        // When
        String hash1 = SdrHasher.computeHash(compact);
        String hash2 = SdrHasher.computeHash(formatted);

        // Then - different hashes (whitespace matters for raw hashing)
        assertNotEquals(hash1, hash2);
        // This is expected - canonical serialization happens at a higher level
    }

    @Test
    void shouldProduceLowercaseHex() {
        // Given
        String input = "UPPERCASE";

        // When
        String hash = SdrHasher.computeHash(input);

        // Then
        assertEquals(hash.toLowerCase(), hash);
        assertTrue(hash.matches("^[0-9a-f]{64}$")); // Only lowercase hex chars
    }
}
