package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.type.ParameterizedTypeImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class AnnotationExpressionImpl extends ExpressionImpl implements AnnotationExpression {
    private final TypeInfo typeInfo;
    private final List<KV> keyValuePairs;

    public AnnotationExpressionImpl(List<Comment> comments, Source source, TypeInfo typeInfo, List<KV> keyValuePairs) {
        super(comments, source, 1 + keyValuePairs.stream().mapToInt(kv -> kv.value().complexity()).sum());
        this.typeInfo = typeInfo;
        this.keyValuePairs = keyValuePairs;
    }

    @Override
    public AnnotationExpression withKeyValuePair(String key, Expression value) {
        return new AnnotationExpressionImpl(comments(), source(), typeInfo,
                Stream.concat(Stream.of(new KVI(key, value)), keyValuePairs.stream()).toList());
    }

    @Override
    public Expression withSource(Source source) {
        return new AnnotationExpressionImpl(comments(), source, typeInfo, keyValuePairs);
    }

    @Override
    public TypeInfo typeInfo() {
        return typeInfo;
    }

    @Override
    public List<KV> keyValuePairs() {
        return keyValuePairs;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            keyValuePairs.forEach(kv -> kv.value().visit(predicate));
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            keyValuePairs.forEach(kv -> kv.value().visit(visitor));
        }
        visitor.afterExpression(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = new OutputBuilderImpl().add(TypeNameImpl.typeName(typeInfo,
                qualification.qualifierRequired(typeInfo), true));
        if (!keyValuePairs.isEmpty()) {
            boolean singleKvPair = keyValuePairs.size() == 1;
            outputBuilder.add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(keyValuePairs.stream()
                            .map(kv ->
                                    new OutputBuilderImpl()
                                            .addIf(!singleKvPair || !kv.keyIsDefault(), new TextImpl(kv.key()))
                                            .addIf(!singleKvPair || !kv.keyIsDefault(), SymbolEnum.assignment("="))
                                            .add(kv.value().print(qualification)))
                            .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return outputBuilder;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    public record KVI(String key, Expression value) implements AnnotationExpression.KV {
        @Override
        public boolean keyIsDefault() {
            return "value".equals(key);
        }

        @Override
        public KV translate(TranslationMap translationMap) {
            Expression tex = value.translate(translationMap);
            if (tex != value) return new KVI(key, tex);
            return this;
        }

        @Override
        public KV rewire(InfoMap infoMap) {
            return new KVI(key, value.rewire(infoMap));
        }
    }

    public static class Builder extends ElementImpl.Builder<AnnotationExpression.Builder> implements AnnotationExpression.Builder {
        private TypeInfo typeInfo;
        private List<KV> keyValuePairs;

        @Override
        public AnnotationExpression.Builder addKeyValuePair(String key, Expression value) {
            if (keyValuePairs == null) keyValuePairs = new ArrayList<>();
            keyValuePairs.add(new KVI(key, value));
            return this;
        }

        @Override
        public AnnotationExpression.Builder setKeyValuesPairs(List<KV> kvs) {
            this.keyValuePairs = kvs;
            return this;
        }

        @Override
        public AnnotationExpression.Builder setTypeInfo(TypeInfo typeInfo) {
            this.typeInfo = typeInfo;
            return this;
        }

        @Override
        public AnnotationExpression build() {
            return new AnnotationExpressionImpl(comments, source, typeInfo,
                    keyValuePairs == null ? List.of() : List.copyOf(keyValuePairs));
        }
    }

    @Override
    public boolean extractBoolean(String key) {
        return keyValuePairs.stream()
                .filter(kv -> key.equals(kv.key())).map(kv -> kv.value().isBoolValueTrue())
                .findFirst().orElse(false);
    }

    @Override
    public int[] extractIntArray(String key) {
        return keyValuePairs.stream()
                .filter(kv -> key.equals(kv.key()))
                .map(kv -> ((ArrayInitializer) kv.value()).expressions().stream()
                        .mapToInt(e -> ((IntConstant) e).constant()).toArray())
                .findFirst().orElse(null);
    }

    @Override
    public List<Float> extractFloatArray(String key) {
        return keyValuePairs.stream()
                .filter(kv -> key.equals(kv.key()))
                .map(kv -> ((ArrayInitializer) kv.value()).expressions().stream()
                        .map(e -> ((FloatConstant) e).constant()).toList())
                .findFirst().orElse(null);
    }

    @Override
    public String[] extractStringArray(String key) {
        return keyValuePairs.stream()
                .filter(kv -> key.equals(kv.key()))
                .map(kv -> ((ArrayInitializer) kv.value()).expressions().stream()
                        .map(e -> ((StringConstant) e).constant()).toArray(String[]::new))
                .findFirst().orElse(null);
    }

    @Override
    public String extractString(String key, String defaultValue) {
        return keyValuePairs.stream()
                .filter(kv -> key.equals(kv.key()))
                .map(kv -> ((StringConstant) kv.value()).constant())
                .findFirst().orElse(defaultValue);
    }



    @Override
    public TypeInfo extractTypeInfo(String key) {
        return keyValuePairs.stream()
                .filter(kv -> key.equals(kv.key()))
                .map(kv -> ((ClassExpression) kv.value()).type().typeInfo())
                .findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return print(QualificationImpl.SIMPLE_NAMES).toString();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(Stream.of(new ElementImpl.TypeReference(typeInfo, true)),
                keyValuePairs.stream().flatMap(kv -> kv.value().typesReferenced()));
    }

    @Override
    public ParameterizedType parameterizedType() {
        return ParameterizedTypeImpl.TYPE_OF_EMPTY_EXPRESSION;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.BOTTOM;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_ANNOTATION_EXPRESSION;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof AnnotationExpression ae) {
            int c = typeInfo.fullyQualifiedName().compareTo(ae.typeInfo().fullyQualifiedName());
            if (c != 0) return 0;
            return keyValuePairs().toString().compareTo(ae.keyValuePairs().toString());
        }
        throw new InternalCompareToException();
    }

    @Override
    public AnnotationExpression translate(TranslationMap translationMap) {
        Expression te = translationMap.translateExpression(this);
        if (te != this) return (AnnotationExpression) te;
        ParameterizedType pt = typeInfo.asSimpleParameterizedType();
        ParameterizedType tpt = translationMap.translateType(pt);
        List<KV> newKv = keyValuePairs.stream()
                .map(kv -> kv.translate(translationMap)).collect(translationMap.toList(keyValuePairs));
        if (pt != tpt || newKv != keyValuePairs) {
            return new AnnotationExpressionImpl(comments(), source(), tpt.typeInfo(), newKv);
        }
        return this;
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new AnnotationExpressionImpl(comments(), source(), infoMap.typeInfo(typeInfo),
                keyValuePairs.stream().map(kv -> kv.rewire(infoMap)).toList());
    }
}
