package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TestPreload {

    // used to capture a bug
    // name: subscribe, num params: 1, paramsCsv: java.util.concurrent.Flow.Subscriber
    @Test
    public void test() throws IOException {
        JavaInspector javaInspector = new org.e2immu.language.inspection.openjdk.JavaInspectorImpl();
        javaInspector.preload("java.base::java.util.");
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPathParts(SourceSetImpl.javaBase())
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.onlyPreload();
    }
}
