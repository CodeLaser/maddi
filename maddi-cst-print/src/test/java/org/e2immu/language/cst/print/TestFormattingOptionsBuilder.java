/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 */

package org.e2immu.language.cst.print;

import org.e2immu.language.cst.api.output.FormattingOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFormattingOptionsBuilder {

    /*
    The copy constructor Builder(FormattingOptions) must reproduce every field of the source
    options; a missing field silently reverts to a default when options are round-tripped
    through the builder (e.g. to tweak a single setting).
     */
    @Test
    public void copyConstructorPreservesAllFields() {
        FormattingOptions original = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(97)
                .setSpacesInTab(3)
                .setTabsForLineSplit(5)
                .setBinaryOperatorsAtEndOfLine(false)
                .setAllFieldsRequireThis(true)
                .setAllStaticFieldsRequireType(true)
                .setSkipComments(true)
                .setWrapStyle(FormattingOptions.WrapStyle.GREEDY_FILL)
                .build();

        FormattingOptions copy = new FormattingOptionsImpl.Builder(original).build();

        assertEquals(original.lengthOfLine(), copy.lengthOfLine());
        assertEquals(original.spacesInTab(), copy.spacesInTab());
        assertEquals(original.tabsForLineSplit(), copy.tabsForLineSplit());
        assertEquals(original.binaryOperatorsAtEndOfLine(), copy.binaryOperatorsAtEndOfLine());
        assertEquals(original.compact(), copy.compact());
        assertEquals(original.allFieldsRequireThis(), copy.allFieldsRequireThis());
        assertEquals(original.allStaticFieldsRequireType(), copy.allStaticFieldsRequireType());
        assertEquals(original.skipComments(), copy.skipComments());
        assertEquals(original.wrapStyle(), copy.wrapStyle());
        assertEquals(original, copy);
    }
}
