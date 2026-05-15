package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record ScanAnnotation(ScanData sd) {

    ParameterizedType convertTypeWithAnnotations(Tree node,
                                                 DetailedSources.Builder dsb,
                                                 Consumer<AnnotationExpression> consumer) {
        Tree rt;
        if (node instanceof JCTree.JCAnnotatedType at) {
            rt = at.getUnderlyingType();
            for (JCTree.JCAnnotation annotationTree : at.getAnnotations()) {
                consumer.accept(convertAnnotation(annotationTree));
            }
        } else {
            rt = node;
        }
        return sd.convertType().convertTree(rt, dsb);
    }

    AnnotationExpression convertAnnotation(JCTree.JCAnnotation annotation) {
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        ParameterizedType at = sd.convertType().convertTree(annotation.getAnnotationType(), dsb);
        List<AnnotationExpression.KV> kvs = new ArrayList<>();
        for (var c : annotation.getArguments()) {
            kvs.add(convertAnnotationKv(c));
        }
        return sd.runtime().newAnnotationExpressionBuilder()
                .setKeyValuesPairs(kvs)
                .setSource(sd.sourceForNode(annotation, dsb))
                .setTypeInfo(at.typeInfo())
                .build();
    }

    AnnotationExpression.KV convertAnnotationKv(JCTree.JCExpression c) {
        String key;
        Expression value;
        if (c instanceof JCTree.JCAssign assign) {
            if (assign.lhs instanceof JCTree.JCIdent ident) {
                key = ident.name.toString();
            } else throw new UnsupportedOperationException();
            scan(assign.rhs, null);
        } else {
            key = "value";
            scan(c, null);
        }
        value = currentExpression;
        return runtime.newAnnotationExpressionKeyValuePair(key, value);
    }

}
