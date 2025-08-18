package org.e2immu.language.cst.impl.statement;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LocalTypeDeclarationImpl extends StatementImpl implements LocalTypeDeclaration {
    private final TypeInfo typeInfo;

    public LocalTypeDeclarationImpl(List<Comment> comments, Source source, List<AnnotationExpression> annotations,
                                    String label, TypeInfo typeInfo) {
        super(comments, source, annotations, 100, label);
        this.typeInfo = typeInfo;
    }

    public static class Builder extends StatementImpl.Builder<LocalTypeDeclaration.Builder>
            implements LocalTypeDeclaration.Builder {
        private TypeInfo typeInfo;

        @Override
        public Builder setTypeInfo(TypeInfo typeInfo) {
            this.typeInfo = typeInfo;
            return this;
        }

        @Override
        public LocalTypeDeclaration build() {
            return new LocalTypeDeclarationImpl(comments, source, annotations, label, typeInfo);
        }
    }

    @Override
    public TypeInfo typeInfo() {
        return typeInfo;
    }

    @Override
    public boolean hasSubBlocks() {
        return false;
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return this;
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;
        List<TypeInfo> translated = typeInfo.translate(translationMap);
        return translated.stream().map(tt -> tt == typeInfo ? this :
                        (Statement) new LocalTypeDeclarationImpl(comments(), source(), annotations(), label(), tt))
                .toList();
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return new LocalTypeDeclarationImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                infoMap.typeInfoRecurseAllPhases(typeInfo));
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
        // following anonymous class, we're not going deeper
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeStatement(this);
        visitor.afterStatement(this);
        // following anonymous class, we're not going deeper here
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return typeInfo.print(qualification, true);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty(); // see anonymous class in ConstructorCallImpl
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return typeInfo.typesReferenced();
    }

    @Override
    public LocalTypeDeclaration withSource(Source newSource) {
        return new LocalTypeDeclarationImpl(comments(), newSource, annotations(), label(), typeInfo);
    }
}
