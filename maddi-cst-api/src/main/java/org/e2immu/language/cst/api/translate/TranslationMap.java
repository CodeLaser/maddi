package org.e2immu.language.cst.api.translate;

import org.e2immu.annotation.Fluent;
import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public interface TranslationMap {

    /*
    The method body is translated 2x during the translation of a whole method, see MethodInfoImpl.translate(..).
    The result of the first iteration is discarded except for the knowledge whether translation is needed or not.
    The following two methods mark such a "test" translation.
     */
    default void methodTranslationInfo(MethodInfo methodInfo, boolean startEnd, boolean test) {
    }

    @NotNull
    default FieldInfo translateFieldInfo(FieldInfo fieldInfo) {
        return fieldInfo;
    }

    @NotNull
    default Expression translateExpression(Expression expression) {
        return expression;
    }

    @NotNull
    default List<MethodInfo> translateMethod(MethodInfo methodInfo) {
        return List.of(methodInfo);
    }

    @NotNull
    default List<FieldInfo> translateField(FieldInfo fieldInfo) {
        return List.of(fieldInfo);
    }

    // contrary to methods and fields, you can always add extra types as subtypes; that's why we're not returning a list
    @NotNull
    default TypeInfo translateTypeInfo(TypeInfo typeInfo) {
        return typeInfo;
    }

    @NotNull
    default Variable translateVariable(Variable variable) {
        return variable;
    }

    /*
    You must implement this method. If you want the default behavior, use

    @Override
    public Variable translateVariableRecursively(Variable variable) {
        return runtime.translateVariableRecursively(this, variable);
    }

    which calls the code of TranslationMapImpl.translateVariableRecursively()
     */
    @NotNull
    Variable translateVariableRecursively(Variable variable);

    @NotNull(content = true)
    default List<Statement> translateStatement(Statement statement) {
        return List.of(statement);
    }

    @NotNull
    default ParameterizedType translateType(ParameterizedType parameterizedType) {
        return parameterizedType;
    }

    default boolean isEmpty() {
        return true;
    }

    /*
     because equality of delayed variables is based on ==
     */
    default Expression translateVariableExpressionNullIfNotTranslated(Variable variable) {
        return null;
    }

    default boolean hasVariableTranslations() {
        return false;
    }

    default boolean expandDelayedWrappedExpressions() {
        return false;
    }

    default boolean translateYieldIntoReturn() {
        return false;
    }

    default Map<? extends Variable, ? extends Variable> variables() {
        return Map.of();
    }

    default Map<? extends Expression, ? extends Expression> expressions() {
        return Map.of();
    }

    default Map<? extends Variable, ? extends Expression> variableExpressions() {
        return Map.of();
    }

    default Map<MethodInfo, List<MethodInfo>> methods() {
        return Map.of();
    }

    default Map<? extends Statement, List<Statement>> statements() {
        return Map.of();
    }

    default Map<ParameterizedType, ParameterizedType> types() {
        return Map.of();
    }

    default boolean isClearAnalysis() {
        return true;
    }

    default Map<FieldInfo, FieldInfo> fieldInfoMap() {
        return Map.of();
    }

    /*
        used by CM
         */
    default boolean translateAgain() {
        return false;
    }

    default TranslationMap delegate() {
        return null;
    }

    interface ModificationTimesHandler {
        String modificationTimes(MethodCall beforeTranslation,
                                 Expression translatedObject, List<Expression> translatedParameters);
    }

    /*
    Note: to avoid cyclic type dependencies, the first parameter takes 'Expression' rather than 'MethodCall'
     */
    default String modificationTimes(Expression methodCallBeforeTranslation,
                                     Expression translatedObject,
                                     List<Expression> translatedParameters) {
        return null;
    }

    default <T> Collector<T, List<T>, List<T>> toList(List<T> original) {
        return staticToList(original);
    }

    // used by ParameterizedTypeImpl
    static <T> Collector<T, List<T>, List<T>> staticToList(List<T> original) {
        return new Collector<>() {
            boolean changes;

            @Override
            public Supplier<List<T>> supplier() {
                return () -> new ArrayList<T>(original.size());
            }

            @Override
            public BiConsumer<List<T>, T> accumulator() {
                return (list, t) -> {
                    if (list.size() < original.size()) {
                        T inOriginal = original.get(list.size());
                        changes |= inOriginal != t;
                    } else {
                        changes = true;
                    }
                    list.add(t);
                };
            }

            @Override
            public BinaryOperator<List<T>> combiner() {
                return (l1, l2) -> {
                    throw new UnsupportedOperationException("Combiner not implemented");
                };
            }

            @Override
            public Function<List<T>, List<T>> finisher() {
                // we also test for different size: this allows for the removal of objects outside a strict
                // Translation setting (see ParameterizedType.replaceTypeBounds)
                return list -> changes || list.size() != original.size() ? List.copyOf(list) : original;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of();
            }
        };
    }

    default <T> Collector<T, Set<T>, Set<T>> toSet(Set<T> original) {
        return staticToSet(original);
    }

    static <T> Collector<T, Set<T>, Set<T>> staticToSet(Set<T> original) {
        return new Collector<>() {
            boolean changes;

            @Override
            public Supplier<Set<T>> supplier() {
                return () -> new HashSet<>(original.size());
            }

            @Override
            public BiConsumer<Set<T>, T> accumulator() {
                return (set, t) -> {
                    boolean inOriginal = original.contains(t);
                    changes |= !inOriginal;
                    set.add(t);
                };
            }

            @Override
            public BinaryOperator<Set<T>> combiner() {
                return (l1, l2) -> {
                    throw new UnsupportedOperationException("Combiner not implemented");
                };
            }

            @Override
            public Function<Set<T>, Set<T>> finisher() {
                return set -> changes || set.size() != original.size() ? Set.copyOf(set) : original;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of();
            }
        };
    }

    default <K, V> Collector<Map.Entry<K, V>, Map<K, V>, Map<K, V>> toMap(Map<K, V> original) {
        return staticToMap(original);
    }

    static <K, V> Collector<Map.Entry<K, V>, Map<K, V>, Map<K, V>> staticToMap(Map<K, V> original) {
        return new Collector<>() {
            boolean changes;

            @Override
            public Supplier<Map<K, V>> supplier() {
                return () -> new HashMap<>(original.size());
            }

            @Override
            public BiConsumer<Map<K, V>, Map.Entry<K, V>> accumulator() {
                return (map, entry) -> {
                    K key = entry.getKey();
                    V inOriginal = original.get(key);
                    V value = entry.getValue();
                    changes |= !Objects.equals(inOriginal, value);
                    map.put(key, value);
                };
            }

            @Override
            public BinaryOperator<Map<K, V>> combiner() {
                return (l1, l2) -> {
                    throw new UnsupportedOperationException("Combiner not implemented");
                };
            }

            @Override
            public Function<Map<K, V>, Map<K, V>> finisher() {
                return map -> changes || map.size() != original.size() ? Map.copyOf(map) : original;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of();
            }
        };
    }

    interface Builder {
        TranslationMap build();

        @Fluent
        Builder setTranslateAgain(boolean translateAgain);

        Builder put(TypeParameter template, TypeParameter actual);

        Builder put(Statement template, Statement actual);

        Builder put(MethodInfo template, List<MethodInfo> actual);

        Builder put(Statement template, List<Statement> statements);

        Builder put(Expression template, Expression actual);

        Builder put(FieldInfo template, FieldInfo actual);

        Builder addVariableExpression(Variable variable, Expression actual);

        Builder renameVariable(Variable variable, Expression actual);

        Builder put(ParameterizedType template, ParameterizedType actual);

        Builder put(Variable template, Variable actual);

        Builder setYieldToReturn(boolean b);

        Builder setExpandDelayedWrapperExpressions(boolean expandDelayedWrappedExpressions);

        boolean translateMethod(MethodInfo methodInfo);

        Builder setModificationTimesHandler(ModificationTimesHandler modificationTimesHandler);

        Builder setClearAnalysis(boolean clearAnalysis);

        Builder setDelegate(TranslationMap delegate);

        boolean isEmpty();
    }
}
