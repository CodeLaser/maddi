/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 */

package org.e2immu.language.cst.print.formatter2;

import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
Regression tests for a GREEDY_FILL spacing defect: the separator space in front of a guide block
that greedy keeps on the current line (position-0 boundary) was dropped, because the split path
never emitted the pending space level. Symptoms were `throwsMalformedURLException` (no space after
`throws`) and `{buff.append(...)` (no space after `{`).
 */
public class TestGreedySpacing {
    private final Runtime runtime = new RuntimeImpl();

    private String greedy(int width) {
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(width).setSpacesInTab(4)
                .setWrapStyle(FormattingOptions.WrapStyle.GREEDY_FILL).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        return formatter.write(Test6.create1());
    }

    @Test
    public void spaceBeforeThrowsClauseIsKept() {
        for (int width : new int[]{120, 80, 60, 40}) {
            String out = greedy(width);
            assertFalse(out.contains("throwsMalformedURLException"),
                    "space after 'throws' dropped at width " + width + ":\n" + out);
            assertTrue(out.contains("throws MalformedURLException") || out.contains("throws\n"),
                    "expected a well-separated throws clause at width " + width + ":\n" + out);
        }
    }

    @Test
    public void spaceAfterOpeningBraceIsKept() {
        // width 60 is where the if-body opens with its first statement greedily kept on the
        // same line as '{'; the separator space must survive.
        String out = greedy(60);
        assertFalse(out.contains("null) {buff"),
                "space after '{' dropped:\n" + out);
        assertTrue(out.contains("null) { buff"),
                "expected a space after '{':\n" + out);
    }
}
