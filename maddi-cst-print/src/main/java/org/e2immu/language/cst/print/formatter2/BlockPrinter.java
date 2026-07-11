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

package org.e2immu.language.cst.print.formatter2;

import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputElement;
import org.e2immu.language.cst.api.output.element.Guide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

public class BlockPrinter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockPrinter.class);
    public static final int GUIDE_SPLIT = 10;

    /*
    Hardening floor for deeply-nested blocks. When indentation consumes most (or all) of the page
    width, the raw content budget (lengthOfLine - indent) drops to ~0 or negative; every candidate
    split position then "overflows", so the formatter breaks at all of them and shatters short lines
    into one-token-per-line garbage (plus spurious blank lines from the "both sides split" heuristic).
    We guarantee each block at least this many columns of content budget: deeply-indented lines may
    exceed lengthOfLine, but they stay intact instead of degenerating. Chosen small enough not to
    interfere with intentionally narrow-but-shallow layouts (the floor only engages once indentation
    already exceeds lengthOfLine - MIN_CONTENT_WIDTH, which shallow blocks never reach).
     */
    static final int MIN_CONTENT_WIDTH = 16;

    /*
    A guide block (parameter list, argument list, method chain, …) marks one of these levels at
    each boundary between its sub-blocks. The level governs what happens at that boundary in the
    two layout paths: chop-down (one element per line) and greedy fill (pack until overflow).

    Listed weakest → strongest; callers can compare with `compareTo(...) >= 0`.

      NONE_IF_COMPACT
        No separator is wanted at this boundary at all — no space, no newline. Used between
        segments that visually belong together, e.g. the dots in a method chain
        `foo.bar().baz()`. Greedy fill will not even insert a space here when staying on the
        same line; the chop-down loop still splits at every boundary (its existing behaviour),
        but no extra space is ever inserted into the joined line.

      SINGLE_NEWLINE
        The "ordinary" split candidate. Chop-down always breaks here. Greedy fill keeps the
        boundary on the same line and inserts a single space if there isn't one already,
        unless the next chunk would overflow the line — then it breaks with one newline and
        continuation indent.

      FORCED_NEWLINE
        Like SINGLE_NEWLINE but greedy MUST break here, regardless of remaining budget. Used
        to isolate sub-blocks that themselves wrap a nested guide block (a structured arg
        like `inner(x, y, z)` inside another call), so the structured arg sits on its own
        rendered line instead of being glued to its flat neighbours. Chop-down treats it
        identically to SINGLE_NEWLINE (one newline, no blank line).

      DOUBLE_NEWLINE
        Strongest level. Break with a blank line in between (newline + newline + indent).
        Used when both neighbours themselves wrapped onto multiple lines, so the eye gets a
        breathing space between the two multi-line blocks. Always honoured by both layout
        paths and never demoted by `isolateNestedGuide`.
     */
    enum SplitLevel {
        NONE_IF_COMPACT, SINGLE_NEWLINE, FORCED_NEWLINE, DOUBLE_NEWLINE;
    }

    // split level (the higher the better) to position to 'doubleSplit'
    // single split = on new line; double split = leave line empty
    record SplitInfo(TreeMap<Integer, TreeMap<Integer, SplitLevel>> map) {
    }

    /**
     * This class's main method returns an Output object.
     * The resulting string has either already been split (extraLines), or not (extraLines).
     *
     * @param string       already indented according to options and block.tab, on all lines except the first.
     * @param hasBeenSplit false = everything fits on one line, string.length == endPos; true = multiple lines
     *                     were needed. all lines except the first have been indented. endPos is computed wrt the last line,
     *                     which is probably only needed for continuing fluent lambda chains, text-blocks, etc.
     * @param splitInfo    even if it fits on one theoretical line, we may still want to split. this map returns
     *                     the best positions to split
     */
    record Output(String string, boolean hasBeenSplit, SplitInfo splitInfo, Line.SpaceLevel spaceLevel) {
        Output {
            assert !string.endsWith(" ") : "An output cannot end in a space";
        }

        int endPos() {
            return hasBeenSplit ? Util.charactersUntilAndExcludingLastNewline(string) : string.length();
        }
    }

    /**
     * main entry point, and point of recursion.
     * When called as the main entry point on a primary type, the splitInfo can be ignored, and only the
     * final space needs converting into a newline.
     *
     * @param block   the block, as prepared in Formatter2Impl
     * @param options overall formatting options, mostly to determine line length, tab spaces
     * @return an output instance
     */
    Output write(Formatter2Impl.Block block, FormattingOptions options) {
        int maxAvailable = Math.max(MIN_CONTENT_WIDTH,
                options.lengthOfLine() - block.tab() * options.spacesInTab());
        if (block.guide() != null) {
            return handleGuideBlock(block, options);
        }
        return handleElements(maxAvailable, block, options);
    }

    /*
    The elements of a guide block are blocks themselves.
    This method does not carry out line splitting, it simply marks possible split positions at the beginning, and at
    the mid positions of the guide. It forwards the 'hasBeenSplit' boolean, and computes an 'endPos'.

    All earlier splits are discarded.

    The doubleSplit is part of our own style feeling: unless caused by comments, we want a blank line
    by lines that have internally been split.

    prevOutput helps in the computation of doubleSplit.
     */
    Output handleGuideBlock(Formatter2Impl.Block block, FormattingOptions options) {
        SplitInfo splitInfo = new SplitInfo(new TreeMap<>());
        TreeMap<Integer, SplitLevel> guideSplits = new TreeMap<>();
        splitInfo.map.put(GUIDE_SPLIT, guideSplits);
        StringBuilder sb = new StringBuilder();
        boolean hasBeenSplit = false;
        Output prevOutput = null;
        boolean prevHasNestedGuide = false;
        // A blank line ("breathing space") between two internally-wrapped sub-blocks is wanted for a
        // statement/declaration block (prioritySplit) or a top-level grouping (tabs==0: file lists,
        // annotation/comment groups). It must NOT appear inside the inline decomposition of a single
        // construct — a method chain, argument list or parameter list (prioritySplit=false, tabs>=1) —
        // where a blank line in the middle of the expression is wrong. See baseSplitLevel.
        boolean guideWantsBreathingSpace = block.guide().prioritySplit() || block.guide().tabs() == 0;
        for (OutputElement element : block.elements()) {
            if (!(element instanceof Formatter2Impl.Block sub)) throw new UnsupportedOperationException();
            Output output = write(sub, options);
            boolean currentHasNestedGuide = containsNestedGuideBlock(sub);
            SplitLevel splitLevel = baseSplitLevel(prevOutput, output, guideWantsBreathingSpace);
            splitLevel = isolateNestedGuide(splitLevel, prevHasNestedGuide, currentHasNestedGuide);
            if (splitLevel == SplitLevel.SINGLE_NEWLINE && output.spaceLevel().isNewLine()) {
                // TODO is this a hack? to ensure that the NEWLINE of '//' passes
                hasBeenSplit = true;
            }
            guideSplits.put(sb.length(), splitLevel);
            sb.append(output.string);
            hasBeenSplit |= output.hasBeenSplit;
            prevOutput = output;
            prevHasNestedGuide = currentHasNestedGuide;
        }
        Line.SpaceLevel spaceLevel = hasBeenSplit ? Line.SpaceLevel.NEWLINE : Line.SpaceLevel.NO_SPACE;
        return new Output(sb.toString(), hasBeenSplit, splitInfo, spaceLevel);
    }

    /*
    The base level computed purely from the previous and current sub-block outputs —
    independent of any "force isolation" decision based on nested guide blocks.
     */
    private static SplitLevel baseSplitLevel(Output prevOutput, Output output, boolean guideWantsBreathingSpace) {
        if (guideWantsBreathingSpace
                && prevOutput != null && prevOutput.hasBeenSplit && output.hasBeenSplit) {
            return SplitLevel.DOUBLE_NEWLINE;
        }
        if (output.spaceLevel().isNoSpace()) {
            // This block's far (trailing) end is no-space, which normally means "glue" (a method-chain
            // link `.foo()` glued to the previous `)`. But the separator between two sub-blocks is
            // governed by whether the PREVIOUS block wants a trailing space, not this block's far end.
            // When the previous block ends in something that wants a space — e.g. a binary operator at
            // end of line: `(a > 0) &&` (SPACE_IS_NICE) followed by `(b < 0)` (NO_SPACE) — keep the
            // separator, otherwise the space is dropped (`(a > 0) &&(b < 0)`).
            Line.SpaceLevel prev = prevOutput == null ? null : prevOutput.spaceLevel();
            boolean prevWantsSpace = prev == Line.SpaceLevel.SPACE || prev == Line.SpaceLevel.SPACE_IS_NICE;
            return prevWantsSpace ? SplitLevel.SINGLE_NEWLINE : SplitLevel.NONE_IF_COMPACT;
        }
        return SplitLevel.SINGLE_NEWLINE;
    }

    /*
    Promote the split at the boundary BEFORE the current sub-block to FORCED_NEWLINE when
    either neighbour wraps a nested guide block (a method-call arg-list, a generic
    parameter list, etc.). That keeps a structured argument visually isolated from the
    arguments around it, both before and after, without affecting DOUBLE_NEWLINE.
     */
    private static SplitLevel isolateNestedGuide(SplitLevel base,
                                                 boolean prevHasNestedGuide,
                                                 boolean currentHasNestedGuide) {
        // DOUBLE_NEWLINE outranks FORCED_NEWLINE (blank line). NONE_IF_COMPACT is a deliberate
        // "no separator wanted" marker (method chains: `.map(...).findAny()`) — leave it alone
        // even if its segments wrap nested guide blocks.
        if (base == SplitLevel.DOUBLE_NEWLINE || base == SplitLevel.NONE_IF_COMPACT) return base;
        if (!prevHasNestedGuide && !currentHasNestedGuide) return base;
        return SplitLevel.FORCED_NEWLINE;
    }

    /*
    True when the sub-block's element list contains (anywhere, recursively) another
    guide-typed Block. Every argument in a guide block is itself a Block, so a plain
    structural check would over-match; we look specifically for a *guide* underneath.
     */
    private static boolean containsNestedGuideBlock(Formatter2Impl.Block sub) {
        for (OutputElement e : sub.elements()) {
            if (e instanceof Formatter2Impl.Block nested) {
                if (nested.guide() != null) return true;
                if (containsNestedGuideBlock(nested)) return true;
            }
        }
        return false;
    }

    /*
    The elements of a normal block are either guide blocks, or non-block (normal) output elements.

    protectSpaces is solely concerned with handling of block comments. Inside block comments, we keep the
    original spaces.
     */
    Output handleElements(int maxAvailable,
                          Formatter2Impl.Block block,
                          FormattingOptions options) {
        boolean hasBeenSplit = false;
        SplitInfo splitInfo = new SplitInfo(new TreeMap<>());
        Line line = new Line(maxAvailable, block.tab() * options.spacesInTab());
        int i = 0;
        int n = block.elements().size();
        boolean protectSpaces = false;
        for (OutputElement element : block.elements()) {
            assert !(element instanceof Guide) : "Should have been filtered out";
            if (element instanceof Formatter2Impl.Block sub) {
                boolean blockHasBeenSplit = handleBlock(line, options, sub);
                hasBeenSplit |= blockHasBeenSplit;
                boolean symmetricalSplit = blockHasBeenSplit && sub.guide().endWithNewLine();
                if (symmetricalSplit) {
                    // this is mainly for the closing '}'
                    line.setSpace(Line.SpaceLevel.NEWLINE);
                }
            } else {
                boolean lastElement = i == n - 1;
                hasBeenSplit |= ElementPrinter.handleElement(line, splitInfo, block, options,
                        element, lastElement, protectSpaces);
                if (element.isLeftBlockComment()) protectSpaces = true;
                if (element.isRightBlockComment()) protectSpaces = false;
            }
            ++i;
        }
        String string = line.toString();
        return new Output(string, hasBeenSplit, splitInfo, line.spaceLevel());
    }

    /*
    Add a guide block to the current output.

    The block will be split across the lines indicated by "handleGuideBlock" if it does not fit on the remainder
    of the line, or if it already contains multiple lines.
     */
    boolean handleBlock(Line line, FormattingOptions options, Formatter2Impl.Block sub) {
        Output output = write(sub, options);
        LOGGER.debug("Result of recursion: received output of block: {}", output);

        TreeMap<Integer, SplitLevel> splits = output.splitInfo.map.getOrDefault(GUIDE_SPLIT, new TreeMap<>());
        int indent = sub.tab() * options.spacesInTab();
        int addToLine = output.endPos() + splits.size();
        // When requested, a priority guide block (class/method body, other generatorForBlock
        // structures) is always broken onto its own lines, even if it would fit — conventional
        // "brace on its own line" layout rather than the compact single-line-if-it-fits default.
        boolean forcePriorityBreak = options.alwaysBreakPriorityBlocks()
                && sub.guide() != null && sub.guide().prioritySplit();
        if (output.hasBeenSplit || addToLine > line.available() || forcePriorityBreak) {
            splitOutputOfBlock(line, output, indent, splits, options);
            return true;
        }
        Line.SpaceLevel spaceLevel = line.spaceLevel();

        // TODO is this a hack? see ElementPrinter.handleNonSpaceNonSymbol TODO
        if (spaceLevel == Line.SpaceLevel.SPACE) line.appendNoNewLine(" ");
        else if(spaceLevel == Line.SpaceLevel.NEWLINE) line.appendNewLine(line.indent);
        boolean doFirst = !spaceLevel.isNoSpace();
        appendAndInsertSpaceSplits(line, output, splits, doFirst);
        return false;
    }

    private void appendAndInsertSpaceSplits(Line line, Output output, Map<Integer, SplitLevel> splits, boolean doFirst) {
        int start = line.length();
        line.appendNoNewLine(output.string);
        for (Map.Entry<Integer, SplitLevel> entry : splits.entrySet()) {
            int pos = entry.getKey();
            if ((doFirst || pos > 0) && entry.getValue() != SplitLevel.NONE_IF_COMPACT) {
                boolean inserted = line.ensureSpace(start + pos);
                if (inserted) ++start;
            }
        }
    }


    private static void splitOutputOfBlock(Line line, Output output, int indent, TreeMap<Integer, SplitLevel> splits,
                                           FormattingOptions options) {
        LOGGER.debug("We need to split, and we'll split along all '{}' splits: {}; each line will be indented {}",
                GUIDE_SPLIT, splits, indent);
        boolean allowSplitAtPosition0 = line.isNotEmptyDoesNotEndInNewLine();

        // 3b fallback: if the sub-block already contains newlines (a nested block had to wrap),
        // greedy fill would be visually confusing, so revert to chop-down.
        boolean greedy = options.wrapStyle() == FormattingOptions.WrapStyle.GREEDY_FILL
                && !output.string.contains("\n");

        // The pending space level that the inline path (BlockPrinter.handleBlock) would emit before
        // the block. Chop-down always breaks the position-0 boundary onto a new line, so it never
        // needs this separator; greedy keeps position 0 on the current line, so without it the block
        // glues onto its neighbour (e.g. `throwsMalformedURLException`, `{buff.append(...)`).
        Line.SpaceLevel pending = line.spaceLevel();
        boolean leadingSpaceWanted = pending == Line.SpaceLevel.SPACE
                || (pending == Line.SpaceLevel.SPACE_IS_NICE && !options.compact());

        int availableBeforeAppend = line.available();
        int start = line.length();
        line.appendBeforeSplit(output.string);

        if (greedy) {
            greedyFill(line, output.string, indent, splits, start,
                    availableBeforeAppend, options.lengthOfLine(), allowSplitAtPosition0, leadingSpaceWanted);
        } else {
            for (Map.Entry<Integer, SplitLevel> entry : splits.entrySet()) {
                int relativePos = entry.getKey();
                int pos = relativePos + start;
                assert pos < line.length();
                if (allowSplitAtPosition0 || pos > 0) {
                    boolean doubleSplit = entry.getValue() == SplitLevel.DOUBLE_NEWLINE;
                    start += line.carryOutSplit(pos, indent, doubleSplit);
                } // else: we never split at the beginning of the line, there must be something already
            }
        }
        line.computeAvailable();
    }

    /*
    Greedy line fill: walk the candidate split positions and only insert a break when the next
    chunk would not fit on the remaining budget of the current rendered line. DOUBLE_NEWLINE
    positions are always split (when allowed); position 0 is never split unless allowed.
     */
    private static void greedyFill(Line line, String content, int indent,
                                   TreeMap<Integer, SplitLevel> splits, int start,
                                   int availableOnCurrentLine, int lengthOfLine,
                                   boolean allowSplitAtPosition0, boolean leadingSpaceWanted) {
        Integer[] positions = splits.keySet().toArray(new Integer[0]);
        int n = positions.length;
        if (n == 0) return;
        int contentLength = content.length();
        int subLineBudget = lengthOfLine - indent;

        // chunk 0 = content[0 .. positions[0]) — always stays on the current line.
        int remainingOnLine = availableOnCurrentLine - positions[0];
        int cumulativeShift = 0;

        for (int i = 0; i < n; i++) {
            int p = positions[i];
            int nextBoundary = (i + 1 < n) ? positions[i + 1] : contentLength;
            char atP = content.charAt(p);
            // If we split at a space, the space is consumed by the inserted "\n + indent",
            // so the rendered chunk on the new sub-line is one char shorter.
            int chunkWidthIfSplit = (atP == ' ') ? (nextBoundary - p - 1) : (nextBoundary - p);
            int chunkWidthIfNotSplit = nextBoundary - p;

            SplitLevel level = splits.get(p);
            boolean canSplit = allowSplitAtPosition0 || (p + start) > 0;
            boolean mustSplit = level == SplitLevel.DOUBLE_NEWLINE || level == SplitLevel.FORCED_NEWLINE;
            boolean doubleNewline = level == SplitLevel.DOUBLE_NEWLINE;

            boolean shouldSplit;
            if (mustSplit) {
                shouldSplit = canSplit;
            } else if (!canSplit) {
                shouldSplit = false;
            } else {
                shouldSplit = chunkWidthIfNotSplit > remainingOnLine;
            }

            if (shouldSplit) {
                int actualPos = p + start + cumulativeShift;
                cumulativeShift += line.carryOutSplit(actualPos, indent, doubleNewline);
                remainingOnLine = subLineBudget - chunkWidthIfSplit;
            } else {
                // mirror appendAndInsertSpaceSplits: keep elements visually separated when they
                // remain on the same line (a Guide ".mid()" between two symbols has no space).
                // Position 0 normally defers to the preceding context (e.g. "(") for its separator;
                // when that context wants a space (leadingSpaceWanted, e.g. after "throws" or "{"),
                // we must emit it here because the split path skipped the inline space-writing.
                boolean wantSeparator = (p > 0 && level != SplitLevel.NONE_IF_COMPACT)
                        || (p == 0 && leadingSpaceWanted);
                if (wantSeparator) {
                    int actualPos = p + start + cumulativeShift;
                    if (line.ensureSpace(actualPos)) {
                        cumulativeShift += 1;
                        remainingOnLine -= 1;
                    }
                }
                remainingOnLine -= chunkWidthIfNotSplit;
            }
        }
    }
}
