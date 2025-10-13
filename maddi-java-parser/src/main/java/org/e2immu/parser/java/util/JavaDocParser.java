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

package org.e2immu.parser.java.util;

import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record JavaDocParser(Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaDocParser.class);

    private static final Pattern BLOCK_TAG = Pattern.compile("^(/\\*\\*|\\s*\\*\\s*)@(\\p{Alpha}+)");
    private static final Pattern INLINE_TAG = Pattern.compile("\\{@(\\p{Alpha}+)\\s+([^}]+)}");

    public JavaDoc extractTags(String comment, Source sourceOfComment) {
        try {
            return internalExtractTags(comment, sourceOfComment);
        } catch (RuntimeException re) {
            LOGGER.error("Caught exception parsing javadoc comment at {}", sourceOfComment.compact2());
            throw re;
        }
    }

    private JavaDoc internalExtractTags(String comment, Source sourceOfComment) {
        List<JavaDoc.Tag> tags = new ArrayList<>();
        int lineCount = 0;
        StringBuilder modifiedComment = new StringBuilder();
        for (String line : comment.split("\\n")) {
            Matcher blockTagMatchar = BLOCK_TAG.matcher(line);
            String restOfLine;
            if (blockTagMatchar.find()) {
                JavaDoc.TagIdentifier identifier = JavaDoc.identifier(blockTagMatchar.group(2));
                int endOfTag = blockTagMatchar.end(2);
                modifiedComment.append(blockTagMatchar.group(1));
                if (identifier != null) {
                    int args = identifier.argumentsAsBlockTag();
                    int end;
                    String content;
                    Source sourceOfReference;
                    if (args > 0 && endOfTag + 1 < line.length()) {
                        int nextSpace = line.indexOf(' ', endOfTag + 1);
                        if (nextSpace == -1) {
                            content = line.substring(endOfTag + 1);
                        } else {
                            content = line.substring(endOfTag + 1, nextSpace);
                        }
                        end = endOfTag + 1 + content.length();
                        // note that there is an offset of 1
                        sourceOfReference = subSource(sourceOfComment, lineCount, endOfTag + 2, end);
                    } else {
                        content = "";
                        end = endOfTag;
                        sourceOfReference = null;
                    }
                    Source sourceOfTag = subSource(sourceOfComment, lineCount, blockTagMatchar.start(2), end);
                    JavaDoc.Tag blockTag = runtime.newJavaDocTag(identifier, content, null, sourceOfTag,
                            sourceOfReference, true);
                    modifiedComment.append("{").append(tags.size()).append("} ");
                    tags.add(blockTag);
                    restOfLine = end + 1 >= line.length() ? "" : line.substring(end + 1);
                } else {
                    restOfLine = endOfTag >= line.length() ? "" : line.substring(endOfTag);
                    modifiedComment.append(blockTagMatchar.group());
                }
            } else {
                restOfLine = line;
            }
            Matcher inlineTagMatcher = INLINE_TAG.matcher(restOfLine);
            while (inlineTagMatcher.find()) {
                String content = inlineTagMatcher.group(2);
                JavaDoc.TagIdentifier identifier = JavaDoc.identifier(inlineTagMatcher.group(1));
                Source sourceOfTag = subSource(sourceOfComment, lineCount, 1 + inlineTagMatcher.start(), inlineTagMatcher.end());
                Source sourceOfReference = subSource(sourceOfComment, lineCount, 1 + inlineTagMatcher.start(2),
                        inlineTagMatcher.end(2));
                JavaDoc.Tag tag = runtime().newJavaDocTag(identifier, content, null, sourceOfTag,
                        sourceOfReference, false);
                inlineTagMatcher.appendReplacement(modifiedComment, "{" + tags.size() + "}");
                tags.add(tag);
            }
            inlineTagMatcher.appendTail(modifiedComment);
            modifiedComment.append('\n');
            ++lineCount;
        }
        return runtime.newJavaDoc(sourceOfComment, modifiedComment.toString(), List.copyOf(tags));
    }

    private Source subSource(Source main, int line, int begin, int end) {
        int absoluteLine = main.beginLine() + line;
        return runtime.newParserSource(main.index(), absoluteLine, begin, absoluteLine, end);
    }
}
