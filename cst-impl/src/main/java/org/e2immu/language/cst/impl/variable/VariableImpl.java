package org.e2immu.language.cst.impl.variable;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.QualificationImpl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class VariableImpl implements Variable {

    private final ParameterizedType parameterizedType;

    public VariableImpl(ParameterizedType parameterizedType) {
        this.parameterizedType = parameterizedType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableImpl variable)) return false;
        return Objects.equals(fullyQualifiedName(), variable.fullyQualifiedName());
    }

    @Override
    public int hashCode() {
        return fullyQualifiedName().hashCode();
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public List<Comment> comments() {
        return List.of();
    }

    @Override
    public Source source() {
        return null;
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
        return print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString();
    }
}
