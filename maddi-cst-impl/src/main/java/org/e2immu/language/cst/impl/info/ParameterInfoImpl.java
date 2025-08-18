package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;
import org.e2immu.language.cst.impl.variable.DescendModeEnum;
import org.e2immu.language.cst.impl.variable.LocalVariableImpl;
import org.e2immu.support.EventuallyFinal;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ParameterInfoImpl implements ParameterInfo {
    private final int index;
    private final String name;
    private final MethodInfo methodInfo;
    private final ParameterizedType parameterizedType;
    private final EventuallyFinal<ParameterInspection> inspection;
    private final PropertyValueMap analysis = new PropertyValueMapImpl();

    public ParameterInfoImpl(MethodInfo methodInfo, int index, String name, ParameterizedType parameterizedType) {
        this.methodInfo = methodInfo;
        this.index = index;
        this.name = name;
        this.parameterizedType = parameterizedType;
        inspection = new EventuallyFinal<>();
        inspection.setVariable(new ParameterInspectionImpl.Builder(this));
    }

    @Override
    public boolean isUnnamed() {
        return LocalVariableImpl.UNNAMED.equals(name);
    }

    @Override
    public String info() {
        return "parameter";
    }

    @Override
    public TypeInfo typeInfo() {
        return methodInfo.typeInfo();
    }

    @Override
    public boolean hasBeenAnalyzed() {
        return methodInfo.hasBeenAnalyzed();
    }

    @Override
    public Access access() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompilationUnit compilationUnit() {
        return methodInfo.compilationUnit();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterInfoImpl that)) return false;
        return index == that.index && Objects.equals(methodInfo, that.methodInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, methodInfo);
    }

    @Override
    public int compareTo(Variable o) {
        if (o instanceof ParameterInfoImpl pi && methodInfo == pi.methodInfo) return index - pi.index;
        return fullyQualifiedName().compareTo(o.fullyQualifiedName());
    }

    @Override
    public Builder builder() {
        if (inspection.isVariable()) return (ParameterInfo.Builder) inspection.get();
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return fullyQualifiedName();
    }

    public boolean hasBeenCommitted() {
        return inspection.isFinal();
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isVarArgs() {
        return inspection.get().isVarArgs();
    }

    @Override
    public boolean isFinal() {
        return inspection.get().isFinal();
    }

    @Override
    public String fullyQualifiedName() {
        return methodInfo.fullyQualifiedName() + ":" + index + ":" + name;
    }

    @Override
    public String simpleName() {
        return name;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public int complexity() {
        return 2;
    }

    @Override
    public List<Comment> comments() {
        return inspection.get().comments();
    }

    @Override
    public Source source() {
        return inspection.get().source();
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
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(new TextImpl(simpleName()));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.of(this);
    }

    @Override
    public Stream<TypeReference> typesReferenced() {
        return parameterizedType.typesReferencedMadeExplicit();
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
    public PropertyValueMap analysis() {
        return analysis;
    }

    @Override
    public boolean isUnmodified() {
        return analysis.getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public boolean isIgnoreModifications() {
        return analysis.getOrDefault(PropertyImpl.IGNORE_MODIFICATIONS_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public Value.AssignedToField assignedToField() {
        return analysis.getOrDefault(PropertyImpl.PARAMETER_ASSIGNED_TO_FIELD, ValueImpl.AssignedToFieldImpl.EMPTY);
    }

    public void commit(ParameterInspection pi) {
        inspection.setFinal(pi);
    }

    @Override
    public boolean isSynthetic() {
        return inspection.get().isSynthetic();
    }

    @Override
    public MethodInfo methodInfo() {
        return methodInfo;
    }

    @Override
    public List<AnnotationExpression> annotations() {
        return inspection.get().annotations();
    }

    @Override
    public List<ParameterInfo> translate(TranslationMap translationMap) {
        throw new UnsupportedOperationException("because of the back-link, translation takes place in MethodImpl");
    }

    @Override
    public Variable rewire(InfoMap infoMap) {
        return infoMap.parameterInfo(this);
    }

    @Override
    public JavaDoc javaDoc() {
        return null; // parameters have no JavaDoc
    }
}
