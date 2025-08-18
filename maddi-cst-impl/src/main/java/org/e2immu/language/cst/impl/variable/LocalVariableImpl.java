package org.e2immu.language.cst.impl.variable;

import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.QualifiedNameImpl;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LocalVariableImpl extends VariableImpl implements LocalVariable {
    public static final String UNNAMED = ":";

    private final Expression assignmentExpression;
    private final String name;

    public LocalVariableImpl(ParameterizedType parameterizedType, Expression assignmentExpression) {
        super(parameterizedType);
        this.name = UNNAMED;
        this.assignmentExpression = assignmentExpression;
    }

    public LocalVariableImpl(String name, ParameterizedType parameterizedType, Expression assignmentExpression) {
        super(parameterizedType);
        this.name = Objects.requireNonNull(name);
        this.assignmentExpression = assignmentExpression;
    }

    @Override
    public boolean isUnnamed() {
        return UNNAMED.equals(name);
    }

    @Override
    public LocalVariable withAssignmentExpression(Expression expression) {
        return new LocalVariableImpl(name, parameterizedType(), expression);
    }

    @Override
    public LocalVariable withName(String name) {
        return new LocalVariableImpl(name, parameterizedType(), assignmentExpression);
    }

    @Override
    public LocalVariable withType(ParameterizedType type) {
        return new LocalVariableImpl(name, type, assignmentExpression);
    }

    @Override
    public Expression assignmentExpression() {
        return assignmentExpression;
    }

    @Override
    public String simpleName() {
        return name;
    }

    @Override
    public int complexity() {
        return 2;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeVariable(this) && assignmentExpression != null) {
            assignmentExpression.visit(visitor);
        }
        visitor.afterVariable(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        String name = qualification.isFullyQualifiedNames() ? fullyQualifiedName() : simpleName();
        return new OutputBuilderImpl().add(new QualifiedNameImpl(name, null, QualifiedNameImpl.Required.NEVER));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.of(this);
    }

    @Override
    public Stream<TypeReference> typesReferenced() {
        return parameterizedType().typesReferenced();
    }

    @Override
    public LocalVariable translate(TranslationMap translationMap) {
        Variable direct = translationMap.translateVariable(this);
        if (direct != this && direct instanceof LocalVariable lv) return lv;
        Expression tex = assignmentExpression == null ? null : assignmentExpression.translate(translationMap);
        ParameterizedType type = translationMap.translateType(parameterizedType());
        if (tex != assignmentExpression || type != parameterizedType()) {
            return new LocalVariableImpl(name, type, tex);
        }
        return this;
    }

    @Override
    public Variable rewire(InfoMap infoMap) {
        return new LocalVariableImpl(name, parameterizedType().rewire(infoMap), assignmentExpression == null ? null
                : assignmentExpression.rewire(infoMap));
    }
}
