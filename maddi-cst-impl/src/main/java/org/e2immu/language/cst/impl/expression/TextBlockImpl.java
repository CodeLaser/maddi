package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.TextBlock;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.output.element.TextBlockFormatting;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.List;

public class TextBlockImpl extends StringConstantImpl implements TextBlock {
    private final TextBlockFormatting textBlockFormatting;

    public TextBlockImpl(List<Comment> comments,
                         Source source,
                         ParameterizedType stringPt,
                         String constant,
                         TextBlockFormatting textBlockFormatting) {
        super(comments, source, stringPt, constant);
        this.textBlockFormatting = textBlockFormatting;
    }

    @Override
    public TextBlockFormatting textBlockFormatting() {
        return textBlockFormatting;
    }

    @Override
    public Expression withSource(Source source) {
        return new TextBlockImpl(comments(), source, parameterizedType(), constant(), textBlockFormatting);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextBlock that)) return false;
        return constant().equals(that.constant());
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(new TextImpl(constant(), textBlockFormatting));
    }

    @Override
    public String toString() {
        return "textBlock@" + source().compact2();
    }
}
