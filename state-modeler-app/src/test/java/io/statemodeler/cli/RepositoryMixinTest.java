package io.statemodeler.cli;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RepositoryMixin}.
 */
class RepositoryMixinTest {

    @Test
    void shouldCreateRepositoryWithDefaultPath() throws Exception {
        // Given
        var mixin = new RepositoryMixin();

        // When
        var repository = mixin.createRepository();

        // Then
        assertNotNull(repository);
        repository.close();
    }

    @Test
    void shouldCreateRepositoryWithCustomPath() throws Exception {
        // Given
        var mixin = new RepositoryMixin();
        mixin.repositoryPath = "/tmp/test-custom-repo";

        // When
        var repository = mixin.createRepository();

        // Then
        assertNotNull(repository);
        repository.close();
    }

    @Test
    void shouldGetRepositoryPath() {
        // Given
        var mixin = new RepositoryMixin();
        mixin.repositoryPath = "/custom/path";

        // When
        String result = mixin.getRepositoryPath();

        // Then
        assertEquals("/custom/path", result);
    }

    @Test
    void shouldReturnNullWhenNoPathConfigured() {
        // Given
        var mixin = new RepositoryMixin();

        // When
        String result = mixin.getRepositoryPath();

        // Then
        assertNull(result);
    }
}
