package io.codelaser.maddi.cst.api.info;

import io.codelaser.maddi.cst.api.output.element.Keyword;

/**
 * Base interface for all modifier tokens that can appear on a type, method, or field declaration
 * ({@code public}, {@code static}, {@code final}, …).
 * <p>
 * Concrete sub-interfaces ({@link TypeModifier}, {@link MethodModifier}, {@link FieldModifier})
 * extend this with element-specific predicates.
 */
public interface Modifier {
    /** Returns the {@link Keyword} token used to print this modifier in source form. */
    Keyword keyword();
}
