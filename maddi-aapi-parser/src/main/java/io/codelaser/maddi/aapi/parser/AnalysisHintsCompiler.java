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
import org.e2immu.analyzer.modification.prepwork.io.WriteAnalysisResults;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.integration.JavaInspectorFactory;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
From analysis hints to machine-readable analysis results in json format.
Process:
    - AnalysisHintsParser (here)
    - ShallowAnalyzer (in modification-common)
    - WriteAnalysisResults (in modification-prepwork)
    - AnalysisHintsWriter (here, to update the hints file), uses AnalysisHintsComposer
 */
public class AnalysisHintsCompiler {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AnalysisHintsCompiler.class);

    private final CompilerVisitor compilerVisitor;
    private final JavaInspectorFactory javaInspectorFactory;

    public AnalysisHintsCompiler(JavaInspectorFactory javaInspectorFactory) {
        this(javaInspectorFactory, null);
    }

    public AnalysisHintsCompiler(JavaInspectorFactory javaInspectorFactory, CompilerVisitor compilerVisitor) {
        this.compilerVisitor = compilerVisitor;
        this.javaInspectorFactory = javaInspectorFactory;
    }

    public List<Message> go(AnalysisHints analysisHints) throws IOException {
        LOGGER.info("Compiling analysis hints {}", analysisHints);
        if (compilerVisitor != null) compilerVisitor.setContext(analysisHints);

        AnalysisHintsParser analysisHintsParser = new AnalysisHintsParser(javaInspectorFactory);
        JavaInspector javaInspector = analysisHintsParser.go(analysisHints);
        if (compilerVisitor != null) {
            compilerVisitor.afterAnnotatedApiParsing(javaInspector);
        }
        Runtime runtime = javaInspector.runtime();

        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, analysisHintsParser,
                true, compilerVisitor == null ? null : compilerVisitor.debugVisitor());
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
        if (compilerVisitor != null) compilerVisitor.writeAnalysis(writeOut);
        WriteAnalysisResults wa = new WriteAnalysisResults(runtime, writeOut::contains);
        Trie<TypeInfo> trie = new Trie<>();
        for (TypeInfo ti : analysisHintsParser.types()) {
            if (ti.isPrimaryType()) {
                trie.add(ti.packageName().split("\\."), ti);
            }
        }
        File subDirOutFile = analysisHints.analysisResultsDir().toFile();
        wa.write(subDirOutFile.getAbsolutePath(), trie);

        if (analysisHints.updatedHintsPath() != null) {
            AnalysisHintsWriter analysisHintsWriter = new AnalysisHintsWriter(javaInspector,
                    analysisHintsParser::data, i -> rs.dataMap().get(i));
            Files.createDirectories(analysisHints.updatedHintsPath());
            analysisHintsWriter.write(analysisHints.updatedHintsPath().toAbsolutePath().toString(),
                    trie, "org.e2immu");
        }
        return rs.messages();
    }
}
