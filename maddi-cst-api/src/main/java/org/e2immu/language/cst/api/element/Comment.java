package org.e2immu.language.cst.api.element;

import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.translate.TranslationMap;

public interface Comment extends Element {

    String comment();

    default Comment rewire(InfoMap infoMap) {
        return this;
    }

    default Comment translate(TranslationMap translationMap) {
        return this;
    }
}
