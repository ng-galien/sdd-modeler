package io.statemodeler.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RepositoryConfig}.
 */
class RepositoryConfigTest {

    @AfterEach
    void cleanup() {
        // Clear any environment variable side effects (can't actually change env vars in tests)
        System.clearProperty("test.repository.path");
    }

    @Test
    void shouldUseDefaultPathWhenNoOptionsProvided() {
        // When
        Path result = RepositoryConfig.resolveRepositoryPath(null);

        // Then
        Path expected = Path.of(System.getProperty("user.home"), ".sdd-modeler", "repository");
        assertEquals(expected, result);
    }

    @Test
    void shouldUseDefaultPathWhenBlankOptionProvided() {
        // When
        Path result = RepositoryConfig.resolveRepositoryPath("  ");

        // Then
        Path expected = Path.of(System.getProperty("user.home"), ".sdd-modeler", "repository");
        assertEquals(expected, result);
    }

    @Test
    void shouldPreferCliOptionOverDefault() {
        // When
        Path result = RepositoryConfig.resolveRepositoryPath("/custom/path/repo");

        // Then
        assertEquals(Path.of("/custom/path/repo"), result);
    }

    @Test
    void shouldCreateRepositoryWithResolvedPath() {
        // When
        var repository = RepositoryConfig.createRepository("/tmp/test-repo");

        // Then
        assertNotNull(repository);
        repository.close();
    }

    @Test
    void shouldCreateRepositoryWithDefaultPath() {
        // When
        var repository = RepositoryConfig.createRepository(null);

        // Then
        assertNotNull(repository);
        repository.close();
    }

    @Test
    void shouldResolveRelativePaths() {
        // When
        Path result = RepositoryConfig.resolveRepositoryPath("./local/repo");

        // Then
        assertEquals(Path.of("./local/repo"), result);
    }

    @Test
    void shouldResolveAbsolutePaths() {
        // When
        Path result = RepositoryConfig.resolveRepositoryPath("/absolute/path/repo");

        // Then
        assertEquals(Path.of("/absolute/path/repo"), result);
    }

    @Test
    void shouldHaveCorrectDefaultConstant() {
        // Then
        Path expected = Path.of(System.getProperty("user.home"), ".sdd-modeler", "repository");
        assertEquals(expected, RepositoryConfig.DEFAULT_REPOSITORY_PATH);
    }

    @Test
    void shouldHaveCorrectEnvVarName() {
        // Then
        assertEquals("SDD_REPOSITORY_PATH", RepositoryConfig.ENV_REPOSITORY_PATH);
    }

    @Test
    void shouldNotBeInstantiable() throws Exception {
        // When/Then
        var constructor = RepositoryConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        var exception =
                assertThrows(java.lang.reflect.InvocationTargetException.class, () -> constructor.newInstance());

        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }
}
