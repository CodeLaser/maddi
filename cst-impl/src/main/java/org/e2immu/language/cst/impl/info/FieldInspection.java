package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldModifier;

import java.util.Set;

public interface FieldInspection extends Inspection {

    /*
    Expressions (currently) don't have an analysis object, so we add one here.
    Required in modification-prepwork. Only implemented on fully built FieldInspectionImpl objects.
     */
    PropertyValueMap analysisOfInitializer();

    Expression initializer();

    Set<FieldModifier> fieldModifiers();

}
