package com.example.sample;

import io.statemodeler.codegen.CodeGenerators;
import io.statemodeler.dsl.YamlModelLoader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CodeGenerationTest {

    @Test
    void generateCode() throws IOException {
        var modelPath = Path.of("src/main/resources/sdd.yaml");
        var model = new YamlModelLoader().loadFromFile(modelPath).get();
        var generator = CodeGenerators.forLanguage("java");
        var generatedFiles = generator.generate(model);

        var outputDir = Path.of("src/main/java");

        for (var entry : generatedFiles.entrySet()) {
            var relativePath = entry.getKey();
            var content = entry.getValue();
            var outputPath = outputDir.resolve(relativePath);

            Files.createDirectories(outputPath.getParent());
            try (var writer = new FileWriter(outputPath.toFile())) {
                writer.write(content);
            }
            System.out.println("Generated: " + outputPath);
        }
    }
}
