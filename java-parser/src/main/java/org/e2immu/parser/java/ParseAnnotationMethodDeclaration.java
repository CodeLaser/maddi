package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.MethodModifier;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Summary;
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
