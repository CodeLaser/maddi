package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.Map;

public class FieldTranslationMap implements TranslationMap {
    private record Wrapped(String name, ParameterizedType type) {
    }

    private static Wrapped of(FieldInfo fieldInfo) {
        return new Wrapped(fieldInfo.name(), fieldInfo.type().withWildcard(null));
    }

    private final Runtime runtime;
    private final Map<Wrapped, FieldInfo> fieldInfoMap = new HashMap<>();

    public FieldTranslationMap(Runtime runtime) {
        this.runtime = runtime;
    }

    public void put(FieldInfo in, FieldInfo out) {
        this.fieldInfoMap.put(of(in), out);
    }

    @Override
    public FieldInfo translateFieldInfo(FieldInfo fieldInfo) {
        FieldInfo translated = fieldInfoMap.get(of(fieldInfo));
        return translated == null ? fieldInfo : translated;
    }

    @Override
    public Variable translateVariableRecursively(Variable variable) {
        return runtime.translateVariableRecursively(this, variable);
    }

    @Override
    public boolean isEmpty() {
        return fieldInfoMap.isEmpty();
    }
}
