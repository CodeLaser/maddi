/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.SingleLineComment;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.QualificationImpl;
import io.codelaser.maddi.cst.impl.variable.DescendModeEnum;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class ElementImpl implements Element {

    /**
     * The '//' comments written after an element, on its last line: {@code a(); // why}. They belong to it
     * (CodeLaser/maddi#128), and the printer writes them after it rather than above it (#137).
     *
     * @param written the source of the whole element as written
     */
    public static List<Comment> commentsAfterOnLastLine(List<Comment> comments, Source written) {
        if (comments.isEmpty() || written == null || written.endLine() <= 0) return List.of();
        return comments.stream().filter(c -> c instanceof SingleLineComment && c.source() != null
                                             && c.source().beginLine() == written.endLine()
                                             && c.source().beginLine() > 0
                                             && (c.source().beginLine() > written.beginLine()
                                                 || c.source().beginPos() > written.beginPos())
                                             && c.source().beginPos() > written.endPos()).toList();
    }

    public record TypeReference(TypeInfo typeInfo, TypeReferenceNature typeReferenceNature, TypeInfo typeToImport)
            implements Element.TypeReference {
        public TypeReference {
            assert typeInfo != null;
            assert typeReferenceNature != null;
        }

        @Override
        public Element.TypeReference with(TypeReferenceNature typeReferenceNature, TypeInfo typeToImport) {
            return typeReferenceNature == this.typeReferenceNature && typeToImport == this.typeToImport ? this
                    : new TypeReference(typeInfo, typeReferenceNature, typeToImport);
        }

        @Override
        public @NotNull String toString() {
            String ie = typeReferenceNature == TypeReferenceNature.IMPLICIT ? "I"
                    : "E" + (typeToImport == null ? " FQN" : typeToImport == typeInfo
                    ? "" : " " + typeToImport.fullyQualifiedName());
            return typeInfo.fullyQualifiedName() + "[" + ie + "]";
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
