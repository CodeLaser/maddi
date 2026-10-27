/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * javac attributes {@code "a" + "a" + … } recursively, a few frames per operand, and so does Lombok's JavacAST when
 * it builds its tree. pulsar-broker-common has such a chain: on laser1 the Lombok round overflowed the test worker's
 * stack, and the source set was parsed without Lombok. Plain javac compiles it on a 1 MiB stack, overflows on 512
 * KiB. The parse runs on a thread of its own with a large stack, so the caller's stack does not matter.
 */
public class TestDeepExpressionOnSmallStack {

    private static String deepSource() {
        // a variable operand: javac's parser folds a chain of literals into one
        StringBuilder sb = new StringBuilder("package io.codelaser.maddi.test;\npublic class X {\n"
                                             + "    String s(int i) {\n        return \"a\"");
        for (int j = 0; j < 3000; j++) sb.append(" + i");
        return sb.append(";\n    }\n}\n").toString();
    }

    @Test
    public void parsedFromASmallStack() throws Exception {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/"))
                .setDependencies(List.of(javaBase)).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet).addClassPath("jmod:java.base").build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).build();

        AtomicReference<Summary> summary = new AtomicReference<>();
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        // 256 KiB: far too little for javac on this chain, if it ran here
        Thread caller = Thread.ofPlatform().stackSize(256 * 1024).unstarted(() -> {
            try {
                summary.set(javaInspector.parseMultiSourceSet(
                        Map.of(sourceSet, Map.of("io.codelaser.maddi.test.X", deepSource())), options));
            } catch (Throwable t) {
                thrown.set(t);
            }
        });
        caller.start();
        caller.join();
        assertNull(thrown.get(), () -> String.valueOf(thrown.get()));
        assertFalse(summary.get().haveErrors(), () -> String.valueOf(summary.get().parseExceptions()));
    }
}
