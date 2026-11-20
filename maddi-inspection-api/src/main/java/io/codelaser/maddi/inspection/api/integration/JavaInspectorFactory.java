package io.codelaser.maddi.inspection.api.integration;

import java.io.IOException;
import java.util.List;

import io.codelaser.maddi.cst.api.element.SourceSet;

public interface JavaInspectorFactory {
    List<SourceSet> dependencies();
    JavaInspector withSources(SourceSet sourceSet) throws IOException;
}
