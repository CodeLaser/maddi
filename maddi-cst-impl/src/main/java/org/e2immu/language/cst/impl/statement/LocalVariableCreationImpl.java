package org.e2immu.language.cst.impl.statement;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.type.DiamondEnum;

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
        ParameterizedType baseType = localVariable.parameterizedType().copyWithoutArrays();
        assert otherLocalVariables.stream()
                .allMatch(lv -> lv.assignmentExpression() != null
                                && lv.parameterizedType().copyWithoutArrays().equals(baseType));
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

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        Stream<Element.TypeReference> trStream = localVariable.parameterizedType().typesReferencedMadeExplicit();
        return Stream.concat(trStream, Stream.concat(localVariable.assignmentExpression().typesReferenced(),
                otherLocalVariables.stream().flatMap(lv -> lv.assignmentExpression().typesReferenced())));
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
            return List.of(newLvc);
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
    public Statement rewire(InfoMap infoMap) {
        return new LocalVariableCreationImpl(rewireComments(infoMap), source(), rewireAnnotations(infoMap), label(),
                (LocalVariable) localVariable.rewire(infoMap),
                otherLocalVariables.stream().map(lv -> (LocalVariable) lv.rewire(infoMap)).toList(),
                modifiers);
    }
}
