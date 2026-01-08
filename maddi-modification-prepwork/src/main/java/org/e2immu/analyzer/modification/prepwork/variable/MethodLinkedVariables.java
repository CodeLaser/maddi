package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;
import java.util.Set;

public interface MethodLinkedVariables extends Value {
    // content

    Set<Variable> modified();

    Links ofReturnValue();

    List<Links> ofParameters();

    // helper

    default boolean isEmpty() {
        return ofParameters().stream().allMatch(Links::isEmpty) && ofReturnValue().isEmpty();
    }

    MethodLinkedVariables removeSomeValue();

    MethodLinkedVariables translate(TranslationMap translationMap);

    boolean virtual();
}
