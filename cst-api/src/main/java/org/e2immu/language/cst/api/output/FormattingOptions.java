package org.e2immu.language.cst.api.output;

public interface FormattingOptions {

    boolean binaryOperatorsAtEndOfLine();

    boolean compact();

    boolean allStaticFieldsRequireType();

    boolean allFieldsRequireThis();

    int lengthOfLine();

    boolean skipComments();

    int spacesInTab();

    int tabsForLineSplit();
}
