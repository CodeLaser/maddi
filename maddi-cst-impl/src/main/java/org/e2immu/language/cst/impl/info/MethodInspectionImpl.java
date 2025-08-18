package org.e2immu.language.cst.impl.info;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.info.Access;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.MethodModifier;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.impl.type.DiamondEnum;
import org.e2immu.language.cst.impl.variable.LocalVariableImpl;
import org.e2immu.support.SetOnce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MethodInspectionImpl extends InspectionImpl implements MethodInspection {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodInspectionImpl.class);

    public enum MissingDataEnum {
        METHOD_BODY, OVERRIDES;
    }

    public record MissingDataImpl(EnumSet<MissingDataEnum> set) implements MethodInfo.MissingData {

        @Override
        public boolean methodBody() {
            return set.contains(MissingDataEnum.METHOD_BODY);
        }

        @Override
        public boolean overrides() {
            return set.contains(MissingDataEnum.OVERRIDES);
        }
    }

    private final ParameterizedType returnType;
    private final OperatorType operatorType;
    private final Block methodBody;
    private final String fullyQualifiedName;
    private final Set<MethodInfo> overrides;
    private final List<TypeParameter> typeParameters;
    private final List<ParameterInfo> parameters;
    private final Set<MethodModifier> methodModifiers;
    private final List<ParameterizedType> exceptionTypes;
    private final MethodInfo.MissingData missingData;

    public MethodInspectionImpl(Inspection inspection,
                                ParameterizedType returnType,
                                List<TypeParameter> typeParameters,
                                List<ParameterInfo> parameters,
                                Set<MethodModifier> methodModifiers,
                                List<ParameterizedType> exceptionTypes,
                                OperatorType operatorType,
                                Block methodBody,
                                String fullyQualifiedName,
                                Set<MethodInfo> overrides,
                                MethodInfo.MissingData missingData) {
        this(inspection, inspection.isSynthetic(), returnType, typeParameters, parameters, methodModifiers,
                exceptionTypes, operatorType, methodBody, fullyQualifiedName, overrides, missingData);
    }

    public MethodInspectionImpl(Inspection inspection,
                                boolean synthetic,
                                ParameterizedType returnType,
                                List<TypeParameter> typeParameters,
                                List<ParameterInfo> parameters,
                                Set<MethodModifier> methodModifiers,
                                List<ParameterizedType> exceptionTypes,
                                OperatorType operatorType,
                                Block methodBody,
                                String fullyQualifiedName,
                                Set<MethodInfo> overrides,
                                MethodInfo.MissingData missingData) {
        super(inspection.access(), inspection.comments(), inspection.source(), synthetic, inspection.annotations(),
                inspection.javaDoc());
        this.returnType = returnType;
        this.operatorType = operatorType;
        this.parameters = parameters;
        this.methodBody = methodBody;
        this.fullyQualifiedName = fullyQualifiedName;
        this.overrides = overrides;
        this.typeParameters = typeParameters;
        this.methodModifiers = methodModifiers;
        this.exceptionTypes = exceptionTypes;
        this.missingData = missingData;
    }

    @Override
    public MethodInspection withSynthetic(boolean synthetic) {
        return new MethodInspectionImpl(this, synthetic, returnType, typeParameters, parameters,
                methodModifiers, exceptionTypes, operatorType, methodBody, fullyQualifiedName, overrides, missingData);
    }

    @Override
    public List<ParameterizedType> exceptionTypes() {
        return exceptionTypes;
    }

    @Override
    public List<TypeParameter> typeParameters() {
        return typeParameters;
    }

    @Override
    public Set<MethodModifier> modifiers() {
        return methodModifiers;
    }

    @Override
    public Set<MethodInfo> overrides() {
        return overrides;
    }

    @Override
    public ParameterizedType returnType() {
        return returnType;
    }

    @Override
    public OperatorType operatorType() {
        return operatorType;
    }

    @Override
    public Block methodBody() {
        return methodBody;
    }

    @Override
    public String fullyQualifiedName() {
        return fullyQualifiedName;
    }

    @Override
    public List<ParameterInfo> parameters() {
        return parameters;
    }

    @Override
    public MethodInfo.MissingData missingData() {
        return missingData;
    }

    public static class Builder extends InspectionImpl.Builder<MethodInfo.Builder> implements MethodInspection, MethodInfo.Builder {
        private ParameterizedType returnType;
        private OperatorType operatorType;
        private Block methodBody;
        private MethodInfo.MissingData missingData = new MissingDataImpl(EnumSet.noneOf(MissingDataEnum.class));
        private final SetOnce<String> fullyQualifiedName = new SetOnce<>();
        private final List<ParameterInfo> parameters = new ArrayList<>();
        private final List<TypeParameter> typeParameters = new ArrayList<>();
        private final Set<MethodInfo> overrides = new HashSet<>();
        private final MethodInfoImpl methodInfo;
        private final Set<MethodModifier> methodModifiers = new HashSet<>();
        private final List<ParameterizedType> exceptionTypes = new ArrayList<>();

        public Builder(MethodInfoImpl methodInfo) {
            this.methodInfo = methodInfo;
            if (methodInfo.isStatic()) {
                addMethodModifier(MethodModifierEnum.STATIC);
            }
        }

        @Override
        public MethodInfo.MissingData missingData() {
            return missingData;
        }

        @Override
        public MethodInspection withSynthetic(boolean synthetic) {
            setSynthetic(synthetic);
            return this;
        }

        @Override
        public MethodInfo.Builder addExceptionType(ParameterizedType exceptionType) {
            this.exceptionTypes.add(exceptionType);
            return this;
        }

        @Override
        public List<ParameterizedType> exceptionTypes() {
            return exceptionTypes;
        }

        @Fluent
        public Builder setReturnType(ParameterizedType returnType) {
            this.returnType = returnType;
            return this;
        }

        @Override
        public ParameterInfo addParameter(String name, ParameterizedType type) {
            assert name != null;
            assert type != null;
            ParameterInfo pi = new ParameterInfoImpl(methodInfo, parameters.size(), name, type);
            parameters.add(pi);
            return pi;
        }

        @Override
        public ParameterInfo addUnnamedParameter(ParameterizedType type) {
            assert type != null;
            ParameterInfo pi = new ParameterInfoImpl(methodInfo, parameters.size(), LocalVariableImpl.UNNAMED, type);
            parameters.add(pi);
            return pi;
        }

        @Override
        public Builder addTypeParameter(TypeParameter typeParameter) {
            assert typeParameter.isMethodTypeParameter();
            assert typeParameter.getOwner().getRight() == methodInfo;
            typeParameters.add(typeParameter);
            return this;
        }

        @Override
        public Set<MethodInfo> overrides() {
            return overrides;
        }

        @Override
        public Builder addOverrides(Collection<MethodInfo> overrides) {
            this.overrides.addAll(overrides);
            return this;
        }

        @Override
        public List<TypeParameter> typeParameters() {
            return typeParameters;
        }

        @Override
        public Set<MethodModifier> modifiers() {
            return methodModifiers;
        }

        @Override
        public ParameterizedType returnType() {
            return returnType;
        }

        @Override
        public OperatorType operatorType() {
            return operatorType;
        }

        @Fluent
        public Builder setOperatorType(OperatorType operatorType) {
            this.operatorType = operatorType;
            return this;
        }

        @Override
        public Block methodBody() {
            return methodBody;
        }

        @Override
        public Builder commitParameters() {
            fullyQualifiedName.set(computeFQN());
            return this;
        }

        private String computeFQN() {
            String owner = methodInfo.typeInfo().fullyQualifiedName();
            try {
                return owner + "." + methodInfo.name() + "(" + parameters.stream()
                        .map(p -> p.parameterizedType().printForMethodFQN(p.isVarArgs(), DiamondEnum.SHOW_ALL))
                        .collect(Collectors.joining(",")) + ")";
            } catch (RuntimeException re) {
                LOGGER.error("Cannot compute fully qualified method name, type {}, method {}, {} params",
                        owner, methodInfo.name(), parameters.size());
                throw re;
            }
        }

        @Override
        public boolean hasBeenCommitted() {
            return methodInfo.hasBeenCommitted();
        }

        @Override
        public Builder computeAccess() {
            if (methodInfo.isCompactConstructor()) {
                setAccess(AccessEnum.PUBLIC);
            } else if (methodModifiers.stream().anyMatch(MethodModifier::isPrivate)) {
                setAccess(AccessEnum.PRIVATE);
            } else {
                boolean isInterface = methodInfo.typeInfo().isInterface();
                if (isInterface && (methodInfo.isAbstract() || methodInfo.isDefault() || methodInfo.isStatic())) {
                    setAccess(AccessEnum.PUBLIC);
                } else {
                    Access fromModifier = accessFromMethodModifier();
                    setAccess(fromModifier);
                }
            }
            return this;
        }

        private Access accessFromMethodModifier() {
            for (MethodModifier methodModifier : methodModifiers) {
                if (methodModifier.isPrivate()) return AccessEnum.PRIVATE;
                if (methodModifier.isPublic()) return AccessEnum.PUBLIC;
                if (methodModifier.isProtected()) return AccessEnum.PROTECTED;
            }
            return AccessEnum.PACKAGE;
        }

        @Override
        public void commit() {
            if (!fullyQualifiedName.isSet()) commitParameters();
            MethodInspection mi = new MethodInspectionImpl(this, returnType, List.copyOf(typeParameters),
                    List.copyOf(parameters), Set.copyOf(methodModifiers), List.copyOf(exceptionTypes),
                    operatorType, methodBody, fullyQualifiedName.get(), Set.copyOf(overrides), missingData);
            methodInfo.commit(mi);
        }

        @Fluent
        public Builder setMethodBody(Block methodBody) {
            this.methodBody = methodBody;
            return this;
        }

        @Override
        public Builder addMethodModifier(MethodModifier methodModifier) {
            methodModifiers.add(methodModifier);
            return this;
        }

        @Override
        public Builder addAndCommitParameter(String name, ParameterizedType type) {
            ParameterInfo pi = addParameter(name, type);
            pi.builder().commit();
            return this;
        }

        @Override
        public String fullyQualifiedName() {
            return fullyQualifiedName.getOrDefault("?.?." + methodInfo.name());
        }

        @Override
        public List<ParameterInfo> parameters() {
            return parameters;
        }

        @Override
        public MethodInfo.Builder addParameter(ParameterInfo parameterInfo) {
            parameters.add(parameterInfo);
            return this;
        }

        @Override
        public Builder setMissingData(MethodInfo.MissingData missingData) {
            this.missingData = missingData;
            return this;
        }
    }

}
