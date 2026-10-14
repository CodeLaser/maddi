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

package io.codelaser.maddi.cst.print.formatter2;

import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputElement;
import io.codelaser.maddi.cst.api.output.element.Space;
import io.codelaser.maddi.cst.api.output.element.Symbol;
import io.codelaser.maddi.cst.api.output.element.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ElementPrinter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElementPrinter.class);

    /*
    The method does not add split points for spaces as the last element.
     */
    static boolean handleElement(Line line,
                                 BlockPrinter.SplitInfo splitInfo,
                                 Formatter2Impl.Block block,
                                 FormattingOptions options,
                                 OutputElement element,
                                 boolean lastElement,
                                 boolean protectSpaces) {

        if (element instanceof Space space) {
            return handleSpace(line, splitInfo, options, space, lastElement);
        }
        if (element instanceof Symbol symbol) {
            return handleSymbol(line, splitInfo, block, options, symbol, lastElement);
        }
        return handleNonSpaceNonSymbol(line, splitInfo, block, options, element);
    }

    private static boolean handleSymbol(Line line,
                                        BlockPrinter.SplitInfo splitInfo,
                                        Formatter2Impl.Block block,
                                        FormattingOptions options,
                                        Symbol symbol,
                                        boolean lastElement) {
        if (symbol.left().withdrawsSplitBefore()) {
            removeSplitPoints(splitInfo, line.length());
        } else if (!lastElement && !symbol.left().split().isNever()) {
            addSplitPoint(splitInfo, line.length(), symbol.left());
        }
        Line.SpaceLevel left = computeSpaceLevel(options, symbol.left(), symbol, true);
        line.mergeSpace(left);
        keepSignsApart(line, symbol.symbol());
        glueAfterTypeArguments(line, symbol.symbol());
        boolean newLine = line.writeSpace(options.compact(), block.tab() * options.spacesInTab());
        if (newLine && !splitInfo.map().isEmpty()) {
            int pos = line.stringBuilder.lastIndexOf("\n");
            updateForSplit(splitInfo, pos);
        }
        // a single-line comment '//' opening the block-line: remember it so a forced split inside
        // the comment does not add continuation indent (replaces the old substring(0,2) sniff).
        boolean atLineStart = line.length() == 0;
        String string = symbol.symbol();
        line.appendNoNewLine(string);
        line.setAfterTypeArguments(TYPE_ARGUMENTS_CLOSE.equals(symbol.constant()));
        if (atLineStart && symbol.isSingleLineComment()) {
            line.markSingleLineComment();
        }
        Line.SpaceLevel right = computeSpaceLevel(options, symbol.right(), symbol, false);
        line.setSpace(right);
        if (!lastElement && !symbol.right().split().isNever()) {
            addSplitPoint(splitInfo, line.length(), symbol.right());
        }
        return newLine;
    }

    private static boolean handleSpace(Line line,
                                       BlockPrinter.SplitInfo splitInfo,
                                       FormattingOptions options,
                                       Space space,
                                       boolean lastElement) {
        Line.SpaceLevel spaceLevel = computeSpaceLevel(options, space, null, false);
        // a pending newline is never undone by a following space: the newline that closes a '//' comment, followed
        // by the space a local class's member happens to start with, put the field inside the comment (GitHub #108)
        if (!line.spaceLevel().isNewLine()) line.setSpace(spaceLevel);
        if (!lastElement && !space.split().isNever()) {
            addSplitPoint(splitInfo, line.length(), space);
        }
        return false; // even if the space is a newline, it has not been written out yet, simply set
    }

    private static Line.SpaceLevel computeSpaceLevel(FormattingOptions options, Space space, Symbol symbol, boolean left) {
        /*
        Hardcoded corrections, needed because of backwards compatibility with older formatter (FormatterImpl, rather
        than Formatter2Impl)
         */
        if (symbol != null && left && symbol.isAt()) {
            return Line.SpaceLevel.NO_SPACE;
        }
        if (symbol != null && left && symbol.strongNoSpace()) {
            return Line.SpaceLevel.STRONG_NO_SPACE;
        }
        /*
        end of hardcoded corrections
         */
        Line.SpaceLevel spaceLevel;
        if (space.isNewLine()) {
            spaceLevel = Line.SpaceLevel.NEWLINE;
        } else {
            String minimal = space.minimal();
            String normal = space.write(options);
            if (normal.isEmpty()) {
                spaceLevel = Line.SpaceLevel.NO_SPACE;
            } else if (minimal.isEmpty()) {
                spaceLevel = Line.SpaceLevel.SPACE_IS_NICE;
            } else {
                spaceLevel = Line.SpaceLevel.SPACE;
            }
        }
        return spaceLevel;
    }

    private static boolean handleNonSpaceNonSymbol(Line line,
                                                   BlockPrinter.SplitInfo splitInfo,
                                                   Formatter2Impl.Block block,
                                                   FormattingOptions options,
                                                   OutputElement element) {
        String string = write(element, options, block);
        keepSignsApart(line, string);
        glueAfterTypeArguments(line, string);
        // two words are separated by a space nobody asked for (EMPTY); an array suffix is not a word: `double[][]`,
        // not `double [][]` (#49). A space asked for -- after a dimension annotation -- stays.
        if (line.spaceLevel() == Line.SpaceLevel.EMPTY && (string.startsWith("[") || string.startsWith("..."))) {
            line.setSpace(Line.SpaceLevel.NO_SPACE);
        }
        line.writeSpace(options.compact(), block.tab() * options.spacesInTab());
        line.appendBeforeSplit(string);
        line.setAfterTypeArguments(false);
        boolean multiLine = line.computeAvailable();
        if (line.available() < 0 && !splitInfo.map().isEmpty()) {
            int indent = block.tab() * options.spacesInTab();
            int pos = updateForSplit(splitInfo, indent);

            // A wrapped single-line comment keeps its continuation at comment level (no extra
            // indent); everything else gets a continuation indent. Works in concert with
            // BlockPrinter.handleBlock (line.appendNewLine(...)).
            int increment = line.isSingleLineComment() ? 0 : options.spacesInTab();

            line.carryOutSplit(pos, indent + increment, false);
            line.computeAvailable();
            return true;
        }
        if (multiLine && !splitInfo.map().isEmpty()) {
            int pos = line.stringBuilder.lastIndexOf("\n");
            updateForSplit(splitInfo, pos);
        }
        return multiLine;
    }

    /*
    '-' followed by '-', or '+' by '+', is a different token when the two touch: `a - -b` written `a --b` decrements.
    A prefix operator has no space of its own on the left (#49), and compact mode drops the nice ones, so this is
    where the space is guaranteed.
     */
    private static void keepSignsApart(Line line, String next) {
        if (next.isEmpty() || line.stringBuilder.isEmpty() || line.spaceLevel().isNewLine()) return;
        char first = next.charAt(0);
        if ((first == '-' || first == '+') && line.lastCharacter() == first) {
            line.mergeSpace(Line.SpaceLevel.SPACE);
        }
    }

    private static final String TYPE_ARGUMENTS_CLOSE = "RIGHT_ANGLE_BRACKET";

    /*
    The '>' closing type arguments wants a space after it where a name follows (`List<T> list`), but not in front of
    an argument list, an array suffix, varargs or a member: `new ArrayList<T>(c)`, `List<T>[]`, `List<T>... ls`,
    `ArrayList<T>::new` (#49). Only the nice space goes; one the printer asked for explicitly stays.
     */
    static void glueAfterTypeArguments(Line line, String next) {
        if (line.afterTypeArguments() && line.spaceLevel() == Line.SpaceLevel.SPACE_IS_NICE && !next.isEmpty()
            && (next.charAt(0) == '(' || next.charAt(0) == '[' || next.charAt(0) == '.' || next.startsWith("::"))) {
            line.setSpace(Line.SpaceLevel.NO_SPACE);
        }
    }

    private static String write(OutputElement element, FormattingOptions options, Formatter2Impl.Block block) {
        if (element instanceof Text tb && tb.textBlockFormatting() != null) {
            return WriteTextBlock.write(options.spacesInTab() * (block.tab() + 2),
                    tb.minimal(), tb.textBlockFormatting());
        }
        return element.write(options);
    }

    private static int updateForSplit(BlockPrinter.SplitInfo splitInfo, int indent) {
        int pos = splitInfo.map().lastEntry().getValue().lastKey();
        Iterator<Map.Entry<Integer, TreeMap<Integer, BlockPrinter.SplitLevel>>> iterator = splitInfo.map().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TreeMap<Integer, BlockPrinter.SplitLevel>> entry = iterator.next();
            TreeMap<Integer, BlockPrinter.SplitLevel> map = entry.getValue();
            TreeMap<Integer, BlockPrinter.SplitLevel> updated = new TreeMap<>();
            for (Map.Entry<Integer, BlockPrinter.SplitLevel> entry2 : map.entrySet()) {
                int i = entry2.getKey() - pos;
                if (i > 0) {
                    int newPos = i + indent;
                    updated.put(newPos, entry2.getValue());
                }
            }
            if (updated.isEmpty()) {
                iterator.remove();
            } else {
                entry.setValue(updated);
            }
        }
        return pos;
    }

    /** Withdraws the split points registered at this position, of every rank: see {@link Space#withdrawsSplitBefore()}. */
    private static void removeSplitPoints(BlockPrinter.SplitInfo splitInfo, int length) {
        splitInfo.map().values().forEach(points -> points.remove(length));
        splitInfo.map().values().removeIf(TreeMap::isEmpty);
    }

    static void addSplitPoint(BlockPrinter.SplitInfo splitInfo, int length, Space space) {
        LOGGER.debug("Adding split point in string builder at position {}", length);
        int rank = space.split().rank();
        splitInfo.map().computeIfAbsent(rank, r -> new TreeMap<>())
                .put(length, BlockPrinter.SplitLevel.SINGLE_NEWLINE);
    }
}
