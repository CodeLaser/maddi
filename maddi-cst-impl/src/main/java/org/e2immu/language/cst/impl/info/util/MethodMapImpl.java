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

    private static void addToNumParam(Map<Integer, Object> map, MethodInfo methodInfo) {
        int n = methodInfo.parameters().size();
        Object prev = map.get(n);
        if (prev instanceof MethodInfo prevMi) {
            Map<String, MethodInfo> paramMap = new HashMap<>();
            paramMap.put(key(prevMi), prevMi);
            map.put(n, paramMap);
            addToParams(paramMap, methodInfo);
        } else if (prev instanceof Map paramMap) {
            addToParams((Map<String, MethodInfo>) paramMap, methodInfo);
        } else {
            assert prev == null;
            map.put(n, methodInfo);
        }
    }

    private static void addToParams(Map<String, MethodInfo> map, MethodInfo methodInfo) {
        String key = key(methodInfo);
        MethodInfo prev = map.put(key, methodInfo);
        assert prev == null : "Two methods with the same FQN? " + prev + " vs " + methodInfo;
    }

    private static @NotNull String key(MethodInfo methodInfo) {
        return methodInfo.parameters().stream()
                .map(pi -> pi.parameterizedType().fullyQualifiedName())
                .collect(Collectors.joining(","));
    }

    @Override
    public MethodInfo get(String name, int numParams, Supplier<String> paramFqnCsv) {
        Object o = byName.get(name);
        if (o instanceof MethodInfo mi) {
            assert mi.parameters().size() == numParams;
            return mi;
        }
        if (o instanceof Map numParamMap) {
            Object o2 = numParamMap.get(numParams);
            if (o2 instanceof MethodInfo mi) {
                return mi;
            }
            if (o2 instanceof Map paramMap) {
                return (MethodInfo) paramMap.get(paramFqnCsv.get());
            }
        }
        throw new NoSuchElementException("name: " + name + ", num params: " + numParams + ", paramsCsv: "
                                         + (paramFqnCsv == null ? "<no supplier>" : paramFqnCsv.get()));
    }
}
