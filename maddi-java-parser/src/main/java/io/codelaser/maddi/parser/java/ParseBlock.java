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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.inspection.api.parser.Context;
import io.codelaser.maddi.util.StringUtil;
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
                io.codelaser.maddi.cst.api.statement.Statement statement = parsers.parseStatement()
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
