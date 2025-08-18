package org.e2immu.language.cst.print.formatter2;

import org.e2immu.language.cst.api.output.element.TextBlockFormatting;

public class WriteTextBlock {
    /*
    starts with """, ends with """, indents according to rules in TextBlockFormatting and given indentation in
    number of spaces.
     */
    public static String write(int indentIn, String string, TextBlockFormatting textBlockFormatting) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"\"\"\n");
        int indent = textBlockFormatting.optOutWhiteSpaceStripping() ? 0 : indentIn;
        sb.append(" ".repeat(indent));
        char[] chars = string.toCharArray();
        int i = 0;
        while (i < chars.length) {
            char c = chars[i];
            if (textBlockFormatting.lineBreaks().contains(i)) {
                sb.append('\\');
                sb.append('\n');
                sb.append(" ".repeat(indent));
                sb.append(c);
            } else if (c == '\n') {
                sb.append('\n');
                sb.append(" ".repeat(indent));
            } else {
                sb.append(c);
            }
            ++i;
        }
        sb.append("\"\"\"");
        return sb.toString();
    }
}
