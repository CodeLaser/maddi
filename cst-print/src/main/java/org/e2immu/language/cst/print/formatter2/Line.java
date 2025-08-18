package org.e2immu.language.cst.print.formatter2;

class Line {

    /*
    Empty means: no space has been set (e.g., after Text)

    A strong "no_space" eats up the space in consecutive symbols, such as ">,".
    We'd normally want a space after >, but not when this is followed by another symbol ('>', ',', ')' etc.)
     */
    enum SpaceLevel {
        EMPTY, NO_SPACE, SPACE_IS_NICE, SPACE, STRONG_NO_SPACE, NEWLINE,
        ;

        public boolean isNewLine() {
            return this == NEWLINE;
        }

        boolean isNoSpace() {
            return this == NO_SPACE || this == STRONG_NO_SPACE;
        }

        public SpaceLevel max(SpaceLevel other) {
            if (this == NEWLINE || other == NEWLINE) return NEWLINE;
            if (this == SPACE || other == SPACE) return SPACE;
            if (this == STRONG_NO_SPACE || other == STRONG_NO_SPACE) return STRONG_NO_SPACE;
            if (this == SPACE_IS_NICE || other == SPACE_IS_NICE) return SPACE_IS_NICE;
            if (this == NO_SPACE || other == NO_SPACE) return NO_SPACE;
            return EMPTY;
        }
    }

    // number of characters allowed on a line, excluding the indentation (shrinks with increasing tabs)
    final int maxAvailable;
    // space taken up by indent, on all lines except the first!!! (grows with increasing tabs)
    final int indent;
    final StringBuilder stringBuilder = new StringBuilder();

    private int available;
    // space level at the end of the string in the stringBuilder.
    // it is NOT counted in "available", and has not been added to the builder.
    private SpaceLevel spaceLevel = SpaceLevel.NO_SPACE;

    Line(int maxAvailable, int indent) {
        this.maxAvailable = maxAvailable;
        available = maxAvailable;
        this.indent = indent;
    }

    public void appendBeforeSplit(String string) {
        stringBuilder.append(string);
        assert !string.endsWith("\n");
    }

    public void setSpace(SpaceLevel spaceLevel) {
        this.spaceLevel = spaceLevel;
    }

    public void mergeSpace(SpaceLevel left) {
        this.spaceLevel = this.spaceLevel.max(left);
    }

    public boolean writeSpace(boolean compact, int indent) {
        SpaceLevel current = spaceLevel;
        spaceLevel = SpaceLevel.EMPTY;
        if (current.isNoSpace()) {
            return false;
        }
        if (compact && current == SpaceLevel.SPACE_IS_NICE) {
            return false;
        }
        if (current == SpaceLevel.NEWLINE) {
            appendNewLine(indent);
            return true;
        }
        // important: we never write a separator space on a new line
        if (!stringBuilder.isEmpty() && lastCharacter() != '\n') {
            appendNoNewLine(" ");
        }
        return false;
    }

    public void appendNewLine(int indent) {
        assert indent >= this.indent; // but could be more
        stringBuilder.append("\n").append(" ".repeat(indent));
        available = maxAvailable - (indent - this.indent);
        spaceLevel = SpaceLevel.EMPTY;
    }

    public boolean computeAvailable() {
        int lastNewLine = stringBuilder.lastIndexOf("\n");
        if (lastNewLine < 0) {
            // there is no indentation on the first line
            available = maxAvailable - stringBuilder.length();
            return false;
        }
        int remainder = stringBuilder.length() - (lastNewLine + 1);
        available = this.indent + maxAvailable - remainder;
        return true;
    }

    public void appendNoNewLine(String string) {
        assert !string.contains("\n");
        stringBuilder.append(string);
        available -= string.length();
    }

    public int available() {
        return available;
    }

    public char lastCharacter() {
        return stringBuilder.charAt(stringBuilder.length() - 1);
    }

    public int length() {
        return stringBuilder.length();
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }

    /*
    spaceLevel has to be written out for this one to be correct
     */
    boolean isNotEmptyDoesNotEndInNewLine() {
        return !stringBuilder.isEmpty() && '\n' != stringBuilder.charAt(stringBuilder.length() - 1);
    }

    /*
    return the number of characters added

    this operation leaves spaceLevel invariant.
    however, available will be incorrect, and a computeAvailable() call will have to be made afterward.
     */
    public int carryOutSplit(int pos, int indent, boolean doubleSplit) {
        char atPos = stringBuilder.charAt(pos);
        if (atPos == '\n') {
            // we don't split at newlines
            return 0;
        }
        assert !stringBuilder.substring(0, pos).isBlank() : "head of split must not be blank";
        assert pos + 1 == stringBuilder.length() || !stringBuilder.substring(pos + 1).isBlank()
                : "tail of split must not be blank";
        String insert = (doubleSplit ? "\n\n" : "\n") + (" ".repeat(indent));
        if (atPos == ' ') {
            // replace the space, rather than inserting
            stringBuilder.replace(pos, pos + 1, insert);
            return insert.length() - 1;
        }
        stringBuilder.insert(pos, insert);
        return insert.length();
    }

    public boolean ensureSpace(int pos) {
        char atPos = stringBuilder.charAt(pos);
        if (atPos == '\n' || atPos == ' ') return false;
        if (pos == 0) return false;
        char oneEarlier = stringBuilder.charAt(pos - 1);
        if (oneEarlier == '\n' || oneEarlier == ' ') return false;
        stringBuilder.insert(pos, ' ');
        return true;
    }


    public SpaceLevel spaceLevel() {
        return spaceLevel;
    }

}
