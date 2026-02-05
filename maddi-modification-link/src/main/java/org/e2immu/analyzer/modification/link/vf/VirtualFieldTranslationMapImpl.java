package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VirtualFieldTranslationMap;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.Wildcard;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer.VF_CHAR;

// uses VariableTranslationMap.translateVariableRecursively()
public class VirtualFieldTranslationMapImpl implements VirtualFieldTranslationMap {

    private final Runtime runtime;
    private final VirtualFieldComputer virtualFieldComputer;
    private final Map<TypeParameter, ParameterizedType> map = new HashMap<>();

    public VirtualFieldTranslationMapImpl(VirtualFieldComputer virtualFieldComputer, Runtime runtime) {
        this.runtime = runtime;
        this.virtualFieldComputer = virtualFieldComputer;
    }

    @Override
    public void put(TypeParameter in, ParameterizedType out) {
        this.map.put(in, out);
    }

    @Override
    public Stream<Link> upgrade(Link link, Link tLink, FieldReference fr) {
        // upgrade: orElseGet==this.§t ==> orElseGet==this.§xs ==> orElseGet.§xs⊆this.§xs
        Variable ttFrom = runtime.newFieldReference(fr.fieldInfo(),
                runtime.newVariableExpression(tLink.from()),
                fr.fieldInfo().type());
        FieldInfo mFieldFrom = virtualFieldComputer.newMField(VariableTranslationMap.owner(runtime, ttFrom));
        assert !(tLink.from() instanceof FieldReference fr2 && Util.virtual(fr2))
                : "Ensure that §m is never appended to a virtual field itself";
        Variable mFrom = runtime.newFieldReference(mFieldFrom, runtime.newVariableExpression(tLink.from()),
                mFieldFrom.type());
        FieldInfo mFieldTo = virtualFieldComputer.newMField(VariableTranslationMap.owner(runtime, ttFrom));
        Variable toPrimary = Util.primary(tLink.to());
        assert !(toPrimary instanceof FieldReference fr2 && Util.virtual(fr2))
                : "Ensure that §m is never appended to a virtual field itself";
        Variable mTo = runtime.newFieldReference(mFieldTo, runtime.newVariableExpression(toPrimary),
                mFieldTo.type());
        Link mLink = new LinksImpl.LinkImpl(mFrom, LinkNatureImpl.IS_ASSIGNED_FROM, mTo);
        Stream<Link> mStream = Stream.of(mLink); // FIXME conditional on mutable
        LinksImpl.LinkImpl newLink = new LinksImpl.LinkImpl(ttFrom, LinkNatureImpl.IS_SUBSET_OF, tLink.to());
        return Stream.concat(mStream, Stream.of(newLink));
    }

