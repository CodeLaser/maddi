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

package org.e2immu.analyzer.run.config.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestStreaming {
    @Test
    public void test() throws JsonProcessingException {
        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        SourceSet sourceSet = new SourceSetImpl("abc", List.of(Path.of("/home/x")), URI.create("file:/home/x"),
                StandardCharsets.UTF_8, true, false, false, false, false,
                Set.of("a.b.c"), Set.of());
        FingerPrint fingerPrint1 = MD5FingerPrint.compute("hello");
        Assertions.assertEquals("XUFAKrxLKna5cZ2REBfFkg==", fingerPrint1.toString());
        sourceSet.setFingerPrint(fingerPrint1);

        SourceSet sourceSet2 = new SourceSetImpl("def", List.of(Path.of("/home/y")), URI.create("file:/home/y"),
                StandardCharsets.UTF_8, true, false, false, false, false,
                Set.of(), Set.of(sourceSet));
        sourceSet2.setAnalysisFingerPrint(MD5FingerPrint.compute("there"));
        InputConfiguration inputConfiguration = new InputConfigurationImpl(Path.of("."),
                List.of(sourceSet, sourceSet2), List.of(), Path.of("/"));
        String json = objectMapper.writeValueAsString(inputConfiguration);
        System.out.println(json);

        InputConfiguration copy = objectMapper.readerFor(InputConfiguration.class).readValue(json);
        Assertions.assertEquals(2, copy.sourceSets().size());
        assertNotNull(copy.sourceSets());
        SourceSet set1 = copy.sourceSets().getFirst();
        Assertions.assertEquals("[a.b.c]", set1.restrictToPackages().toString());
        Assertions.assertEquals(fingerPrint1, set1.fingerPrintOrNull());

        SourceSet set2 = copy.sourceSets().get(1);
        Assertions.assertSame(set1, set2.dependencies().stream().findFirst().orElseThrow());
    }
}
