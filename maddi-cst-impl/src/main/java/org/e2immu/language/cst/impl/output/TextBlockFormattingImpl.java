package org.e2immu.language.cst.impl.output;

import org.e2immu.language.cst.api.output.element.TextBlockFormatting;

import java.util.HashSet;
import java.util.Set;

public record TextBlockFormattingImpl(Set<Integer> lineBreaks,
                                      boolean optOutWhiteSpaceStripping,
                                      boolean trailingClosingQuotes) implements TextBlockFormatting {

    public static class Builder implements TextBlockFormatting.Builder {
        private final Set<Integer> lineBreaks = new HashSet<>();
        private boolean optOutWhiteSpaceStripping;
        private boolean trailingClosingQuotes;

        @Override
        public TextBlockFormatting.Builder setTrailingClosingQuotes(boolean trailingClosingQuotes) {
            this.trailingClosingQuotes = trailingClosingQuotes;
            return this;
        }

        @Override
        public TextBlockFormatting.Builder setOptOutWhiteSpaceStripping(boolean optOutWhiteSpaceStripping) {
            this.optOutWhiteSpaceStripping = optOutWhiteSpaceStripping;
            return this;
        }

        @Override
        public TextBlockFormatting.Builder addLineBreak(int pos) {
            lineBreaks.add(pos);
            return this;
        }

        @Override
        public TextBlockFormatting build() {
            return new TextBlockFormattingImpl(Set.copyOf(lineBreaks), optOutWhiteSpaceStripping, trailingClosingQuotes);
        }
    }
}
