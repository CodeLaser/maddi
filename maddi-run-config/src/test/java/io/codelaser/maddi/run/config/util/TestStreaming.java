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

package io.codelaser.maddi.run.config.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codelaser.maddi.aapi.parser.AnalysisHintsConfiguration;
import io.codelaser.maddi.aapi.parser.AnalysisHintsConfigurationImpl;
import io.codelaser.maddi.run.config.Configuration;
import io.codelaser.maddi.cst.api.element.FingerPrint;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.api.resource.MD5FingerPrint;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestStreaming {
    @Test
    public void test() throws JsonProcessingException {
        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName("abc")
                .setBuildUnit("io.codelaser.maddi:abc")
                .setSourceDirectories(List.of(Path.of("/home/x")))
                .setUri(URI.create("file:/home/x"))
                .setTest(true)
                .setRestrictToPackages(Set.of("a.b.c"))
                .build();
        FingerPrint fingerPrint1 = MD5FingerPrint.compute("hello");
        Assertions.assertEquals("XUFAKrxLKna5cZ2REBfFkg==", fingerPrint1.toString());
        sourceSet.setFingerPrint(fingerPrint1);

        SourceSet sourceSet2 = new SourceSetImpl.Builder()
                .setName("def")
                .setSourceDirectories(List.of(Path.of("/home/y")))
                .setUri(URI.create("file:/home/y"))
                .setTest(true)
                .setDependencies(List.of(sourceSet))
                .build();

        sourceSet2.setAnalysisFingerPrint(MD5FingerPrint.compute("there"));
        InputConfiguration inputConfiguration = new InputConfigurationImpl(Path.of("."),
                List.of(sourceSet, sourceSet2), List.of(), Path.of("/"), 0);
        String json = objectMapper.writeValueAsString(inputConfiguration);
        System.out.println(json);

        InputConfiguration copy = objectMapper.readerFor(InputConfiguration.class).readValue(json);
        Assertions.assertEquals(2, copy.sourceSets().size());
        assertNotNull(copy.sourceSets());
        SourceSet set1 = copy.sourceSets().getFirst();
        Assertions.assertEquals("[a.b.c]", set1.restrictToPackages().toString());
        Assertions.assertEquals(fingerPrint1, set1.fingerPrintOrNull());
        Assertions.assertEquals("io.codelaser.maddi:abc", set1.buildUnit());

        SourceSet set2 = copy.sourceSets().get(1);
        Assertions.assertSame(set1, set2.dependencies().stream().findFirst().orElseThrow());
        // no build unit was set: it must stay absent from the json, and read back as null rather than as ""
        Assertions.assertFalse(json.contains("\"buildUnit\":\"\""));
        Assertions.assertNull(set2.buildUnit());
    }

    @Test
    public void testRuntimeOnlySurvives() throws JsonProcessingException {
        SourceSet runtimeOnly = new SourceSetImpl.Builder()
                .setName("some-runtime-dep.jar")
                .setUri(URI.create("file:/repo/some-runtime-dep.jar"))
                .setLibrary(true)
                .setExternalLibrary(true)
                .setRuntimeOnly(true)
                .build();

        // the copy constructor must carry the flag over: the GRADLE plugin's ComputeDependencies keeps
        // runtime-only libraries off a compile class path, so losing the flag silently widens it.
        // ⚠ Not this module's ComputeDependencies, which has no notion of runtimeOnly at all -- the comment
        // that used to be here named the class in this package and described the behaviour of its twin.
        SourceSet renamed = new SourceSetImpl.Builder(runtimeOnly).setName("renamed.jar").build();
        Assertions.assertTrue(renamed.runtimeOnly());

        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        InputConfiguration inputConfiguration = new InputConfigurationImpl(Path.of("."),
                List.of(), List.of(runtimeOnly), Path.of("/"), 21);
        String json = objectMapper.writeValueAsString(inputConfiguration);
        InputConfiguration copy = objectMapper.readerFor(InputConfiguration.class).readValue(json);
        Assertions.assertTrue(copy.classPathParts().getFirst().runtimeOnly());
        // the corpus's --release must survive the round trip: it decides which JDK API the parse compiles
        // against, and losing it silently reinstates "whatever JDK maddi runs on"
        Assertions.assertEquals(21, copy.sourceRelease());
    }

    @Test
    public void testSourceReleaseAbsentReadsAsZero() throws JsonProcessingException {
        // ⚠ AND AN OLDER CONFIGURATION MUST STILL READ. sourceRelease is omitted when unknown, so a file
        // written before this field existed has no such key; it must come back as 0 ("use the running JDK")
        // rather than fail to deserialize.
        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        String json = "{\"workingDirectory\":\".\",\"classPathParts\":[],\"sourceSets\":[],"
                      + "\"alternativeJREDirectory\":null}";
        InputConfiguration copy = objectMapper.readerFor(InputConfiguration.class).readValue(json);
        Assertions.assertEquals(0, copy.sourceRelease());
    }

    /**
     * The per-set javac options, which exist because a REACTOR has no single answer: OpenSearch states three
     * releases (44 sets at 21, buildSrc/reaper at 11, libs/common at 8) and the global field can only abstain.
     * <p>
     * ⛔ The copy Builder is asserted too, and deliberately: it copies field by field, so a field added to
     * SourceSetImpl and forgotten there is dropped SILENTLY — the positional constructor stops compiling, this
     * does not. That is the failure {@code testRuntimeOnlySurvives} already guards for {@code runtimeOnly}, and
     * a dropped {@code sourceRelease} is worse: it silently reinstates "whatever JDK maddi runs on" for that
     * one set, which is the bug this whole field exists to remove.
     */
    @Test
    public void testPerSourceSetJavacOptionsSurvive() throws JsonProcessingException {
        SourceSet old = new SourceSetImpl.Builder()
                .setName("libs-common")
                .setUri(URI.create("file:/repo/libs/common"))
                .setSourceRelease(8)
                .setAddModules(List.of("jdk.incubator.vector"))
                .build();
        SourceSet modern = new SourceSetImpl.Builder()
                .setName("server-main")
                .setUri(URI.create("file:/repo/server"))
                .setSourceRelease(21)
                .build();
        SourceSet silent = new SourceSetImpl.Builder()
                .setName("says-nothing")
                .setUri(URI.create("file:/repo/other"))
                .build();

        // the copy Builder must carry both over
        SourceSet renamed = new SourceSetImpl.Builder(old).setName("renamed").build();
        Assertions.assertEquals(8, renamed.sourceRelease());
        Assertions.assertEquals(List.of("jdk.incubator.vector"), renamed.addModules());

        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        // global sourceRelease 0: the mixed corpus the global field cannot express
        InputConfiguration inputConfiguration = new InputConfigurationImpl(Path.of("."),
                List.of(old, modern, silent), List.of(), Path.of("/"), 0);
        String json = objectMapper.writeValueAsString(inputConfiguration);
        InputConfiguration copy = objectMapper.readerFor(InputConfiguration.class).readValue(json);

        Assertions.assertEquals(8, copy.sourceSets().get(0).sourceRelease());
        Assertions.assertEquals(List.of("jdk.incubator.vector"), copy.sourceSets().get(0).addModules());
        Assertions.assertEquals(21, copy.sourceSets().get(1).sourceRelease());
        // a set that states nothing keeps stating nothing, and says so by omission rather than by a 0 key
        Assertions.assertEquals(0, copy.sourceSets().get(2).sourceRelease());
        Assertions.assertTrue(copy.sourceSets().get(2).addModules().isEmpty());
        Assertions.assertFalse(json.contains("\"sourceRelease\":0"), json);
        Assertions.assertFalse(json.contains("\"addModules\":[]"), json);
    }

    @Test
    public void testPerSetOptionsAbsentInAnOlderConfiguration() throws JsonProcessingException {
        // the same backwards-compatibility rule the global field's test states: a configuration written before
        // these fields existed has no such keys, and must read rather than fail
        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        String json = "{\"workingDirectory\":\".\",\"classPathParts\":[],\"sourceSets\":["
                      + "{\"name\":\"main\",\"uri\":\"file:/x\"}],\"alternativeJREDirectory\":null}";
        InputConfiguration copy = objectMapper.readerFor(InputConfiguration.class).readValue(json);
        Assertions.assertEquals(0, copy.sourceSets().getFirst().sourceRelease());
        Assertions.assertTrue(copy.sourceSets().getFirst().addModules().isEmpty());
    }

    @Test
    public void testAnalysisHintsConfiguration() throws JsonProcessingException {
        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        AnalysisHintsConfiguration hints = new AnalysisHintsConfigurationImpl.Builder()
                .addPreloadAnalysisResultsDirs("dir1", "dir2")
                .addHintsPackages("java.util.")
                .setUpdatedHintsDir("/tmp/aapi")
                .setUpdatedHintsPackage("a.b")
                .build();
        Configuration configuration = new Configuration.Builder().setAnalysisHintsConfiguration(hints).build();

        String json = objectMapper.writeValueAsString(configuration);
        Configuration copy = objectMapper.readerFor(Configuration.class).readValue(json);

        AnalysisHintsConfiguration copyHints = copy.analysisHintsConfiguration();
        assertNotNull(copyHints);
        Assertions.assertEquals(List.of("dir1", "dir2"), copyHints.preloadAnalysisResultsDirs());
        Assertions.assertEquals(List.of("java.util."), copyHints.hintsPackages());
        Assertions.assertEquals("/tmp/aapi", copyHints.updatedHintsDir());
        Assertions.assertEquals("a.b", copyHints.updatedHintsPackage());
    }
}
