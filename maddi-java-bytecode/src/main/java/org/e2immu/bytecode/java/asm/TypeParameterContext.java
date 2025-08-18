package org.e2immu.bytecode.java.asm;

import org.e2immu.language.cst.api.info.TypeParameter;

import java.util.HashMap;
import java.util.Map;

public class TypeParameterContext {
    private final Map<String, TypeParameter> map = new HashMap<>();
    private final TypeParameterContext parent;

    public TypeParameterContext() {
        this(null);
    }

    private TypeParameterContext(TypeParameterContext parent) {
        this.parent = parent;
    }

    public void add(TypeParameter typeParameter) {
        map.put(typeParameter.simpleName(), typeParameter);
    }

    public TypeParameter get(String typeParamName) {
        TypeParameter here = map.get(typeParamName);
        if (here != null || parent == null) return here;
        return parent.get(typeParamName);
    }

    public TypeParameterContext newContext() {
        return new TypeParameterContext(this);
    }
}
