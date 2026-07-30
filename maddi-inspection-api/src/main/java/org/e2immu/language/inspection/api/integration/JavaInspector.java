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

package org.e2immu.language.inspection.api.integration;


import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.SourceFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/*
Unless you have very good reasons not to, please use the openjdk implementation of the JavaInspector.
 */
public interface JavaInspector {

    String TEST_PROTOCOL = "test-protocol";

    // used as the marker to distinguish between the different parsers
    // there are differences in type resolution, #statement prediction, etc.
    boolean isOpenJdk();

    // for tests
    void invalidateAllSources();

    // don't parse sources, only load the preloaded types into the compiledTypesManager
    // NOTE: needs InputConfiguration.addSourceSets(InputConfigurationImpl.TEST_PROTOCOL_SOURCE_SET)
    void onlyPreload();

    String print2(CompilationUnit compilationUnit,
                  Qualification qualification,
                  ImportComputer importComputer);

    // for tests
    SourceSet javaBase();

    // for tests
    SourceSet mainSources();

    // for parsing a single java class in the context of a parse result
    List<TypeInfo> parse(String transformedString, CompilationUnit compilationUnit, ParseResult parseResult,
                         ParseOptions parseOptions);

    @FunctionalInterface
    interface Invalidated extends Function<TypeInfo, InvalidationState> {
    }

    Invalidated INVALIDATED_ALL = t -> InvalidationState.INVALID;

    /**
     * The {@link ParseOptions.Builder} default: the caller did not ask for incremental parsing. Distinguished from a
     * user-supplied "everything is unchanged" <em>by identity</em>, because the two mean different things to a parse
     * that already holds types: the caller who never asked for incremental behaviour wants a full scan, whereas the
     * caller who deliberately says UNCHANGED wants exactly what it says — keep them.
     */
    Invalidated NOT_INVALIDATED = t -> InvalidationState.UNCHANGED;

    record ParseOptions(boolean failFast,
                        boolean detailedSources,
                        Invalidated invalidated,
                        boolean parallel,
                        boolean lombok,
                        boolean ignoreModule,
                        boolean parameterNames,
                        boolean syntheticListField) {
        public static class Builder {
            boolean failFast;
            boolean detailedSources;
            Invalidated invalidated = NOT_INVALIDATED;
            boolean parallel;
            boolean lombok;
            boolean ignoreModule;
            boolean parameterNames;
            // default on: java.util.List.get/set get a synthetic '_synthetic_list' element field so list-element
            // access is standardized as array access (see CreateSyntheticFieldsForGetSet). Turn off to keep the
            // leaner model without the synthetic field.
            boolean syntheticListField = true;

            public Builder setParameterNames(boolean parameterNames) {
                this.parameterNames = parameterNames;
                return this;
            }

            public Builder setFailFast(boolean failFast) {
                this.failFast = failFast;
                return this;
            }

            public Builder setDetailedSources(boolean detailedSources) {
                this.detailedSources = detailedSources;
                return this;
            }

            public Builder setIgnoreModule(boolean ignoreModule) {
                this.ignoreModule = ignoreModule;
                return this;
            }

            public Builder setInvalidated(Invalidated invalidated) {
                this.invalidated = invalidated;
                return this;
            }

            public Builder setParallel(boolean parallel) {
                this.parallel = parallel;
                return this;
            }

            public Builder setLombok(boolean lombok) {
                this.lombok = lombok;
                return this;
            }

            public Builder setSyntheticListField(boolean syntheticListField) {
                this.syntheticListField = syntheticListField;
                return this;
            }

            public ParseOptions build() {
                return new ParseOptions(failFast, detailedSources, invalidated, parallel, lombok, ignoreModule,
                        parameterNames, syntheticListField);
            }
        }
    }

    /*
    Was there a change to this type?
    from high to low in the dependency tree of types: unchanged, invalid/removed, rewire

    REWIRE = the type isn't changed at all, but it accesses invalidated (and hence re-parsed, new) type info objects.
     */
    enum InvalidationState {
        UNCHANGED, INVALID, REWIRE, REMOVED
    }

    ParseOptions failFast();

    // when enabled, class-file methods receive faithful formal parameter names (from the shipped
    // .paramnames.gz index) instead of javac's synthetic arg0, arg1, ... Must be set before any class-file
    // loading (e.g. before onlyPreload()). Default implementation is a no-op (loaders that already read real
    // names, such as the congocc-based one, do not need it).
    default void setParameterNames(boolean parameterNames) {
    }

    // "we're working with JDK internals": the openjdk loader then loads jdk.internal.* types (instead of leaving
    // them as bare stubs) and opens javac up to the internal packages. Default no-op for loaders that don't need it.
    default void setJdkInternals(boolean jdkInternals) {
    }

