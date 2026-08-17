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
