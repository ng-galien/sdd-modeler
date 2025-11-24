package io.statemodeler.codegen;

import io.statemodeler.core.*;
import java.util.Map;

public class DebugGeneratorRunner {
    public static void main(String[] args) {
        try {
            var idAttr = new AttributeDef("id", "serial", false, true, null, null);
            var createdAtAttr = new AttributeDef("created_at", "timestamptz", false, false, null, null);
            var created =
                    new StateDef("created", "order_created", true, java.util.List.of(), java.util.List.of(), Map.of());
            var states = Map.of("created", created);
            var entity = new EntityDef(
                    "order_item", "orders", idAttr, Map.of("created_at", createdAtAttr), states, Map.of(), Map.of());
            var database = new DatabaseConfig("postgres", "public", null, Map.of("packageName", "com.example"));
            var model = new SddModel("1.0.0", "test", database, Map.of(entity.name(), entity));

            CodeGenerator generator = CodeGenerators.forLanguage("java");
            var generated = generator.generate(model);
            System.out.println("Generated files:");
            generated.forEach((k, v) -> {
                System.out.println("--- " + k + " ---");
                System.out.println(v);
                System.out.println("--- end ---\n");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
