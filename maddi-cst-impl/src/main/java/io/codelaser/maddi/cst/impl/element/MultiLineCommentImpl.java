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

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.cst.api.element.*;
import io.codelaser.maddi.cst.api.output.Formatter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MultiLineCommentImpl implements MultiLineComment {
    private final String comment;
    private final Source source;
    private final boolean addNewline;

    public MultiLineCommentImpl(Source source, String comment, boolean addNewline) {
        this.comment = strip(comment.trim());
        this.source = source;
        this.addNewline = addNewline;
    }

    private static String strip(String s) {
        String leading = SymbolEnum.LEFT_BLOCK_COMMENT.symbol();
        String s1 = s.startsWith(leading) ? s.substring(leading.length()) : s;
        String trailing = SymbolEnum.RIGHT_BLOCK_COMMENT.symbol();
        return s1.endsWith(trailing) ? s1.substring(0, s1.length() - trailing.length()) : s1;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MultiLineCommentImpl that)) return false;
        return addNewline == that.addNewline && Objects.equals(comment, that.comment) && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment, source, addNewline);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        boolean multiLine = comment.contains("\n");
        if(multiLine) {
            return multilinePrint();
        }
        OutputBuilder ob = Arrays.stream(comment.split("\n"))
                .filter(line -> !line.isBlank())
                .map(line -> new OutputBuilderImpl().add(new TextImpl(line)))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE,
                        SymbolEnum.LEFT_BLOCK_COMMENT,
                        SymbolEnum.RIGHT_BLOCK_COMMENT,
                        GuideImpl.defaultGuideGenerator()));
        if (addNewline) ob.add(SpaceEnum.NEWLINE);
        return ob;
    }

    protected OutputBuilder multilinePrint() {
        GuideImpl.GuideGenerator gg = GuideImpl.generatorForMultilineComment();
        String text = "/*" + comment() + "*/";
        String[] split = text.split("\n");
        // stripTrailing: every other line is trim()ed below, but the first line kept its trailing
        // whitespace (construction trims only the comment's ENDS) — and a trailing space here trips the
        // formatter's Output invariant ("An output cannot end in a space", BlockPrinter) on real-world
        // sources with whitespace before the newline inside block comments
        OutputBuilder firstLine = new OutputBuilderImpl().add(new TextImpl(split[0].stripTrailing()));
        OutputBuilder joinedText = Stream.concat(Stream.of(firstLine), Arrays.stream(split).skip(1)
                        .filter(line -> !line.isBlank())
                        .map(line -> new OutputBuilderImpl()
                                .add(new TextImpl(Formatter.HARD_SPACE + line.trim()))))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, gg));
        return new OutputBuilderImpl().add(joinedText);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        return Stream.empty();
    }

    @Override
    public Stream<Variable> variableStreamDescend() {
        return Stream.empty();
    }

    @Override
    public Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
        return Stream.empty();
    }

    @Override
    public String comment() {
        return comment;
    }

    @Override
    public int complexity() {
        return 0;
    }

    @Override
    public List<Comment> comments() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Source source() {
        return source;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        // do nothing
    }

    @Override
    public void visit(Visitor visitor) {
        // do nothing
    }

    @Override
    public String toString() {
        return "multiLineComment@" + source.compact2();
    }
}
