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

package io.codelaser.maddi.cst.impl.statement;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.*;
import io.codelaser.maddi.cst.impl.type.DiamondEnum;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LocalVariableCreationImpl extends StatementImpl implements LocalVariableCreation {

    private final LocalVariable localVariable;
    private final List<LocalVariable> otherLocalVariables;
    private final Set<Modifier> modifiers;

    public enum ModifierEnum implements Modifier {
        FiNAL, VAR;

        @Override
        public boolean isFinal() {
            return this == FiNAL;
        }

        @Override
        public boolean isWithoutTypeSpecification() {
            return this == VAR;
        }

    }

    public LocalVariableCreationImpl(LocalVariable localVariable) {
        this.localVariable = localVariable;
        assert localVariable != null && localVariable.assignmentExpression() != null;
        this.modifiers = Set.of();
        this.otherLocalVariables = List.of();
    }

    public LocalVariableCreationImpl(List<Comment> comments,
                                     Source source,
                                     List<AnnotationExpression> annotations,
                                     String label,
                                     LocalVariable localVariable,
                                     List<LocalVariable> otherLocalVariables,
                                     Set<Modifier> modifiers) {
        super(comments, source, annotations, 0, label);
        assert localVariable.assignmentExpression() != null;
        this.localVariable = localVariable;
        // every 'other' variable has an initializer. They usually share the base type (Java `int a, b`), but a
        // Kotlin destructuring `val (a, b) = pair` binds independently-typed components (component1()/component2())
        // to a single creation, so equal base types are NOT required here -- each variable carries its own type.
        assert otherLocalVariables.stream().allMatch(lv -> lv.assignmentExpression() != null);
        this.otherLocalVariables = otherLocalVariables;
        this.modifiers = modifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalVariableCreationImpl that)) return false;
        return Objects.equals(localVariable, that.localVariable)
               && Objects.equals(otherLocalVariables, that.otherLocalVariables)
               && Objects.equals(modifiers, that.modifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localVariable, otherLocalVariables, modifiers);
    }

    public static class Builder extends StatementImpl.Builder<LocalVariableCreation.Builder>
            implements LocalVariableCreation.Builder {
        private final Set<Modifier> modifiers = new HashSet<>();
        private LocalVariable localVariable;
        private final List<LocalVariable> otherLocalVariables = new ArrayList<>();

        @Override
        public LocalVariableCreation.Builder addModifier(Modifier modifier) {
            modifiers.add(modifier);
            return this;
        }

        @Override
        public LocalVariableCreation.Builder setLocalVariable(LocalVariable localVariable) {
            this.localVariable = localVariable;
            return this;
        }

        @Override
        public LocalVariableCreation.Builder addOtherLocalVariable(LocalVariable localVariable) {
            otherLocalVariables.add(localVariable);
            return this;
        }

        @Override
        public LocalVariableCreation build() {
            return new LocalVariableCreationImpl(comments, source, annotations, label, localVariable,
                    List.copyOf(otherLocalVariables), Set.copyOf(modifiers));
        }
    }

    @Override
    public Set<Modifier> modifiers() {
        return modifiers;
    }

    @Override
    public boolean isVar() {
        return modifiers.stream().anyMatch(Modifier::isWithoutTypeSpecification);
    }

    @Override
    public boolean isFinal() {
        return modifiers.stream().anyMatch(Modifier::isFinal);
    }

    @Override
    public LocalVariable localVariable() {
        return localVariable;
    }

    @Override
    public List<LocalVariable> otherLocalVariables() {
        return otherLocalVariables;
    }

    @Override
    public Stream<LocalVariable> localVariableStream() {
        return Stream.concat(Stream.of(localVariable), otherLocalVariables.stream());
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            localVariable.assignmentExpression().visit(predicate);
            for (LocalVariable lv : otherLocalVariables) {
                lv.assignmentExpression().visit(predicate);
            }
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeStatement(this)) {
            localVariable.visit(visitor);
            for (LocalVariable lv : otherLocalVariables) {
                lv.visit(visitor);
            }
        }
        visitor.afterStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        // annotations
        OutputBuilder outputBuilder = outputBuilder(qualification);

        // modifiers, in the correct order!
        boolean isFinal = modifiers.stream().anyMatch(Modifier::isFinal);
        if (isFinal) {
            outputBuilder.add(KeywordImpl.FINAL).add(SpaceEnum.ONE);
        }
        boolean isVar = modifiers.stream().anyMatch(Modifier::isWithoutTypeSpecification);

        // var or type
        if (isVar) {
            outputBuilder.add(KeywordImpl.VAR);
        } else {
            outputBuilder.add(localVariable.parameterizedType()
                    .print(qualification, false, DiamondEnum.SHOW_ALL));
        }

        // declarations
        outputBuilder.add(SpaceEnum.ONE);
        OutputBuilder first = new OutputBuilderImpl().add(new TextImpl(localVariable.simpleName()));
        if (!localVariable.assignmentExpression().isEmpty()) {
            first.add(SymbolEnum.assignment("=")).add(localVariable.assignmentExpression().print(qualification));
        }
        ParameterizedType base = localVariable().parameterizedType();
        Stream<OutputBuilder> rest = otherLocalVariables.stream().map(d -> {
            OutputBuilder ob = new OutputBuilderImpl().add(new TextImpl(d.simpleName()));
            // old-style array declarations, see TestParseArray,3C
            if (d.parameterizedType().arrays() != base.arrays()) {
                for (int i = 0; i < d.parameterizedType().arrays(); i++) {
                    ob.add(SymbolEnum.OPEN_CLOSE_BRACKETS);
                }
            }
            if (!d.assignmentExpression().isEmpty()) {
                ob.add(SymbolEnum.assignment("="))
                        .add(d.assignmentExpression().print(qualification));
            }
            return ob;
        });
        outputBuilder.add(Stream.concat(Stream.of(first), rest).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
        return outputBuilder.add(SymbolEnum.SEMICOLON);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(localVariable.assignmentExpression().variables(descendMode),
                otherLocalVariables.stream().flatMap(lv -> lv.assignmentExpression().variables(descendMode)));
    }

    /*
    The declaration's own annotations must be streamed: a local variable declaration is the one place where an
    annotation type can be the ONLY reason its import exists, and omitting them made an import-removal tool delete
    a live import. Compare ParameterInfoImpl.explicitTypesReferenced, which concatenates annotations() for exactly
    this reason; the annotations live on the LocalVariableCreation (see translate(), which translates them), not on
    the LocalVariable, which has no annotations() at all.
     */
    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        // A `var` declaration's type is INFERRED, not written, and TypeReference's own contract makes that the
        // difference between the two natures: IMPLICIT means typeToImport() is null, i.e. this reference earns no
        // import. Reporting an inferred type as EXPLICIT made every consumer believe the name appears in the source.
        // Measured consequence in a downstream refactoring tool: a move's import reconciler ADDED `import c.Widget`
        // to a file whose only mention of Widget is `var w = Factory.create()` -- an import that is dead the moment
        // it is written. On a large open-source corpus that was its biggest single style-gate family, 942 unused
        // imports in one run.
        // ⚠ Only the DECLARED type turns implicit. The initialiser is walked as before, so `var w = new Widget()`
        // still reports Widget -- written there, and it does earn the import.
        TypeReferenceNature declaredTypeNature = isVar() ? TypeReferenceNature.IMPLICIT : TypeReferenceNature.EXPLICIT;
        Stream<Element.TypeReference> trStream = localVariable.parameterizedType()
                .typesReferenced(declaredTypeNature, source() == null ? null : source().detailedSources());
        Stream<Element.TypeReference> fromAnnotations = annotations().stream()
                .flatMap(annotationExpression -> annotationExpression.typesReferenced(predicate));
        return Stream.concat(fromAnnotations,
                Stream.concat(trStream, Stream.concat(localVariable.assignmentExpression().typesReferenced(predicate),
                        otherLocalVariables.stream().flatMap(lv -> lv.assignmentExpression().typesReferenced(predicate)))));
    }

    @Override
    public boolean hasSubBlocks() {
        return false;
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;
        LocalVariable tlv = localVariable.translate(translationMap);
        List<LocalVariable> tList = otherLocalVariables.stream()
                .map(lv -> lv.translate(translationMap)).collect(translationMap.toList(otherLocalVariables));
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        if (tlv != localVariable || tList != otherLocalVariables
            || translationMap.isClearAnalysis() && !analysis().isEmpty()
            || tAnnotations != annotations()) {
            LocalVariableCreationImpl newLvc = new LocalVariableCreationImpl(comments(), source(), tAnnotations,
                    label(), tlv, tList, modifiers);
            if (!translationMap.isClearAnalysis()) newLvc.analysis().setAll(analysis());
            return translationMap.postTranslationHandler(this, List.of(newLvc));
        }
        return List.of(this);
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return this;// no blocks
    }

    @Override
    public LocalVariableCreation withSource(Source newSource) {
        return new LocalVariableCreationImpl(comments(), newSource, annotations(), label(), localVariable,
                otherLocalVariables, modifiers);
    }

    @Override
    public LocalVariableCreation withAdditionalLocalVariable(LocalVariableCreation singleLvc) {
        // the merged statement must span from the first declarator up to and including the newly added one;
        // keeping only source() would truncate the statement's source at the first declarator's comma.
        Source mergedDetails = source().mergeDetailedSources(singleLvc.source().detailedSources());
        Source spanned = source().max(singleLvc.source())
                .withIndex(source().index())
                .withDetailedSources(mergedDetails.detailedSources());
        return new LocalVariableCreationImpl(comments(), spanned, annotations(), label(), localVariable,
                Stream.concat(otherLocalVariables.stream(), Stream.of(singleLvc.localVariable())).toList(), modifiers);
    }

    @Override
    public Statement rewire(InfoMapView infoMap) {
        return new LocalVariableCreationImpl(rewireComments(infoMap), source(), rewireAnnotations(infoMap), label(),
                (LocalVariable) localVariable.rewire(infoMap),
                otherLocalVariables.stream().map(lv -> (LocalVariable) lv.rewire(infoMap)).toList(),
                modifiers);
    }
}
