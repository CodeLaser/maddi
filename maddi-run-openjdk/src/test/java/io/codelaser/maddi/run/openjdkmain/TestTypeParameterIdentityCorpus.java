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
package io.codelaser.maddi.run.openjdkmain;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.run.config.util.JsonStreaming;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Over a whole corpus parse: every occurrence of a type parameter in a field, return or parameter type is the
 * instance its owner declares, and every declared type parameter is committed.
 * <p>
 * The invariant behind {@code TestClassTypeParameterIdentity} (2026-09-14). Counting declared type parameters without a
 * source (the earlier instrument) could not see that route, because the owner's list held the right instance and only
 * the signatures held a second one: 1,731 of guava's 8,946 class type-parameter occurrences, 332 of jenkins' 826.
 * Scan order decides it, so it takes a real corpus, not a unit test, to cover the orders that occur.
 */
@Tag("slow")
public class TestTypeParameterIdentityCorpus {

    @Test
    public void guava() throws Exception {
        check("guava");
    }

    @Test
    public void jenkins() throws Exception {
        check("jenkins");
    }

    private static void check(String corpus) throws Exception {
        Path config = TestOssCorpus.config(corpus);
        Assumptions.assumeTrue(Files.exists(config), () -> "requires the " + corpus + " corpus at " + config);
        JavaInspector javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.setJdkInternals(true); // guava-tests reads a non-exported java.base package; see TestGuava
        javaInspector.initialize(JsonStreaming.objectMapper().readValue(config.toFile(), InputConfigurationImpl.class));
        javaInspector.preload("java.base::java.util");
        Summary summary = javaInspector.parse(new JavaInspector.ParseOptions.Builder().setDetailedSources(true)
                .setFailFast(false).setIgnoreModule(true).build());
        List<TypeInfo> types = summary.parseResult().primaryTypes().stream()
                .flatMap(TypeInfo::recursiveSubTypeStream).toList();

        List<String> secondInstances = new ArrayList<>();
        List<String> uncommitted = new ArrayList<>();
        int[] occurrences = {0};
        for (TypeInfo type : types) {
            type.typeParameters().stream().filter(tp -> !tp.hasBeenInspected())
                    .forEach(tp -> uncommitted.add(tp + " of " + type));
            type.fields().forEach(f -> visit(f.type(), "field " + f.fullyQualifiedName(), occurrences, secondInstances));
            for (MethodInfo m : type.constructorsAndMethods()) {
                m.typeParameters().stream().filter(tp -> !tp.hasBeenInspected())
                        .forEach(tp -> uncommitted.add(tp + " of " + m));
                visit(m.returnType(), "return type of " + m, occurrences, secondInstances);
                m.parameters().forEach(p -> visit(p.parameterizedType(), "parameter " + p.name() + " of " + m,
                        occurrences, secondInstances));
            }
        }
        assertTrue(occurrences[0] > 500, "measured only " + occurrences[0] + " type-parameter occurrences");
        assertEquals(List.of(), secondInstances.stream().limit(20).toList(),
                secondInstances.size() + " occurrence(s) hold a second instance of their type parameter");
        assertEquals(List.of(), uncommitted.stream().limit(20).toList(),
                uncommitted.size() + " type parameter(s) never committed");
    }

    private static void visit(ParameterizedType pt, String where, int[] occurrences, List<String> secondInstances) {
        if (pt == null) return;
        TypeParameter tp = pt.typeParameter();
        if (tp != null) {
            occurrences[0]++;
            List<TypeParameter> declared = tp.getOwner().isLeft() ? tp.getOwner().getLeft().typeParameters()
                    : tp.getOwner().getRight().typeParameters();
            if (tp.getIndex() >= declared.size() || declared.get(tp.getIndex()) != tp) {
                secondInstances.add(tp + " in " + where);
            }
        }
        pt.parameters().forEach(p -> visit(p, where, occurrences, secondInstances));
    }
}
