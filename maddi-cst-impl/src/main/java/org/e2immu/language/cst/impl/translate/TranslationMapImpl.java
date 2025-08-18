package org.e2immu.language.cst.impl.translate;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.variable.*;
import org.e2immu.language.cst.impl.type.ParameterizedTypeImpl;
import org.e2immu.language.cst.impl.variable.DependentVariableImpl;
import org.e2immu.language.cst.impl.variable.FieldReferenceImpl;
import org.e2immu.language.cst.impl.variable.ThisImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TranslationMapImpl implements TranslationMap {

    private final Map<? extends Variable, ? extends Variable> variables;
    private final Map<MethodInfo, List<MethodInfo>> methods;
    private final Map<? extends Expression, ? extends Expression> expressions;
    private final Map<? extends Statement, List<Statement>> statements;
    private final Map<ParameterizedType, ParameterizedType> types;
    private final Map<LocalVariable, LocalVariable> localVariables;
    private final Map<? extends Variable, ? extends Expression> variableExpressions;
    private final Map<FieldInfo, FieldInfo> fieldInfoMap;
    private final Map<TypeParameter, TypeParameter> typeParameterMap;

    private final boolean expandDelayedWrappedExpressions;
    private final boolean yieldIntoReturn;
    private final boolean translateAgain;
    private final boolean clearAnalysis;
    private final ModificationTimesHandler modificationTimesHandler;
    private final TranslationMap delegate;

    private TranslationMapImpl(Map<? extends Statement, List<Statement>> statements,
                               Map<? extends Expression, ? extends Expression> expressions,
                               Map<? extends Variable, ? extends Expression> variableExpressions,
                               Map<? extends Variable, ? extends Variable> variables,
                               Map<MethodInfo, List<MethodInfo>> methods,
                               Map<ParameterizedType, ParameterizedType> types,
                               Map<FieldInfo, FieldInfo> fieldInfoMap,
                               Map<TypeParameter, TypeParameter> typeParameterMap,
                               ModificationTimesHandler modificationTimesHandler,
                               boolean expandDelayedWrappedExpressions,
                               boolean yieldIntoReturn,
                               boolean translateAgain,
                               boolean clearAnalysis,
                               TranslationMap delegate) {
        this.variables = variables;
        this.expressions = expressions;
        this.variableExpressions = variableExpressions;
        this.statements = statements;
        this.methods = methods;
        this.types = types;//checkForCycles(types);
        this.fieldInfoMap = fieldInfoMap;
        this.typeParameterMap = typeParameterMap;
        this.yieldIntoReturn = yieldIntoReturn;
        localVariables = variables.entrySet().stream()
                .filter(e -> e.getKey() instanceof LocalVariable && e.getValue() instanceof LocalVariable)
                .collect(Collectors.toMap(e -> ((LocalVariable) e.getKey()), e -> ((LocalVariable) e.getValue())));
        this.expandDelayedWrappedExpressions = expandDelayedWrappedExpressions;
        this.translateAgain = translateAgain;
        this.modificationTimesHandler = modificationTimesHandler;
        this.clearAnalysis = clearAnalysis;
        this.delegate = delegate;
    }

    public static class Builder implements TranslationMap.Builder {

        private final Map<Variable, Variable> variables = new HashMap<>();
        private final Map<Expression, Expression> expressions = new HashMap<>();
        private final Map<Variable, Expression> variableExpressions = new HashMap<>();
        private final Map<MethodInfo, List<MethodInfo>> methods = new HashMap<>();
        private final Map<Statement, List<Statement>> statements = new HashMap<>();
        private final Map<ParameterizedType, ParameterizedType> types = new HashMap<>();
        private final Map<FieldInfo, FieldInfo> fieldInfoMap = new HashMap<>();
        private final Map<TypeParameter, TypeParameter> typeParameterMap = new HashMap<>();
        private ModificationTimesHandler modificationTimesHandler;
        private boolean expandDelayedWrappedExpressions;
        private boolean doNotRecurseIntoScopeVariables;
        private boolean yieldIntoReturn;
        private boolean translateAgain;
        private boolean clearAnalysis;
        private TranslationMap delegate;

        public Builder() {
        }

        // IMPORTANT: we explicitly write TMI here rather than TM, because otherwise functionality might get lost:
        // it looks like you make a wrapper, but you don't.
        public Builder(TranslationMapImpl other) {
            variables.putAll(other.variables());
            expressions.putAll(other.expressions());
            variableExpressions.putAll(other.variableExpressions());
            methods.putAll(other.methods());
            statements.putAll(other.statements());
            types.putAll(other.types());
            fieldInfoMap.putAll(other.fieldInfoMap());
            expandDelayedWrappedExpressions = other.expandDelayedWrappedExpressions();
            yieldIntoReturn = other.translateYieldIntoReturn();
            translateAgain = other.translateAgain();
            delegate = other.delegate();
        }

        @Override
        public TranslationMap build() {
            return new TranslationMapImpl(statements, expressions, variableExpressions, variables, methods, types,
                    Map.copyOf(fieldInfoMap), Map.copyOf(typeParameterMap), modificationTimesHandler,
                    expandDelayedWrappedExpressions, yieldIntoReturn, translateAgain, clearAnalysis, delegate);
        }

        @Override
        public Builder setDelegate(TranslationMap delegate) {
            this.delegate = delegate;
            return this;
        }

        @Override
        public Builder setTranslateAgain(boolean translateAgain) {
            this.translateAgain = translateAgain;
            return this;
        }

        @Override
        public Builder put(FieldInfo template, FieldInfo actual) {
            fieldInfoMap.put(template, actual);
            return this;
        }

        @Override
        public Builder put(TypeParameter template, TypeParameter actual) {
            typeParameterMap.put(template, actual);
            return this;
        }

        @Override
        public Builder put(Statement template, Statement actual) {
            statements.put(template, List.of(actual));
            return this;
        }

        @Override
        public Builder put(MethodInfo template, List<MethodInfo> actual) {
            methods.put(template, actual);
            return this;
        }

        @Override
        public Builder put(Statement template, List<Statement> statements) {
            this.statements.put(template, statements);
            return this;
        }

        @Override
        public Builder put(Expression template, Expression actual) {
            this.expressions.put(template, actual);
            return this;
        }

        @Override
        public Builder addVariableExpression(Variable variable, Expression actual) {
            variableExpressions.put(variable, actual);
            return this;
        }

        @Override
        public Builder renameVariable(Variable variable, Expression actual) {
            variableExpressions.put(variable, actual);
            return this;
        }

        @Override
        public Builder put(ParameterizedType template, ParameterizedType actual) {
            types.put(template, actual);
            return this;
        }

        @Override
        public TranslationMap.Builder put(Variable template, Variable actual) {
            variables.put(template, actual);
            return this;
        }

        @Override
        public Builder setYieldToReturn(boolean b) {
            this.yieldIntoReturn = b;
            return this;
        }

        @Override
        public Builder setExpandDelayedWrapperExpressions(boolean expandDelayedWrappedExpressions) {
            this.expandDelayedWrappedExpressions = expandDelayedWrappedExpressions;
            return this;
        }

        @Override
        public boolean translateMethod(MethodInfo methodInfo) {
            return methods.containsKey(methodInfo);
        }

        @Override
        public Builder setModificationTimesHandler(ModificationTimesHandler modificationTimesHandler) {
            this.modificationTimesHandler = modificationTimesHandler;
            return this;
        }

        @Override
        public TranslationMap.Builder setClearAnalysis(boolean clearAnalysis) {
            this.clearAnalysis = clearAnalysis;
            return this;
        }

        @Override
        public boolean isEmpty() {
            return statements.isEmpty()
                   && expressions.isEmpty()
                   && variables.isEmpty()
                   && methods.isEmpty()
                   && types.isEmpty()
                   && variableExpressions.isEmpty();
        }
    }

    @Override
    public boolean expandDelayedWrappedExpressions() {
        return expandDelayedWrappedExpressions;
    }

    @Override
    public String toString() {
        return "TM{" + variables.size() + "," + methods.size() + "," + expressions.size() + "," + statements.size()
               + "," + types.size() + "," + localVariables.size() + "," + variableExpressions.size() +
               (expandDelayedWrappedExpressions ? ",expand" : "") + "}";
    }

    @Override
    public FieldInfo translateFieldInfo(FieldInfo fieldInfo) {
        return fieldInfoMap.getOrDefault(fieldInfo, fieldInfo);
    }

    @Override
    public boolean translateYieldIntoReturn() {
        return yieldIntoReturn;
    }

    @Override
    public boolean hasVariableTranslations() {
        return !variables.isEmpty();
    }

    private TranslationMap withoutDelegate() {
        return new TranslationMapImpl(statements, expressions, variableExpressions, variables, methods, types,
                fieldInfoMap, typeParameterMap, modificationTimesHandler, expandDelayedWrappedExpressions,
                yieldIntoReturn, translateAgain, clearAnalysis, null);
    }

    @Override
    public Expression translateExpression(Expression expression) {
        if (delegate != null) {
            Expression translatedExpression = expression.translate(delegate);
            return translatedExpression.translate(withoutDelegate());
        }
        return Objects.requireNonNullElse(expressions.get(expression), expression);
    }

    @Override
    public Variable translateVariable(Variable variable) {
        Variable v = delegate == null ? variable : delegate.translateVariable(variable);
        Variable vv = variables.get(v);
        return vv != null ? vv : variable;
    }


    @Override
    public Expression translateVariableExpressionNullIfNotTranslated(Variable variable) {
        if (delegate != null) {
            Expression e = delegate.translateVariableExpressionNullIfNotTranslated(variable);
            if (e instanceof VariableExpression ve) {
                return variableExpressions.get(ve.variable());
            }
            if (e != null) {
                return e; // not a variable expression, so we cannot delegate easily
            }
        }
        return variableExpressions.get(variable);
    }

    @Override
    public List<MethodInfo> translateMethod(MethodInfo methodInfo) {
        if (delegate != null) {
            List<MethodInfo> list = methodInfo.translate(delegate);
            TranslationMap withoutDelegate = withoutDelegate();
            return list.stream().flatMap(mi -> mi.translate(withoutDelegate).stream()).toList();
        }
        return methods.getOrDefault(methodInfo, List.of(methodInfo));
    }

    @Override
    public List<Statement> translateStatement(Statement statement) {
        if (delegate != null) {
            List<Statement> translated = statement.translate(delegate);
            TranslationMap withoutDelegate = withoutDelegate();
            return translated.stream().flatMap(s -> s.translate(withoutDelegate).stream()).toList();
        }
        List<Statement> list = statements.get(statement);
        return list == null ? List.of(statement) : list;
    }

    public ParameterizedType translateType(ParameterizedType parameterizedType) {
        ParameterizedType pt = delegate == null ? parameterizedType : delegate.translateType(parameterizedType);
        return internalTranslateType(pt);
    }

    private ParameterizedType internalTranslateType(ParameterizedType parameterizedType) {
        assert parameterizedType != null;
        ParameterizedType inMap = types.get(parameterizedType);
        if (inMap != null) return inMap;
        TypeParameter typeParameter = parameterizedType.typeParameter();
        if (typeParameter != null) {
            TypeParameter tTypeParameter = typeParameterMap.get(typeParameter);
            if (tTypeParameter != null) {
                return new ParameterizedTypeImpl(null, tTypeParameter, List.of(), parameterizedType.arrays(), parameterizedType.wildcard());
            }
        }
        List<ParameterizedType> params = parameterizedType.parameters();
        List<ParameterizedType> translatedTypes = params.isEmpty() ? params :
                params.stream().map(this::translateType).collect(toList(params));
        if (params == translatedTypes) return parameterizedType;
        return new ParameterizedTypeImpl(parameterizedType.typeInfo(), null, translatedTypes,
                parameterizedType.arrays(), parameterizedType.wildcard());
    }

    @Override
    public boolean isEmpty() {
        return statements.isEmpty() && expressions.isEmpty() && methods.isEmpty() &&
               types.isEmpty() && variables.isEmpty() && localVariables.isEmpty() && variableExpressions.isEmpty();
    }

    @Override
    public Map<? extends Variable, ? extends Variable> variables() {
        return variables;
    }

    @Override
    public Map<? extends Expression, ? extends Expression> expressions() {
        return expressions;
    }

    @Override
    public Map<? extends Variable, ? extends Expression> variableExpressions() {
        return variableExpressions;
    }

    @Override
    public Map<MethodInfo, List<MethodInfo>> methods() {
        return methods;
    }

    @Override
    public Map<ParameterizedType, ParameterizedType> types() {
        return types;
    }

    @Override
    public Map<? extends Statement, List<Statement>> statements() {
        return statements;
    }

    @Override
    public Map<FieldInfo, FieldInfo> fieldInfoMap() {
        return fieldInfoMap;
    }

    @Override
    public boolean translateAgain() {
        return translateAgain;
    }

    @Override
    public TranslationMap delegate() {
        return delegate;
    }

    @Override
    public String modificationTimes(Expression methodCallBeforeTranslation,
                                    Expression translatedObject, List<Expression> translatedParameters) {
        if (modificationTimesHandler == null) return null;
        // type cast: see interface spec: methodCallBeforeTranslation is of type Expression to avoid cyclic type dependencies
        MethodCall beforeTranslation;
        if ((beforeTranslation = methodCallBeforeTranslation.asInstanceOf(MethodCall.class)) != null) {
            return modificationTimesHandler.modificationTimes(beforeTranslation, translatedObject, translatedParameters);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClearAnalysis() {
        return clearAnalysis;
    }

    @Override
    public Variable translateVariableRecursively(Variable variable) {
        return translateVariableRecursively(this, variable);
    }

    // also accessible via runtime
    public static Variable translateVariableRecursively(TranslationMap tm, Variable variable) {
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
                return new FieldReferenceImpl(newField, tScope, null, fr.parameterizedType());
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
