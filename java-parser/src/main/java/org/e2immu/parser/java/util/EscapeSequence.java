package org.e2immu.parser.java.util;

public class EscapeSequence {
    static char escapeSequence(char c2) {
        return switch (c2) {
            case '0' -> '\0';
            case 'b' -> '\b';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'n' -> '\n';
            case 'f' -> '\f';
            case 's' -> ' '; // does not seem to be official?
            case '\'' -> '\'';
            case '\\' -> '\\';
            case '"' -> '"';
            default -> {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static char escapeSequence(String c2) {
        char c = c2.charAt(0);
        if (c >= '0' && c <= '7') {
            StringBuilder sb = new StringBuilder();
            octal(c2.toCharArray(), sb, c, 0);
            return sb.charAt(0);
        }
        return escapeSequence(c);
    }

    // replace everything except for \<line terminator>
    public static String translateEscapeInTextBlock(String in) {
        StringBuilder sb = new StringBuilder(in.length());
        int i = 0;
        char[] chars = in.toCharArray();
        while (i < chars.length) {
            char c = chars[i];
            if (c == '\\' && i + 1 < chars.length) {
                char c2 = chars[i + 1];
                if (c2 != '\n') {
                    if (c2 >= '0' && c2 <= '7') {
                        i = octal(chars, sb, c2, i);
                    } else {
                        sb.append(escapeSequence(c2));
                        ++i;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
            ++i;
        }
        return sb.toString();
    }

    private static int octal(char[] chars, StringBuilder sb, char first, int start) {
        int i = start + 1;
        StringBuilder newSb = new StringBuilder();
        newSb.append(first);
        while (i < chars.length && chars[i] >= '0' && chars[i] <= '7') {
            newSb.append(chars[i]);
            ++i;
        }
        int value = Integer.parseInt(newSb.toString(), 8);
        sb.append((char) value);
        return i;
    }
}
