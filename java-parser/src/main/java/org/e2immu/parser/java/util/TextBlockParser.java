package org.e2immu.parser.java.util;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.output.element.TextBlockFormatting;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.parsers.java.ast.StringLiteral;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record TextBlockParser(Runtime runtime) {
    private static final Pattern START = Pattern.compile("^\"\"\"[\t ]*\n", Pattern.MULTILINE);

    private static String stripQuotes(String s) {
        Matcher m = START.matcher(s);
        if (!m.find()) throw new UnsupportedOperationException();
        // we leave the initial \n, and trailing
        return s.substring(m.end() - 1, s.length() - 3);
    }

    private static final Pattern BLANK = Pattern.compile("\n[\t ]+(?=\n)", Pattern.MULTILINE);

    private static String replaceBlankLines(String s) {
        Matcher m = BLANK.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, "\n");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String removeIndentation(String s, int indent, TextBlockFormatting.Builder builder) {
        String indentString = " ".repeat(indent);
        Pattern slashNewline = Pattern.compile("(\\\\)?(\\\\)?\n" + indentString);
        Matcher m = slashNewline.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            if (m.group(2) == null && m.group(1) == null) {
                m.appendReplacement(sb, "\n");
            } else {
                m.appendReplacement(sb, "");
                // NOTE: the -1 is because we must remove the initial \n at the end
                builder.addLineBreak(sb.length() - 1);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern INDENT_PATTERN = Pattern.compile("\n([\t ]*)([^\n]*)", Pattern.MULTILINE);

    public Expression parseTextBlock(List<Comment> comments, Source source, StringLiteral sl) {
        String string1 = stripQuotes(sl.getSource());
        String string = replaceBlankLines(string1);

        TextBlockFormatting.Builder formatting = runtime.newTextBlockFormattingBuilder();
        int indent;

        boolean optOutStripping = string.endsWith("\n");
        if (optOutStripping) {
            formatting.setOptOutWhiteSpaceStripping(true);
            indent = 0;
        } else {
            indent = Integer.MAX_VALUE;
            Matcher matcherQuotes = INDENT_PATTERN.matcher(string);
            boolean empty = false;
            while (matcherQuotes.find()) {
                empty = matcherQuotes.group(2).isEmpty();
                boolean last = matcherQuotes.end() == string.length();
                if (last || !empty) {
                    indent = Math.min(matcherQuotes.group(1).length(), indent);
                }
            }
            if (!empty) formatting.setTrailingClosingQuotes(true);
        }
        String indentationRemoved = optOutStripping ? string : removeIndentation(string, indent, formatting);
        String beforeEscapeProcessing = indentationRemoved.substring(1);
        String content = EscapeSequence.translateEscapeInTextBlock(beforeEscapeProcessing);
        return runtime.newTextBlock(comments, source, content, formatting.build());
    }
}
