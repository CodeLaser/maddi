package org.e2immu.language.cst.impl.runtime;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.element.SourceImpl;
import org.e2immu.language.cst.impl.expression.AnnotationExpressionImpl;
import org.e2immu.language.cst.impl.element.CompilationUnitImpl;
import org.e2immu.language.cst.impl.info.InspectionImpl;
import org.e2immu.language.cst.impl.info.MethodInfoImpl;
import org.e2immu.language.cst.impl.info.TypeInfoImpl;
import org.e2immu.language.cst.impl.info.TypeNatureEnum;

import java.util.*;

public class PredefinedImpl implements Predefined {
    private static final CompilationUnit JAVA_PRIMITIVE = new CompilationUnitImpl.Builder()
            .setURIString("predefined://primitive")
            .build();
    private static final CompilationUnit JAVA_LANG = new CompilationUnitImpl.Builder()
            .setURIString("predefined://java/lang")
            .setPackageName("java.lang").build();

    private final TypeInfo intTypeInfo = new TypeInfoImpl(JAVA_PRIMITIVE, "int");
    private final ParameterizedType intParameterizedType = intTypeInfo.asSimpleParameterizedType();

    private final TypeInfo integerTypeInfo = new TypeInfoImpl(JAVA_LANG, "Integer");

    private final TypeInfo charTypeInfo = new TypeInfoImpl(JAVA_PRIMITIVE, "char");
    private final ParameterizedType charParameterizedType = charTypeInfo.asSimpleParameterizedType();

    private final TypeInfo characterTypeInfo = new TypeInfoImpl(JAVA_LANG, "Character");

    private final TypeInfo booleanTypeInfo = new TypeInfoImpl(JAVA_PRIMITIVE, "boolean");
    private final ParameterizedType booleanParameterizedType = booleanTypeInfo.asSimpleParameterizedType();

    private final TypeInfo boxedBooleanTypeInfo = new TypeInfoImpl(JAVA_LANG, "Boolean");
    private final ParameterizedType boxedBooleanParameterizedType = boxedBooleanTypeInfo.asSimpleParameterizedType();

    private final TypeInfo longTypeInfo = new TypeInfoImpl(JAVA_PRIMITIVE, "long");
    private final ParameterizedType longParameterizedType = longTypeInfo.asSimpleParameterizedType();

    private final TypeInfo boxedLongTypeInfo = new TypeInfoImpl(JAVA_LANG, "Long");

    private final TypeInfo shortTypeInfo = new TypeInfoImpl(JAVA_PRIMITIVE, "short");
    private final ParameterizedType shortParameterizedType = shortTypeInfo.asSimpleParameterizedType();

    private final TypeInfo boxedShortTypeInfo = new TypeInfoImpl(JAVA_LANG, "Short");

    private final TypeInfo byteTypeInfo = new TypeInfoImpl(JAVA_PRIMITIVE, "byte");
    private final ParameterizedType byteParameterizedType = byteTypeInfo.asSimpleParameterizedType();

    private final TypeInfo boxedByteTypeInfo = new TypeInfoImpl(JAVA_LANG, "Byte");

    private final TypeInfo doubleTypeInfo = new TypeInfoImpl(JAVA_PRIMITIVE, "double");
    private final ParameterizedType doubleParameterizedType = doubleTypeInfo.asSimpleParameterizedType();

    private final TypeInfo boxedDoubleTypeInfo = new TypeInfoImpl(JAVA_LANG, "Double");

    private final TypeInfo floatTypeInfo = new TypeInfoImpl(JAVA_PRIMITIVE, "float");
    private final ParameterizedType floatParameterizedType = floatTypeInfo.asSimpleParameterizedType();

    private final TypeInfo boxedFloatTypeInfo = new TypeInfoImpl(JAVA_LANG, "Float");

