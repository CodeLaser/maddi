package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.List;

public interface MethodLinkedVariables extends Value {
    default boolean isEmpty() {
        return ofParameters().stream().allMatch(Links::isEmpty) && ofReturnValue().isEmpty();
    }

    Links ofReturnValue();

    List<Links> ofParameters();

    MethodLinkedVariables removeSomeValue();

    MethodLinkedVariables translate(TranslationMap translationMap);

    boolean virtual();
}
