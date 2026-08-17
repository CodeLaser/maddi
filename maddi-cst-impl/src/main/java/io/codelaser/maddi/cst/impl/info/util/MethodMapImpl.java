package io.codelaser.maddi.cst.impl.info.util;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.impl.info.TypeInspection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Index of a type's methods by name → parameter count → erased-parameter FQNs → method. A fourth level,
 * keyed by the erased <em>return</em> type, is inserted only when two methods share the same erased parameters
 * but differ by return type. That is illegal in Java (so Java never reaches it), but legal at the JVM bytecode
 * level and used by Kotlin's inline numeric specializations (e.g. {@code maxOf((T)->Double):Double} vs
 * {@code maxOf((T)->Float):Float} vs {@code maxOf((T)->R):R}, all erasing to {@code maxOf(Object[],Function1)}).
 */
public class MethodMapImpl implements TypeInspection.MethodMap {
    private final Map<String, Object> byName;

    @SuppressWarnings("unchecked")
    public MethodMapImpl(List<MethodInfo> methods) {
        // accumulate locally and commit once: filling a final field with put() leaves it a mutable
        // container ever after -- part-of-construction excuses the assignment, not the content calls
        // (the FactoryImpl.precedenceMap finding, docs/eventual-design-improvements.md)
        Map<String, Object> accumulator = new HashMap<>();
        for (MethodInfo methodInfo : methods) {
            String name = methodInfo.name();
            Object prev = accumulator.get(name);
            if (prev instanceof MethodInfo prevMi) {
                Map<Integer, Object> byParams = new HashMap<>();
                byParams.put(prevMi.parameters().size(), prevMi);
                accumulator.put(name, byParams);
                addToNumParam(byParams, methodInfo);
            } else if (prev instanceof Map numParamMap) {
                addToNumParam((Map<Integer, Object>) numParamMap, methodInfo);
            } else {
                assert prev == null;
                accumulator.put(name, methodInfo);
            }
        }
        this.byName = Map.copyOf(accumulator);
    }

    @SuppressWarnings("unchecked")
    private static void addToNumParam(Map<Integer, Object> map, MethodInfo methodInfo) {
        int n = methodInfo.parameters().size();
        Object prev = map.get(n);
        if (prev instanceof MethodInfo prevMi) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(key(prevMi), prevMi);
            map.put(n, paramMap);
            addToParams(paramMap, methodInfo);
        } else if (prev instanceof Map paramMap) {
            addToParams((Map<String, Object>) paramMap, methodInfo);
        } else {
            assert prev == null;
            map.put(n, methodInfo);
        }
    }

    @SuppressWarnings("unchecked")
    private static void addToParams(Map<String, Object> map, MethodInfo methodInfo) {
        String key = key(methodInfo);
        Object prev = map.get(key);
        if (prev == null) {
            map.put(key, methodInfo);
        } else if (prev instanceof MethodInfo prevMi) {
            // identical erased params, so (necessarily) different return types -- legal on the JVM, used by
            // Kotlin inline numeric specializations. Java never reaches here. Nest by return type.
            Map<String, MethodInfo> byReturn = new HashMap<>();
            byReturn.put(returnKey(prevMi), prevMi);
            addToReturn(byReturn, methodInfo);
            map.put(key, byReturn);
        } else {
            addToReturn((Map<String, MethodInfo>) prev, methodInfo);
        }
    }

    private static void addToReturn(Map<String, MethodInfo> map, MethodInfo methodInfo) {
        MethodInfo prev = map.put(returnKey(methodInfo), methodInfo);
        assert prev == null : "Two methods with the same FQN and return type? " + prev + " vs " + methodInfo;
    }

    private static @NotNull String key(MethodInfo methodInfo) {
        return methodInfo.parameters().stream()
                .map(pi -> pi.parameterizedType().erasedForFQN().fullyQualifiedName())
                .collect(Collectors.joining(","));
    }

    private static @NotNull String returnKey(MethodInfo methodInfo) {
        return methodInfo.returnType().erasedForFQN().fullyQualifiedName();
    }

    @Override
    public MethodInfo get(String name, int numParams, Supplier<String> paramFqnCsv) {
        MethodInfo methodInfo = getOrNull(name, numParams, paramFqnCsv);
        if (methodInfo == null) {
            throw new NoSuchElementException("name: " + name + ", num params: " + numParams + ", paramsCsv: "
                                             + (paramFqnCsv == null ? "<no supplier>" : paramFqnCsv.get()));
        }
        return methodInfo;
    }

    /**
     * ⛔ <b>THE ONE-METHOD SHORTCUT USED TO IGNORE THE PARAMETER COUNT, and an {@code assert} is not a
     * check.</b> With a single method under a name it returned that method for <em>every</em> arity: a test
     * JVM ({@code -ea}) got an {@code AssertionError}, and a production run got a method it had not asked
     * for — the silent half being the worse one. Arity is part of a method's identity; a different one is a
     * MISS, which this method is allowed to say.
     * <p>
     * Measured on timefold-solver: <b>99 of 100</b> remaining dropped compilation units were this assertion,
     * every one of them a class-file load of a method the type does not declare — for which the loader's own
     * contract ({@code ClassSymbolScanner.getMethod} → null → {@code getOrLoadMethod} → load it) is exactly
     * what the no-method-map branch beside it does.
     */
    @Override
    @SuppressWarnings("unchecked")
    public MethodInfo getOrNull(String name, int numParams, Supplier<String> paramFqnCsv) {
        Object o = byName.get(name);
        if (o instanceof MethodInfo mi) {
            return mi.parameters().size() == numParams ? mi : null;
        }
        if (o instanceof Map numParamMap) {
            Object o2 = ((Map<Integer, Object>) numParamMap).get(numParams);
            if (o2 instanceof MethodInfo mi) {
                return mi;
            }
            if (o2 instanceof Map paramMap) {
                Object o3 = ((Map<String, Object>) paramMap).get(paramFqnCsv.get());
                if (o3 instanceof MethodInfo mi) {
                    return mi;
                }
                if (o3 instanceof Map byReturn) {
                    // return-type-differentiated overloads: this params-only lookup cannot pick one, so
                    // return any -- the Kotlin resolver disambiguates by return type where it matters
                    return ((Map<String, MethodInfo>) byReturn).values().iterator().next();
                }
            }
        }
        return null;
    }
}
