package io.statemodeler.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main entry point for the SDD modeler CLI.
 */
@Command(
        name = "sdd-modeler",
        description = "State-Driven Design modeler CLI for generating SQL from YAML/JSON models",
        mixinStandardHelpOptions = true,
        version = "sdd-modeler 0.1.0",
        subcommands = {ValidateCommand.class, SqlCommand.class, DiagramCommand.class, RegisterCommand.class})
public class Main implements Runnable {

    @Override
    public void run() {
        // When no subcommand is specified, show help
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
