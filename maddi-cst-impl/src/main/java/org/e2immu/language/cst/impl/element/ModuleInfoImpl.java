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

package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.info.Access;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.InfoMapView;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.GuideImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.output.TextImpl;
import org.e2immu.support.SetOnce;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ModuleInfoImpl extends ElementImpl implements ModuleInfo {
    private final CompilationUnit compilationUnit;
    private final String name;
    private final List<Comment> comments;
    private final Source source;
    private final List<Requires> requires;
    private final List<Exports> exports;
    private final List<Opens> opens;
    private final List<Uses> uses;
    private final List<Provides> provides;
    private final boolean open;

    public ModuleInfoImpl(CompilationUnit compilationUnit,
                          List<Comment> comments, Source source, String name,
                          List<Requires> requires, List<Exports> exports,
                          List<Opens> opens, List<Uses> uses, List<Provides> provides,
                          boolean open) {
        this.compilationUnit = compilationUnit;
        this.name = name;
        this.comments = comments;
        this.source = source;
        this.requires = requires;
        this.exports = exports;
        this.opens = opens;
        this.uses = uses;
        this.provides = provides;
        this.open = open;
    }

    @Override
    public boolean hasBeenInspected() {
        return true;
    }

    @Override
    public String info() {
        return "module";
    }

    @Override
    public Access access() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompilationUnit compilationUnit() {
        return compilationUnit;
    }

    /*
    Identity, not source. ElementImpl derives toString() from print(), so implementing the declaration printer
    silently turned every module-info label -- graph vertices among them -- into the whole `module N { ... }` body.
    TypeInfo draws the same line by returning its fully qualified name here.
     */
    @Override
    public String toString() {
        return name;
    }

    @Override
    public String simpleName() {
        return name;
    }

    @Override
    public String fullyQualifiedName() {
        return name;
    }

    @Override
    public String descriptor() {
        return compilationUnit.sourceSet() + "::" + name;
    }

    @Override
    public boolean isSynthetic() {
        return false;
    }

    @Override
    public TypeInfo typeInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasBeenAnalyzed() {
        return false;
    }

    @Override
    public JavaDoc javaDoc() {
        return null;
    }

    @Override
    public List<? extends Info> translate(TranslationMap translationMap) {
        throw new UnsupportedOperationException("NYI");
    }

    /*
    Shared by the five directive printers below.

    Every directive is `<word> <name> [<connector> a, b, c];`, so the only thing that differs between exports and
    opens is one word. Printing from the CST rather than concatenating strings is the point: a lever that has to
    add one target to `exports p to a, b;` should hand back a directive, not a rewritten substring.
     */
    private static OutputBuilder printComments(Qualification qualification, List<Comment> comments) {
        OutputBuilder ob = new OutputBuilderImpl();
        if (comments != null && !comments.isEmpty()) {
            ob.add(comments.stream().map(c -> c.print(qualification))
                    .collect(OutputBuilderImpl.joining(SpaceEnum.NONE, GuideImpl.multipleComments())));
        }
        return ob;
    }

    /*
    Comma-separated, WITHOUT the joining collector's guides.

    joining() wraps its result in guideGenerator.start()/end(), and the formatter renders that trailing guide as a
    split point -- which came out as `exports p to a, b, c ;`, a space before the semicolon. A directive is short
    and never wants to be split, so the separator is written out instead. The space is explicit, which also makes
    the MINIMAL rendering (toString) match how the directive is actually written in a file.
     */
    private static OutputBuilder commaSeparated(List<String> names) {
        OutputBuilder ob = new OutputBuilderImpl();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) ob.add(SymbolEnum.COMMA).add(SpaceEnum.ONE);
            ob.add(new TextImpl(names.get(i)));
        }
        return ob;
    }

    private static OutputBuilder printPackageDirective(Qualification qualification, List<Comment> comments,
                                                       String word, String packageName, List<String> toModules) {
        OutputBuilder ob = printComments(qualification, comments)
                .add(new TextImpl(word)).add(SpaceEnum.ONE).add(new TextImpl(packageName));
        // an UNQUALIFIED directive has no `to` clause at all; an empty target list is not `to ;`
        if (toModules != null && !toModules.isEmpty()) {
            ob.add(SpaceEnum.ONE).add(new TextImpl("to")).add(SpaceEnum.ONE).add(commaSeparated(toModules));
        }
        return ob.add(SymbolEnum.SEMICOLON);
    }

    private record RequiresImpl(Source source, List<Comment> comments, String name, boolean isStatic,
                                boolean isTransitive) implements Requires {
        RequiresImpl {
            Objects.requireNonNull(name);
        }

        @Override
        public int complexity() {
            return 0;
        }

        @Override
        public Element rewire(InfoMapView infoMap) {
            return null;
        }

        @Override
        public void visit(Predicate<Element> predicate) {
            predicate.test(this);
        }

        @Override
        public void visit(Visitor visitor) {

        }

        /*
        A module directive prints from the CST, so a lever never has to build its text by hand. These words are
        RESTRICTED keywords -- contextual to a module declaration and legal identifiers everywhere else -- so they
        are TextImpl and not KeywordImpl, the same choice ImportStatementImpl makes for `import`.
         */
        @Override
        public OutputBuilder print(Qualification qualification) {
            OutputBuilder ob = printComments(qualification, comments)
                    .add(new TextImpl("requires")).add(SpaceEnum.ONE);
            if (isStatic) ob.add(new TextImpl("static")).add(SpaceEnum.ONE);
            // JLS 7.7.1 fixes this order: `requires static transitive M`, never the reverse
            if (isTransitive) ob.add(new TextImpl("transitive")).add(SpaceEnum.ONE);
            return ob.add(new TextImpl(name)).add(SymbolEnum.SEMICOLON);
        }

        @Override
        public Stream<Variable> variables(DescendMode descendMode) {
            return Stream.empty();
        }

        @Override
        public Stream<Variable> variableStreamDoNotDescend() {
            return Stream.empty();
        }

        @Override
        public Stream<Variable> variableStreamDescend() {
            return Stream.empty();
        }

        @Override
        public Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
            return Stream.empty();
        }
    }

    private record ExportsImpl(Source source, List<Comment> comments, String packageName,
                               List<String> toModulesOrEmpty) implements Exports {
        ExportsImpl {
            Objects.requireNonNull(packageName);
            Objects.requireNonNull(toModulesOrEmpty);
        }

        @Override
        public int complexity() {
            return 0;
        }

        @Override
        public Element rewire(InfoMapView infoMap) {
            return null;
        }

        @Override
        public void visit(Predicate<Element> predicate) {
            predicate.test(this);
        }

        @Override
        public void visit(Visitor visitor) {

        }

        @Override
        public Exports withToModules(List<String> toModules) {
            return new ExportsImpl(source, comments, packageName, List.copyOf(toModules));
        }

        @Override
        public OutputBuilder print(Qualification qualification) {
            return printPackageDirective(qualification, comments, "exports", packageName, toModulesOrEmpty);
        }

        @Override
        public Stream<Variable> variables(DescendMode descendMode) {
            return Stream.empty();
        }

        @Override
        public Stream<Variable> variableStreamDoNotDescend() {
            return Stream.empty();
        }

        @Override
        public Stream<Variable> variableStreamDescend() {
            return Stream.empty();
        }

        @Override
        public Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
            return Stream.empty();
        }
    }

    private record OpensImpl(Source source, List<Comment> comments, String packageName,
                             List<String> toModulesOrEmpty) implements Opens {
        OpensImpl {
            Objects.requireNonNull(packageName);
            Objects.requireNonNull(toModulesOrEmpty);
        }

        @Override
        public int complexity() {
            return 0;
        }

        @Override
        public Element rewire(InfoMapView infoMap) {
            return null;
        }

        @Override
        public void visit(Predicate<Element> predicate) {
            predicate.test(this);
        }

        @Override
        public void visit(Visitor visitor) {

        }

        @Override
        public OutputBuilder print(Qualification qualification) {
            return printPackageDirective(qualification, comments, "opens", packageName, toModulesOrEmpty);
        }

        @Override
        public Stream<Variable> variables(DescendMode descendMode) {
            return Stream.empty();
        }

        @Override
        public Stream<Variable> variableStreamDoNotDescend() {
            return Stream.empty();
        }

        @Override
        public Stream<Variable> variableStreamDescend() {
            return Stream.empty();
        }

        @Override
        public Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
            return Stream.empty();
        }
    }

    private static class UsesImpl implements Uses {
        private final Source source;
        private final List<Comment> comments;
        private final String api;
        private final SetOnce<TypeInfo> apiResolved = new SetOnce<>();

        UsesImpl(Source source, List<Comment> comments, String api) {
            this.source = source;
            this.comments = comments;
            this.api = Objects.requireNonNull(api);
        }

        @Override
        public Source source() {
            return source;
        }

        @Override
        public List<Comment> comments() {
            return comments;
        }

        @Override
        public String api() {
            return api;
        }

        @Override
        public TypeInfo apiResolved() {
            return apiResolved.getOrDefaultNull();
        }

        @Override
        public void setApiResolved(TypeInfo typeInfo) {
            this.apiResolved.set(typeInfo);
        }

        @Override
        public int complexity() {
            return 0;
        }

        @Override
        public Element rewire(InfoMapView infoMap) {
            return null;
        }

        @Override
        public void visit(Predicate<Element> predicate) {
            predicate.test(this);
        }

        @Override
        public void visit(Visitor visitor) {

        }

        @Override
        public OutputBuilder print(Qualification qualification) {
            return printComments(qualification, comments).add(new TextImpl("uses")).add(SpaceEnum.ONE)
                    .add(new TextImpl(api)).add(SymbolEnum.SEMICOLON);
        }

        @Override
        public Stream<Variable> variables(DescendMode descendMode) {
            return Stream.empty();
        }

        @Override
        public Stream<Variable> variableStreamDoNotDescend() {
            return Stream.empty();
        }

        @Override
        public Stream<Variable> variableStreamDescend() {
            return Stream.empty();
        }

        @Override
        public Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
            return typeReference(apiResolved(), source);

        }
    }

    private static Stream<Element.TypeReference> typeReference(TypeInfo resolved, Source source) {
        if (resolved == null) return Stream.empty();
        return Stream.of(new ElementImpl.TypeReference(resolved, TypeReferenceNature.EXPLICIT,
                source.detailedSources() == null ? resolved : source.detailedSources().qualifier(resolved)));
    }

    private static class ProvidesImpl implements Provides {
        private final Source source;
        private final List<Comment> comments;
        private final String api;
        private final List<String> implementations;
        private final SetOnce<TypeInfo> apiResolved = new SetOnce<>();
        private final List<TypeInfo> implementationsResolved = new ArrayList<>();

        ProvidesImpl(Source source, List<Comment> comments, String api, List<String> implementations) {
            this.source = source;
            this.comments = comments;
            this.api = Objects.requireNonNull(api);
            this.implementations = List.copyOf(Objects.requireNonNull(implementations));
        }

        @Override
        public Source source() {
            return source;
        }

        @Override
        public List<Comment> comments() {
            return comments;
        }

        @Override
        public String api() {
            return api;
        }

        @Override
        public List<String> implementations() {
            return implementations;
        }

        @Override
        public void setApiResolved(TypeInfo typeInfo) {
            this.apiResolved.set(typeInfo);
        }

        @Override
        public TypeInfo apiResolved() {
            return apiResolved.getOrDefaultNull();
        }

        @Override
        public void addImplementationResolved(TypeInfo typeInfo) {
            this.implementationsResolved.add(typeInfo);
        }

        @Override
        public List<TypeInfo> implementationsResolved() {
            return implementationsResolved;
        }

        @Override
        public int complexity() {
            return 0;
        }

        @Override
        public Element rewire(InfoMapView infoMap) {
            return null;
        }

        @Override
        public void visit(Predicate<Element> predicate) {
            predicate.test(this);
        }

        @Override
        public void visit(Visitor visitor) {

        }

        @Override
        public OutputBuilder print(Qualification qualification) {
            OutputBuilder ob = printComments(qualification, comments).add(new TextImpl("provides")).add(SpaceEnum.ONE)
                    .add(new TextImpl(api)).add(SpaceEnum.ONE).add(new TextImpl("with")).add(SpaceEnum.ONE);
            // every implementation, not just the first: `provides X with A, B, C` losing B and C is the shape that
            // stranded implementations in the Elasticsearch carve
            return ob.add(commaSeparated(implementations)).add(SymbolEnum.SEMICOLON);
        }

        @Override
        public Stream<Variable> variables(DescendMode descendMode) {
            return Stream.empty();
        }

        @Override
        public Stream<Variable> variableStreamDoNotDescend() {
            return Stream.empty();
        }

        @Override
        public Stream<Variable> variableStreamDescend() {
            return Stream.empty();
        }

        @Override
        public Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
            Stream<Element.TypeReference> s1 = typeReference(apiResolved(), source);
            Stream<Element.TypeReference> s2 = implementationsResolved.stream()
                    .flatMap(impl -> typeReference(impl, source));
            return Stream.concat(s1, s2);
        }
    }

    public static class BuilderImpl extends ElementImpl.Builder<ModuleInfo.Builder> implements ModuleInfo.Builder {
        private CompilationUnit compilationUnit;
        private String name;
        private boolean open;
        private final List<Requires> requiresList = new ArrayList<>();
        private final List<Exports> exports = new ArrayList<>();
        private final List<Opens> opens = new ArrayList<>();
        private final List<Uses> uses = new ArrayList<>();
        private final List<Provides> provides = new ArrayList<>();

        @Override
        public BuilderImpl setOpen(boolean open) {
            this.open = open;
            return this;
        }

        @Override
        public ModuleInfo.Builder setCompilationUnit(CompilationUnit compilationUnit) {
            this.compilationUnit = compilationUnit;
            return this;
        }

        @Override
        public ModuleInfo.Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ModuleInfo build() {
            return new ModuleInfoImpl(compilationUnit, comments, source, name, List.copyOf(requiresList),
                    List.copyOf(exports), List.copyOf(opens), List.copyOf(uses), List.copyOf(provides), open);
        }

        @Override
        public ModuleInfo.Builder addRequires(Source source, List<Comment> comments, String name, boolean isStatic, boolean isTransitive) {
            requiresList.add(new RequiresImpl(source, comments, name, isStatic, isTransitive));
            return this;
        }

        @Override
        public ModuleInfo.Builder addExports(Source source, List<Comment> comments, String packageName, List<String> toModules) {
            exports.add(new ExportsImpl(source, comments, packageName, List.copyOf(toModules)));
            return this;
        }

        @Override
        public ModuleInfo.Builder addOpens(Source source, List<Comment> comments, String packageName, List<String> toModules) {
            opens.add(new OpensImpl(source, comments, packageName, List.copyOf(toModules)));
            return this;
        }

        @Override
        public ModuleInfo.Builder addUses(Source source, List<Comment> comments, String api) {
            uses.add(new UsesImpl(source, comments, api));
            return this;
        }

        @Override
        public ModuleInfo.Builder addProvides(Source source, List<Comment> comments, String api, List<String> implementations) {
            provides.add(new ProvidesImpl(source, comments, api, implementations));
            return this;
        }
    }

    @Override
    public int complexity() {
        return 0;
    }

    @Override
    public List<Comment> comments() {
        return comments;
    }

    @Override
    public Element rewire(InfoMapView infoMap) {
        throw new UnsupportedOperationException("To implement!");
    }

    @Override
    public Source source() {
        return source;
    }

    @Override
    public List<Requires> requires() {
        return requires;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            requires.forEach(r -> r.visit(predicate));
            exports.forEach(r -> r.visit(predicate));
            opens.forEach(r -> r.visit(predicate));
            uses.forEach(r -> r.visit(predicate));
            provides.forEach(r -> r.visit(predicate));
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeModule(this)) {
            requires.forEach(r -> r.visit(visitor));
            exports.forEach(r -> r.visit(visitor));
            opens.forEach(r -> r.visit(visitor));
            uses.forEach(r -> r.visit(visitor));
            provides.forEach(r -> r.visit(visitor));
        }
        visitor.afterModule();
    }

    /*
    The whole declaration: `[open] module N { <directives> }`, in JLS 7.7 order.

    This used to print the module NAME alone, which is why nothing downstream could render a module-info and every
    lever that touched one built its text by hand. The body is joined with generatorForBlock, the same generator a
    type body uses, so the formatter indents and splits it like any other block -- in particular a long qualified
    export, which is where hand-built text goes wrong first.
     */
    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        comments().forEach(c -> outputBuilder.add(c.print(qualification)));
        if (open) outputBuilder.add(new TextImpl("open")).add(SpaceEnum.ONE);
        outputBuilder.add(new TextImpl("module")).add(SpaceEnum.ONE).add(new TextImpl(name)).add(SpaceEnum.ONE);
        // JLS 7.7: requires, exports, opens, uses, provides -- the order a reader expects, and the order the
        // parser hands them back within each kind
        OutputBuilder body = Stream.of(requires.stream(), exports.stream(), opens.stream(), uses.stream(),
                        provides.stream())
                .flatMap(st -> st)
                .map(d -> d.print(qualification))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NONE, SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE,
                        GuideImpl.generatorForBlock()));
        return outputBuilder.add(body);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return Stream.concat(uses.stream().flatMap(uses1 -> uses1.typesReferenced(predicate)),
                provides.stream().flatMap(provides1 -> provides1.typesReferenced(predicate)));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean open() {
        return open;
    }

    @Override
    public List<Provides> provides() {
        return provides;
    }

    @Override
    public List<Uses> uses() {
        return uses;
    }

    @Override
    public List<Opens> opens() {
        return opens;
    }

    @Override
    public List<Exports> exports() {
        return exports;
    }
}
