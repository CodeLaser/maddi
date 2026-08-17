package io.codelaser.maddi.inspection.api.integration;

import io.codelaser.maddi.cst.api.element.SourceSet;

import java.io.IOException;
import java.util.List;

public interface JavaInspectorFactory {
    List<SourceSet> dependencies();
    JavaInspector withSources(SourceSet sourceSet) throws IOException;
}
