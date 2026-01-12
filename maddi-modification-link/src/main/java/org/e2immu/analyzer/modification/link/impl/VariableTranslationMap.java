package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.*;
import org.e2immu.language.cst.impl.variable.DependentVariableImpl;
import org.e2immu.language.cst.impl.variable.FieldReferenceImpl;
import org.e2immu.language.cst.impl.variable.ThisImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VariableTranslationMap implements TranslationMap {
    private final Runtime runtime;
    private final Map<Variable, Variable> map = new HashMap<>();

    public VariableTranslationMap(Runtime runtime) {
        this.runtime = runtime;
    }

    public VariableTranslationMap put(Variable from, Variable to) {
        this.map.put(from, to);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public Variable translateVariable(Variable variable) {
        return this.map.getOrDefault(variable, variable);
    }

    /*
     this method is an adaptation of the one in TranslationMapImpl

     different: virtual fields change owner recursively.
     for consistency's sake a virtual field's owner must be set correctly, i.e. to the type of its scope

     Set<Map.Entry<X,Y>> §m -> set
     Set<Map.Entry<X,Y>> §xys -> §xys set, the individual fields inside the container should belong to
        the container/Map.Entry ??
     */

    public static TypeInfo owner(Runtime runtime, ParameterizedType pt) {
        if (pt.typeParameter() != null) {
            return pt.typeParameter().typeInfo();
        }
        if (pt.typeInfo() != null) {
            return pt.typeInfo();
        }
        return runtime.objectTypeInfo();
    }

    public static TypeInfo owner(Runtime runtime, Variable v) {
        if (v instanceof FieldReference fr) return fr.fieldInfo().owner();
        if (v instanceof DependentVariable dv) return owner(runtime, dv.arrayVariable());
        return owner(runtime, v.parameterizedType());
    }

    @Override
    public Variable translateVariableRecursively(Variable variable) {
        return translateVariableRecursively(runtime, this, variable);
    }

    public static Variable translateVariableRecursively(Runtime runtime, TranslationMap tm, Variable variable) {
        Variable translated = tm.translateVariable(variable);
        if (translated != variable) {
            if (variable instanceof LocalVariable from && from.assignmentExpression() != null
                && translated instanceof LocalVariable to && to.assignmentExpression() == null) {
                Expression te = from.assignmentExpression().translate(tm);
                if (te.variableStreamDescend().anyMatch(to::equals)) {
                    // this goes in conjunction with the assertion
                    return from.withAssignmentExpression(null);
                }
                return to.withAssignmentExpression(te);
            }
            return translated;
        }
        if (variable instanceof FieldReference fr) {
            Expression tScope = fr.scope().translate(tm);
            FieldInfo newField = tm.translateFieldInfo(fr.fieldInfo());
            if (tScope != fr.scope() || newField != fr.fieldInfo()) {
                TypeInfo newOwner = owner(runtime, tScope.parameterizedType());
                FieldInfo changedOwner = newField.withOwner(newOwner);
                return new FieldReferenceImpl(changedOwner, tScope, null, fr.parameterizedType());
            }
        } else if (variable instanceof DependentVariable dv) {
            Expression translatedArray = dv.arrayExpression().translate(tm);
            Expression translatedIndex = dv.indexExpression().translate(tm);
            if (translatedArray != dv.arrayExpression() || translatedIndex != dv.indexExpression()) {
                return DependentVariableImpl.create(translatedArray, translatedIndex, null);
            }
        } else if (variable instanceof This thisVar) {
            ParameterizedType thisVarPt = thisVar.parameterizedType();
            ParameterizedType translatedType = tm.translateType(thisVarPt);
            TypeInfo tExplicitly;
            if (thisVar.explicitlyWriteType() == null) {
                tExplicitly = null;
            } else {
                ParameterizedType explicitlyPt = thisVar.explicitlyWriteType().asSimpleParameterizedType();
                ParameterizedType tExplicitlyPt = tm.translateType(explicitlyPt);
                tExplicitly = tExplicitlyPt.typeInfo();
            }
            if (translatedType != thisVarPt || !Objects.equals(thisVar.explicitlyWriteType(), tExplicitly)) {
                return new ThisImpl(translatedType, tExplicitly, thisVar.writeSuper());
            }
        }

        return variable;
    }
}
