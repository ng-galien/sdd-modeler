package io.statemodeler.codegen;

import java.util.Locale;
import java.util.Map;

/**
 * Configuration DTO for code generation feature toggles and MCP settings.
 */
public record CodegenConfig(
        String packageName,
        boolean generateController,
        boolean generateRepository,
        boolean generateMcp,
        McpProtocol mcpProtocol,
        McpServerType mcpServerType,
        boolean mcpStdio) {

    public enum McpProtocol {
        SSE,
        STREAMABLE,
        STATELESS
    }

    public enum McpServerType {
        SYNC,
        ASYNC
    }

    public static CodegenConfig fromOptions(Map<String, String> options) {
        var opts = options != null ? options : Map.<String, String>of();
        var rawPackage = opts.get("packageName");
        var packageName = (rawPackage == null || rawPackage.isBlank()) ? "com.example" : rawPackage;
        var generateController = parseBoolean(opts.get("generateController"), true);
        var generateRepository = parseBoolean(opts.get("generateRepository"), true);
        var generateMcp = parseBoolean(opts.get("generateMcp"), true);
        var mcpStdio = parseBoolean(opts.get("mcpStdio"), false);
        var protocol = parseProtocol(opts.get("mcpProtocol"));
        var serverType = parseServerType(opts.get("mcpServerType"));
        return new CodegenConfig(
                packageName, generateController, generateRepository, generateMcp, protocol, serverType, mcpStdio);
    }

    private static boolean parseBoolean(String raw, boolean defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        var normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return defaultValue;
        }
        return normalized.equals("true")
                || normalized.equals("1")
                || normalized.equals("yes")
                || normalized.equals("y")
                || normalized.equals("on");
    }

    private static McpProtocol parseProtocol(String raw) {
        if (raw == null || raw.isBlank()) {
            return McpProtocol.SSE;
        }
        var normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return McpProtocol.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return McpProtocol.SSE;
        }
    }

    private static McpServerType parseServerType(String raw) {
        if (raw == null || raw.isBlank()) {
            return McpServerType.SYNC;
        }
        var normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return McpServerType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return McpServerType.SYNC;
        }
    }
}
