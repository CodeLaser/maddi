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

package org.e2immu.language.cst.impl.info;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.Access;
import org.e2immu.language.cst.api.info.Info;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class InspectionImpl implements Inspection {
    private final Access access;
    private final List<Comment> comments;
    private final Source source;
    private final boolean synthetic;
    private final List<AnnotationExpression> annotations;
    private final JavaDoc javaDoc;

    public enum AccessEnum implements Access {
        PRIVATE(0), PACKAGE(1), PROTECTED(2), PUBLIC(3);

        private final int level;

        AccessEnum(int level) {
            this.level = level;
        }

        public Access combine(Access other) {
            if (level < other.level()) return this;
            return other;
        }

        @Override
        public Access max(Access other) {
            if (level < other.level()) return other;
            return this;
        }

        @Override
        public boolean isPublic() {
            return this == PUBLIC;
        }

        @Override
        public boolean isPackage() {
            return this == PACKAGE;
        }

        @Override
        public boolean isPrivate() {
            return this == PRIVATE;
        }

        @Override
        public boolean isProtected() {
            return this == PROTECTED;
        }

        @Override
        public int level() {
            return level;
        }
    }

    public InspectionImpl(Access access,
                          List<Comment> comments,
                          Source source,
                          boolean synthetic,
                          List<AnnotationExpression> annotations,
                          JavaDoc javaDoc) {
        this.access = access;
        this.comments = comments;
        this.source = source;
        this.synthetic = synthetic;
        this.annotations = annotations;
        this.javaDoc = javaDoc;
    }

    @Override
    public Access access() {
        return access;
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
    public boolean isSynthetic() {
        return synthetic;
    }

    @Override
    public List<AnnotationExpression> annotations() {
        return annotations;
    }

    @Override
    public JavaDoc javaDoc() {
        return javaDoc;
    }

    @SuppressWarnings("unchecked")
    public static abstract class Builder<B extends Info.Builder<?>> implements Inspection, Info.Builder<B> {
        private Access access;
        private List<Comment> comments = new ArrayList<>();
        private Source source;
        private boolean synthetic;
        private List<AnnotationExpression> annotations = new ArrayList<>();
        private JavaDoc javaDoc;

        @Fluent
        public B setAccess(Access access) {
            this.access = access;
            return (B) this;
        }

        @Override
        public B setAnnotationExpression(int index, AnnotationExpression annotationExpression) {
            annotations.set(index, annotationExpression);
            return (B) this;
        }

        @Override
        public AnnotationExpression haveAnnotation(String fullyQualifiedName) {
            return annotations.stream()
                    .filter(ae -> fullyQualifiedName.equals(ae.typeInfo().fullyQualifiedName()))
                    .findFirst().orElse(null);
        }

        @Override
        public Stream<AnnotationExpression> annotationStream() {
            return annotations.stream();
        }

        @Fluent
        public B setAnnotations(List<AnnotationExpression> annotations) {
            this.annotations = annotations;
            return (B) this;
        }

        @Fluent
        public B setComments(List<Comment> comments) {
            this.comments = comments;
            return (B) this;
        }

        @Fluent
        public B setSource(Source source) {
            this.source = source;
            return (B) this;
        }

        @Override
        public B addComment(Comment comment) {
            comments.add(comment);
            return (B) this;
        }

        @Override
        public B addComments(List<Comment> comments) {
            this.comments.addAll(comments);
            return (B) this;
        }

        @Override
        public B addAnnotation(AnnotationExpression annotation) {
            annotations.add(annotation);
            return (B) this;
        }

        @Override
        public B addAnnotations(List<AnnotationExpression> annotations) {
            this.annotations.addAll(annotations);
            return (B) this;
        }

        @Fluent
        public B setSynthetic(boolean synthetic) {
            this.synthetic = synthetic;
            return (B) this;
        }

        @Override
        public B setJavaDoc(JavaDoc javaDoc) {
            this.javaDoc = javaDoc;
            return (B) this;
        }

        @Override
        public Access access() {
            return access;
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
        public boolean isSynthetic() {
            return synthetic;
        }

        @Override
        public List<AnnotationExpression> annotations() {
            return annotations;
        }

        @Override
        public JavaDoc javaDoc() {
            return javaDoc;
        }
    }
}
