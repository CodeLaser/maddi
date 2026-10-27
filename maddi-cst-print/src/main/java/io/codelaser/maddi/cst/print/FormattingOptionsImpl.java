/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.print;

import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.cst.api.output.FormattingOptions;

public record FormattingOptionsImpl(int lengthOfLine,
                                int spacesInTab,
                                boolean compact,
                                boolean allFieldsRequireThis,
                                boolean allStaticFieldsRequireType,
                                boolean skipComments,
                                WrapStyle wrapStyle,
                                boolean alwaysBreakPriorityBlocks,
                                boolean bracesOnOwnLine,
                                boolean blankLinesFromSource) implements FormattingOptions {

    public static final FormattingOptions DEFAULT = new Builder().build();

    @Container
    public static class Builder {

        private int lengthOfLine;
        private int spacesInTab;
        private boolean compact;
        private boolean allFieldsRequireThis;
        private boolean allStaticFieldsRequireType;
        private boolean skipComments;
        private FormattingOptions.WrapStyle wrapStyle;
        private boolean alwaysBreakPriorityBlocks;
        private boolean bracesOnOwnLine;
        private boolean blankLinesFromSource;

        public Builder() {
            this.lengthOfLine = 120;
            this.spacesInTab = 4;
            this.wrapStyle = FormattingOptions.WrapStyle.CHOP_DOWN;
        }

        public Builder(FormattingOptions options) {
            this.lengthOfLine = options.lengthOfLine();
            this.spacesInTab = options.spacesInTab();
            this.compact = options.compact();
            this.allFieldsRequireThis = options.allFieldsRequireThis();
            this.allStaticFieldsRequireType = options.allStaticFieldsRequireType();
            this.skipComments = options.skipComments();
            this.wrapStyle = options.wrapStyle();
            this.alwaysBreakPriorityBlocks = options.alwaysBreakPriorityBlocks();
            this.bracesOnOwnLine = options.bracesOnOwnLine();
            this.blankLinesFromSource = options.blankLinesFromSource();
        }

        public Builder setLengthOfLine(int lengthOfLine) {
            this.lengthOfLine = lengthOfLine;
            return this;
        }

        public Builder setSpacesInTab(int spacesInTab) {
            this.spacesInTab = spacesInTab;
            return this;
        }

        public Builder setCompact(boolean compact) {
            this.compact = compact;
            if (compact) {
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

        public Builder setWrapStyle(FormattingOptions.WrapStyle wrapStyle) {
            this.wrapStyle = wrapStyle;
            return this;
        }

        public Builder setAlwaysBreakPriorityBlocks(boolean alwaysBreakPriorityBlocks) {
            this.alwaysBreakPriorityBlocks = alwaysBreakPriorityBlocks;
            return this;
        }

        public Builder setBracesOnOwnLine(boolean bracesOnOwnLine) {
            this.bracesOnOwnLine = bracesOnOwnLine;
            return this;
        }

        public Builder setBlankLinesFromSource(boolean blankLinesFromSource) {
            this.blankLinesFromSource = blankLinesFromSource;
            return this;
        }

        @NotModified
        public FormattingOptions build() {
            return new FormattingOptionsImpl(lengthOfLine, spacesInTab, compact, allFieldsRequireThis,
                    allStaticFieldsRequireType, skipComments, wrapStyle, alwaysBreakPriorityBlocks, bracesOnOwnLine,
                    blankLinesFromSource);
        }
    }
}
