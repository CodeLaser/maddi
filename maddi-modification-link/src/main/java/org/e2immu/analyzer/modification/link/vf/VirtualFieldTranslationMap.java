package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VirtualFieldTranslationMap implements TranslationMap {

    private final Runtime runtime;
    private final Map<TypeParameter, R> map = new HashMap<>();

    public VirtualFieldTranslationMap(Runtime runtime) {
        this.runtime = runtime;
    }

    private record R(TypeParameter tp, int arrayDiff) {
    }

    public void put(TypeParameter in, TypeParameter out, int arrayDiff) {
        this.map.put(in, new R(out, arrayDiff));
    }

    @Override
    public Variable translateVariable(Variable variable) {
        ParameterizedType pt = variable.parameterizedType();
        if (pt.typeParameter() != null) {
            R newTpR = map.get(pt.typeParameter());
            if (newTpR != null) {
                TypeParameter newTp = newTpR.tp;
                int newArrays = pt.arrays() + newTpR.arrayDiff;
                ParameterizedType newType = runtime.newParameterizedType(newTp, newArrays, pt.wildcard());
                String newName = newTp.simpleName().toLowerCase() + "s".repeat(newArrays);
                return switch (variable) {
                    case ReturnVariable rv when pt.arrays() == 0 && newTpR.arrayDiff == 0 ->
                            rv.withParameterizedType(newType);
                    case ParameterInfo pi when pt.arrays() == 0 -> pi.with(newName, newType);
                    case FieldReference fr -> handleFieldReference(fr, newTp, newName, newType);
                    default -> variable; // no change
                };
            }
        } else if (variable instanceof FieldReference fr) {
            StringBuilder sbOld = new StringBuilder();
            StringBuilder sbNew = new StringBuilder();
            List<FieldInfo> fields = fr.fieldInfo().type().typeInfo().fields();
            List<FieldInfo> newFields = new ArrayList<>(fields.size());
            List<TypeParameter> newTypeParameters = new ArrayList<>(fields.size());
            boolean change = false;
            for (FieldInfo fieldInfo : fields) {
                TypeParameter tp = fieldInfo.type().typeParameter();
                if (tp != null && fieldInfo.type().arrays() == 0) {
                    R newTpRecord = map.get(tp);
                    if (newTpRecord != null) {
                        TypeParameter newTp = newTpRecord.tp;
                        change = true;
                        ParameterizedType newType = runtime.newParameterizedType(newTp, newTpRecord.arrayDiff, null);
                        String name = newTp.simpleName().toLowerCase() + "s".repeat(newTpRecord.arrayDiff);
                        FieldInfo newField = runtime.newFieldInfo(name, false, newType, newTp.typeInfo());
                        newTypeParameters.add(newTp);
                        newFields.add(newField);
                        sbNew.append(newField.simpleName());
                    } else {
                        newFields.add(fieldInfo);
                        newTypeParameters.add(tp);
                        sbNew.append(fieldInfo.simpleName());
                    }
                    sbOld.append(fieldInfo.simpleName());
                } else {
                    return variable;// does not follow the 'container' scheme
                }
            }
            String oldName = sbOld + "s".repeat(fr.parameterizedType().arrays());
            if (change && fr.fieldInfo().name().equals(oldName)) {
                // ok we can make a new contain, with the new name, and the translated fields
                String newName = sbNew + "s".repeat(fr.parameterizedType().arrays());
                String typeName = sbNew.toString().toUpperCase();
                TypeInfo container = makeContainer(fr.fieldInfo().owner(), typeName, newFields, newTypeParameters);
                ParameterizedType containerPt = runtime.newParameterizedType(container, fr.parameterizedType().arrays());
                FieldInfo newField = runtime.newFieldInfo(newName, false, containerPt, fr.fieldInfo().owner());
                return runtime.newFieldReference(newField, fr.scope(), newField.type());
            }
        }
        return variable; // no change
    }

    private TypeInfo makeContainer(TypeInfo typeInfo, String name,
                                   List<FieldInfo> newFields,
                                   List<TypeParameter> newTypeParameters) {
        TypeInfo newType = runtime.newTypeInfo(typeInfo, name);
        TypeInfo.Builder builder = newType.builder();
        builder.setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .setAccess(runtime.accessPublic());
        newFields.forEach(builder::addField);
        newTypeParameters.forEach(builder::addOrSetTypeParameter);
        builder.commit();
        return newType;
    }

    private FieldReference handleFieldReference(FieldReference fr, TypeParameter newTp, String newName,
                                                ParameterizedType newType) {
        FieldInfo newFieldInfo = runtime.newFieldInfo(newName, false, newType, newTp.typeInfo());
        Expression tScope = fr.scope().translate(this);
        return runtime.newFieldReference(newFieldInfo, tScope, newType);
    }

    @Override
    public Variable translateVariableRecursively(Variable variable) {
        return runtime.translateVariableRecursively(this, variable);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public String toString() {
        return map.entrySet().stream()
                .map(e -> e.getKey().toStringWithTypeBounds()
                          + " --> " + e.getValue().tp.toStringWithTypeBounds() + niceArrayDiff(e.getValue().arrayDiff))
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    private static String niceArrayDiff(int i) {
        if (i > 0) return " +" + i;
        if (i < 0) return " " + i;
        return "";
    }
}