    /**
     * Have the inspector compile the source sets it parses into class files of its own, in {@code directory}, and
     * resolve every source-set dependency against <em>those</em> rather than against the build's output directory.
     * The directory is the switch: {@code null} (the default) leaves the feature off.
     * <p>
     * Why it exists. The openjdk inspector drives javac one source set at a time, and javac has no view of the CST:
     * when it type-checks {@code test}, every reference into {@code main} is resolved from {@code main}'s
     * <em>class files</em> (see {@code JavaInspectorImpl.createTask}). So the build's output directory has to be
     * present and up to date, which in real projects it often is not — it was cleaned, never built, or is one edit
     * behind. The failure is quiet: the references do not resolve, the compilation units that hold them are dropped
     * as warnings, and the analysis silently covers less than it appears to. Pointing the inspector at a directory
     * of its own removes the dependency on the build's state: each source set is compiled by the very javac task
     * that just parsed it, so what its dependents resolve against is by construction the code maddi read.
     * <p>
     * A source set is generated into {@code directory/<source set>}, which is wiped before each scan of that set, so
     * a type that was renamed or deleted cannot linger. A set that is not re-scanned keeps the class files of its
     * last scan — which is what its unchanged sources compiled to. When generation yields nothing for a set (it does
     * not compile), the build's output directory is used for it as before.
     * <p>
     * Only scans that read a source set from disk generate: a scan driven by in-memory sources is either a whole
     * test-protocol set, which has no dependents wanting class files, or a single file of a disk-backed set
     * ({@code parseSingleFileInSourceSet}), for which emptying the set's directory and refilling it with that one
     * file would be actively wrong.
     * <p>
     * The cost is javac's code-generation phase on top of parse and analyze; that is why this is opt-in. Callers own
     * the directory and its lifecycle: put it inside the build directory to have {@code clean} clear it, or in a
     * user-level cache to have it survive. Default no-op for front ends that do not resolve through class files.
     */
    default void setGeneratedClassesDirectory(Path directory) {
    }

    default ImportComputer importComputer(int minStar, SourceSet sourceSetOfRequest) {
        return runtime().newImportComputer(minStar, packageName ->
                compiledTypesManager().primaryTypesInPackageEnsureLoaded(packageName, sourceSetOfRequest));
    }

    record InitializationProblem(String errorMsg, Throwable throwable) {
    }

    List<InitializationProblem> initialize(InputConfiguration inputConfiguration) throws IOException;

    void preload(String thePackage);

    default void preload(String... packages) {
        Arrays.stream(packages).forEach(this::preload);
    }

    // main parse method, from sources specified in InputConfiguration
    default Summary parse(ParseOptions parseOptions) {
        return parse(Map.of(), parseOptions);
    }

    // only for testing
    Summary parse(Map<String, String> sourcesByTestProtocolURIString, ParseOptions parseOptions);

    // only for testing (openjdk)
    Summary parseMultiSourceSet(Map<SourceSet, Map<String, String>> sourcesByFqnBySourceSet, ParseOptions parseOptions);

    // only for testing, uses FAIL_FAST default
    default TypeInfo parse(String input) {
        return parseReturnAll(input, "main", failFast()).getFirst();
    }

    // only for testing, uses FAIL_FAST default; OpenJDK/maddi compatible method
    TypeInfo parse(String fqn, String input);

    // only for testing, OpenJDK/maddi compatible method
    TypeInfo parse(String fqn, String input, ParseOptions parseOptions);

    // only for testing, uses FAIL_FAST default
    default TypeInfo parse(String input, String inputName, String sourceSetName) {
        return parseReturnAll(input, inputName, sourceSetName, failFast()).getFirst();
    }

    // only for testing, after general parse()
    default TypeInfo parse(String input, ParseOptions parseOptions) {
        return parseReturnAll(input, "main", parseOptions).getFirst();
    }

    List<TypeInfo> parseReturnAll(String input, String inputName, String sourceSetName, ParseOptions parseOptions);

    // only for testing, after general parse();
    Summary parseSingleFileInSourceSet(URI typeInfo, SourceSet sourceSet, ParseOptions parseOptions);

    // only for testing, after general parse();
    default List<TypeInfo> parseReturnAll(String input, String sourceSetName, ParseOptions parseOptions) {
        return parseReturnAll(input, "input", sourceSetName, parseOptions);
    }

    default String print2(CompilationUnit compilationUnit) {
        return print2(compilationUnit, (Qualification.Decorator) null, importComputer(4,
                compilationUnit.sourceSet()));
    }

    default String print2(CompilationUnit compilationUnit, Qualification.Decorator decorator, ImportComputer importComputer) {
        return print2(compilationUnit, runtime().qualificationQualifyFromPrimaryType(decorator), importComputer);
    }

    /**
     * Parse a {@code module-info.java} that this parse does not hold, given nothing but its path.
     * <p>
     * The descriptors of the parsed source sets are in {@link ParseResult#sourceSetToModuleInfoMap()}; use those when
     * they exist, because they are the objects the rest of the run edits. This is for the module descriptor of a
     * project that was never analysed — the sibling that a refactoring nonetheless has to touch, typically because a
     * qualified {@code exports} in it has to gain the name of a module that did not exist before the refactoring.
     * <p>
     * Purely syntactic: it neither loads types nor resolves the names in {@code uses} / {@code provides ... with},
     * and it never puts javac in module mode. The returned descriptor carries {@link org.e2immu.language.cst.api.element.Source}
     * for each directive, so it can be printed back over the original text; its compilation unit has the file's URI
     * and no source set. Returns {@code null} when the file cannot be read or does not parse as a module
     * declaration — this is a best-effort read of a file outside the analysed tree, never a reason to fail a run.
     */
    ModuleInfo parseModuleInfo(Path moduleInfoFile);

    Runtime runtime();

    CompiledTypesManager compiledTypesManager();

    Set<SourceFile> sourceFiles();

    record ReloadResult(List<InitializationProblem> problems, Set<TypeInfo> sourceHasChanged) {
    }

    ReloadResult reloadSources(InputConfiguration inputConfiguration, Map<String, String> sourcesByTestProtocolURIString) throws IOException;

    /**
     * The read-only {@link org.e2immu.language.cst.api.info.InfoMapView} of the most recent re-parse's rewire (old
     * object → new object), or {@code null} if the last parse did no rewiring. Lets a caller carry a spared REWIRE
     * type's analysis onto its new object <em>outside</em> the reload, via the {@code rewire(InfoMapView, …)} path —
     * see {@code docs/analysis-rewiring.md}. Valid until the next parse.
     */
    default org.e2immu.language.cst.api.info.InfoMapView lastRewireInfoMap() {
        return null;
    }
}
