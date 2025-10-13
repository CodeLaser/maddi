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
