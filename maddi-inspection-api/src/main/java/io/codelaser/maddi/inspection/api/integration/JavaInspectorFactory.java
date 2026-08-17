package org.e2immu.language.inspection.api.integration;

import org.e2immu.language.cst.api.element.SourceSet;

import java.io.IOException;
import java.util.List;

public interface JavaInspectorFactory {
    List<SourceSet> dependencies();
    JavaInspector withSources(SourceSet sourceSet) throws IOException;
}
