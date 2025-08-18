package org.e2immu.parser.java.erasure;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.Objects;
import java.util.Set;

public class ConstructorCallErasure extends ErasureExpressionImpl {
    private final ParameterizedType formalType;

    public ConstructorCallErasure(Runtime runtime, Source source, ParameterizedType formalType) {
        super(runtime, source);
        this.formalType = formalType;
    }

    @Override
    public Expression withSource(Source source) {
        return new ConstructorCallErasure(runtime, source, formalType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConstructorCallErasure that)) return false;
        return Objects.equals(formalType, that.formalType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(formalType);
    }

    @Override
    public Set<ParameterizedType> erasureTypes() {
        return Set.of(formalType);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return formalType;
    }

    @Override
    public Precedence precedence() {
        return runtime.precedenceTop();
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return runtime.newOutputBuilder().add(runtime.newText("<constructor call erasure, type " + formalType + ">"));
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        throw new UnsupportedOperationException();
    }
}
