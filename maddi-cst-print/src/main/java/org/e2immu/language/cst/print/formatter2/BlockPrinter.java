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

    enum SplitLevel {
        NONE_IF_COMPACT, SINGLE_NEWLINE, DOUBLE_NEWLINE;
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
        int maxAvailable = options.lengthOfLine() - block.tab() * options.spacesInTab();
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
        for (OutputElement element : block.elements()) {
            if (element instanceof Formatter2Impl.Block sub) {
                Output output = write(sub, options);
                SplitLevel splitLevel;
                if (prevOutput != null && prevOutput.hasBeenSplit && output.hasBeenSplit) {
                    splitLevel = SplitLevel.DOUBLE_NEWLINE;
                } else if (output.spaceLevel().isNoSpace()) {
                    splitLevel = SplitLevel.NONE_IF_COMPACT;
                } else {
                    splitLevel = SplitLevel.SINGLE_NEWLINE;
                    if(output.spaceLevel().isNewLine()) { // TODO is this a hack? to ensure that the NEWLINE of '//' passes
                        hasBeenSplit = true;
                    }
                }
                guideSplits.put(sb.length(), splitLevel);
                sb.append(output.string);
                hasBeenSplit |= output.hasBeenSplit;
                prevOutput = output;
            } else throw new UnsupportedOperationException();
        }
        Line.SpaceLevel spaceLevel = hasBeenSplit ? Line.SpaceLevel.NEWLINE : Line.SpaceLevel.NO_SPACE;
        return new Output(sb.toString(), hasBeenSplit, splitInfo, spaceLevel);
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
        if (output.hasBeenSplit || addToLine > line.available()) {
            splitOutputOfBlock(line, output, indent, splits);
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


    private static void splitOutputOfBlock(Line line, Output output, int indent, TreeMap<Integer, SplitLevel> splits) {
        LOGGER.debug("We need to split, and we'll split along all '{}' splits: {}; each line will be indented {}",
                GUIDE_SPLIT, splits, indent);
        boolean allowSplitAtPosition0 = line.isNotEmptyDoesNotEndInNewLine();

        int start = line.length();
        line.appendBeforeSplit(output.string);
        for (Map.Entry<Integer, SplitLevel> entry : splits.entrySet()) {
            int relativePos = entry.getKey();
            int pos = relativePos + start;
            assert pos < line.length();
            if (allowSplitAtPosition0 || pos > 0) {
                boolean doubleSplit = entry.getValue() == SplitLevel.DOUBLE_NEWLINE;
                start += line.carryOutSplit(pos, indent, doubleSplit);
            } // else: we never split at the beginning of the line, there must be something already
        }
        line.computeAvailable();
    }
}
