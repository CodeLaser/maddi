package org.e2immu.language.cst.api.info;

import java.util.Set;

/*
is used for rewiring, objects change but their fqn stays
for methods and parameters, we're having to add the object before its FQN has been fully computed
 */
public interface InfoMap {
    // can only be done once for each typeInfo object
    void put(TypeInfo typeInfo);

    void put(MethodInfo original, MethodInfo rewired);

    void put(FieldInfo fieldInfo);

    void put(ParameterInfo original, ParameterInfo rewired);

    Set<TypeInfo> rewireAll();

    // do not recurse, error if absent
    TypeInfo typeInfo(TypeInfo typeInfo);

    // only to be used in phase 3, inside expressions
    TypeInfo typeInfoRecurseAllPhases(TypeInfo typeInfo);

    TypeInfo typeInfoNullIfAbsent(TypeInfo typeInfo);

    MethodInfo methodInfo(MethodInfo methodInfo);

    FieldInfo fieldInfo(FieldInfo fieldInfo);

    ParameterInfo parameterInfo(ParameterInfo parameterInfo);
}
