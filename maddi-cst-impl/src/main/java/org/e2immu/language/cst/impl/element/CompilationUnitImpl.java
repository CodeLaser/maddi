package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;
import org.e2immu.support.SetOnce;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CompilationUnitImpl extends ElementImpl implements CompilationUnit {
    private final URI uri;
    private final String packageName;
    private final List<ImportStatement> importStatements;
    private final List<Comment> comments;
    private final Source source;
    private final SourceSet sourceSet;
    private final SetOnce<FingerPrint> fingerPrint = new SetOnce<>();

    public CompilationUnitImpl(SourceSet sourceSet,
                               URI uri,
                               List<Comment> comments,
                               Source source,
                               List<ImportStatement> importStatements,
                               String packageName,
                               FingerPrint fingerPrint) {
        this.sourceSet = sourceSet;
        this.uri = uri;
        this.packageName = packageName;
        this.comments = comments;
        this.source = source;
        this.importStatements = importStatements;
        if (fingerPrint != null) {
            this.fingerPrint.set(fingerPrint);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompilationUnitImpl that)) return false;
        return Objects.equals(uri, that.uri) && Objects.equals(sourceSet, that.sourceSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, sourceSet);
    }

    @Override
    public FingerPrint fingerPrintOrNull() {
        return fingerPrint.getOrDefaultNull();
    }

    @Override
    public void setFingerPrint(FingerPrint fingerPrint) {
        this.fingerPrint.set(fingerPrint);
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public String packageName() {
        return packageName;
    }

    @Override
    public List<ImportStatement> importStatements() {
        return importStatements;
    }

    @Override
    public int complexity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Comment> comments() {
        return comments;
    }

    @Override
    public Source source() {
        return source;
    }

    @Override
    public SourceSet sourceSet() {
        return sourceSet;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        importStatements.forEach(predicate::test);
    }

    @Override
    public void visit(Visitor visitor) {
        importStatements.forEach(is -> is.visit(visitor));
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder ob = new OutputBuilderImpl();
        if (packageName != null && !packageName.isBlank()) ob.add(new TextImpl(packageName));
        if (uri != null) ob.add(new TextImpl(" [" + uri + "]"));
        return ob;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Element rewire(InfoMap infoMap) {
        return this;
    }

    public static class Builder extends ElementImpl.Builder<CompilationUnit.Builder> implements CompilationUnit.Builder {
        private String packageName;
        private URI uri;
        private final List<ImportStatement> importStatements = new LinkedList<>();
        private SourceSet sourceSet;
        private FingerPrint fingerPrint;

        @Override
        public Builder setFingerPrint(FingerPrint fingerPrint) {
            this.fingerPrint = fingerPrint;
            return this;
        }

        @Override
        public Builder setSourceSet(SourceSet sourceSet) {
            this.sourceSet = sourceSet;
            return this;
        }

        @Override
        public CompilationUnit.Builder addImportStatement(ImportStatement importStatement) {
            this.importStatements.add(importStatement);
            return this;
        }

        @Override
        public CompilationUnit.Builder setURI(URI uri) {
            this.uri = uri;
            return this;
        }

        @Override
        public CompilationUnit.Builder setURIString(String s) {
            try {
                this.uri = new URI(s);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return this;
        }


        @Override
        public CompilationUnit.Builder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        @Override
        public CompilationUnit build() {
            return new CompilationUnitImpl(sourceSet, uri, comments, source, List.copyOf(importStatements), packageName,
                    fingerPrint);
        }
    }
}
