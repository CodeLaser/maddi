package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.support.SetOnce;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ModuleInfoImpl extends ElementImpl implements ModuleInfo {
    private final String name;
    private final List<Comment> comments;
    private final Source source;
    private final List<Requires> requires;
    private final List<Exports> exports;
    private final List<Opens> opens;
    private final List<Uses> uses;
    private final List<Provides> provides;

    public ModuleInfoImpl(List<Comment> comments, Source source, String name,
                          List<Requires> requires, List<Exports> exports,
                          List<Opens> opens, List<Uses> uses, List<Provides> provides) {
        this.name = name;
        this.comments = comments;
        this.source = source;
        this.requires = requires;
        this.exports = exports;
        this.opens = opens;
        this.uses = uses;
        this.provides = provides;
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
        public Element rewire(InfoMap infoMap) {
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
            return null;
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
        public Stream<TypeReference> typesReferenced() {
            return Stream.empty();
        }
    }

    private record ExportsImpl(Source source, List<Comment> comments, String packageName,
                               String toPackageNameOrNull) implements Exports {
        ExportsImpl {
            Objects.requireNonNull(packageName);
        }

        @Override
        public int complexity() {
            return 0;
        }

        @Override
        public Element rewire(InfoMap infoMap) {
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
            return null;
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
        public Stream<TypeReference> typesReferenced() {
            return Stream.empty();
        }
    }

    private record OpensImpl(Source source, List<Comment> comments, String packageName,
                             String toPackageNameOrNull) implements Opens {
        OpensImpl {
            Objects.requireNonNull(packageName);
        }

        @Override
        public int complexity() {
            return 0;
        }

        @Override
        public Element rewire(InfoMap infoMap) {
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
            return null;
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
        public Stream<TypeReference> typesReferenced() {
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
        public Element rewire(InfoMap infoMap) {
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
            return null;
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
        public Stream<TypeReference> typesReferenced() {
            TypeInfo resolved = apiResolved();
            return resolved == null ? Stream.empty() : Stream.of(new ElementImpl.TypeReference(resolved, true));
        }
    }

    private static class ProvidesImpl implements Provides {
        private final Source source;
        private final List<Comment> comments;
        private final String api;
        private final String implementation;
        private final SetOnce<TypeInfo> apiResolved = new SetOnce<>();
        private final SetOnce<TypeInfo> implementationResolved = new SetOnce<>();

        ProvidesImpl(Source source, List<Comment> comments, String api, String implementation) {
            this.source = source;
            this.comments = comments;
            this.api = Objects.requireNonNull(api);
            this.implementation = Objects.requireNonNull(implementation);
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
        public String implementation() {
            return implementation;
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
        public void setImplementationResolved(TypeInfo typeInfo) {
            this.implementationResolved.set(typeInfo);
        }

        @Override
        public TypeInfo implementationResolved() {
            return implementationResolved.getOrDefaultNull();
        }

        @Override
        public int complexity() {
            return 0;
        }

        @Override
        public Element rewire(InfoMap infoMap) {
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
            return null;
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
        public Stream<TypeReference> typesReferenced() {
            TypeInfo a = apiResolved();
            Stream<ElementImpl.TypeReference> s1 = a == null ? Stream.empty()
                    : Stream.of(new ElementImpl.TypeReference(a, true));
            TypeInfo i = implementationResolved();
            Stream<ElementImpl.TypeReference> s2 = i == null ? Stream.empty()
                    : Stream.of(new ElementImpl.TypeReference(i, true));
            return Stream.concat(s1, s2);
        }
    }

    public static class BuilderImpl extends ElementImpl.Builder<ModuleInfo.Builder> implements ModuleInfo.Builder {
        private String name;
        private final List<Requires> requiresList = new ArrayList<>();
        private final List<Exports> exports = new ArrayList<>();
        private final List<Opens> opens = new ArrayList<>();
        private final List<Uses> uses = new ArrayList<>();
        private final List<Provides> provides = new ArrayList<>();

        @Override
        public ModuleInfo.Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ModuleInfo build() {
            return new ModuleInfoImpl(comments, source, name, List.copyOf(requiresList), List.copyOf(exports),
                    List.copyOf(opens), List.copyOf(uses), List.copyOf(provides));
        }

        @Override
        public void addRequires(Source source, List<Comment> comments, String name, boolean isStatic, boolean isTransitive) {
            requiresList.add(new RequiresImpl(source, comments, name, isStatic, isTransitive));
        }

        @Override
        public void addExports(Source source, List<Comment> comments, String packageName, String toPackageNameOrNull) {
            exports.add(new ExportsImpl(source, comments, packageName, toPackageNameOrNull));
        }

        @Override
        public void addOpens(Source source, List<Comment> comments, String packageName, String toPackageNameOrNull) {
            opens.add(new OpensImpl(source, comments, packageName, toPackageNameOrNull));
        }

        @Override
        public void addUses(Source source, List<Comment> comments, String api) {
            uses.add(new UsesImpl(source, comments, api));
        }

        @Override
        public void addProvides(Source source, List<Comment> comments, String api, String implementation) {
            provides.add(new ProvidesImpl(source, comments, api, implementation));
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
    public Element rewire(InfoMap infoMap) {
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

    @Override
    public OutputBuilder print(Qualification qualification) {
        return null;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(uses.stream().flatMap(Uses::typesReferenced),
                provides.stream().flatMap(Provides::typesReferenced));
    }

    @Override
    public String name() {
        return name;
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
