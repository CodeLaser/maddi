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

package org.e2immu.analyzer.aapi.parser;

import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.analyzer.modification.prepwork.io.WriteAnalysis;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/*
From analysis hints to machine-readable analysis results in json format.
 */
public class AnalysisHintsCompiler {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AnalysisHintsCompiler.class);

    public static final String AAPI_DIR =
            "../maddi-aapi-archive/src/main/java/org/e2immu/analyzer/aapi/archive/v2";
    public static final String APF_DIR =
            "../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles";

    private final RunVisitor runVisitor;
    private final JavaInspector javaInspector;

    public AnalysisHintsCompiler(JavaInspector javaInspector) {
        this(javaInspector, null);
    }

    public AnalysisHintsCompiler(JavaInspector javaInspector, RunVisitor runVisitor) {
        this.runVisitor = runVisitor;
        this.javaInspector = javaInspector;
    }

    public List<Message> go(AnalysisHints analysisHints) throws IOException {
        LOGGER.info("Compiling analysis hints {}", analysisHints);
        if (runVisitor != null) runVisitor.setContext(analysisHints);

        AnalysisHintsParser analysisHintsParser = new AnalysisHintsParser(javaInspector);
        analysisHintsParser.initialize(jre.path(),
                classPath,
                List.of(AAPI_DIR),
                List.of(subDirIn));
        if (runVisitor != null) {
            runVisitor.afterAnnotatedApiParsing(analysisHintsParser.javaInspector());
        }
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(analysisHintsParser.runtime(), analysisHintsParser,
                true, runVisitor == null ? null : runVisitor.debugVisitor());
        ShallowAnalyzer.Result rs = shallowAnalyzer.go(analysisHintsParser.types());

        Set<Element> hasAnnotations = analysisHintsParser.infos();
        LOGGER.info("Parsed and analyzed {} types; {} info objects", analysisHintsParser.types().size(),
                hasAnnotations.size());
        hasAnnotations.forEach(ha -> {
            if (ha instanceof Info i && !i.analysis().haveAnalyzedValueFor(PropertyImpl.ANNOTATED_API)) {
                i.analysis().set(PropertyImpl.ANNOTATED_API, ValueImpl.BoolImpl.TRUE);
            }
        });

        Set<TypeInfo> writeOut = new HashSet<>(analysisHintsParser.types());
        if (runVisitor != null) runVisitor.writeAnalysis(writeOut);
        WriteAnalysis wa = new WriteAnalysis(analysisHintsParser.runtime(), writeOut::contains);
        Trie<TypeInfo> trie = new Trie<>();
        for (TypeInfo ti : analysisHintsParser.types()) {
            if (ti.isPrimaryType()) {
                trie.add(ti.packageName().split("\\."), ti);
            }
        }
        File dir = new File(APF_DIR);
        File subDirOutFile = new File(dir, subDirOut);
        wa.write(subDirOutFile.getAbsolutePath(), trie);

        AnalysisHintsWriter analysisHintsWriter = new AnalysisHintsWriter(analysisHintsParser.javaInspector(),
                analysisHintsParser::data, i -> rs.dataMap().get(i));
        File decorated = new File("build/decorated");
        File subDirDeco = new File(decorated, subDirOut);
        if (subDirDeco.mkdirs()) {
            LOGGER.info("Created directory {}", subDirDeco);
        }
        analysisHintsWriter.write(subDirDeco.getAbsolutePath(), trie, "org.e2immu");
        return rs.messages();
    }
}
