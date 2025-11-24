package io.statemodeler.codegen.java;

import io.pebbletemplates.pebble.PebbleEngine;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import java.util.HashMap;
import java.util.Map;

public class JavaEntityGenerator extends JavaGeneratorBase {

    public JavaEntityGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        super(engine, contextBuilder);
    }

    @Override
    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        for (EntityDef entity : model.entities().values()) {
            String content = generateFile(entity, model, "templates/java/entity.java.pebble");
            String filename = resolveFilename(model, contextBuilder.toPascal(entity.name()) + "State");
            generatedFiles.put(filename, content);

            String idContent = generateFile(entity, model, "templates/java/id.java.pebble");
            String idFilename = resolveFilename(model, contextBuilder.toPascal(entity.name()) + "Id");
            generatedFiles.put(idFilename, idContent);

            String domainStateContent = generateFile(entity, model, "templates/java/domain_state_entity.java.pebble");
            String domainStateFilename = resolveFilename(model, contextBuilder.toPascal(entity.name()) + "DomainState");
            generatedFiles.put(domainStateFilename, domainStateContent);
        }
        return generatedFiles;
    }
}
