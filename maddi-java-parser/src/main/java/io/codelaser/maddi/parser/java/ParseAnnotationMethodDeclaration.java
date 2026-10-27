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

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.MethodModifier;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.parser.Context;
import io.codelaser.maddi.inspection.api.parser.Summary;
import org.parsers.java.Node;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.List;

public class ParseAnnotationMethodDeclaration extends CommonParse {

    public ParseAnnotationMethodDeclaration(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public MethodInfo parse(Context context, AnnotationMethodDeclaration amd) {
        assert context.enclosingType() != null;
        int i = 0;
        List<Annotation> annotations = new ArrayList<>();
        List<MethodModifier> methodModifiers = new ArrayList<>();
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();

        label:
        while (true) {
            Node mdi = amd.get(i);
            switch (mdi) {
                case Annotation a:
                    annotations.add(a);
                    break;
                case Modifiers modifiers:
                    for (Node node : modifiers.children()) {
                        if (node instanceof Annotation a) {
                            annotations.add(a);
                        } else if (node instanceof KeyWord keyWord) {
                            MethodModifier m = methodModifier(keyWord);
                            methodModifiers.add(m);
                            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(m, source(keyWord));
                        }
                    }
                    break;
                case KeyWord keyWord:
                    MethodModifier m = methodModifier(keyWord);
                    methodModifiers.add(m);
                    if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(m, source(keyWord));
                    break;
                case TypeParameters _:
                    throw new UnsupportedOperationException("? type parameters in annotations?");
                case null:
                default:
                    break label;
            }
            i++;
        }

        MethodInfo.MethodType methodType;
        ParameterizedType returnType;
        Node typeNode = amd.children().get(i);
        if (typeNode instanceof Type type) {
            // depending on the modifiers...
            methodType = runtime.methodTypeMethod();
            returnType = parsers.parseType().parse(context, type, detailedSourcesBuilder);
            i++;
        } else {
            throw new RuntimeException("Expect Type, got " + typeNode.getClass());
        }
        String name;
        Node identifierNode = amd.children().get(i);
        if (identifierNode instanceof Identifier identifier) {
            name = identifier.getSource();
            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(name, source(identifier));
        } else throw new Summary.ParseException(context, "Expect Identifier, got " + identifierNode.getClass());
        MethodInfo methodInfo = runtime.newMethod(context.enclosingType(), name, methodType);
        MethodInfo.Builder builder = methodInfo.builder()
                .setMethodBody(runtime.emptyBlock())
                .setAccess(runtime.accessPublic())
                .setReturnType(returnType);

        parseAnnotations(context, builder, annotations);
        methodModifiers.forEach(builder::addMethodModifier);

        builder.commitParameters();

        builder.addComments(comments(amd));
        Source source = source(amd);
        builder.setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()));
        return methodInfo;
    }
}
