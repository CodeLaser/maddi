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
import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.parser.Context;
import io.codelaser.maddi.inspection.api.parser.ForwardType;
import io.codelaser.maddi.inspection.api.parser.MethodResolution;
import io.codelaser.maddi.parser.java.erasure.LambdaErasure;
import io.codelaser.maddi.support.Either;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.Identifier;
import org.parsers.java.ast.ObjectType;
import org.parsers.java.ast.Type;

import java.util.List;
import java.util.Set;

public class ParseMethodReference extends CommonParse {

    public ParseMethodReference(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public Expression parse(Context context, List<Comment> comments, Source source, String index,
                            ForwardType forwardType,
                            org.parsers.java.ast.MethodReference mr) {
        Expression scope;
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        Node n0 = mr.getFirst();
        if (n0 instanceof Type) {
            // BEWARE! even if n0 represents a variable, we may end up in this branch
            ParameterizedType pt = parsers.parseType().parse(context, n0, false, null, detailedSourcesBuilder);
            if (pt != null) {
                scope = runtime.newTypeExpressionBuilder().setParameterizedType(pt)
                        .setDiamond(runtime.diamondNo()).
                        setSource(source(n0)).build();
            } else if (n0 instanceof ObjectType ot) {
                // try again, cannot be a type
                scope = parsers.parseExpression().parse(context, index, forwardType, ot);
            } else throw new UnsupportedOperationException("NYI");
        } else {
            scope = parsers.parseExpression().parse(context, index, forwardType, n0);
        }
        String methodName;
        Node mr2 = mr.get(2);
        if (mr2 instanceof Identifier id) {
            methodName = id.getSource();
        } else if (Token.TokenType.NEW.equals(mr2.getType())) {
            methodName = "new";
        } else {
            throw new UnsupportedOperationException();
        }
        if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(methodName, source(mr2));
        Source source1 = detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build());
        if (forwardType.erasure()) {
            Either<Set<MethodResolution.Count>, Expression> either = context.methodResolution()
                    .computeMethodReferenceErasureCounts(context, comments, source1, scope, methodName);
            if (either.isRight()) return either.getRight();
            return new LambdaErasure(runtime, either.getLeft(), source1);
        }

        return context.methodResolution().resolveMethodReference(context, comments, source1, index, forwardType,
                scope, methodName);
    }

}
