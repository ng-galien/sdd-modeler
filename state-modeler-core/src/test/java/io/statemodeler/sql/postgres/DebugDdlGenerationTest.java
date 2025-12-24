package io.statemodeler.sql.postgres;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.core.SddModel;
import io.statemodeler.dsl.YamlModelLoader;
import io.statemodeler.sql.DdlGenerators;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class DebugDdlGenerationTest {

    @Test
    void printOrdersDdl() throws Exception {
        var yamlLoader = new YamlModelLoader();
        Path modelPath = Paths.get("src/test/resources/orders-sdd-model.yaml");
        var modelResult = yamlLoader.loadFromFile(modelPath);
        assertTrue(modelResult.isSuccess(), "Model should load successfully");
        SddModel model = modelResult.get();

        var generator = DdlGenerators.forDialect("postgres");
        String ddl = generator.generateDdl(model);

        assertNotNull(ddl);
        System.out.println("--- GENERATED DDL ---\n" + ddl);
    }
}
