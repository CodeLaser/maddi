package org.e2immu.language.cst.impl.analysis;

import org.e2immu.language.cst.api.analysis.Property;

import java.util.*;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;

public class PropertyProviderImpl {

    private PropertyProviderImpl() {}

    private static final List<Property> properties = new ArrayList<>();
    private static final Map<String, Property> propertyMap = new HashMap<>();

    static {
        Collections.addAll(properties,
                ALWAYS_ESCAPES,
                ANALYZER_ERROR,
                ANNOTATED_API,
                COMMUTABLE_METHODS,
                CONTAINER_FIELD,
                CONTAINER_METHOD,
                CONTAINER_PARAMETER,
                CONTAINER_TYPE,
                DEFAULTS_ANALYZER,
                FINAL_FIELD,
                FINAL_TYPE,
                FINALIZER_METHOD,
                FLUENT_METHOD,
                GET_SET_EQUIVALENT,
                GET_SET_FIELD,
                IDENTITY_METHOD,
                IGNORE_MODIFICATIONS_FIELD,
                IGNORE_MODIFICATION_METHOD,
                IGNORE_MODIFICATIONS_PARAMETER,
                IMMUTABLE_FIELD,
                IMMUTABLE_METHOD,
                IMMUTABLE_PARAMETER,
                IMMUTABLE_TYPE,
                IMMUTABLE_TYPE_INDEPENDENT_OF_TYPE_PARAMETERS,
                INDEPENDENT_FIELD,
                INDEPENDENT_METHOD,
                INDEPENDENT_PARAMETER,
                INDEPENDENT_TYPE,
                INDICES_OF_ESCAPE_METHOD);
        Collections.addAll(properties,
                METHOD_ALLOWS_INTERRUPTS,
                MODIFIED_FI_COMPONENTS_PARAMETER,
                MODIFIED_COMPONENTS_METHOD,
                MODIFIED_COMPONENTS_PARAMETER,
                NON_MODIFYING_METHOD,
                NOT_NULL_FIELD,
                NOT_NULL_METHOD,
                NOT_NULL_PARAMETER,
                OWN_FIELDS_READ_MODIFIED_IN_METHOD,
                PARALLEL_PARAMETER_GROUPS,
                PARAMETER_ASSIGNED_TO_FIELD,
                POST_CONDITIONS_METHOD,
                PRECONDITION_METHOD,
                UNMODIFIED_FIELD,
                UNMODIFIED_PARAMETER,
                UTILITY_CLASS
        );
        properties.forEach(p -> propertyMap.put(p.key(), p));
    }

    public static Property get(String propertyName) {
        return propertyMap.get(propertyName);
    }
}
