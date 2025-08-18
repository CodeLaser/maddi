package org.e2immu.language.cst.print.formatter2;

import org.e2immu.language.cst.api.output.OutputElement;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static int charactersUntilAndExcludingLastNewline(String string) {
        int nl = string.lastIndexOf('\n');
        if (nl < 0) {
            return string.length();
        }
        return string.length() - (nl + 1);
    }

    public static List<OutputElement> removeComments(List<OutputElement> list) {
        List<OutputElement> elements = new ArrayList<>(list.size());
        int commentDepth = 0;
        boolean dropUntilNewline = false;
        for (OutputElement outputElement : list) {
            if (outputElement.isLeftBlockComment()) {
                commentDepth++;
            } else if (outputElement.isRightBlockComment()) {
                commentDepth--;
            } else if (commentDepth == 0) {
                if (outputElement.isSingleLineComment()) {
                    dropUntilNewline = true;
                } else if (outputElement.isNewLine()) {
                    dropUntilNewline = false;
                }
                if (!dropUntilNewline) {
                    elements.add(outputElement);
                }
            }
        }
        return elements;
    }
}
