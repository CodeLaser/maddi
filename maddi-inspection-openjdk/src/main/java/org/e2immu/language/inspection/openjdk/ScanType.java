package org.e2immu.language.inspection.openjdk;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.util.RecordSynthetics;

public record ScanType(ScanData sd) {

    private void continueType(TypeInfo typeInfo, JCTree.JCClassDecl jcClassDecl) {
        typeStack.addLast(typeInfo);
        sd.elementStack().push();

        // flags: modifiers, type nature
        sd.flagHelper().type(jcClassDecl.sym, typeInfo.builder());
        typeInfo.builder().computeAccess();

        // type parameters
        int index = 0;
        for (JCTree.JCTypeParameter jcTypeParameter : jcClassDecl.getTypeParameters()) {
            String name = jcTypeParameter.getName().toString();
            TypeParameter tp = sd.runtime().newTypeParameter(index, name, typeInfo);
            typeInfo.builder().addOrSetTypeParameter(tp);
            sd.elementStack().put(name, tp);
            ++index;
        }

        DetailedSources.Builder dsb = sd.runtime().newDetailedSourcesBuilder();
        ParameterizedType parentClass;
        if (typeInfo.typeNature().isEnum()) {
            if (jcClassDecl.type instanceof Type.ClassType ct) {
                parentClass = sd.convertType().convert(ct.supertype_field);
            } else throw new UnsupportedOperationException("NYI");
        } else {
            ParameterizedType explicitParentClass = sd.convertType().convertTree(jcClassDecl.extending, dsb);
            parentClass = explicitParentClass.isVoid() ? sd.runtime().objectParameterizedType()
                    : explicitParentClass;
        }
        typeInfo.builder().setParentClass(parentClass);
        if (!jcClassDecl.implementing.isEmpty()) {
            String keyword = typeInfo.isInterface() ? "extends" : "implements";
            Source source = sd.scanResult().find(keyword, sd.sourceForNode(jcClassDecl.implementing.getFirst()));
            dsb.put(DetailedSources.IMPLEMENTS, source);
            for (JCTree.JCExpression i : jcClassDecl.implementing) {
                typeInfo.builder().addInterfaceImplemented(sd.convertType().convertTree(i, dsb));
            }
        }
        if (typeInfo.typeNature().isAnnotation()) {
            ParameterizedType javaLangAnnotationAnnotation = sd.convertType().convert(jcClassDecl.sym.getInterfaces().getFirst());
            typeInfo.builder().addInterfaceImplemented(javaLangAnnotationAnnotation);
        }
        for (JCTree.JCExpression permits : jcClassDecl.permitting) {
            TypeInfo permitted = sd.convertType().convert(permits.type).typeInfo();
            typeInfo.builder().addPermittedType(permitted);
            dsb.put(permitted, sd.sourceForNode(permits));
        }

        // record components: fields and accessors
        if (typeInfo.typeNature().isRecord()) {
            RecordSynthetics recordSynthetics = new RecordSynthetics(sd.runtime(), typeInfo);
            for (var rc : jcClassDecl.sym.getRecordComponents()) {
                ParameterizedType pt = sd.convertType().convert(rc.type);
                FieldInfo fieldInfo = sd.runtime().newFieldInfo(rc.name.toString(), false, pt, typeInfo);
                fieldInfo.builder().addFieldModifier(sd.runtime().fieldModifierFinal())
                        .addFieldModifier(sd.runtime().fieldModifierPrivate());
                typeInfo.builder().addField(fieldInfo);
                MethodInfo accessor = recordSynthetics.createAccessor(fieldInfo);
                typeInfo.builder().addMethod(accessor);
                sd.typeData().put(rc.accessor, accessor);
            }
        }
        // annotations
        for (JCTree.JCAnnotation annotation : jcClassDecl.getModifiers().getAnnotations()) {
            AnnotationExpression ae = convertAnnotation(annotation);
            typeInfo.builder().addAnnotation(ae);
        }

        // members: methods, fields
        for (var member : jcClassDecl.getMembers()) {
            currentMethod = null;
            scan(member, null);
        }
        MethodInfo singleAbstractMethod = sd.convertType().computeSAM(jcClassDecl.type);
        typeInfo.builder().setSingleAbstractMethod(singleAbstractMethod);

        Source source = sd.sourceForNode(jcClassDecl, dsb);
        typeInfo.builder()
                .addTrailingComments(sd.trailingCommentsForNode(source))
                .addComments(sd.commentsForNode(source))
                .setSource(source);

        typeStack.removeLast();
        sd.elementStack().pop();
    }

}
