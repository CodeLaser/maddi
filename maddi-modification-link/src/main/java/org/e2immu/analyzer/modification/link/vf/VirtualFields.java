package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.jetbrains.annotations.NotNull;

public record VirtualFields(FieldInfo mutable, FieldInfo hiddenContent) {
    public static VirtualFields NONE = new VirtualFields(null, null);

    @Override
    public @NotNull String toString() {
        return mutable == null ? "/" : mutable.simpleName() + " - "
                                       + hiddenContent.type().simpleString() + " " + hiddenContent.name();
    }
}
