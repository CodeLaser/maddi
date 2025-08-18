package org.e2immu.parser.java.erasure;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.MethodResolution;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class LambdaErasure extends ErasureExpressionImpl {
    private final Set<MethodResolution.Count> counts;

    public LambdaErasure(Runtime runtime, Set<MethodResolution.Count> counts, Source source) {
        super(runtime, source);
        Objects.requireNonNull(counts);
        Objects.requireNonNull(source);
        this.counts = counts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LambdaErasure that)) return false;
        return Objects.equals(counts, that.counts) && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(counts, source);
    }

    @Override
    public Expression withSource(Source source) {
        return new LambdaErasure(runtime, counts, source);
    }

    @Override
    public Set<ParameterizedType> erasureTypes() {
        return counts.stream()
                .map(count -> runtime.syntheticFunctionalType(count.parameters(), !count.isVoid()))
                .map(NamedType::asParameterizedType)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Precedence precedence() {
        return runtime.precedenceBottom();
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return runtime.newOutputBuilder().add(runtime.newText("<Lambda Erasure at " + source + ": " + counts + ">"));
    }

    public Set<MethodResolution.Count> counts() {
        return counts;
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        throw new UnsupportedOperationException();
    }
}
