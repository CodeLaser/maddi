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
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SingleLineCommentImpl implements SingleLineComment {
    private final String comment;
    private final Source source;

    public SingleLineCommentImpl(Source source, String comment) {
        this.comment = strip(comment.trim());
        this.source = source;
    }

    private static String strip(String s) {
        String leading = SymbolEnum.SINGLE_LINE_COMMENT.symbol();
        if (s.startsWith(leading)) return s.substring(leading.length());
        return s;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder ob = new OutputBuilderImpl().add(SymbolEnum.SINGLE_LINE_COMMENT);
        if (!comment.isEmpty()) ob.add(new TextImpl(comment));
        return ob.add(SpaceEnum.NEWLINE);
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
        return "singleLineComment@" + source.compact2();
    }
}
