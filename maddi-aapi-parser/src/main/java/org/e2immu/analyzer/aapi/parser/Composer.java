/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.analyzer.aapi.parser;


import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.util.internal.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/*
Given a number of types, compose one annotated API "file" per package, in the style of the JavaUtil, JavaLang classes.
The file is generated in the form of a TypeInfo object, which can be output.

The general structure is

public class NameOfPackageWithoutDots {
    final static String PACKAGE_NAME = "name.of.package.without.dots";

    // the $ means that we'll relocate towards PACKAGE_NAME

    public static class/public interface Type1Name$ {
        public methods or constructors
            methods return { null } or the correct primary type
    }
    ...
    public static class/public interface Type2Name$ {

        public class InnerType { // doesn't need a $ anymore

        }
    }
 }

- The purpose of this class is to generate an AnnotatedAPI file for others to start editing.
  This can be run on byte-code inspected Java, meaning the JavaParser needn't used, so we can do Java 16 already.

- Only public methods, types and fields will be shown.

 */
public class Composer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Composer.class);
    private final Runtime runtime;
    private final JavaInspector javaInspector;
    private final Function<SourceSet, String> destinationPackage;
    private final Predicate<Info> predicate;
    private final Map<Element, Element> translateFromDollarToReal = new HashMap<>();
    private final ImportComputer importComputer;

    public Composer(JavaInspector javaInspector,
                    Function<SourceSet, String> destinationPackage,
                    Predicate<Info> predicate) {
        this(javaInspector, javaInspector.importComputer(4), destinationPackage, predicate);
    }

    public Composer(JavaInspector javaInspector,
                    ImportComputer importComputer,
                    Function<SourceSet, String> destinationPackage,
                    Predicate<Info> predicate) {
        this.runtime = javaInspector.runtime();
        this.javaInspector = javaInspector;
        this.destinationPackage = destinationPackage;
        this.predicate = predicate;
        this.importComputer = importComputer;
    }

    public Collection<TypeInfo> compose(Collection<TypeInfo> primaryTypes) {
        SourceSet javaBase = javaInspector.compiledTypesManager().get(AutoCloseable.class).compilationUnit().sourceSet();
        if (javaBase == null || !javaBase.name().contains("java.base")) {
            throw new RuntimeException("?");
        }

        Map<String, TypeInfo> typesPerPackage = new HashMap<>();
        for (TypeInfo primaryType : primaryTypes) {
            if (acceptTypeOrAnySubType(primaryType)) {
                assert primaryType.isPrimaryType();
                String packageName = primaryType.packageName();
                TypeInfo packageType = typesPerPackage.computeIfAbsent(packageName,
                        pn -> newPackageType(Objects.requireNonNullElse(primaryType.compilationUnit().sourceSet(), javaBase), pn));
                appendType(packageType, primaryType, null);
            }
        }
        List<TypeInfo> allTypes = typesPerPackage.values().stream().toList();
        allTypes.forEach(t -> t.builder().commit());
        return allTypes;
    }

    private void appendType(TypeInfo parentType, TypeInfo typeInfo, TranslationMap tm) {
        if (!acceptTypeOrAnySubType(typeInfo)) return;
        TypeTm newTypeTm = createType(parentType, typeInfo, tm);
        TypeInfo newType = newTypeTm.typeInfo;

        translateFromDollarToReal.put(newType, typeInfo);

        newType.builder().addComment(addCommentLine(typeInfo));

        for (TypeInfo subType : typeInfo.subTypes()) {
            appendType(newType, subType, newTypeTm.tm);
        }
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            if (fieldInfo.access().isPublic() && predicate.test(fieldInfo)) {
                FieldInfo newField = createField(fieldInfo, newType);
                translateFromDollarToReal.put(newField, fieldInfo);
                newType.builder().addField(newField);
            }
        }
        for (MethodInfo constructor : typeInfo.constructors()) {
            if (constructor.isPublic() && !constructor.isSynthetic() && predicate.test(constructor)) {
                MethodInfo newConstructor = createMethod(constructor, newType, newTypeTm.tm);
                translateFromDollarToReal.put(newConstructor, constructor);
                newConstructor.parameters().forEach(newPi ->
                        translateFromDollarToReal.put(newPi, constructor.parameters().get(newPi.index())));
                newType.builder().addMethod(newConstructor);
            }
        }
        for (MethodInfo methodInfo : typeInfo.methods()) {
            if (methodInfo.isPublic()
                && !methodInfo.isSynthetic()
                && predicate.test(methodInfo)
                && validJavaName(methodInfo.name()) != null
                && methodInfo.parameters().stream().allMatch(p -> validJavaName(p.name()) != null)
            ) {
                MethodInfo newMethod = createMethod(methodInfo, newType, newTypeTm.tm);
                translateFromDollarToReal.put(newMethod, methodInfo);
                newMethod.parameters().forEach(newPi ->
                        translateFromDollarToReal.put(newPi, methodInfo.parameters().get(newPi.index())));
                newType.builder().addMethod(newMethod);
            }
        }
        newType.builder().commit();
        parentType.builder().addSubType(newType);
    }

    private Comment addCommentLine(MethodInfo methodInfo) {
        String shortString = "override from " + methodInfo.overrides()
                .stream().map(mi -> mi.typeInfo().fullyQualifiedName()).sorted().collect(Collectors.joining(", "));
        return runtime.newSingleLineComment(null, shortString);
    }

    private Comment addCommentLine(TypeInfo typeInfo) {
        String access = runtime.newTypePrinter(typeInfo, true).minimalModifiers(typeInfo)
                .stream().map(m -> m.keyword().minimal())
                .collect(Collectors.joining(" ", "", " "));
        String type = typeInfo.typeNature().keyword().minimal() + " ";
        String extendString = typeInfo.parentClass() == null || typeInfo.parentClass().isJavaLangObject() ? ""
                : " extends " + typeInfo.parentClass().simpleString();
        String implementString = typeInfo.interfacesImplemented().isEmpty() ? ""
                : " implements " + typeInfo.interfacesImplemented().stream()
                .map(i -> i.print(runtime.qualificationQualifyFromPrimaryType(), false, runtime.diamondShowAll()).toString())
                .collect(Collectors.joining(", "));
        String shortString = access + type + typeInfo.simpleName() + extendString + implementString;
        return runtime.newSingleLineComment(null, shortString);
    }

    private boolean acceptTypeOrAnySubType(TypeInfo typeInfo) {
        if (!typeInfo.isPubliclyAccessible()) return false;
        if (predicate.test(typeInfo)) return true;
        return typeInfo.subTypes().stream().anyMatch(this::acceptTypeOrAnySubType);
    }

    private FieldInfo createField(FieldInfo fieldInfo, TypeInfo owner) {
        FieldInfo newField = runtime.newFieldInfo(fieldInfo.name(), fieldInfo.isStatic(), fieldInfo.type(), owner);
        FieldInfo.Builder builder = newField.builder();
        fieldInfo.modifiers().stream().filter(m -> !m.isPublic()).forEach(builder::addFieldModifier);
        if (fieldInfo.isFinal()) {
            builder.setInitializer(runtime.nullValue(fieldInfo.type()));
        } else {
            builder.setInitializer(runtime.newEmptyExpression());
        }
        builder.setAccess(runtime.accessPackage()).commit();
        return newField;
    }

    private MethodInfo createMethod(MethodInfo methodInfo, TypeInfo owner, TranslationMap typeTm) {
        MethodInfo newMethod;
        if (methodInfo.isConstructor()) {
            newMethod = runtime.newConstructor(owner);
        } else {
            MethodInfo.MethodType methodType = methodInfo.isStatic() ? runtime.methodTypeStaticMethod()
                    : runtime.methodTypeMethod();
            String name;
            if (methodInfo.typeInfo().isJavaLangObject() && methodInfo.isFinal()) {
                name = methodInfo.name() + "$";
            } else {
                name = validJavaName(methodInfo.name());
            }
            newMethod = runtime.newMethod(owner, name, methodType);
            if (!methodInfo.overrides().isEmpty()) {
                newMethod.builder().addComment(addCommentLine(methodInfo));
            }
        }
        ParameterizedType returnType = methodInfo.returnType();
        newMethod.builder().setAccess(runtime.accessPackage());
        if (methodInfo.hasReturnValue()) {
            Expression defaultReturnValue = runtime.nullValue(returnType);
            Statement returnStatement = runtime.newReturnStatement(defaultReturnValue);
            Block block = runtime.newBlockBuilder().addStatement(returnStatement).build();
            newMethod.builder().setMethodBody(block);
        } else {
            newMethod.builder().setMethodBody(runtime.newBlockBuilder().build());
        }

        // 2-stage type parameter copying: we must handle self-references properly
        TranslationMap.Builder tmb = runtime.newTranslationMapBuilder(typeTm);
        List<TypeParameter> newTypeParameters = new ArrayList<>();
        for (TypeParameter tp : methodInfo.typeParameters()) {
            TypeParameter newTp = runtime.newTypeParameter(tp.getIndex(), tp.simpleName(), newMethod);
            newMethod.builder().addTypeParameter(newTp);
            tmb.put(tp, newTp);
            newTypeParameters.add(newTp);
        }
        TranslationMap tm = tmb.build();
        int i = 0;
        for (TypeParameter tp : methodInfo.typeParameters()) {
            TypeParameter newTp = newTypeParameters.get(i++);
            newTp.builder().setTypeBounds(tp.typeBounds().stream().map(tm::translateType).toList());
            newTp.builder().commit();
        }

        for (ParameterInfo p : methodInfo.parameters()) {
            String name = validJavaName(p.name());
            assert name != null;
            ParameterizedType newPt = tm.translateType(p.parameterizedType());
            ParameterInfo pi = newMethod.builder().addParameter(name, newPt);
            pi.builder().setVarArgs(p.isVarArgs()).commit();
        }
        newMethod.builder().setReturnType(tm.translateType(returnType));

        if (methodInfo.isOverloadOfJLOMethod()) {
            LOGGER.info("Method {} is overload", methodInfo);
            if ("clone".equals(methodInfo.name()) || "finalize".equals(methodInfo.name())) {
                newMethod.builder().addMethodModifier(runtime.methodModifierProtected()).setAccess(runtime.accessProtected());
            } else {
                newMethod.builder().addMethodModifier(runtime.methodModifierPublic()).setAccess(runtime.accessPublic());
            }
        }
        newMethod.builder().commitParameters().commit();
        return newMethod;
    }


    private static final Set<String> keywords = Set.of("boolean", "byte", "char", "class", "int", "double", "float", "long", "short");

    private static String validJavaName(String name) {
        if (keywords.contains(name)) return name + "0";
        if (name.contains("-")) return null;
        return name;
    }

    private record TypeTm(TypeInfo typeInfo, TranslationMap tm) {
    }

    private TypeTm createType(TypeInfo parent, TypeInfo typeToCopy, TranslationMap parentTm) {
        String typeName = typeToCopy.simpleName();
        boolean topLevel = parentTm == null;
        TypeInfo typeInfo = runtime.newTypeInfo(parent, topLevel ? typeName + "$" : typeName);
        typeInfo.builder().setParentClass(runtime.objectParameterizedType())
                .setTypeNature(runtime.typeNatureClass())
                .setAccess(runtime.accessPackage())
                .setSingleAbstractMethod(null);
        // 2-stage type parameter copying: we must handle self-references properly
        TranslationMap.Builder tmb = parentTm == null ? runtime.newTranslationMapBuilder()
                : runtime.newTranslationMapBuilder(parentTm);
        List<TypeParameter> newTypeParameters = new ArrayList<>();
        for (TypeParameter tp : typeToCopy.typeParameters()) {
            TypeParameter newTp = runtime.newTypeParameter(tp.getIndex(), tp.simpleName(), typeInfo);
            typeInfo.builder()
                    .addComments(tp.comments())
                    .addAnnotations(tp.annotations())
                    .setSource(tp.source()).addOrSetTypeParameter(newTp);
            tmb.put(tp, newTp);
            newTypeParameters.add(newTp);
            translateFromDollarToReal.put(newTp, tp);
        }
        TranslationMap tm = tmb.build();
        int i = 0;
        for (TypeParameter tp : typeToCopy.typeParameters()) {
            TypeParameter newTp = newTypeParameters.get(i++);
            newTp.builder().setTypeBounds(tp.typeBounds().stream().map(tm::translateType).toList()).commit();
        }
        return new TypeTm(typeInfo, tm);
    }

    public Map<Element, Element> translateFromDollarToReal() {
        return translateFromDollarToReal;
    }

    private TypeInfo newPackageType(SourceSet sourceSet, String packageName) {
        String camelCasePackageName = convertToCamelCase(packageName);
        CompilationUnit compilationUnit = runtime.newCompilationUnitBuilder()
                .setPackageName(destinationPackage.apply(sourceSet)).build();
        TypeInfo typeInfo = runtime.newTypeInfo(compilationUnit, camelCasePackageName);
        TypeInfo.Builder builder = typeInfo.builder();
        builder.setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .addTypeModifier(runtime.typeModifierPublic())
                .setAccess(runtime.accessPublic())
                .setSingleAbstractMethod(null);
        FieldInfo packageField = runtime.newFieldInfo("PACKAGE_NAME", true,
                runtime.stringParameterizedType(), typeInfo);
        packageField.builder()
                .addFieldModifier(runtime.fieldModifierFinal())
                .addFieldModifier(runtime.fieldModifierStatic())
                .addFieldModifier(runtime.fieldModifierPublic())
                .setAccess(runtime.accessPublic())
                .setInitializer(runtime.newStringConstant(packageName))
                .commit();
        builder.addField(packageField);
        return typeInfo;
    }

    static String convertToCamelCase(String packageName) {
        String[] components = packageName.split("\\.");
        return Arrays.stream(components).map(StringUtil::capitalize).collect(Collectors.joining());
    }

    public void write(Collection<TypeInfo> apiTypes,
                      String writeAnnotatedAPIsDir,
                      Qualification.Decorator decorator) throws IOException {
        File base = new File(writeAnnotatedAPIsDir);
        if (base.mkdirs()) {
            LOGGER.info("Created annotated API destination folder '{}'", base.getAbsolutePath());
        }
        write(apiTypes, base, decorator);
    }

    public void write(Collection<TypeInfo> apiTypes,
                      File base,
                      Qualification.Decorator decorator) throws IOException {
        int count = 0;
        for (TypeInfo apiType : apiTypes) {
            assert apiType.isPrimaryType() && apiType.hasBeenInspected();
            if (apiType.packageName() == null) {
                LOGGER.error("Empty package for {}", apiType);
            } else {
                String convertedPackage = apiType.packageName().replace(".", "/");
                File directory = new File(base, convertedPackage);
                if (directory.mkdirs()) {
                    LOGGER.info("Created annotated API destination package folder '{}'", directory.getAbsolutePath());
                }
                File outputFile = new File(directory, apiType.simpleName() + ".java");
                try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outputFile),
                        StandardCharsets.UTF_8)) {
                    outputStreamWriter.write(javaInspector.print2(apiType, decorator, importComputer));
                }
                LOGGER.info("Wrote {}", apiType);
                ++count;
            }
        }
        LOGGER.info("Wrote {} files", count);
    }
}
