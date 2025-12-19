package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.List;

public interface MethodLinkedVariables extends Value {
    Links ofReturnValue();

    List<Links> ofParameters();

    MethodLinkedVariables translate(TranslationMap translationMap);
}
