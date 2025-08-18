package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.info.InfoMap;
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

    public MultiLineCommentImpl(Source source, String comment) {
        this.comment = strip(comment.trim());
        this.source = source;
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
        OutputBuilder joinedText = Arrays.stream(comment.split("\n"))
                .filter(line -> !line.isBlank())
                .map(line -> new OutputBuilderImpl().add(new TextImpl(line)))
                .collect(OutputBuilderImpl.joining(SpaceEnum.ONE_IS_NICE_EASY_SPLIT,
                        SymbolEnum.LEFT_BLOCK_COMMENT,
                        SymbolEnum.RIGHT_BLOCK_COMMENT,
                        gg));
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
