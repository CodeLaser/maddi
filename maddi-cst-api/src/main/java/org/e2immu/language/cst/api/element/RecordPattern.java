package org.e2immu.language.cst.api.element;

import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;

import java.util.List;

public interface RecordPattern extends Element {
    // situation 1

    boolean unnamedPattern(); // "_" without a type specification

    // situation 2

    LocalVariable localVariable(); // "Circle c" or "Circle _", "var _", but not "_"

    // situation 3

    ParameterizedType recordType(); // Box<String>(...)

    List<RecordPattern> patterns();

    RecordPattern translate(TranslationMap translationMap);

    interface Builder extends Element.Builder<Builder> {
        Builder setUnnamedPattern(boolean unnamedPattern);

        Builder setLocalVariable(LocalVariable localVariable);

        Builder setRecordType(ParameterizedType recordType);

        Builder setPatterns(List<RecordPattern> patterns);

        RecordPattern build();
    }
}