    @Override
    public Variable translateVariable(Variable variable) {
        ParameterizedType pt = variable.parameterizedType();
        if (pt.typeParameter() != null) {
            ParameterizedType out = map.get(pt.typeParameter());
            if (out != null) {
                C c = newTypeNewName(out, pt.arrays(), pt.wildcard());
                return switch (variable) {
                    case ReturnVariable rv when pt.arrays() == 0 && c.newType.arrays() == 0 ->
                            rv.withParameterizedType(c.newType);
                    case ParameterInfo pi when pt.arrays() == 0 -> pi.with(c.newName, c.newType);
                    case FieldReference fr -> handleFieldReference(fr, c.newName, c.newType);
                    default -> variable; // no change
                };
            }
        } else if (variable instanceof FieldReference fr) {
            StringBuilder sbOld = new StringBuilder();
            StringBuilder sbNew = new StringBuilder();
            List<FieldInfo> fields = fr.fieldInfo().type().typeInfo().fields();
            List<FieldInfo> newFields = new ArrayList<>(fields.size());
            boolean change = false;
            TypeInfo owner = owner(fr.scope().parameterizedType());
            for (FieldInfo fieldInfo : fields) {
                TypeParameter tp = fieldInfo.type().typeParameter();
                if (tp != null) {
                    ParameterizedType out = map.get(tp);
                    if (out != null) {
                        C c = newTypeNewName(out, fieldInfo.type().arrays(), fieldInfo.type().wildcard());
                        change = true;
                        FieldInfo newField = runtime.newFieldInfo(VF_CHAR + c.newName, false,
                                c.newType, owner);
                        newFields.add(newField);
                        sbNew.append(newField.simpleName());
                    } else {
                        newFields.add(fieldInfo);
                        sbNew.append(fieldInfo.simpleName());
                    }
                    sbOld.append(fieldInfo.simpleName());
                } else {
                    return variable;// does not follow the 'container' scheme
                }
            }
            String old = VF_CHAR + sbOld.toString().replace(VF_CHAR, "");
            String oldName = old + "s".repeat(fr.parameterizedType().arrays());
            if (change && fr.fieldInfo().name().equals(oldName)) {
                // ok we can make a new contain, with the new name, and the translated fields
                String cleanNew = sbNew.toString().replace(VF_CHAR, "");
                String typeName = cleanNew.toUpperCase();
                TypeInfo container = VirtualFieldComputer.makeContainer(runtime, fr.fieldInfo().owner(), typeName, newFields);
                ParameterizedType containerPt = runtime.newParameterizedType(container, fr.parameterizedType().arrays());
                String newName = cleanNew + "s".repeat(fr.parameterizedType().arrays());
                FieldInfo newField = runtime.newFieldInfo(VF_CHAR + newName, false, containerPt,
                        owner);
                return runtime.newFieldReference(newField, fr.scope(), newField.type());
            }
        }
        return variable; // no change
    }

    private C newTypeNewName(ParameterizedType out, int arrays, Wildcard wildcard) {
        int newArrays = arrays + out.arrays();
        String newName;
        ParameterizedType newType;
        if (out.typeParameter() != null) {
            TypeParameter newTp = out.typeParameter();
            newName = newTp.simpleName().toLowerCase() + "s".repeat(newArrays);
            newType = runtime.newParameterizedType(newTp, newArrays, wildcard);
        } else if (out.typeInfo() != null && Util.isContainerType(out.typeInfo())) {
            newName = out.typeInfo().simpleName().toLowerCase() + "s".repeat(newArrays);
            newType = out.typeInfo().asSimpleParameterizedType().copyWithArrays(newArrays);
        } else {
            // concrete type
            newName = VirtualFieldComputer.VF_CONCRETE + "s".repeat(newArrays);
            newType = out.copyWithArrays(newArrays);
        }
        return new C(newName, newType);
    }

    private record C(String newName, ParameterizedType newType) {
    }


    private FieldReference handleFieldReference(FieldReference fr, String newName, ParameterizedType newType) {
        TypeInfo owner = owner(fr.scope().parameterizedType());
        String cleanName = newName.replace(VF_CHAR, "");
        FieldInfo newFieldInfo = runtime.newFieldInfo(VF_CHAR + cleanName, false, newType, owner);
        Expression tScope = fr.scope().translate(this);
        return runtime.newFieldReference(newFieldInfo, tScope, newType);
    }

    @Override
    public Variable translateVariableRecursively(Variable variable) {
        return VariableTranslationMap.translateVariableRecursively(runtime, this, variable);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public String toString() {
        return map.entrySet().stream()
                .map(e -> e.getKey().toStringWithTypeBounds()
                          + " --> " + nice(e.getValue()))
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    private static String nice(ParameterizedType pt) {
        if (pt.typeParameter() != null) {
            String dim = pt.arrays() == 0 ? "" : " dim " + pt.arrays();
            return pt.typeParameter().toStringWithTypeBounds() + dim;
        }
        return pt.detailedString();
    }

    private TypeInfo owner(ParameterizedType pt) {
        if (pt.typeParameter() != null) {
            return pt.typeParameter().typeInfo();
        }
        if (pt.typeInfo() != null) {
            return pt.typeInfo();
        }
        return runtime.objectTypeInfo();
    }
}