    private final TypeInfo voidTypeInfo = new TypeInfoImpl(JAVA_PRIMITIVE, "void");
    private final ParameterizedType voidParameterizedType = voidTypeInfo.asSimpleParameterizedType();
    private final TypeInfo boxedVoidTypeInfo = new TypeInfoImpl(JAVA_LANG, "Void");

    private final TypeInfo stringTypeInfo = new TypeInfoImpl(JAVA_LANG, "String");
    private final ParameterizedType stringParameterizedType = stringTypeInfo.asSimpleParameterizedType();

    public ParameterizedType stringParameterizedType() {
        return stringParameterizedType;
    }

    @Override
    public ParameterizedType intParameterizedType() {
        return intParameterizedType;
    }

    @Override
    public ParameterizedType booleanParameterizedType() {
        return booleanParameterizedType;
    }

    @Override
    public ParameterizedType boxedBooleanParameterizedType() {
        return boxedBooleanParameterizedType;
    }

    @Override
    public TypeInfo boxedLongTypeInfo() {
        return boxedLongTypeInfo;
    }

    @Override
    public ParameterizedType longParameterizedType() {
        return longParameterizedType;
    }

    @Override
    public ParameterizedType doubleParameterizedType() {
        return doubleParameterizedType;
    }

    @Override
    public ParameterizedType floatParameterizedType() {
        return floatParameterizedType;
    }

    @Override
    public ParameterizedType shortParameterizedType() {
        return shortParameterizedType;
    }

    @Override
    public ParameterizedType charParameterizedType() {
        return charParameterizedType;
    }

    public MethodInfo createOperator(TypeInfo owner,
                                     String name,
                                     List<ParameterizedType> parameterizedTypes,
                                     ParameterizedType returnType) {
        MethodInfoImpl mi = new MethodInfoImpl(MethodInfoImpl.MethodTypeEnum.STATIC_METHOD, name, owner);
        int i = 0;
        for (ParameterizedType parameterizedType : parameterizedTypes) {
            ParameterInfo pi = mi.builder()
                    .addParameter("p" + i, parameterizedType); // inspection built when method is built
            pi.builder().setVarArgs(false).commit();
        }
        MethodInfo.Builder builder = mi.builder().setReturnType(returnType);
        mi.inspectionBuilder().setAccess(InspectionImpl.AccessEnum.PUBLIC);
        builder.commit();
        return mi;
    }

    private final TypeInfo functionalInterface = new TypeInfoImpl(JAVA_LANG, "FunctionalInterface");
    private final AnnotationExpression functionalInterfaceAnnotationExpression =
            new AnnotationExpressionImpl(List.of(), null, functionalInterface, List.of());

    private final TypeInfo classTypeInfo = new TypeInfoImpl(JAVA_LANG, "Class");

    private final TypeInfo objectTypeInfo = new TypeInfoImpl(JAVA_LANG, "Object");
    private final ParameterizedType objectParameterizedType = objectTypeInfo.asSimpleParameterizedType();

    private final List<ParameterizedType> intInt = List.of(intParameterizedType, intParameterizedType);
    private final List<ParameterizedType> boolBool = List.of(booleanParameterizedType, booleanParameterizedType);

    private final MethodInfo plusOperatorInt = createOperator(intTypeInfo, "+", intInt, intParameterizedType);
    private final MethodInfo minusOperatorInt = createOperator(intTypeInfo, "-", intInt, intParameterizedType);
    private final MethodInfo bitwiseOrOperatorInt = createOperator(intTypeInfo, "|", intInt, intParameterizedType);
    private final MethodInfo bitwiseAndOperatorInt = createOperator(intTypeInfo, "&", intInt, intParameterizedType);
    private final MethodInfo bitwiseXorOperatorInt = createOperator(intTypeInfo, "^", intInt, intParameterizedType);
    private final MethodInfo remainderOperatorInt = createOperator(intTypeInfo, "%", intInt, intParameterizedType);
    private final MethodInfo signedRightShiftOperatorInt = createOperator(intTypeInfo, ">>", intInt, intParameterizedType);
    private final MethodInfo unsignedRightShiftOperatorInt = createOperator(intTypeInfo, ">>>", intInt, intParameterizedType);
    private final MethodInfo leftShiftOperatorInt = createOperator(intTypeInfo, "<<", intInt, intParameterizedType);
    private final MethodInfo divideOperatorInt = createOperator(intTypeInfo, "/", intInt, intParameterizedType);
    private final MethodInfo multiplyOperatorInt = createOperator(intTypeInfo, "*", intInt, intParameterizedType);

