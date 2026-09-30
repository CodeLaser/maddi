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

package io.codelaser.maddi.analysis.api;

import io.codelaser.maddi.cst.api.analysis.Message;
import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.integration.JavaInspectorFactory;
import io.codelaser.maddi.util.Trie;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * ⭐ <b>The only way into the modification analysis from outside maddi-mod.</b> The base tier (the run drivers,
 * the refactor engine) and the ext tier (the IDE daemon, the build plugins) compile against this interface; the
 * implementation arrives on the class path at run time from {@code maddi-run-analysis}, found by
 * {@link java.util.ServiceLoader} through {@link AnalysisEngines}. See
 * {@code docs/roadmap/split-maddi-into-three-repositories.md} §1 and stage 3.
 * <p>
 * One method per step of the {@code --analysis-steps} vocabulary ({@code prep}, {@code modification}) plus the
 * services around them (results IO, the analysis-hints compiler and composer, the annotation decorator). Typed
 * methods rather than a generic step over a context: every caller consumes a typed result -- the call graph,
 * the isolated problems, the messages -- and a context bag would carry them untyped.
 * <p>
 * Everything a method takes or returns is a base-tier type. Nothing here names a class of maddi-mod.
 */
public interface AnalysisEngine {

    /** The provider's name, a literal; {@link AnalysisEngines} reports it when two are on the class path. */
    String name();

    // ---- prep: call graph + per-method preparation

    /**
     * Run the prep analyzer over {@code request.primaryTypes()}: it writes its per-method results on the CST and
     * returns the call graph it computed on the way. With {@code faultTolerant} a failing type or method is
     * isolated and reported in {@link PrepOutcome#problems()} instead of aborting the run.
     */
    PrepOutcome prep(PrepRequest request);

    // ---- modification: the iterating modification/immutability analysis

    /**
     * Run the modification analysis over {@code request.order()}. Throws what the analyzer throws (a caller that
     * wants to survive it catches); isolated failures come back as error messages when fault-tolerant.
     */
    ModificationOutcome modification(ModificationRequest request) throws IOException;

    /**
     * The properties the analysis writes for its own bookkeeping during a run. They are not results: a client
     * showing analysis values filters them out.
     */
    Set<Property> bookkeepingProperties();

    // ---- results IO

    /** A loader of analysis results (the AAPI archive, a previous run) into the CST of {@code runtime}. */
    ResultsLoader resultsLoader(Runtime runtime, SourceSet sourceSetOfRequest);

    /** Write the results of every type in {@code types}, with the prep-work codec (the Java drivers' format). */
    void writeResults(Runtime runtime, String targetDirectory, Trie<TypeInfo> types) throws IOException;

    /**
     * Write the results with the codec that knows the link properties ({@code methodLinks}): the one a reader
     * needs after a full modification run. The prep-work codec cannot read those back.
     */
    void writeResultsWithLinks(Runtime runtime, JavaInspector javaInspector, SourceSet sourceSetOfRequest,
                               File targetDirectory, Trie<TypeInfo> types) throws IOException;

    // ---- analysis hints (the AAPI): compile, compose, decorate

    /** The compiler from analysis-hints sources to analysis results (use cases 2 and 3 of the CLI). */
    HintsCompiler hintsCompiler(JavaInspectorFactory javaInspectorFactory);

    /** The composer of analysis-hints skeletons from library types (the Maven plugin's hints writer). */
    HintsComposer hintsComposer(JavaInspector javaInspector, Function<SourceSet, String> destinationPackage,
                                Predicate<Info> accept);

    /**
     * The decorator that renders analysis values as annotations and comments on printed code.
     * {@code translationMap} may be null.
     */
    Qualification.Decorator decorator(Runtime runtime, SourceSet sourceSetOfRequest,
                                      Map<Element, Element> translationMap);

    /**
     * Give every method in {@code methods} the defaults the analysis would assume for it when nothing better is
     * known (the shallow method analyzer): an instrument's way to see what the analysis will read.
     */
    void applyShallowDefaults(Runtime runtime, Collection<MethodInfo> methods);

    // ---- nested service types

    interface ResultsLoader {
        /** Load every results file under the directories ({@code resource:} prefixes allowed). */
        int load(List<String> directories) throws IOException;

        /** Load one results file's content, read with the prep-work codec. */
        int loadContent(String content);
    }

    interface HintsCompiler {
        List<Message> compile(HintsSpec spec) throws IOException;
    }

    interface HintsComposer {
        Collection<TypeInfo> compose(Collection<TypeInfo> primaryTypes);

        Map<Element, Element> translateFromDollarToReal();

        void write(Collection<TypeInfo> apiTypes, File base, Qualification.Decorator decorator) throws IOException;
    }
}
