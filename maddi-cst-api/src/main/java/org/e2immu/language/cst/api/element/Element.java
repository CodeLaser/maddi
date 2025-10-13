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

package org.e2immu.language.cst.api.element;

import org.e2immu.annotation.Fluent;
import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.NotNull;
import org.e2immu.annotation.rare.IgnoreModifications;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents an element in the Abstract Syntax Tree.
 * Each element has a
 * <ol>
 *     <li>Source: non-essential client-facing information about where in the source this element sits, meant
 *     for error reporting</li>
 *     <li>zero or more Comments: as attached to this element; non-essential to almost all proceedings.</li>
 *     <li>Complexity: used in computations, to avoid expanding expressions beyond reasonable limits</li>
 * </ol>
 * Each element can
 * <ul>
 *     <li>be printed, using the <code>print(Qualification)</code> method</li>
 *     <li>be visited recursively, using the <code>visitor</code> methods and the {@link Visitor} interface</li>
 *     <li>be cast, using two dedicated methods that allow one to override the type system (for wrappers)</li>
 *     <li>have its variables listed</li>
 *     <li>have its types referenced listed, as an efficient system to compute import statements</li>
 * </ul>
 */
@ImmutableContainer
public interface Element {
    int complexity();

    @NotNull
    List<Comment> comments();

    Element rewire(InfoMap infoMap);

    default List<Comment> rewireComments(InfoMap infoMap) {
        return comments().stream().map(c -> c.rewire(infoMap)).toList();
    }

    default List<Comment> translateComments(TranslationMap translationMap) {
        List<Comment> comments = comments();
        if (comments.isEmpty()) return comments;
        return comments.stream().map(c -> c.translate(translationMap)).collect(translationMap.toList(comments));
    }

    default List<AnnotationExpression> translateAnnotations(TranslationMap translationMap) {
        List<AnnotationExpression> annotations = annotations();
        return annotations.stream().map(a -> (AnnotationExpression) a.translate(translationMap))
                .collect(translationMap.toList(annotations));
    }

    @NotNull
    Source source();

    void visit(@NotNull Predicate<Element> predicate);

    /**
     * this variant can go inside the variable hierarchy as well
     *
     * @param visitor the visitor object that's being called recursively
     */
    void visit(@NotNull @IgnoreModifications Visitor visitor);

    @NotNull
    OutputBuilder print(Qualification qualification);

    @SuppressWarnings("unchecked")
    default <T extends Element> T asInstanceOf(Class<T> clazz) {
        if (clazz.isAssignableFrom(getClass())) {
            return (T) this;
        }
        return null;
    }

    default boolean isInstanceOf(Class<? extends Element> clazz) {
        return clazz.isAssignableFrom(getClass());
    }

    /**
     * Construct a stream of all variables occurring in this element.
     *
     * @param descendMode null for the default descend mode "YES"
     * @return a stream of all variables in this element; variables may occur multiple times.
     * The order is based on the normal recursive descend
     */
    @NotNull
    Stream<Variable> variables(DescendMode descendMode);

    @NotNull
    Stream<Variable> variableStreamDoNotDescend();

    @NotNull
    Stream<Variable> variableStreamDescend();

    interface TypeReference {
        /**
         * @return true if the reference to the type is explicit, i.e., it must appear in an import statement
         */
        boolean explicit();

        TypeInfo typeInfo();

        TypeReference withExplicit();
    }

    @NotNull
    Stream<TypeReference> typesReferenced();

    default List<AnnotationExpression> annotations() {
        return List.of();
    }

    interface Builder<B extends Builder<?>> {
        @Fluent
        B setSource(Source source);

        @Fluent
        B addComment(Comment comment);

        @Fluent
        B addComments(List<Comment> comments);

        @Fluent
        B addAnnotation(AnnotationExpression annotation);

        @Fluent
        B addAnnotations(List<AnnotationExpression> annotations);
    }

    default PropertyValueMap analysis() {
        throw new UnsupportedOperationException();
    }

}
