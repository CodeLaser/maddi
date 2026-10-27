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

package io.codelaser.maddi.cst.print.formatter2;

import io.codelaser.maddi.cst.api.output.Formatter;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.OutputElement;
import io.codelaser.maddi.cst.api.output.element.Guide;
import io.codelaser.maddi.cst.api.runtime.Runtime;

import java.util.*;

/*
 Block structure: fully recursive

 element
 element (last element before start)
 block + guide
    block (tab + 1)
        element (first element after start)
        element
    block (tab + 1)
        element (first element after first mid)
        element
    block (tab + 1)
        element (first element after 2nd mid)
        ...
        element (last element before end)
 element (first element after end)

 */
public record Formatter2Impl(Runtime runtime, FormattingOptions options) implements Formatter {
    @Override
    public String write(OutputBuilder outputBuilder) {
        List<OutputElement> list = options.skipComments() ? Util.removeComments(outputBuilder.list())
                : outputBuilder.list();
        Iterator<OutputElement> iterator = list.iterator();
        MidBlock mb = collectElements(iterator, 0);
        return mb.block == null ? "" : mb.block.write(options);
    }

    // for testing
    public String minimal(OutputBuilder outputBuilder) {
        Iterator<OutputElement> iterator = outputBuilder.list().iterator();
        MidBlock mb = collectElements(iterator, 0);
        return mb.block == null ? "" : mb.block.minimal();
    }

    record Block(int tab, List<OutputElement> elements, Guide guide) implements OutputElement {
        Block(int tab, List<OutputElement> elements, Guide guide) {
            this.tab = tab;
            this.elements = Objects.requireNonNull(elements);
            assert !elements.isEmpty();
            this.guide = guide;
        }

        /*
        Extremely simple system that shows the block structure, and can therefore be used to test exactly that.
         */
        @Override
        public String minimal() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            indent(sb);
            for (OutputElement element : elements) {
                if (element.isNewLine()) {
                    trim(sb);
                    sb.append('\n');
                    indent(sb);
                } else {
                    String minimal = element.minimal();
                    if (minimal.startsWith("\n")) trim(sb);
                    sb.append(minimal);
                }
            }
            return sb.toString();
        }

        private static void trim(StringBuilder sb) {
            while (!sb.isEmpty() && sb.charAt(sb.length() - 1) == ' ') sb.deleteCharAt(sb.length() - 1);
        }

        private void indent(StringBuilder sb) {
            int count = tab * 2;
            sb.append(" ".repeat(count));
        }

        @Override
        public String generateJavaForDebugging() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String write(FormattingOptions options) {
            BlockPrinter.Output output = new BlockPrinter().write(this, options);
            return output.string().replace(Formatter.HARD_SPACE, ' ') + "\n";
        }
    }

    // ------- code for collectElements ---------

    private record MidBlock(boolean mid, Block block) {
    }

    private static MidBlock collectElements(Iterator<OutputElement> iterator, int tab) {
        List<OutputElement> elements = new ArrayList<>();
        Guide endGuide = null;
        while (iterator.hasNext()) {
            OutputElement element = iterator.next();
            if (element instanceof Guide g) {
                if (g.positionIsStart()) {
                    Block block = parseBlock(iterator, tab + g.tabs(), g);
                    if (block != null) {
                        elements.add(block);
                    }
                } else {
                    endGuide = g;
                    break;
                }
            } else {
                elements.add(element);
            }
        }
        boolean mid = endGuide != null && endGuide.positionIsMid();
        if (!elements.isEmpty()) {
            return new MidBlock(mid, new Block(tab, List.copyOf(elements), null));
        }
        return new MidBlock(mid, null);
    }

    private static Block parseBlock(Iterator<OutputElement> iterator, int tab, Guide g) {
        List<OutputElement> subBlocks = new ArrayList<>();
        while (true) {
            MidBlock r = collectElements(iterator, tab);
            if (r.block != null) {
                subBlocks.add(r.block);
            }
            if (!r.mid) {
                break;
            }
        }
        if (subBlocks.isEmpty()) return null;
        return new Block(tab, subBlocks, g);
    }
}
