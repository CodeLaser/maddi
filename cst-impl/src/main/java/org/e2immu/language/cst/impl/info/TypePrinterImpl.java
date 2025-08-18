package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.type.DiamondEnum;
import org.e2immu.language.cst.impl.variable.ThisImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public record TypePrinterImpl(TypeInfo typeInfo, boolean formatter2) implements TypePrinter {

    @Override
    public OutputBuilder print(ImportComputer importComputer, Qualification qualification, boolean doTypeDeclaration) {
        Set<String> imports;
        Qualification insideType;
        if (typeInfo.isPrimaryType() && typeInfo.hasBeenInspected()) {
            ImportComputer.Result res = importComputer.go(typeInfo, qualification);
            imports = res.imports();
            insideType = res.qualification();
        } else {
            imports = Set.of();
            insideType = typeInfo.primaryType().hasBeenInspected() && qualification instanceof QualificationImpl
                    ? new QualificationImpl(false, qualification, qualification.typeNameRequired())
                    : qualification;
        }
        assert insideType != null;

        // add the methods that we can call without having to qualify (method() instead of super.method())
        if (insideType instanceof QualificationImpl qi) {
            typeInfo.fields().forEach(qi::addField);
            addMethodsToQualification(typeInfo, qi);
            addThisToQualification(typeInfo, qi);
        }

        // PACKAGE AND IMPORTS

        OutputBuilder packageAndImports = new OutputBuilderImpl();
        boolean isRecord = typeInfo.typeNature().isRecord();
        OutputBuilder afterAnnotations = new OutputBuilderImpl();
        if (doTypeDeclaration) {
            if (typeInfo.isPrimaryType()) {
                String packageName = typeInfo.packageName();
                if (!packageName.isEmpty()) {
                    packageAndImports.add(KeywordImpl.PACKAGE).add(SpaceEnum.ONE).add(new TextImpl(packageName))
                            .add(SymbolEnum.SEMICOLON)
                            .add(SpaceEnum.NEWLINE);
                }
                imports.stream().sorted().forEach(i -> packageAndImports.add(KeywordImpl.IMPORT).add(SpaceEnum.ONE)
                        .add(new TextImpl(i)).add(SymbolEnum.SEMICOLON).add(SpaceEnum.NEWLINE));
            }

            // special case: the package-info.java file
            if (typeInfo.typeNature().isPackageInfo()) {
                OutputBuilder annotationStream = typeInfo.annotations().stream()
                        .map(ae -> ae.print(qualification))
                        .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE));
                return annotationStream.add(SpaceEnum.NEWLINE).add(packageAndImports);
            }

            // the modifiers
            OutputBuilder minimalModifiers = minimalModifiers(typeInfo).stream()
                    .map(mod -> new OutputBuilderImpl().add(mod.keyword()))
                    .collect(OutputBuilderImpl.joining(SpaceEnum.ONE));
            afterAnnotations.add(minimalModifiers);
            if (!minimalModifiers.isEmpty()) afterAnnotations.add(SpaceEnum.ONE);

            // the class nature and name
            afterAnnotations
                    .add(typeInfo.typeNature().keyword())
                    .add(SpaceEnum.ONE)
                    .add(new TextImpl(typeInfo.simpleName()));

            if (!typeInfo.typeParameters().isEmpty()) {
                afterAnnotations.add(SymbolEnum.LEFT_ANGLE_BRACKET);
                afterAnnotations.add(typeInfo.typeParameters().stream().map(tp ->
                                tp.print(insideType, true))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
                afterAnnotations.add(SymbolEnum.RIGHT_ANGLE_BRACKET);
            }
            if (isRecord) {
                afterAnnotations.add(outputNonStaticFieldsAsParameters(insideType, typeInfo.fields()));
            }
            if (typeInfo.parentClass() != null && !typeInfo.parentClass().isJavaLangObject()
                && (!typeInfo.typeNature().isEnum()
                    || !"java.lang.Enum".equals(typeInfo.parentClass().typeInfo().fullyQualifiedName()))) {
                afterAnnotations.add(SpaceEnum.ONE).add(KeywordImpl.EXTENDS).add(SpaceEnum.ONE)
                        .add(typeInfo.parentClass().print(insideType, false, DiamondEnum.SHOW_ALL));
            }
            if (!typeInfo.interfacesImplemented().isEmpty()) {
                afterAnnotations.add(SpaceEnum.ONE)
                        .add(typeInfo.isInteger() ? KeywordImpl.EXTENDS : KeywordImpl.IMPLEMENTS)
                        .add(SpaceEnum.ONE);
                afterAnnotations.add(typeInfo.interfacesImplemented().stream()
                        .map(pi -> pi.print(insideType, false, DiamondEnum.SHOW_ALL))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
            }
        }

        /*
        we allow for a different type when translating types, see TypeInfoImpl.translate()... where
        method ownership is not changed correctly.
         */
        OutputBuilder main = Stream.concat(Stream.concat(Stream.concat(Stream.concat(
                                                enumConstantStream(typeInfo, insideType),
                                                typeInfo.fields().stream()
                                                        .filter(f -> !f.isSynthetic() && (!isRecord || f.isStatic()))
                                                        .map(f -> new FieldPrinterImpl(f, formatter2)
                                                                .print(insideType, false))),
                                        typeInfo.subTypes().stream()
                                                .filter(st -> !st.isSynthetic())
                                                .map(ti -> new TypePrinterImpl(ti, formatter2)
                                                        .print(importComputer, insideType, true))),
                                typeInfo.constructors().stream()
                                        .filter(c -> !c.isSynthetic())
                                        .map(c -> new MethodPrinterImpl(typeInfo, c, formatter2).print(insideType))),
                        typeInfo.methods().stream()
                                .filter(m -> !m.isSynthetic())
                                .map(m -> new MethodPrinterImpl(typeInfo, m, formatter2).print(insideType)))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NONE, SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE,
                        GuideImpl.generatorForBlock()));
        afterAnnotations.add(main);

        // annotations and the rest of the type are at the same level
        Stream<AnnotationExpression> allAnnots = Stream.concat(typeInfo.annotations().stream(),
                qualification.decorator() == null ? Stream.of() : qualification.decorator().annotations(typeInfo).stream());
        Stream<OutputBuilder> annotationStream = doTypeDeclaration
                ? allAnnots.map(ae -> ae.print(qualification))
                : Stream.of();
        if (typeInfo().comments() != null) {
            typeInfo.comments().forEach(c -> packageAndImports.add(c.print(qualification)));
        }
        if (qualification.decorator() != null) {
            qualification.decorator().comments(typeInfo).forEach(c -> packageAndImports.add(c.print(qualification)));
        }
        if (qualification.decorator() != null && typeInfo.isPrimaryType()) {
            qualification.decorator().importStatements().forEach(is -> packageAndImports.add(is.print(qualification)));
        }
        return packageAndImports.add(Stream.concat(annotationStream, Stream.of(afterAnnotations))
                .collect(OutputBuilderImpl.joining(SpaceEnum.ONE_REQUIRED_EASY_SPLIT,
                        GuideImpl.generatorForAnnotationList())));
    }

    @Override
    public List<TypeModifier> minimalModifiers(TypeInfo typeInfo) {
        Set<TypeModifier> modifiers = typeInfo.typeModifiers();
        List<TypeModifier> list = new ArrayList<>();

        // access
        Access access = typeInfo.access();
        assert access != null : "Access has not yet been computed on " + typeInfo.fullyQualifiedName();
        Access enclosedAccess = typeInfo.compilationUnitOrEnclosingType().isLeft()
                ? InspectionImpl.AccessEnum.PUBLIC
                : typeInfo.compilationUnitOrEnclosingType().getRight().access();
        if (!enclosedAccess.isPrivate() && !access.isPackage()) {
            list.add(typeModifier(access));
        } // else there really is no point anymore to show any access modifier, let's keep it brief

        // 'abstract', 'static'
        if (typeInfo.typeNature().isClass()) {
            if (modifiers.contains(TypeModifierEnum.ABSTRACT)) {
                list.add(TypeModifierEnum.ABSTRACT);
            }
            if (modifiers.contains(TypeModifierEnum.STATIC)) {
                list.add(TypeModifierEnum.STATIC);
            }
            if (modifiers.contains(TypeModifierEnum.FINAL)) {
                list.add(TypeModifierEnum.FINAL);
            }
            if (modifiers.contains(TypeModifierEnum.SEALED)) {
                list.add(TypeModifierEnum.SEALED);
            }
            if (modifiers.contains(TypeModifierEnum.NON_SEALED)) {
                list.add(TypeModifierEnum.NON_SEALED);
            }
        } // else: records, interfaces, annotations, primitives are always static, never abstract

        return list;
    }

    private static TypeModifier typeModifier(Access access) {
        if (access.isPublic()) return TypeModifierEnum.PUBLIC;
        if (access.isProtected()) return TypeModifierEnum.PROTECTED;
        if (access.isPrivate()) return TypeModifierEnum.PRIVATE;
        throw new UnsupportedOperationException();
    }

    private static OutputBuilder outputNonStaticFieldsAsParameters(Qualification qualification, List<FieldInfo> fields) {
        return fields.stream()
                .filter(fieldInfo -> !fieldInfo.isStatic())
                .map(fieldInfo -> fieldInfo.print(qualification, true))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS, SymbolEnum.RIGHT_PARENTHESIS,
                        GuideImpl.generatorForParameterDeclaration()));
    }

    private static void addThisToQualification(TypeInfo typeInfo, QualificationImpl insideType) {
        insideType.addThis(new ThisImpl(typeInfo.asSimpleParameterizedType()));
        ParameterizedType parentClass = typeInfo.parentClass();
        if (parentClass != null && !parentClass.isJavaLangObject()) {
            insideType.addThis(new ThisImpl(parentClass.typeInfo().asSimpleParameterizedType(), null, true));
        }
    }

    private static void addMethodsToQualification(TypeInfo typeInfo, QualificationImpl qImpl) {
        typeInfo.methods().forEach(qImpl::addMethodUnlessOverride);
        if (!typeInfo.isJavaLangObject()) {
            addMethodsToQualification(typeInfo.parentClass().typeInfo(), qImpl);
        }
        for (ParameterizedType interfaceType : typeInfo.interfacesImplemented()) {
            addMethodsToQualification(interfaceType.typeInfo(), qImpl);
        }
    }

    private static Stream<OutputBuilder> enumConstantStream(TypeInfo typeInfo, Qualification qualification) {
        if (typeInfo.typeNature().isEnum()) {
            GuideImpl.GuideGenerator gg = GuideImpl.generatorForEnumDefinitions();
            OutputBuilder outputBuilder = new OutputBuilderImpl().add(gg.start());
            boolean first = true;
            for (FieldInfo fieldInfo : typeInfo.fields()) {
                if (fieldInfo.isSynthetic()) {
                    if (first) {
                        first = false;
                    } else {
                        outputBuilder.add(SymbolEnum.COMMA).add(gg.mid());
                    }
                    outputBuilder.add(new TextImpl(fieldInfo.name()));
                    Expression initializer = fieldInfo.initializer();
                    if (initializer instanceof ConstructorCall constructorCall) {
                        if (!constructorCall.parameterExpressions().isEmpty()) {
                            GuideImpl.GuideGenerator args = GuideImpl.defaultGuideGenerator();
                            outputBuilder.add(SymbolEnum.LEFT_PARENTHESIS).add(args.start());
                            boolean firstParam = true;
                            for (Expression expression : constructorCall.parameterExpressions()) {
                                if (firstParam) {
                                    firstParam = false;
                                } else {
                                    outputBuilder.add(SymbolEnum.COMMA).add(args.mid());
                                }
                                outputBuilder.add(expression.print(qualification));
                            }
                            outputBuilder.add(args.end()).add(SymbolEnum.RIGHT_PARENTHESIS);
                        }
                    } else if (initializer != null && !initializer.isEmpty()) {
                        throw new UnsupportedOperationException("Expect initializer to be a ConstructorCall expression, but have "
                                                                + initializer.getClass() + " = " + initializer);
                    }
                }
            }
            outputBuilder.add(gg.end()).add(SymbolEnum.SEMICOLON);
            return Stream.of(outputBuilder);
        }
        return Stream.of();
    }
}
