package io.statemodeler.codegen.java;

import io.statemodeler.core.SddModel;
import java.util.Map;

public interface JavaArtifactGenerator {
    Map<String, String> generate(SddModel model);
}
