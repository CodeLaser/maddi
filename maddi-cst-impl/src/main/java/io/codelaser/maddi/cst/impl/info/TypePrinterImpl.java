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

package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.*;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.*;
import io.codelaser.maddi.cst.impl.type.DiamondEnum;
import io.codelaser.maddi.cst.impl.variable.ThisImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record TypePrinterImpl(TypeInfo typeInfo, boolean formatter2) implements TypePrinter {

    @Override
    public OutputBuilder print(ImportComputer importComputer, Qualification qualification, boolean doTypeDeclaration) {
        CompilationUnitPrinterImpl printer = new CompilationUnitPrinterImpl(typeInfo.compilationUnit(), formatter2);
        CompilationUnitPrinter.ImportData importData = printer.computeImportData(importComputer, qualification);
        return print(importData, doTypeDeclaration);
    }


    private static void addThisToQualification(TypeInfo typeInfo, Qualification insideType) {
        insideType.addThis(new ThisImpl(typeInfo.asSimpleParameterizedType()));
        ParameterizedType parentClass = typeInfo.parentClass();
        if (parentClass != null && !parentClass.isJavaLangObject()) {
            insideType.addThis(new ThisImpl(parentClass.typeInfo().asSimpleParameterizedType(), null,
                    true));
        }
    }

    // the member types this type inherits, directly or through an enclosing type, are in scope by their simple name
    // in its body -- and only there, not in the other types of the file (see QualificationImpl.addInheritedIntoScope)
    private static void addInheritedMemberTypesToQualification(TypeInfo typeInfo, Qualification insideType) {
        if (insideType instanceof QualificationImpl qi) {
            qi.inheritedMemberTypes().stream()
                    .filter(memberType -> ImportComputerImpl.inScopeInBodyOf(typeInfo, memberType))
                    .forEach(qi::addInheritedIntoScope);
        }
    }

    private static void addMethodsToQualification(TypeInfo typeInfo, Qualification qualification) {
        typeInfo.methods().forEach(qualification::addMethodUnlessOverride);
        if (!typeInfo.isJavaLangObject()) {
            addMethodsToQualification(typeInfo.parentClass().typeInfo(), qualification);
        }
        for (ParameterizedType interfaceType : typeInfo.interfacesImplemented()) {
            if (interfaceType.typeInfo() != typeInfo) {
                addMethodsToQualification(interfaceType.typeInfo(), qualification);
            }
        }
    }

    @Override
    public OutputBuilder print(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration) {
        return print(importData, doTypeDeclaration, MethodPrinterImpl::new, FieldPrinterImpl::new, TypePrinterImpl::new);
    }

    @Override
    public OutputBuilder print(CompilationUnitPrinter.ImportData importData,
                               boolean doTypeDeclaration,
                               MethodPrinterFactory methodPrinterFactory,
                               FieldPrinterFactory fieldPrinterFactory,
                               EnclosedTypePrinterFactory enclosedTypePrinterFactory) {

        // add the methods that we can call without having to qualify (method() instead of super.method())
        Qualification insideType = new QualificationImpl(importData.insideType());

        typeInfo.fields().forEach(insideType::addField);
        addMethodsToQualification(typeInfo, insideType);
        addThisToQualification(typeInfo, insideType);
        addInheritedMemberTypesToQualification(typeInfo, insideType);

        boolean isRecord = typeInfo.typeNature().isRecord();
        OutputBuilder afterAnnotations = new OutputBuilderImpl();
        Qualification qualification = importData.qualification();

        if (doTypeDeclaration) {
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
                    || !"java.lang.Enum".equals(typeInfo.parentClass().typeInfo().fullyQualifiedName()))
                && (!typeInfo.typeNature().isRecord()
                    || !"java.lang.Record".equals(typeInfo.parentClass().typeInfo().fullyQualifiedName()))) {
                afterAnnotations.add(SpaceEnum.ONE).add(KeywordImpl.EXTENDS).add(SpaceEnum.ONE)
                        .add(typeInfo.parentClass().print(insideType, false, DiamondEnum.SHOW_ALL));
            }
            // an annotation type implicitly implements java.lang.annotation.Annotation; that interface is never
            // written out in source (and 'implements Annotation' on an '@interface' would not compile), so skip it
            List<ParameterizedType> interfaces = typeInfo.typeNature().isAnnotation()
                    ? typeInfo.interfacesImplemented().stream()
                    .filter(pt -> pt.typeInfo() == null
                                  || !"java.lang.annotation.Annotation".equals(pt.typeInfo().fullyQualifiedName()))
                    .toList()
                    : typeInfo.interfacesImplemented();
            if (!interfaces.isEmpty()) {
                afterAnnotations.add(SpaceEnum.ONE)
                        // an interface 'extends' its super-interfaces; a class 'implements' them
                        .add(typeInfo.isInterface() ? KeywordImpl.EXTENDS : KeywordImpl.IMPLEMENTS)
                        .add(SpaceEnum.ONE);
                afterAnnotations.add(interfaces.stream()
                        .map(pi -> pi.print(insideType, false, DiamondEnum.SHOW_ALL))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
            }
            // 'permits' is REQUIRED when the permitted subtypes live in other compilation units; javac infers
            // it only when they sit alongside. It was never printed at all, so a sealed type re-written by any
            // verb lost the clause and no longer compiled. Emitting it whenever the list is populated is always
            // legal Java -- an explicit permits that agrees with the subtypes is valid in the nested case too.
            List<TypeInfo> permitted = typeInfo.permittedWhenSealed();
            if (!permitted.isEmpty()) {
                afterAnnotations.add(SpaceEnum.ONE).add(KeywordImpl.PERMITS).add(SpaceEnum.ONE);
                afterAnnotations.add(permitted.stream()
                        .map(ti -> ti.asSimpleParameterizedType().print(insideType, false, DiamondEnum.SHOW_ALL))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
            }
        }

        /*
        we allow for a different type when translating types, see TypeInfoImpl.translate()... where
        method ownership is not changed correctly.
         */
        Stream<Member> fieldStream = typeInfo.fields().stream()
                .filter(f -> !f.isSynthetic() && (!isRecord || f.isStatic()))
                .map(f -> new Member(f, fieldPrinterFactory.create(f, formatter2)
                        .print(insideType, false)));
        Stream<Member> subTypeStream = typeInfo.subTypes().stream()
                .filter(st -> !st.isSynthetic())
                .map(ti -> new Member(ti, enclosedTypePrinterFactory.create(ti, formatter2)
                        .print(importData, true)));
        Stream<Member> constructorStream = typeInfo.constructors().stream()
                .filter(c -> !c.isSynthetic())
                .map(c -> new Member(c, methodPrinterFactory.create(typeInfo, c, formatter2).print(insideType)));
        Stream<Member> methodStream = typeInfo.methods().stream()
                .filter(m -> !m.isSynthetic())
                .map(m -> new Member(m, methodPrinterFactory.create(typeInfo, m, formatter2).print(insideType)));
        Stream<Member> trailingCommentStream = typeInfo.trailingComments().stream()
                .map(c -> new Member(null, c.print(qualification)));
        List<Member> declared = inSourceOrder(typeInfo.source(), Stream.concat(Stream.concat(Stream.concat(
                fieldStream, subTypeStream), constructorStream), methodStream).toList());
        List<Member> members = Stream.concat(Stream.concat(
                enumConstantStream(typeInfo, insideType).map(ob -> new Member(null, ob)), declared.stream()),
                trailingCommentStream).toList();
        // a blank line in the source between two members, for FormattingOptions.blankLinesFromSource (#49)
        OutputBuilder main = IntStream.range(0, members.size())
                .mapToObj(i -> i > 0 && BlankLineImpl.between(members.get(i - 1).element, members.get(i).element)
                        ? new OutputBuilderImpl().add(BlankLineImpl.INSTANCE).add(members.get(i).output)
                        : members.get(i).output)
                .collect(OutputBuilderImpl.joining(SpaceEnum.NONE, SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE,
                        GuideImpl.generatorForBlock()));
        afterAnnotations.add(main);

        // annotations and the rest of the type are at the same level
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        if (typeInfo.comments() != null) {
            typeInfo.comments().forEach(c -> outputBuilder.add(c.print(qualification)));
        }
        Stream<AnnotationExpression> allAnnots = Stream.concat(typeInfo.annotations().stream(),
                qualification.decorator() == null ? Stream.of() : qualification.decorator().annotations(typeInfo).stream());
        // with the import computer's qualification, as the members are: it knows the types it did not import, and a
        // class literal written qualified, '@Restricted(org.kohsuke.accmod.restrictions.NoExternalUse.class)', must
        // stay qualified (GitHub #169). Not insideType: the type's own members are not in scope in its annotations.
        Stream<OutputBuilder> annotationStream = doTypeDeclaration
                ? allAnnots.map(ae -> ae.print(importData.insideType()))
                : Stream.of();

        return outputBuilder.add(Stream.concat(annotationStream, Stream.of(afterAnnotations))
                .collect(OutputBuilderImpl.joining(SpaceEnum.ONE_REQUIRED_EASY_SPLIT,
                        GuideImpl.generatorForAnnotationList())));
    }

    // a member of the type body and its output; no element for the enum constants and the trailing comments
    private record Member(Element element, OutputBuilder output) {
        Source source() {
            Source source = element == null ? null : element.source();
            return source == null || source.beginLine() <= 0 ? null : source;
        }
    }

    private static final Comparator<Source> POSITION = Comparator.comparingInt(Source::beginLine)
            .thenComparingInt(Source::beginPos);

    /*
    The members as they were written (CodeLaser/maddi#137): fields, nested types, constructors and methods came out in
    that order, whatever the source order. The members with a source take the source order among the places they hold
    in the kind order; a member without one (added by a tool) keeps its place. Only when every source lies within the
    type's and none overlap: a member moved in from another file has positions in that file, and then the kind order
    is kept.
     */
    private static List<Member> inSourceOrder(Source typeSource, List<Member> members) {
        if (typeSource == null || typeSource.beginLine() <= 0) return members;
        List<Member> sourced = members.stream().filter(m -> m.source() != null)
                .sorted(Comparator.comparing(Member::source, POSITION)).toList();
        if (sourced.size() < 2) return members;
        for (int i = 0; i < sourced.size(); i++) {
            Source source = sourced.get(i).source();
            if (POSITION.compare(source, typeSource) < 0 || source.endLine() > typeSource.endLine()) return members;
            // two fields of one declaration ('int a, b;') begin together
            if (i > 0 && POSITION.compare(sourced.get(i - 1).source(), source) != 0) {
                Source previous = sourced.get(i - 1).source();
                if (previous.endLine() > source.beginLine()
                    || previous.endLine() == source.beginLine() && previous.endPos() >= source.beginPos()) {
                    return members;
                }
            }
        }
        List<Member> result = new ArrayList<>(members.size());
        int next = 0;
        for (Member member : members) {
            result.add(member.source() == null ? member : sourced.get(next++));
        }
        return result;
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
        if (!enclosedAccess.isPrivate() && !access.isPackage() && !enclosingIsInterface(typeInfo)
            && typeInfo.enclosingMethod() == null) {
            list.add(typeModifier(access));
        } // else there really is no point anymore to show any access modifier, let's keep it brief
        // (for a local class — enclosingMethod() != null — an access modifier is illegal Java)

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
        } // else: records, interfaces, annotations, primitives are always static, never abstract

        // ⚠ 'sealed'/'non-sealed' ARE NOT CONFINED TO CLASSES, and used to sit inside the isClass() block
        // above, so 'sealed interface Vehicle' printed as plain 'interface Vehicle' -- silently unsealing
        // every sealed interface any verb re-printed, while 'non-sealed class Truck' (a class) came out
        // fine. The comment above is true of 'abstract' and 'static'; it is not true of these two.
        // Records and enums are implicitly final and cannot carry either, so the guard is class-or-interface.
        if (typeInfo.typeNature().isClass() || typeInfo.typeNature().isInterface()) {
            if (modifiers.contains(TypeModifierEnum.SEALED)) {
                list.add(TypeModifierEnum.SEALED);
            }
            if (modifiers.contains(TypeModifierEnum.NON_SEALED)) {
                list.add(TypeModifierEnum.NON_SEALED);
            }
        }

        return list;
    }

    private static boolean enclosingIsInterface(TypeInfo typeInfo) {
        return typeInfo.compilationUnitOrEnclosingType().isRight()
               && typeInfo.compilationUnitOrEnclosingType().getRight().isInterface();
    }

    private static TypeModifier typeModifier(Access access) {
        if (access.isPublic()) return TypeModifierEnum.PUBLIC;
        if (access.isProtected()) return TypeModifierEnum.PROTECTED;
        if (access.isPrivate()) return TypeModifierEnum.PRIVATE;
        if (access.isInternal()) return TypeModifierEnum.INTERNAL;
        throw new UnsupportedOperationException();
    }

    private static OutputBuilder outputNonStaticFieldsAsParameters(Qualification qualification, List<FieldInfo> fields) {
        return fields.stream()
                .filter(fieldInfo -> !fieldInfo.isStatic())
                .map(fieldInfo -> fieldInfo.print(qualification, true))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS, SymbolEnum.RIGHT_PARENTHESIS,
                        GuideImpl.generatorForParameterDeclaration()));
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
