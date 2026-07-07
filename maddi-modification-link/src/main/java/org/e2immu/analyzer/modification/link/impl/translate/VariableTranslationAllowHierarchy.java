package org.e2immu.analyzer.modification.link.impl.translate;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.Map;

/*
 see e.g. TestStaticValuesRecord,7, interface inbetween

 mainly necessary because all getters have been replaced by field indexing; and getters can be inherited.
 */
public class VariableTranslationAllowHierarchy implements TranslationMap {
    private final Runtime runtime;
    private final Map<Variable, Variable> map = new HashMap<>();

    public VariableTranslationAllowHierarchy(Runtime runtime) {
        this.runtime = runtime;
    }

    public void put(Variable from, Variable to) {
        this.map.put(from, to);
    }

    @Override
    public Variable translateVariable(Variable variable) {
        Variable t = map.get(variable);
        if (t != null) return t;
        // map contains r.function (field a.b.X.R.function, r of type a.b.X.R)
        // actual variable is r.function (field a.b.X.RI.function, r of type a.b.X.R)
        if (variable instanceof FieldReference fr && fr.scopeVariable() != null) {
            FieldInfo f = fr.fieldInfo();
            TypeInfo typeInfo = f.owner();
            if (typeInfo != null) {
                for (TypeInfo superType : typeInfo.superTypesExcludingJavaLangObject()) {
                    FieldInfo newField = runtime.newFieldInfo(f.name(), f.isStatic(), f.type(), superType);
                    Variable fr2 = runtime.newFieldReference(newField, fr.scope(), newField.type());
                    Variable tt = map.get(fr2);
                    if (tt != null) return tt;
                }
            }
        }
        return variable; // no change
    }

    @Override
    public Variable translateVariableRecursively(Variable variable) {
        return runtime.translateVariableRecursively(this, variable);
    }
}
