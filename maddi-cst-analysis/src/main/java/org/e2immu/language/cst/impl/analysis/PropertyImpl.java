package org.e2immu.language.cst.impl.analysis;

import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;

public class PropertyImpl implements Property {
    // type
    public static final Property IMMUTABLE_TYPE = new PropertyImpl("immutableType",
            ValueImpl.ImmutableImpl.MUTABLE);
    public static final Property CONTAINER_TYPE = new PropertyImpl("containerType");
    public static final Property INDEPENDENT_TYPE = new PropertyImpl("independentType",
            ValueImpl.IndependentImpl.DEPENDENT);
    public static final Property IMMUTABLE_TYPE_INDEPENDENT_OF_TYPE_PARAMETERS
            = new PropertyImpl("immutableTypeDeterminedByParameters");
    public static final Property FINAL_TYPE = new PropertyImpl("finalType");
    public static final Property UTILITY_CLASS = new PropertyImpl("utilityClass");

    // method
    public static final Property NON_MODIFYING_METHOD = new PropertyImpl("nonModifyingMethod");
    public static final Property MODIFIED_COMPONENTS_METHOD = new PropertyImpl("modifiedComponentsMethod",
            ValueImpl.VariableBooleanMapImpl.EMPTY);
    public static final Property FLUENT_METHOD = new PropertyImpl("fluentMethod");
    public static final Property IDENTITY_METHOD = new PropertyImpl("identityMethod");
    public static final Property NOT_NULL_METHOD = new PropertyImpl("notNullMethod", ValueImpl.NotNullImpl.NULLABLE);
    public static final Property IGNORE_MODIFICATION_METHOD = new PropertyImpl("ignoreModMethod");
    public static final Property POST_CONDITIONS_METHOD = new PropertyImpl("postConditionsMethod",
            ValueImpl.PostConditionsImpl.EMPTY);
    public static final Property PRECONDITION_METHOD = new PropertyImpl("preconditionMethod",
            ValueImpl.PreconditionImpl.EMPTY);
    public static final Property INDICES_OF_ESCAPE_METHOD = new PropertyImpl("indicesOfEscapesNotInPrePostCondition",
            ValueImpl.IndicesOfEscapesImpl.EMPTY);
    public static final Property METHOD_ALLOWS_INTERRUPTS = new PropertyImpl("methodAllowsInterrupts");
    public static final Property OWN_FIELDS_READ_MODIFIED_IN_METHOD = new PropertyImpl("areOwnFieldsReadModified",
            ValueImpl.FieldBooleanMapImpl.EMPTY);
    public static final Property INDEPENDENT_METHOD = new PropertyImpl("independentMethod",
            ValueImpl.IndependentImpl.DEPENDENT);
    public static final Property FINALIZER_METHOD = new PropertyImpl("finalizerMethod");
    // dynamic return type
    public static final Property IMMUTABLE_METHOD = new PropertyImpl("immutableMethod"
            , ValueImpl.ImmutableImpl.MUTABLE);
    public static final Property CONTAINER_METHOD = new PropertyImpl("containerMethod");

    // commutation on methods
    public static final Property PARALLEL_PARAMETER_GROUPS = new PropertyImpl("parallelParameterGroups",
            ValueImpl.ParameterParSeqImpl.EMPTY);
    public static final Property COMMUTABLE_METHODS = new PropertyImpl("commutableMethods",
            ValueImpl.CommutableDataImpl.NONE);
    public static final Property GET_SET_FIELD = new PropertyImpl("getSetField",
            ValueImpl.GetSetValueImpl.EMPTY);
    public static final Property GET_SET_EQUIVALENT = new PropertyImpl("getSetEquivalent",
            ValueImpl.GetSetEquivalentImpl.EMPTY);

    // parameter
    public static final Property UNMODIFIED_PARAMETER = new PropertyImpl("unmodifiedParameter");
    public static final Property MODIFIED_FI_COMPONENTS_PARAMETER = new PropertyImpl("modifiedFunctionalInterfaceComponentsParameter",
            ValueImpl.VariableBooleanMapImpl.EMPTY);
    public static final Property MODIFIED_COMPONENTS_PARAMETER = new PropertyImpl("modifiedComponentsParameter",
            ValueImpl.VariableBooleanMapImpl.EMPTY);
    public static final Property IGNORE_MODIFICATIONS_PARAMETER = new PropertyImpl("ignoreModsParameter");
    public static final Property PARAMETER_ASSIGNED_TO_FIELD = new PropertyImpl("parameterAssignedToField",
            ValueImpl.AssignedToFieldImpl.EMPTY);
    public static final Property NOT_NULL_PARAMETER = new PropertyImpl("notNullParameter", ValueImpl.NotNullImpl.NULLABLE);
    public static final Property IMMUTABLE_PARAMETER = new PropertyImpl("immutableParameter"
            , ValueImpl.ImmutableImpl.MUTABLE);
    public static final Property CONTAINER_PARAMETER = new PropertyImpl("containerParameter");
    public static final Property INDEPENDENT_PARAMETER = new PropertyImpl("independentParameter",
            ValueImpl.IndependentImpl.DEPENDENT);
    public static final Property DOWNCAST_PARAMETER = new PropertyImpl("downcastParameter", ValueImpl.SetOfTypeInfoImpl.EMPTY);

    // field
    public static final Property FINAL_FIELD = new PropertyImpl("finalField");
    public static final Property NOT_NULL_FIELD = new PropertyImpl("notNullField", ValueImpl.NotNullImpl.NULLABLE);
    public static final Property IGNORE_MODIFICATIONS_FIELD = new PropertyImpl("ignoreModificationsField");
    public static final Property UNMODIFIED_FIELD = new PropertyImpl("unmodifiedField");
    public static final Property IMMUTABLE_FIELD = new PropertyImpl("immutableField"
            , ValueImpl.ImmutableImpl.MUTABLE);
    public static final Property CONTAINER_FIELD = new PropertyImpl("containerField");
    public static final Property INDEPENDENT_FIELD = new PropertyImpl("independentField",
            ValueImpl.IndependentImpl.DEPENDENT);
    public static final Property DOWNCAST_FIELD = new PropertyImpl("downcastField", ValueImpl.SetOfTypeInfoImpl.EMPTY);

    // statement
    public static final Property ALWAYS_ESCAPES = new PropertyImpl("statementAlwaysEscapes");

    // any element
    public static final Property DEFAULTS_ANALYZER = new PropertyImpl("defaultsAnalyzer");
    public static final Property ANNOTATED_API = new PropertyImpl("annotatedApi");
    public static final Property ANALYZER_ERROR = new PropertyImpl("analyzerError", ValueImpl.MessageImpl.EMPTY);

    // type parameter
    public static final Property INDEPENDENT_TYPE_PARAMETER = new PropertyImpl("independentTypeParameter",
            ValueImpl.IndependentImpl.DEPENDENT);

    private final String key;
    private final Value defaultValue;

    public PropertyImpl(String key) {
        this(key, ValueImpl.BoolImpl.FALSE);
    }

    public PropertyImpl(String key, Value defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public Value defaultValue() {
        return defaultValue;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Class<? extends Value> classOfValue() {
        return defaultValue.getClass();
    }

    @Override
    public String toString() {
        return key;
    }
}
