package org.e2immu.language.cst.impl.info.util;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.impl.info.TypeInspection;
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
    private final Map<String, Object> byName = new HashMap<>();

    @SuppressWarnings("unchecked")
    public MethodMapImpl(List<MethodInfo> methods) {
        for (MethodInfo methodInfo : methods) {
            String name = methodInfo.name();
            Object prev = byName.get(name);
            if (prev instanceof MethodInfo prevMi) {
                Map<Integer, Object> byParams = new HashMap<>();
                byParams.put(prevMi.parameters().size(), prevMi);
                byName.put(name, byParams);
                addToNumParam(byParams, methodInfo);
            } else if (prev instanceof Map numParamMap) {
                addToNumParam((Map<Integer, Object>) numParamMap, methodInfo);
            } else {
                assert prev == null;
                byName.put(name, methodInfo);
            }
        }
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
    @SuppressWarnings("unchecked")
    public MethodInfo get(String name, int numParams, Supplier<String> paramFqnCsv) {
        Object o = byName.get(name);
        if (o instanceof MethodInfo mi) {
            assert mi.parameters().size() == numParams;
            return mi;
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
        throw new NoSuchElementException("name: " + name + ", num params: " + numParams + ", paramsCsv: "
                                         + (paramFqnCsv == null ? "<no supplier>" : paramFqnCsv.get()));
    }
}
