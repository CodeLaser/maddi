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

package io.codelaser.maddi.cst.impl.output;


import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.element.Guide;

import java.util.concurrent.atomic.AtomicInteger;

@ImmutableContainer
public record GuideImpl(String name,
                        int index,
                        Position position,
                        int tabs,
                        boolean prioritySplit,
                        boolean startWithNewLine,
                        boolean endWithNewLine,
                        boolean allowNewLineBefore) implements Guide {

    public GuideImpl {
        assert position != null;
    }

    @IgnoreModifications
    private static final AtomicInteger generator = new AtomicInteger();

    public enum Position {
        START("S"), MID(""), END("E");

        private final String msg;

        Position(String msg) {
            this.msg = msg;
        }

        public String msg() {
            return msg;
        }

    }

    public static GuideGenerator generatorForBlock() {
        return new GuideGenerator("block", 1, true, true, true, true);
    }

    public static GuideGenerator generatorForEnumDefinitions() {
        return new GuideGenerator("enum", 0, false, false, true, true);
    }

    public static GuideGenerator generatorForCompanionList() {
        return new GuideGenerator("companion", 0, true, false, false, true);
    }

    public static GuideGenerator defaultGuideGenerator() {
        return new GuideGenerator("default", 1, false, false, false, false);
    }

    public static GuideGenerator multipleComments() {
        return new GuideGenerator("comments", 0, false, false, true, false);
    }

    public static GuideGenerator generatorForMultilineComment() {
        // EXPLAIN prioritySplit must be false, otherwise empty stack exception in Formatter; see TestFormatter5
        return new GuideGenerator("multiLineComment", 0, false, true, true, true);
    }

    public static GuideGenerator generatorForParameterDeclaration() {
        return new GuideGenerator("params", 1, false, true, false, false);
    }

    public static GuideGenerator generatorForAnnotationList() {
        return new GuideGenerator("annotations", 0, false, false, false, false);
    }

    @ImmutableContainer
    public static class GuideGenerator {
        public final String name;
        public final int index;
        private final int tabs;
        private final boolean prioritySplit;
        private final boolean startWithNewLine;
        private final boolean endWithNewLine;
        private final boolean allowNewLineBefore;

        private GuideGenerator(String name,
                               int tabs,
                               boolean prioritySplit,
                               boolean startWithNewLine,
                               boolean endWithNewLine,
                               boolean allowNewLineBefore) {
            index = generator.incrementAndGet();
            this.name = name;
            this.tabs = tabs;
            this.startWithNewLine = startWithNewLine;
            this.endWithNewLine = endWithNewLine;
            this.prioritySplit = prioritySplit;
            this.allowNewLineBefore = allowNewLineBefore;
        }

        public GuideImpl start() {
            return new GuideImpl(name, index, Position.START, tabs, prioritySplit, startWithNewLine, endWithNewLine, allowNewLineBefore);
        }

        public GuideImpl mid() {
            return new GuideImpl(name, index, Position.MID, tabs, prioritySplit, startWithNewLine, endWithNewLine, allowNewLineBefore);
        }

        public GuideImpl end() {
            return new GuideImpl(name, index, Position.END, tabs, prioritySplit, startWithNewLine, endWithNewLine, allowNewLineBefore);
        }

        public boolean keepGuidesWithoutMid() {
            return prioritySplit || startWithNewLine || endWithNewLine;
        }
    }


    @Override
    public String minimal() {
        return "";
    }

    @Override
    public int length(FormattingOptions options) {
        return 0;
    }

    @Override
    public String write(FormattingOptions options) {
        return "";
    }

    @Override
    public String generateJavaForDebugging() {
        return ".add(gg" + index + "." + (switch (position) {
            case START -> "start";
            case MID -> "mid";
            case END -> "end";
        }) + "()) // priority=" + prioritySplit + ", startNL=" + startWithNewLine + ", endNL=" + endWithNewLine;
    }

    @Override
    public boolean positionIsMid() {
        return position == Position.MID;
    }

    @Override
    public boolean positionIsStart() {
        return position == Position.START;
    }

    @Override
    public boolean positionIsEnd() {
        return position == Position.END;
    }
}
