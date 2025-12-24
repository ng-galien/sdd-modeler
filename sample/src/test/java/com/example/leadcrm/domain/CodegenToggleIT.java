package com.example.leadcrm.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.statemodeler.codegen.CodeGenerators;
import io.statemodeler.dsl.YamlModelLoader;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class CodegenToggleIT {

    @Test
    void mcpConfigIsGeneratedWhenEnabled() throws Exception {
        var loader = new YamlModelLoader();
        try (InputStream input = getClass().getResourceAsStream("/sdd.yaml")) {
            assertNotNull(input);
            var model = loader.loadFromStream(input).get();
            var generated = CodeGenerators.forLanguage("java").generate(model);
            assertTrue(generated.containsKey("resources/application-mcp.properties"));
            var content = generated.get("resources/application-mcp.properties");
            assertTrue(content.contains("spring.ai.mcp.server.protocol=SSE"));
            assertTrue(content.contains("spring.ai.mcp.server.type=SYNC"));
        }
    }

    @Test
    void togglesDisableControllersAndMcp() throws Exception {
        var loader = new YamlModelLoader();
        try (InputStream input = getClass().getResourceAsStream("/sdd-toggle.yaml")) {
            assertNotNull(input);
            var model = loader.loadFromStream(input).get();
            var generated = CodeGenerators.forLanguage("java").generate(model);
            assertFalse(generated.keySet().stream().anyMatch(path -> path.endsWith("Controller.java")));
            assertFalse(generated.keySet().stream().anyMatch(path -> path.endsWith("Api.java")));
            assertFalse(generated.keySet().stream().anyMatch(path -> path.endsWith("McpServer.java")));
            assertFalse(generated.containsKey("resources/application-mcp.properties"));
        }
    }
}
