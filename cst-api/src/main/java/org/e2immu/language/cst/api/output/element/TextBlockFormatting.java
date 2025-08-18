package org.e2immu.language.cst.api.output.element;

import java.util.Set;

public interface TextBlockFormatting {
    Set<Integer> lineBreaks();

    boolean optOutWhiteSpaceStripping();

    boolean trailingClosingQuotes();

    interface Builder {
        Builder setTrailingClosingQuotes(boolean trailingClosingQuotes);

        Builder setOptOutWhiteSpaceStripping(boolean optOutWhiteSpaceStripping);

        Builder addLineBreak(int pos);

        TextBlockFormatting build();
    }
}
