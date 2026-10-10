/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
