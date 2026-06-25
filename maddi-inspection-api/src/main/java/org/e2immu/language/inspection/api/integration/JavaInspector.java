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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/*

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

    record ParseOptions(boolean failFast,
                        boolean detailedSources,
                        Invalidated invalidated,
                        boolean parallel,
                        boolean lombok,
                        boolean ignoreModule) {
        public static class Builder {
            boolean failFast;
            boolean detailedSources;
            Invalidated invalidated;
            boolean parallel;
            boolean lombok;
            boolean ignoreModule;

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

            public ParseOptions build() {
                return new ParseOptions(failFast, detailedSources, invalidated, parallel, lombok, ignoreModule);
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

    Runtime runtime();

    CompiledTypesManager compiledTypesManager();

    Set<SourceFile> sourceFiles();

    record ReloadResult(List<InitializationProblem> problems, Set<TypeInfo> sourceHasChanged) {
    }

    ReloadResult reloadSources(InputConfiguration inputConfiguration, Map<String, String> sourcesByTestProtocolURIString) throws IOException;
}
