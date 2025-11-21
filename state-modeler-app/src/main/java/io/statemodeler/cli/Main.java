package io.statemodeler.cli;

import io.statemodeler.cli.commands.DeleteCommand;
import io.statemodeler.cli.commands.DiagramCommand;
import io.statemodeler.cli.commands.DiffCommand;
import io.statemodeler.cli.commands.ListCommand;
import io.statemodeler.cli.commands.MigrateCommand;
import io.statemodeler.cli.commands.RegisterCommand;
import io.statemodeler.cli.commands.ShowCommand;
import io.statemodeler.cli.commands.ShowMigrationCommand;
import io.statemodeler.cli.commands.SqlCommand;
import io.statemodeler.cli.commands.ValidateCommand;
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
        subcommands = {
            ValidateCommand.class,
            SqlCommand.class,
            io.statemodeler.cli.commands.GenerateCommand.class,
            DiagramCommand.class,
            DiffCommand.class,
            RegisterCommand.class,
            ListCommand.class,
            ShowCommand.class,
            ShowMigrationCommand.class,
            DeleteCommand.class,
            MigrateCommand.class
        })
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
