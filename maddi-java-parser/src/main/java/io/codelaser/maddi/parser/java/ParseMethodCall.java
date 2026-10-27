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
import io.codelaser.maddi.parser.java.erasure.MethodCallErasure;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ParseMethodCall extends CommonParse {
    private final static Logger LOGGER = LoggerFactory.getLogger(ParseMethodCall.class);

    protected ParseMethodCall(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public Expression parse(Context context, List<Comment> comments, Source source,
                            String index, ForwardType forwardType, org.parsers.java.ast.MethodCall mc) {
        List<Object> unparsedArguments = new ArrayList<>();
        Node name = mc.getFirst();
        assert name instanceof Name || name instanceof DotName || name instanceof SuperDotName;
        Node methodNameNode = name.getLast();
        String methodName = methodNameNode.getSource();
        Source sourceOfName = source(methodNameNode);

        Name unparsedObject = newNameObject(name);
        DetailedSources.Builder dsb = context.newDetailedSourcesBuilder();

        InvocationArguments ia = (InvocationArguments) mc.get(1);
        int i = 1;
        List<Node> commas = dsb == null ? null : new ArrayList<>();
        while (i < ia.size() && !(ia.get(i) instanceof Delimiter)) {
            if (dsb != null && i + 1 < ia.size()
                && ia.get(i + 1) instanceof Delimiter d && d.getType() == Token.TokenType.COMMA) {
                commas.add(d);
            }
            unparsedArguments.add(ia.get(i));
            i += 2;
        }
        if (dsb != null) {
            addCommaList(commas, dsb, DetailedSources.ARGUMENT_COMMAS);
            Node last = ia.getLast();
            assert last.getType() == Token.TokenType.RPAREN;
            dsb.put(DetailedSources.END_OF_ARGUMENT_LIST, source(last));
        }

        if (forwardType.erasure()) {
            Set<ParameterizedType> types = context.methodResolution().computeScope(context, index, methodName,
                    unparsedObject, unparsedArguments);
            LOGGER.debug("Erasure types: {}", types);
            ParameterizedType common;
            ParameterizedType first = types.stream().findFirst().orElseThrow();
            if (types.size() > 1) {
                common = types.stream().reduce(first, runtime::commonType);
            } else {
                common = first;
            }
            return new MethodCallErasure(runtime, source, types, common, methodName);
        }
        List<ParameterizedType> methodTypeArguments = methodTypeArguments(context, name, dsb);

        // now we should have a more correct forward type!
        return context.methodResolution().resolveMethod(context, comments, source, sourceOfName,
                index, forwardType, methodName,
                unparsedObject,
                unparsedObject == null ? runtime.noSource() : source(unparsedObject),
                methodTypeArguments,
                dsb,
                unparsedArguments);
    }

    private List<ParameterizedType> methodTypeArguments(Context context, Node dotName, DetailedSources.Builder dsb) {
        TypeArguments typeArguments = dotName.firstChildOfType(TypeArguments.class);
        if (typeArguments == null) {
            return List.of();
        }
        List<ParameterizedType> list = new ArrayList<>();
        List<Node> commas = dsb == null ? null : new ArrayList<>();
        for (int i = 1; i < typeArguments.size(); i += 2) {
            if (dsb != null && i + 1 < typeArguments.size()
                && typeArguments.get(i + 1) instanceof Delimiter d && d.getType() == Token.TokenType.COMMA) {
                commas.add(d);
            }
            ParameterizedType pt = parsers.parseType().parse(context, typeArguments.get(i), true, null, dsb);
            list.add(pt);
        }
        if (dsb != null) addCommaList(commas, dsb, DetailedSources.TYPE_ARGUMENT_COMMAS);
        return List.copyOf(list);
    }

    private Name newNameObject(Node name) {
        if (name.size() == 1) return null;
        Name n = new Name();
        for (int i = 0; i < name.size() - 2; i++) {
            n.add(i, name.get(i));
        }
        n.setParent(name.getParent());
        n.setTokenSource(name.getTokenSource());
        n.setBeginOffset(name.getBeginOffset());
        n.setEndOffset(name.get(name.size() - 3).getEndOffset());
        return n;
    }
}

