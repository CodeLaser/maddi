package org.e2immu.language.cst.api.output;

public interface OutputElement {

    default boolean isLeftBlockComment() {
        return false;
    }

    default boolean isNewLine() {
        return false;
    }

    default boolean isRightBlockComment() {
        return false;
    }

    default boolean isSingleLineComment() { return false; }

    String minimal();

    String write(FormattingOptions options);

    default int length(FormattingOptions options) {
        return write(options).length();
    }

    /**
     * used for testing only
     *
     * @return Java-code that can be pasted into a test
     */
    String generateJavaForDebugging();
}
