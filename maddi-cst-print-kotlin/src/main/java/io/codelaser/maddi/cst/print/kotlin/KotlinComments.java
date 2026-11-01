/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.element.MultiLineComment;
import io.codelaser.maddi.cst.api.output.Formatter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.element.MultiLineCommentImpl;
import io.codelaser.maddi.cst.impl.output.GuideImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Java's comments as Kotlin's: a line or block comment as written, a Javadoc as a KDoc. KDoc keeps the
 * {@code /** … *}{@code /} form and the block tags; the inline tags are Markdown there, so {@code {@link Foo#bar}}
 * becomes {@code [Foo.bar]} and {@code {@code x}} becomes {@code `x`}.
 * <p>
 * ⚠ Kotlin's block comments NEST, Java's do not: a {@code /*} inside a Java comment ({@code src/**}{@code /*.java}
 * in a Javadoc) opens a second comment in Kotlin, and the file no longer compiles. It is printed {@code / *}.
 */
final class KotlinComments {

    private static final Pattern STAR = Pattern.compile("^\\s*\\*\\s?");
    private static final Pattern WHITE_SPACE = Pattern.compile("\\s");

    private KotlinComments() {
    }

    /** Each comment, in order; a line comment ends its own line. */
    static OutputBuilder print(List<Comment> comments, Qualification qualification) {
        OutputBuilder ob = new OutputBuilderImpl();
        if (comments != null) comments.forEach(c -> ob.add(print(c, qualification)));
        return ob;
    }

    static OutputBuilder print(Comment comment, Qualification qualification) {
        if (comment instanceof JavaDoc javaDoc) return kdoc(javaDoc);
        if (comment instanceof MultiLineComment && comment.comment().contains("/*")) {
            // with its delimiters, or the constructor trims the spaces just inside them
            return new MultiLineCommentImpl(comment.source(), "/*" + unnest(comment.comment()) + "*/", true)
                    .print(qualification);
        }
        return comment.print(qualification);
    }

    private static String unnest(String text) {
        return text.replace("/*", "/ *");
    }

    /**
     * A Javadoc's text holds no {@code *}{@code /} as written, but can as the front end hands it over: it drops HTML
     * elements and entities, so jenkins's {@code &lt;url-pattern&gt;/*&lt;/url-pattern&gt;} arrives as
     * {@code url-pattern/*}{@code /url-pattern}, which closed the KDoc early and broke the whole file.
     */
    private static String unclosed(String text) {
        return text.replace("*/", "* /");
    }

    /** As JavaDocImpl prints a Javadoc, with the tags in KDoc's spelling. */
    private static OutputBuilder kdoc(JavaDoc javaDoc) {
        // the text with its tags as written: a {1} in it is the text's own, guava's Sets.powerSet documents
        // {@code {{}, {1}, {2}, {1, 2}}} (#166)
        // without its blank first and last lines; and nothing at all when only {@inheritDoc} was there
        String text = unclosed(unnest(kdocText(javaDoc.comment()))).replaceAll("^(\\s*\\n)+", "").stripTrailing();
        if (text.isBlank()) return new OutputBuilderImpl();
        String[] lines = text.split("\n", -1);
        boolean starred = Arrays.stream(lines).filter(l -> STAR.matcher(l).find()).count() > 1;
        Stream<OutputBuilder> body = Arrays.stream(lines)
                .map(l -> starred ? STAR.matcher(l).replaceFirst("") : l)
                .map(l -> new OutputBuilderImpl().add(new TextImpl(Formatter.HARD_SPACE + "*"
                                                                    + (l.isBlank() ? "" : " " + l.trim()))));
        return Stream.concat(Stream.concat(Stream.of(new OutputBuilderImpl().add(new TextImpl("/**"))), body),
                        Stream.of(new OutputBuilderImpl().add(new TextImpl(Formatter.HARD_SPACE + "*/"))))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForMultilineComment()));
    }

    /** The Javadoc text with its inline tags as KDoc's Markdown, and its block tags as KDoc names them. */
    static String kdocText(String javadoc) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < javadoc.length()) {
            int open = javadoc.indexOf("{@", i);
            if (open < 0) break;
            int close = matchingBrace(javadoc, open);
            if (close < 0) break;
            out.append(javadoc, i, open).append(inlineTag(javadoc.substring(open + 2, close)));
            i = close + 1;
        }
        out.append(javadoc.substring(i));
        return BLOCK_SEE.matcher(BLOCK_EXCEPTION.matcher(out.toString()).replaceAll("$1@throws"))
                .replaceAll(r -> r.group(1) + "@see " + Matcher.quoteReplacement(target(r.group(2))));
    }

    private static final Pattern BLOCK_EXCEPTION = Pattern.compile("(?m)^(\\s*)@exception\\b");
    private static final Pattern BLOCK_SEE = Pattern.compile("(?m)^(\\s*)@see\\s+([\\w.$]*#?[\\w$]+(?:\\([^)]*\\))?)");

    /** The '}' that closes the tag opening at {@code open}; {@code {@code {}}} holds braces of its own. */
    private static int matchingBrace(String s, int open) {
        int depth = 0;
        for (int k = open; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return k;
        }
        return -1;
    }

    /** {@code link Foo#bar label}, the inside of an inline tag, in KDoc's Markdown; an unknown tag as written. */
    private static String inlineTag(String inside) {
        // the name ends at any white space: '{@code' at the end of a line has its content on the next one
        Matcher nameEnd = WHITE_SPACE.matcher(inside);
        int space = nameEnd.find() ? nameEnd.start() : -1;
        String name = space < 0 ? inside : inside.substring(0, space);
        String content = space < 0 ? "" : inside.substring(space).strip();
        return switch (name) {
            case "link", "linkplain" -> link(content.replaceAll("\\s+", " "));
            case "code", "literal" -> content.contains("`") ? "`` " + content + " ``" : "`" + content + "`";
            // {@snippet : code}: KDoc's is a fenced block (attributes before the ':' are dropped)
            case "snippet" -> "\n```\n" + (content.contains(":") ? content.substring(content.indexOf(':') + 1)
                    : content).strip() + "\n```\n";
            // KDoc inherits an overridden member's documentation by itself
            case "inheritDoc" -> "";
            default -> "{@" + inside + "}";
        };
    }

    /** {@code Foo#bar(int) the label} is {@code [the label][Foo.bar]}. */
    static String link(String content) {
        String[] referenceAndLabel = reference(content);
        String target = target(referenceAndLabel[0]);
        return referenceAndLabel[1].isEmpty() ? "[" + target + "]" : "[" + referenceAndLabel[1] + "][" + target + "]";
    }

    /** The reference, which may hold a parameter list with spaces, and the label after it. */
    private static String[] reference(String content) {
        int paren = content.indexOf('(');
        int space = content.indexOf(' ');
        int end;
        if (paren >= 0 && (space < 0 || paren < space)) {
            int close = content.indexOf(')', paren);
            end = close < 0 ? content.length() : close + 1;
        } else {
            end = space < 0 ? content.length() : space;
        }
        return new String[]{content.substring(0, end), content.substring(end).strip()};
    }

    /** KDoc names a member with a dot and no parameter list; {@code #bar} is a member of the documented type. */
    private static String target(String reference) {
        String noParameters = reference.replaceFirst("\\(.*\\)$", "");
        return noParameters.startsWith("#") ? noParameters.substring(1) : noParameters.replace('#', '.');
    }
}
