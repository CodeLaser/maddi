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

package org.e2immu.language.cst.print;

import org.e2immu.annotation.Container;
import org.e2immu.annotation.NotModified;
import org.e2immu.language.cst.api.output.FormattingOptions;

public record FormattingOptionsImpl(int lengthOfLine,
                                int spacesInTab,
                                int tabsForLineSplit,
                                boolean binaryOperatorsAtEndOfLine,
                                boolean compact,
                                boolean allFieldsRequireThis,
                                boolean allStaticFieldsRequireType,
                                boolean skipComments) implements FormattingOptions {

    public static final FormattingOptions DEFAULT = new Builder().build();

    @Container
    public static class Builder {

        private int lengthOfLine;
        private int spacesInTab;
        private int tabsForLineSplit;
        private boolean binaryOperatorsAtEndOfLine;
        private boolean compact;
        private boolean allFieldsRequireThis;
        private boolean allStaticFieldsRequireType;
        private boolean skipComments;

        public Builder() {
            this.lengthOfLine = 120;
            this.spacesInTab = 4;
            this.tabsForLineSplit = 2;
            this.binaryOperatorsAtEndOfLine = true;
        }

        public Builder(FormattingOptions options) {
            this.lengthOfLine = options.lengthOfLine();
            this.spacesInTab = options.spacesInTab();
            this.tabsForLineSplit = options.tabsForLineSplit();
            this.binaryOperatorsAtEndOfLine = options.binaryOperatorsAtEndOfLine();
        }

        public Builder setLengthOfLine(int lengthOfLine) {
            this.lengthOfLine = lengthOfLine;
            return this;
        }

        public Builder setSpacesInTab(int spacesInTab) {
            this.spacesInTab = spacesInTab;
            return this;
        }

        public Builder setTabsForLineSplit(int tabsForLineSplit) {
            this.tabsForLineSplit = tabsForLineSplit;
            return this;
        }

        public Builder setBinaryOperatorsAtEndOfLine(boolean binaryOperatorsAtEndOfLine) {
            this.binaryOperatorsAtEndOfLine = binaryOperatorsAtEndOfLine;
            return this;
        }

        public Builder setCompact(boolean compact) {
            this.compact = compact;
            if (compact) {
                this.tabsForLineSplit = 0;
                this.spacesInTab = 0;
            }
            return this;
        }

        public Builder setAllFieldsRequireThis(boolean allFieldsRequireThis) {
            this.allFieldsRequireThis = allFieldsRequireThis;
            return this;
        }

        public Builder setAllStaticFieldsRequireType(boolean allStaticFieldsRequireType) {
            this.allStaticFieldsRequireType = allStaticFieldsRequireType;
            return this;
        }

        public Builder setSkipComments(boolean skipComments) {
            this.skipComments = skipComments;
            return this;
        }

        @NotModified
        public FormattingOptions build() {
            return new FormattingOptionsImpl(lengthOfLine, spacesInTab, tabsForLineSplit, binaryOperatorsAtEndOfLine, compact,
                    allFieldsRequireThis, allStaticFieldsRequireType, skipComments);
        }
    }
}