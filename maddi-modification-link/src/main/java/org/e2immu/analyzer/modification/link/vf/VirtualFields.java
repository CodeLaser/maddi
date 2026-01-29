package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.jetbrains.annotations.NotNull;

public record VirtualFields(FieldInfo mutable, FieldInfo hiddenContent) implements Value {
    public static VirtualFields NONE = new VirtualFields(null, null);
    public static final PropertyImpl VIRTUAL_FIELDS = new PropertyImpl("virtualFields", NONE);

    @Override
    public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDefault() {
        return equals(NONE);
    }

    @Override
    public @NotNull String toString() {
        return (mutable == null ? "/" : mutable.simpleName()) + " - "
               + (hiddenContent == null ? "/"
                : hiddenContent.type().simpleString() + " " + hiddenContent.name());
    }
}
