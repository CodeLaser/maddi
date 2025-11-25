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

package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.*;

import java.util.Arrays;
import java.util.List;
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
    public OutputBuilder print(Qualification qualification) {
        boolean multiLine = comment.contains("\n");
        GuideImpl.GuideGenerator gg = multiLine ? GuideImpl.generatorForMultilineComment()
                : GuideImpl.defaultGuideGenerator();
        OutputBuilder ob = Arrays.stream(comment.split("\n"))
                .filter(line -> !line.isBlank())
                .map(line -> new OutputBuilderImpl().add(new TextImpl(line)))
                .collect(OutputBuilderImpl.joining(SpaceEnum.ONE_IS_NICE_EASY_SPLIT,
                        SymbolEnum.LEFT_BLOCK_COMMENT,
                        SymbolEnum.RIGHT_BLOCK_COMMENT,
                        gg));
        if (addNewline) ob.add(SpaceEnum.NEWLINE);
        return ob;
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
    public Stream<TypeReference> typesReferenced() {
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
