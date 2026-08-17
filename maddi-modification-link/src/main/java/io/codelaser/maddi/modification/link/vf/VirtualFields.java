package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.jetbrains.annotations.NotNull;

/*
The (mutable, hiddenContent) pair of virtual fields computed for a type; see vf/virtual-fields.md.
Virtual fields are recomputed on demand, so this is a plain value object: it used to implement the cst 'Value'
interface with a 'VIRTUAL_FIELDS' property and an (unsupported) encode(), but nothing ever stored it on an
analysis() or encoded it, so that machinery was removed.
 */
public record VirtualFields(FieldInfo mutable, FieldInfo hiddenContent) {
    public static VirtualFields NONE = new VirtualFields(null, null);

    @Override
    public @NotNull String toString() {
        return (mutable == null ? "/" : mutable.simpleName()) + " - "
               + (hiddenContent == null ? "/"
                : hiddenContent.type().simpleString() + " " + hiddenContent.name());
    }
}
