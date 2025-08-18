package org.e2immu.analyzer.modification.prepwork.variable.impl;

import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class ReturnVariableImpl implements ReturnVariable {
    public final ParameterizedType returnType;
    public final String simpleName;
    public final String fqn;
    private final MethodInfo methodInfo;

    public ReturnVariableImpl(MethodInfo methodInfo) {
        this.returnType = methodInfo.returnType();
        simpleName = methodInfo.name();
        fqn = methodInfo.fullyQualifiedName();
        this.methodInfo = methodInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReturnVariable that = (ReturnVariable) o;
        return returnType.equals(that.parameterizedType()) &&
               fqn.equals(that.fullyQualifiedName());
    }

    @Override
    public MethodInfo methodInfo() {
        return methodInfo;
    }

    public TypeInfo getOwningType() {
        return methodInfo.typeInfo();
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, fqn);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return returnType;
    }

    @Override
    public String simpleName() {
        return "return " + simpleName;
    }

    @Override
    public String fullyQualifiedName() {
        return fqn;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(KeywordImpl.RETURN).add(SpaceEnum.ONE).add(new TextImpl(simpleName));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.of(this);
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        return Stream.of(this);
    }

    @Override
    public Stream<Variable> variableStreamDescend() {
        return Stream.of(this);
    }

    @Override
    public Stream<TypeReference> typesReferenced() {
        return Stream.of(new ElementImpl.TypeReference(parameterizedType().typeInfo(), false));
    }

    @Override
    public String toString() {
        return simpleName;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    @Override
    public int complexity() {
        return 1;
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
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeVariable(this);
        visitor.afterVariable(this);
    }

    @Override
    public Variable rewire(InfoMap infoMap) {
        return new ReturnVariableImpl(infoMap.methodInfo(methodInfo));
    }
}
