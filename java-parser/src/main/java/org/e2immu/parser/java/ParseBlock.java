package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.util.internal.util.StringUtil;
import org.parsers.java.Node;
import org.parsers.java.ast.*;

import java.util.List;

public class ParseBlock extends CommonParse {

    public ParseBlock(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public Block parse(Context context, String index, String label, CodeBlock codeBlock) {
        return parse(context, index, label, codeBlock, false, 0);
    }

    public Block parse(Context context, String index, String label, CodeBlock codeBlock,
                       boolean asSeparateStatement, int startCount) {
        Source source = source(index, codeBlock);
        List<Comment> comments = comments(codeBlock);
        Block.Builder builder = runtime.newBlockBuilder();
        int count = startCount;
        int n = codeBlock.size() - 2; // delimiters at front and back: '{', '}'
        String dot = asSeparateStatement ? ".0." : ".";
        for (Node child : codeBlock) {
            String sIndex = (index.isEmpty() ? "" : index + dot) + StringUtil.pad(count, n);
            if (child instanceof Statement s) {
                org.e2immu.language.cst.api.statement.Statement statement = parsers.parseStatement()
                        .parse(context, sIndex, s);
                builder.addStatement(statement);
                count++;
            } else if (child instanceof TypeDeclaration classDeclaration) {
                // local class declaration
                LocalTypeDeclaration lcd = new ParseLocalTypeDeclaration(runtime, parsers)
                        .parse(context, sIndex, classDeclaration);
                builder.addStatement(lcd);
                count++;
            } else if (!(child instanceof Delimiter)) {
                throw new UnsupportedOperationException("NYI: " + child.getClass());
            }
        }
        List<Comment> trailingComments = comments(codeBlock.getLastChild());
        return builder.addTrailingComments(trailingComments)
                .setSource(source).addComments(comments).setLabel(label).build();
    }
}
