package io.statemodeler.cli;

import picocli.CommandLine;
import org.junit.jupiter.api.Test;

class MainSmokeTest {

    @Test
    void mainShouldExitZeroOnHelp() {
        int exit = new CommandLine(new Main()).execute("--help");
        org.junit.jupiter.api.Assertions.assertEquals(0, exit);
    }
}
