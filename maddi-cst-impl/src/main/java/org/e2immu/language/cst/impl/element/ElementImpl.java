package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.QualificationImpl;
import org.e2immu.language.cst.impl.variable.DescendModeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class ElementImpl implements Element {

    public record TypeReference(TypeInfo typeInfo, boolean explicit) implements Element.TypeReference {
        public TypeReference {
            assert typeInfo != null;
        }

        @Override
        public Element.TypeReference withExplicit() {
            return new TypeReference(typeInfo, true);
        }
    }

    @Override
    public Stream<Variable> variableStreamDescend() {
        return variables(DescendModeEnum.YES);
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        return variables(DescendModeEnum.NO);
    }

    @Override
    public String toString() {
        OutputBuilder print = print(QualificationImpl.SIMPLE_NAMES);
        if (print == null) return "<print returns null>";
        return print.toString();
    }

    @SuppressWarnings("unchecked")
    public abstract static class Builder<B extends Element.Builder<?>> implements Element.Builder<B> {
        protected final List<Comment> comments = new ArrayList<>();
        protected final List<AnnotationExpression> annotations = new ArrayList<>();
        protected Source source;

        public Builder() {
        }

        public Builder(Element e) {
            addComments(e.comments());
            setSource(e.source());
        }

        @Override
        public B setSource(Source source) {
            this.source = source;
            return (B) this;
        }

        @Override
        public B addComment(Comment comment) {
            this.comments.add(comment);
            return (B) this;
        }

        @Override
        public B addComments(List<Comment> comments) {
            this.comments.addAll(comments);
            return (B) this;
        }

        @Override
        public B addAnnotation(AnnotationExpression annotation) {
            this.annotations.add(annotation);
            return (B) this;
        }

        @Override
        public B addAnnotations(List<AnnotationExpression> annotations) {
            this.annotations.addAll(annotations);
            return (B) this;
        }


    }
}