    private final MethodInfo equalsOperatorInt = createOperator(intTypeInfo, "==", intInt, booleanParameterizedType);
    private final MethodInfo notEqualsOperatorInt = createOperator(intTypeInfo, "!=", intInt, booleanParameterizedType);
    private final MethodInfo greaterOperatorInt = createOperator(intTypeInfo, ">", intInt, booleanParameterizedType);
    private final MethodInfo greaterEqualsOperatorInt = createOperator(intTypeInfo, ">=", intInt, booleanParameterizedType);
    private final MethodInfo lessOperatorInt = createOperator(intTypeInfo, "<", intInt, booleanParameterizedType);
    private final MethodInfo lessEqualsOperatorInt = createOperator(intTypeInfo, "<=", intInt, booleanParameterizedType);

    private final MethodInfo assignOperatorInt = createOperator(intTypeInfo, "=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignPlusOperatorInt = createOperator(intTypeInfo, "+=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignMinusOperatorInt = createOperator(intTypeInfo, "-=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignMultiplyOperatorInt = createOperator(intTypeInfo, "*=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignDivideOperatorInt = createOperator(intTypeInfo, "/=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignOrOperatorInt = createOperator(intTypeInfo, "|=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignAndOperatorInt = createOperator(intTypeInfo, "&=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignXorOperatorInt = createOperator(intTypeInfo, "^=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignLeftShiftOperator = createOperator(intTypeInfo, "<<=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignSignedRightShiftOperator = createOperator(intTypeInfo, ">>=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignUnsignedRightShiftOperator = createOperator(intTypeInfo, ">>>=", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo assignRemainderOperatorInt = createOperator(intTypeInfo, "%=", List.of(intParameterizedType), intParameterizedType);

    // TODO long instead of int to distinguish statically (isPostfix) This is a hack!
    private final MethodInfo postfixIncrementOperatorInt = createOperator(intTypeInfo, "++", List.of(), longParameterizedType);
    private final MethodInfo prefixIncrementOperatorInt = createOperator(intTypeInfo, "++", List.of(), intParameterizedType);
    private final MethodInfo postfixDecrementOperatorInt = createOperator(intTypeInfo, "--", List.of(), longParameterizedType);
    private final MethodInfo prefixDecrementOperatorInt = createOperator(intTypeInfo, "--", List.of(), intParameterizedType);

    private final MethodInfo unaryPlusOperatorInt = createOperator(intTypeInfo, "+", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo unaryMinusOperatorInt = createOperator(intTypeInfo, "-", List.of(intParameterizedType), intParameterizedType);

    private final MethodInfo bitWiseNotOperatorInt = createOperator(intTypeInfo, "~", List.of(intParameterizedType), intParameterizedType);
    private final MethodInfo logicalNotOperatorBool = createOperator(booleanTypeInfo, "!", List.of(booleanParameterizedType), booleanParameterizedType);
    private final MethodInfo assignOrOperatorBool = createOperator(booleanTypeInfo, "|=", List.of(booleanParameterizedType), booleanParameterizedType);
    private final MethodInfo assignAndOperatorBool = createOperator(booleanTypeInfo, "&=", List.of(booleanParameterizedType), booleanParameterizedType);
    private final MethodInfo orOperatorBool = createOperator(booleanTypeInfo, "||", boolBool, booleanParameterizedType);
    private final MethodInfo andOperatorBool = createOperator(booleanTypeInfo, "&&", boolBool, booleanParameterizedType);
    private final MethodInfo xorOperatorBool = createOperator(booleanTypeInfo, "^", boolBool, booleanParameterizedType);

    private final MethodInfo plusOperatorString = createOperator(stringTypeInfo, "+", List.of(stringParameterizedType,
            stringParameterizedType), stringParameterizedType);
    private final MethodInfo assignPlusOperatorString = createOperator(stringTypeInfo, "+=",
            List.of(stringParameterizedType), stringParameterizedType);

    private final MethodInfo equalsOperatorObject = createOperator(objectTypeInfo, "==",
            List.of(objectParameterizedType, objectParameterizedType), booleanParameterizedType);
    private final MethodInfo notEqualsOperatorObject = createOperator(objectTypeInfo, "!=",
            List.of(objectParameterizedType, objectParameterizedType), booleanParameterizedType);

    private final Map<String, TypeInfo> primitiveByName = new HashMap<>();
    private final List<TypeInfo> predefinedObjects;

    public PredefinedImpl() {
        Set<TypeInfo> primitives = Set.of(booleanTypeInfo, byteTypeInfo, doubleTypeInfo, floatTypeInfo,
                longTypeInfo, shortTypeInfo, voidTypeInfo, intTypeInfo, charTypeInfo);
        for (TypeInfo ti : primitives) {
            ti.builder()
                    .setAccess(InspectionImpl.AccessEnum.PUBLIC)
                    .setTypeNature(TypeNatureEnum.PRIMITIVE)
                    .setSource(SourceImpl.forCompiledClass(JAVA_PRIMITIVE))
                    .commit();
            primitiveByName.put(ti.simpleName(), ti);
        }
        List<TypeInfo> objects = new ArrayList<>();
        for (TypeInfo ti : List.of(stringTypeInfo, objectTypeInfo, classTypeInfo, functionalInterface)) {
            ti.builder().setAccess(InspectionImpl.AccessEnum.PUBLIC)
                    .setTypeNature(TypeNatureEnum.CLASS)
                    .setSource(SourceImpl.forCompiledClass(JAVA_LANG));
            if (ti != objectTypeInfo) {
                ti.builder().setParentClass(objectParameterizedType);
            }
            objects.add(ti);
        }
        for (TypeInfo ti : List.of(functionalInterface)) {
            ti.builder().setAccess(InspectionImpl.AccessEnum.PUBLIC)
                    .setTypeNature(TypeNatureEnum.ANNOTATION);
        }
        Set<TypeInfo> boxed = Set.of(boxedBooleanTypeInfo, boxedByteTypeInfo, boxedDoubleTypeInfo, boxedFloatTypeInfo,
                boxedLongTypeInfo, boxedShortTypeInfo, boxedVoidTypeInfo, integerTypeInfo, characterTypeInfo);
        for (TypeInfo ti : boxed) {
            ti.builder().setAccess(InspectionImpl.AccessEnum.PUBLIC)
                    .setSource(SourceImpl.forCompiledClass(JAVA_LANG))
                    .setTypeNature(TypeNatureEnum.CLASS)
                    .setParentClass(objectParameterizedType);
            objects.add(ti);
        }
        predefinedObjects = List.copyOf(objects);
    }

    @Override
    public List<TypeInfo> predefinedObjects() {
        return predefinedObjects;
    }

    @Override
    public Collection<TypeInfo> primitives() {
        return primitiveByName.values();
    }

    @Override
    public ParameterizedType widestType(ParameterizedType t1, ParameterizedType t2) {
        int o1 = primitiveTypeOrder(Objects.requireNonNull(t1));
        int o2 = primitiveTypeOrder(Objects.requireNonNull(t2));
        if (o1 >= o2) return t1;
        return t2;
    }


    /*
    used by binaryOperator, the result cannot be null, so must be the unboxed version (this contrasts with
    ParameterizedType.commonType)
     */
    @Override
    public ParameterizedType widestTypeUnbox(ParameterizedType t1, ParameterizedType t2) {
        ParameterizedType u1, u2;
        if (t1.isBoxedExcludingVoid()) {
            u1 = unboxed(t1.typeInfo()).asSimpleParameterizedType();
        } else {
            u1 = t1;
        }
        if (t2.isBoxedExcludingVoid()) {
            u2 = unboxed(t2.typeInfo()).asSimpleParameterizedType();
        } else {
            u2 = t2;
        }
        int o1 = primitiveTypeOrder(u1);
        int o2 = primitiveTypeOrder(u2);
        if (o1 >= o2) return u1;
        return u2;
    }

    @Override
    public int primitiveTypeOrder(ParameterizedType pt) {
        if (pt == null) throw new NullPointerException();
        if (pt.isType()) {
            TypeInfo typeInfo = pt.typeInfo();
            if (typeInfo == booleanTypeInfo) return 1;
            if (typeInfo == byteTypeInfo) return 2;
            if (typeInfo == charTypeInfo) return 3;
            if (typeInfo == shortTypeInfo) return 4;
            if (typeInfo == intTypeInfo) return 5;
            if (typeInfo == floatTypeInfo) return 6;
            if (typeInfo == longTypeInfo) return 7;
            if (typeInfo == doubleTypeInfo) return 8;
            if (typeInfo == stringTypeInfo) return 9;
        }
        return 0;
    }

    @Override
    public int reversePrimitiveTypeOrder(ParameterizedType pt) {
        return 9 - primitiveTypeOrder(pt);
    }

    @Override
    public TypeInfo primitiveByName(String asString) {
        TypeInfo ti = primitiveByName.get(asString);
        if (ti == null) throw new UnsupportedOperationException("Type " + asString + " not (yet) a primitive");
        return ti;
    }

    @Override
    public int isAssignableFromTo(ParameterizedType from, ParameterizedType to, boolean covariant) {
        int fromOrder = primitiveTypeOrder(from);
        if (fromOrder <= 1 || fromOrder >= 9) return -1;
        int toOrder = primitiveTypeOrder(to);
        if (toOrder <= 1 || toOrder >= 9) return -1;
        int diff = covariant ? toOrder - fromOrder : fromOrder - toOrder;
        return diff < 0 ? -1 : diff;
    }

    @Override
    public boolean isPreOrPostFixOperator(MethodInfo operator) {
        return operator == postfixDecrementOperatorInt || // i--;
               operator == postfixIncrementOperatorInt || // i++;
               operator == prefixDecrementOperatorInt || // --i;
               operator == prefixIncrementOperatorInt; // ++i;
    }

    @Override
    public boolean isPrefixOperator(MethodInfo operator) {
        return operator == prefixDecrementOperatorInt || operator == prefixIncrementOperatorInt;
    }

    @Override
    public MethodInfo prePostFixToAssignment(MethodInfo operator) {
        if (operator == postfixDecrementOperatorInt || operator == prefixDecrementOperatorInt) {
            return assignMinusOperatorInt;
        }
        if (operator == postfixIncrementOperatorInt || operator == prefixIncrementOperatorInt) {
            return assignPlusOperatorInt;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeInfo boxed(TypeInfo typeInfo) {
        if (typeInfo == longTypeInfo)
            return boxedLongTypeInfo;
        if (typeInfo == intTypeInfo)
            return integerTypeInfo;
        if (typeInfo == shortTypeInfo)
            return boxedShortTypeInfo;
        if (typeInfo == byteTypeInfo)
            return boxedByteTypeInfo;
        if (typeInfo == charTypeInfo)
            return characterTypeInfo;
        if (typeInfo == booleanTypeInfo)
            return boxedBooleanTypeInfo;
        if (typeInfo == floatTypeInfo)
            return boxedFloatTypeInfo;
        if (typeInfo == doubleTypeInfo)
            return boxedDoubleTypeInfo;
        if (typeInfo == voidTypeInfo) {
            return boxedVoidTypeInfo;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeInfo unboxed(TypeInfo typeInfo) {
        if (typeInfo == boxedLongTypeInfo)
            return longTypeInfo;
        if (typeInfo == integerTypeInfo)
            return intTypeInfo;
        if (typeInfo == boxedShortTypeInfo)
            return shortTypeInfo;
        if (typeInfo == boxedByteTypeInfo)
            return byteTypeInfo;
        if (typeInfo == characterTypeInfo)
            return charTypeInfo;
        if (typeInfo == boxedBooleanTypeInfo)
            return booleanTypeInfo;
        if (typeInfo == boxedFloatTypeInfo)
            return floatTypeInfo;
        if (typeInfo == boxedDoubleTypeInfo)
            return doubleTypeInfo;
        if (typeInfo == boxedVoidTypeInfo) {
            return voidTypeInfo;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodInfo assignAndOperatorBool() {
        return assignAndOperatorBool;
    }

    @Override
    public MethodInfo assignOrOperatorBool() {
        return assignOrOperatorBool;
    }

    @Override
    public MethodInfo assignOperatorInt() {
        return assignOperatorInt;
    }

    @Override
    public MethodInfo assignPlusOperatorInt() {
        return assignPlusOperatorInt;
    }

    @Override
    public MethodInfo assignPlusOperatorString() {
        return assignPlusOperatorString;
    }

    @Override
    public MethodInfo assignMinusOperatorInt() {
        return assignMinusOperatorInt;
    }

    @Override
    public MethodInfo assignMultiplyOperatorInt() {
        return assignMultiplyOperatorInt;
    }

    @Override
    public MethodInfo assignDivideOperatorInt() {
        return assignDivideOperatorInt;
    }

    @Override
    public MethodInfo assignOrOperatorInt() {
        return assignOrOperatorInt;
    }

    @Override
    public MethodInfo assignAndOperatorInt() {
        return assignAndOperatorInt;
    }

    @Override
    public MethodInfo assignLeftShiftOperatorInt() {
        return assignLeftShiftOperator;
    }

    @Override
    public MethodInfo assignSignedRightShiftOperatorInt() {
        return assignSignedRightShiftOperator;
    }

    @Override
    public MethodInfo assignUnsignedRightShiftOperatorInt() {
        return assignUnsignedRightShiftOperator;
    }

    @Override
    public MethodInfo assignXorOperatorInt() {
        return assignXorOperatorInt;
    }

    @Override
    public MethodInfo assignRemainderOperatorInt() {
        return assignRemainderOperatorInt;
    }

    @Override
    public MethodInfo plusOperatorInt() {
        return plusOperatorInt;
    }

    @Override
    public MethodInfo minusOperatorInt() {
        return minusOperatorInt;
    }

    @Override
    public MethodInfo multiplyOperatorInt() {
        return multiplyOperatorInt;
    }

    @Override
    public MethodInfo divideOperatorInt() {
        return divideOperatorInt;
    }

    @Override
    public MethodInfo orOperatorInt() {
        return bitwiseOrOperatorInt;
    }

    @Override
    public MethodInfo andOperatorInt() {
        return bitwiseAndOperatorInt;
    }

    @Override
    public MethodInfo xorOperatorInt() {
        return bitwiseXorOperatorInt;
    }

    @Override
    public MethodInfo orOperatorBool() {
        return orOperatorBool;
    }

    @Override
    public MethodInfo andOperatorBool() {
        return andOperatorBool;
    }

    @Override
    public MethodInfo equalsOperatorObject() {
        return equalsOperatorObject;
    }

    @Override
    public MethodInfo equalsOperatorInt() {
        return equalsOperatorInt;
    }

    @Override
    public MethodInfo notEqualsOperatorObject() {
        return notEqualsOperatorObject;
    }

    @Override
    public MethodInfo notEqualsOperatorInt() {
        return notEqualsOperatorInt;
    }

    @Override
    public MethodInfo plusOperatorString() {
        return plusOperatorString;
    }

    @Override
    public MethodInfo xorOperatorBool() {
        return xorOperatorBool;
    }

    @Override
    public MethodInfo bitwiseXorOperatorInt() {
        return bitwiseXorOperatorInt;
    }

    @Override
    public MethodInfo leftShiftOperatorInt() {
        return leftShiftOperatorInt;
    }

    @Override
    public MethodInfo signedRightShiftOperatorInt() {
        return signedRightShiftOperatorInt;
    }

    @Override
    public MethodInfo unsignedRightShiftOperatorInt() {
        return unsignedRightShiftOperatorInt;
    }

    @Override
    public MethodInfo greaterOperatorInt() {
        return greaterOperatorInt;
    }

    @Override
    public MethodInfo greaterEqualsOperatorInt() {
        return greaterEqualsOperatorInt;
    }

    @Override
    public MethodInfo lessEqualsOperatorInt() {
        return lessEqualsOperatorInt;
    }

    @Override
    public MethodInfo lessOperatorInt() {
        return lessOperatorInt;
    }

    @Override
    public MethodInfo remainderOperatorInt() {
        return remainderOperatorInt;
    }

    @Override
    public TypeInfo stringTypeInfo() {
        return stringTypeInfo;
    }

    @Override
    public TypeInfo booleanTypeInfo() {
        return booleanTypeInfo;
    }

    @Override
    public TypeInfo charTypeInfo() {
        return charTypeInfo;
    }

    @Override
    public ParameterizedType byteParameterizedType() {
        return byteParameterizedType;
    }

    @Override
    public TypeInfo classTypeInfo() {
        return classTypeInfo;
    }

    @Override
    public ParameterizedType objectParameterizedType() {
        return objectParameterizedType;
    }

    @Override
    public ParameterizedType voidParameterizedType() {
        return voidParameterizedType;
    }

    @Override
    public MethodInfo logicalNotOperatorBool() {
        return logicalNotOperatorBool;
    }

    @Override
    public MethodInfo unaryMinusOperatorInt() {
        return unaryMinusOperatorInt;
    }

    @Override
    public MethodInfo unaryPlusOperatorInt() {
        return unaryPlusOperatorInt;
    }

    @Override
    public MethodInfo prefixIncrementOperatorInt() {
        return prefixIncrementOperatorInt;
    }

    @Override
    public MethodInfo postfixIncrementOperatorInt() {
        return postfixIncrementOperatorInt;
    }

    @Override
    public MethodInfo prefixDecrementOperatorInt() {
        return prefixDecrementOperatorInt;
    }

    @Override
    public MethodInfo postfixDecrementOperatorInt() {
        return postfixDecrementOperatorInt;
    }

    @Override
    public MethodInfo bitWiseNotOperatorInt() {
        return bitWiseNotOperatorInt;
    }

    @Override
    public TypeInfo integerTypeInfo() {
        return integerTypeInfo;
    }

    @Override
    public TypeInfo intTypeInfo() {
        return intTypeInfo;
    }

    @Override
    public TypeInfo boxedBooleanTypeInfo() {
        return boxedBooleanTypeInfo;
    }

    @Override
    public TypeInfo characterTypeInfo() {
        return characterTypeInfo;
    }

    @Override
    public TypeInfo objectTypeInfo() {
        return objectTypeInfo;
    }

    @Override
    public AnnotationExpression functionalInterfaceAnnotationExpression() {
        return functionalInterfaceAnnotationExpression;
    }

    @Override
    public MethodInfo assignOperator(ParameterizedType returnType) {
        // NOTE: we have only one at the moment, no distinction between the types
        return assignOperatorInt;
    }
}
