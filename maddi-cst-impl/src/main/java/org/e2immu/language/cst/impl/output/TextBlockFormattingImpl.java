/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
