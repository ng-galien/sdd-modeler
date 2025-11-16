package io.statemodeler.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class MainTest {

    @Test
    void shouldShowHelpWhenNoSubcommandSpecified() {
        // Given
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        Main main = new Main();
        main.run();

        // Then
        System.setOut(originalOut);
        String output = outContent.toString();
        assertTrue(output.contains("sdd-modeler"), "Help should mention command name");
        assertTrue(
                output.contains("State-Driven Design") || output.contains("Usage"),
                "Help should show usage or description");
    }

    @Test
    void shouldHaveValidateSubcommand() {
        // Given
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);

        // When/Then - verify subcommand exists
        assertNotNull(cmd.getSubcommands().get("validate"), "Should have validate subcommand");
    }

    @Test
    void shouldHaveSqlSubcommand() {
        // Given
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);

        // When/Then
        assertNotNull(cmd.getSubcommands().get("sql"), "Should have sql subcommand");
    }

    @Test
    void shouldHaveDiagramSubcommand() {
        // Given
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);

        // When/Then
        assertNotNull(cmd.getSubcommands().get("diagram"), "Should have diagram subcommand");
    }

    @Test
    void shouldHaveRegisterSubcommand() {
        // Given
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);

        // When/Then
        assertNotNull(cmd.getSubcommands().get("register"), "Should have register subcommand");
    }

    @Test
    void shouldHaveListSubcommand() {
        // Given
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);

        // When/Then
        assertNotNull(cmd.getSubcommands().get("list"), "Should have list subcommand");
    }

    @Test
    void shouldHaveShowSubcommand() {
        // Given
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);

        // When/Then
        assertNotNull(cmd.getSubcommands().get("show"), "Should have show subcommand");
    }

    @Test
    void shouldHaveDeleteSubcommand() {
        // Given
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);

        // When/Then
        assertNotNull(cmd.getSubcommands().get("delete"), "Should have delete subcommand");
    }

    @Test
    void shouldShowVersionWithVersionFlag() {
        // Given
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);

        // When
        int exitCode = cmd.execute("--version");

        // Then
        assertEquals(0, exitCode, "Version flag should return exit code 0");
    }

    @Test
    void shouldShowHelpWithHelpFlag() {
        // Given
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);

        // When
        int exitCode = cmd.execute("--help");

        // Then
        assertEquals(0, exitCode, "Help flag should return exit code 0");
    }

    @Test
    void shouldImplementRunnable() {
        // Given/When/Then
        assertTrue(Runnable.class.isAssignableFrom(Main.class), "Main should implement Runnable for Picocli");
    }
}
