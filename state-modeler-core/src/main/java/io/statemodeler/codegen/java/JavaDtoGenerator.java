package io.statemodeler.codegen.java;

import io.pebbletemplates.pebble.PebbleEngine;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import java.util.HashMap;
import java.util.Map;

public class JavaDtoGenerator extends JavaGeneratorBase {

    public JavaDtoGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        super(engine, contextBuilder);
    }

    @Override
    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        for (EntityDef entity : model.entities().values()) {
            String content = generateFile(entity, model, "templates/java/dto.java.pebble");
            String filename = resolveFilename(model, contextBuilder.toPascal(entity.name()) + "Dto");
            generatedFiles.put(filename, content);
        }
        return generatedFiles;
    }
}
