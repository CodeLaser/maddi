package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.Map;

public class FieldTranslationMap2 implements TranslationMap {

    private final Runtime runtime;
    private final Map<TypeParameter, TypeParameter> map = new HashMap<>();

    public FieldTranslationMap2(Runtime runtime) {
        this.runtime = runtime;
    }

    public void put(TypeParameter in, TypeParameter out) {
        this.map.put(in, out);
    }

    @Override
    public Variable translateVariable(Variable variable) {
        ParameterizedType pt = variable.parameterizedType();
        if (pt.typeParameter() != null) {
            TypeParameter newTp = map.get(pt.typeParameter());
            if (newTp != null) {
                ParameterizedType newType = runtime.newParameterizedType(newTp, pt.arrays(), pt.wildcard());
                return switch (variable) {
                    case ReturnVariable rv when pt.arrays() == 0 -> rv.withParameterizedType(newType);
                    case ParameterInfo pi when pt.arrays() == 0 -> pi.withParameterizedType(newType);
                    case FieldReference fr -> handleFieldReference(fr, newTp, newType);
                    default -> variable; // no change
                };
            }
        }
        return variable; // no change
    }

    private FieldReference handleFieldReference(FieldReference fr, TypeParameter newTp, ParameterizedType newType) {
        String name = newTp.simpleName().toLowerCase() + "s".repeat(newType.arrays());
        FieldInfo newFieldInfo = runtime.newFieldInfo(name, false, newType, newTp.typeInfo());
        return runtime.newFieldReference(newFieldInfo, fr.scope(), newType);
    }

    @Override
    public Variable translateVariableRecursively(Variable variable) {
        return runtime.translateVariableRecursively(this, variable);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}
