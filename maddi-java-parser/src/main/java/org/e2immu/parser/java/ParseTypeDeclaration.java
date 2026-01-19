/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Lombok;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.parser.TypeContext;
import org.e2immu.language.inspection.api.util.CreateSyntheticFieldsForGetSet;
import org.e2immu.support.Either;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.e2immu.language.inspection.api.parser.TypeContext.*;

public class ParseTypeDeclaration extends CommonParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseTypeDeclaration.class);
    private final CreateSyntheticFieldsForGetSet createSyntheticFieldsForGetSet;

    public ParseTypeDeclaration(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
        this.createSyntheticFieldsForGetSet = new CreateSyntheticFieldsForGetSet(runtime);
    }

    public Either<TypeInfo, DelayedParsingInformation> parse(Context context,
                                                             Either<CompilationUnit, TypeInfo> packageNameOrEnclosing,
                                                             TypeDeclaration td,
                                                             boolean mustDelayForStaticStarImport) {
        try {
            return internalParse(context, packageNameOrEnclosing,
                    simpleName -> {
                        // during the "scan" phase, we have already created all the TypeInfo objects
                        String fqn = fullyQualifiedName(packageNameOrEnclosing, simpleName);
                        List<? extends NamedType> nts = context.typeContext().getWithQualification(fqn, true);
                        return (TypeInfo) nts.getLast();
                    }, td, mustDelayForStaticStarImport);
        } catch (Summary.FailFastException ffe) {
            throw ffe;
        } catch (RuntimeException re) {
            Summary.ParseException parseException;
            if (packageNameOrEnclosing.isLeft()) {
                LOGGER.error("Caught exception parsing type in {}", packageNameOrEnclosing.getLeft());
                parseException = new Summary.ParseException(packageNameOrEnclosing.getLeft(),
                        packageNameOrEnclosing.getLeft(), re.getMessage(), re);
            } else {
                LOGGER.error("Caught exception parsing type in {}", packageNameOrEnclosing.getRight());
                parseException = new Summary.ParseException(packageNameOrEnclosing.getRight().compilationUnit(),
                        packageNameOrEnclosing.getRight(), re.getMessage(), re);
            }
            context.summary().addParseException(parseException);
            return null;
        }
    }

    public TypeInfo parseLocal(Context context, MethodInfo enclosingMethod, TypeDeclaration classDeclaration) {
        try {
            return internalParse(context, Either.right(enclosingMethod.typeInfo()),
                    simpleName -> {
                        int typeIndex = enclosingMethod.typeInfo().builder().getAndIncrementAnonymousTypes();
                        TypeInfo typeInfo = runtime.newTypeInfo(enclosingMethod, simpleName, typeIndex);
                        handleTypeModifiers(classDeclaration, typeInfo, context.isDetailedSources());
                        return typeInfo;
                    },
                    classDeclaration, false).getLeft();
        } catch (RuntimeException | AssertionError e) {
            LOGGER.error("Caught exception", e);
            throw e;
        }
    }

    public record RecordField(FieldInfo fieldInfo, boolean varargs) {
    }

    public record DelayedParsingInformation(int iHierarchy,
                                            TypeInfo typeInfo,
                                            TypeInfo.Builder builder,
                                            TypeDeclaration td,
                                            Context context,
                                            TypeNature typeNature,
                                            Context newContext,
                                            DetailedSources.Builder detailedSourcesBuilder,
                                            int iStart,
                                            List<Annotation> annotations,
                                            List<RecordComponent> recordComponents) {

    }

    private Either<TypeInfo, DelayedParsingInformation> internalParse(Context context,
                                                                      Either<CompilationUnit, TypeInfo> packageNameOrEnclosing,
                                                                      Function<String, TypeInfo> bySimpleName,
                                                                      TypeDeclaration td,
                                                                      boolean mustDelayForStaticStarImport) {
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();

        int i = 0;
        // in the annotations, we can refer to our own static fields, so we wait a little to parse them
        // See TestAnnotations,3
        List<Annotation> annotations = new ArrayList<>();
        while (true) {
            Node tdi = td.get(i);
            if (tdi instanceof Annotation a) {
                annotations.add(a);
            } else if (tdi instanceof Modifiers modifiers) {
                for (Node node : modifiers.children()) {
                    if (node instanceof Annotation a) {
                        annotations.add(a);
                    }
                }
            } else if (tdi instanceof Delimiter) {
                if (!Token.TokenType.AT.equals(tdi.getType())) {
                    throw new Summary.ParseException(context, "Expect @ delimiter");
                }
            } else if (!(tdi instanceof KeyWord) && !(tdi instanceof Token token && token.getType().equals(Token.TokenType.RECORD))) {
                break;
            }
            i++;
        }
        String simpleName;
        Identifier identifier = (Identifier) td.get(i);
        simpleName = identifier.getSource();
        i++;

        TypeInfo typeInfo = bySimpleName.apply(simpleName);
        assert typeInfo != null;

        if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(typeInfo.simpleName(), source(identifier));

        TypeInfo.Builder builder = typeInfo.builder();
        List<Comment> comments = comments(td, context, typeInfo, builder);
        builder.addComments(comments);
        builder.computeAccess();
        builder.setEnclosingMethod(context.enclosingMethod());

        Context newContext = context.newSubType(typeInfo);
        newContext.typeContext().addToContext(typeInfo, CURRENT_TYPE_PRIORITY);
        collectNamesOfSubTypesIntoTypeContext(newContext.typeContext(), typeInfo);

        List<Node> typeParametersToParse = new ArrayList<>();

        if (td.get(i) instanceof TypeParameters typeParameters) {
            int j = 1;
            while (j < typeParameters.size()) {
                typeParametersToParse.add(typeParameters.get(j));
                j += 2; // skip the ',' or '>' delimiter
            }
            i++;
        }
        typeInfo.typeParameters().forEach(tp ->
                newContext.typeContext().addToContext(tp, TYPE_PARAMETER_PRIORITY));
        assert typeInfo.typeParameters().size() == typeParametersToParse.size();
        if (!typeParametersToParse.isEmpty()) {
            parseAndResolveTypeParameterBounds(typeParametersToParse, typeInfo.typeParameters(), newContext);
        }

        TypeNature typeNature = builder.typeNature();
        assert typeNature != null;
        List<RecordComponent> recordComponents;
        if (td.get(i) instanceof RecordHeader rh) {
            assert typeNature.isRecord();
            recordComponents = new ArrayList<>();
            for (int j = 1; j < rh.size(); j += 2) {
                Node rhj = rh.get(j);
                if (rhj instanceof Delimiter) break; // empty parameter list
                if (rhj instanceof RecordComponent rc) {
                    recordComponents.add(rc);
                } else {
                    throw new Summary.ParseException(newContext, "Expected record component");
                }
            }
            if (detailedSourcesBuilder != null) {
                detailedSourcesBuilder.put(DetailedSources.END_OF_PARAMETER_LIST, source(rh.getLast()));
            }
            i++;
        } else {
            recordComponents = null;
        }

        Hierarchy hierarchy = doHierarchy(i, builder, td, newContext, typeNature, detailedSourcesBuilder);

        if (typeNature.isAnnotation()) {
            TypeInfo annotationTypeInfo = runtime.getFullyQualified("java.lang.annotation.Annotation", true);
            builder.addInterfaceImplemented(annotationTypeInfo.asParameterizedType());
        }
        if (hierarchy.done) {
            builder.hierarchyIsDone();
        }
        // IMPORTANT: delaying is only done at the top-level; not for subtypes. See inspection-integration/
        // do not change the order in the OR disjunction; we must add the subtypes!
        if (hierarchy.done && !mustDelayForStaticStarImport
            && (newContext.typeContext().addSubTypesOfHierarchyReturnAllDefined(typeInfo, SUBTYPE_HIERARCHY_PRIORITY)
                && hierarchyOfImportsAllDefined(typeInfo.compilationUnit(), context.typeContext())
                || packageNameOrEnclosing.isRight())) {
            return Either.left(continueParsingTypeDeclaration(typeInfo, builder, td, context, typeNature, newContext,
                    detailedSourcesBuilder, hierarchy.i, annotations, recordComponents));
        }
        return Either.right(new DelayedParsingInformation(hierarchy.hierarchyStart, typeInfo, builder, td, context, typeNature,
                newContext, detailedSourcesBuilder, hierarchy.i, annotations, recordComponents));
    }

    private record Hierarchy(int hierarchyStart, int i, boolean done) {
    }

    private Hierarchy doHierarchy(int i, TypeInfo.Builder builder, TypeDeclaration td, Context newContext,
                                  TypeNature typeNature, DetailedSources.Builder detailedSourcesBuilder) {
        boolean hierarchyDone = true;
        int hierarchyStart = i;
        builder.setParentClass(runtime.objectParameterizedType());
        if (td.get(i) instanceof ExtendsList extendsList) {
            if (detailedSourcesBuilder != null) {
                detailedSourcesBuilder.put(DetailedSources.EXTENDS, source(extendsList.getFirst()));
            }
            List<Node> commas = detailedSourcesBuilder == null ? null : new ArrayList<>();
            for (int j = 1; j < extendsList.size(); j += 2) {
                if (detailedSourcesBuilder != null && j + 1 < extendsList.size()
                    && extendsList.get(j + 1) instanceof Delimiter d && d.getType() == Token.TokenType.COMMA) {
                    commas.add(d);
                }
                ParameterizedType pt = parsers.parseType().parse(newContext, extendsList.get(j),
                        false, null, detailedSourcesBuilder);
                if (pt == null) {
                    hierarchyDone = false;
                } else if (typeNature.isInterface()) {
                    builder.addInterfaceImplemented(pt);
                } else {
                    builder.setParentClass(pt);
                }
            }
            i++;
            if (detailedSourcesBuilder != null) addCommaList(commas, detailedSourcesBuilder,
                    DetailedSources.EXTENDS_COMMAS);
        }

        if (td.get(i) instanceof ImplementsList implementsList) {
            if (detailedSourcesBuilder != null) {
                detailedSourcesBuilder.put(DetailedSources.IMPLEMENTS, source(implementsList.getFirst()));
            }
            List<Node> commas = detailedSourcesBuilder == null ? null : new ArrayList<>();

            for (int j = 1; j < implementsList.size(); j += 2) {
                if (detailedSourcesBuilder != null && j + 1 < implementsList.size()
                    && implementsList.get(j + 1) instanceof Delimiter d && d.getType() == Token.TokenType.COMMA) {
                    commas.add(d);
                }
                ParameterizedType pt = parsers.parseType().parse(newContext, implementsList.get(j),
                        false, null, detailedSourcesBuilder);
                if (pt == null) {
                    hierarchyDone = false;
                } else {
                    builder.addInterfaceImplemented(pt);
                }
            }
            i++;
            if (detailedSourcesBuilder != null) addCommaList(commas, detailedSourcesBuilder,
                    DetailedSources.IMPLEMENTS_COMMAS);
        }
        return new Hierarchy(hierarchyStart, i, hierarchyDone);
    }

    private boolean hierarchyOfImportsAllDefined(CompilationUnit compilationUnit, TypeContext typeContext) {
        for (ImportStatement is : compilationUnit.importStatements()) {
            if (!is.isStatic() && !is.isStar()) {
                TypeInfo typeInfo = (TypeInfo) typeContext.getWithQualification(is.importString(), true).getLast();
                if (!typeInfo.compilationUnit().equals(compilationUnit) && hierarchyNotYetDone(typeInfo, new HashSet<>())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hierarchyNotYetDone(TypeInfo typeInfo, Set<TypeInfo> visited) {
        if (visited.add(typeInfo)) {
            if (typeInfo.hierarchyNotYetDone()) return true;
            if (typeInfo.parentClass() != null) {
                if (hierarchyNotYetDone(typeInfo.parentClass().typeInfo(), visited)) return true;
            }
            for (ParameterizedType interfaceImplemented : typeInfo.interfacesImplemented()) {
                if (hierarchyNotYetDone(interfaceImplemented.typeInfo(), visited)) return true;
            }
        }
        return false;
    }

    public Either<TypeInfo, DelayedParsingInformation> continueParsingTypeDeclaration(DelayedParsingInformation d) {
        // try again...
        boolean hierarchyDone;
        if (d.builder.hierarchyNotYetDone()) {
            d.builder.clearInterfacesImplemented();
            Hierarchy hierarchy = doHierarchy(d.iHierarchy, d.builder, d.td, d.newContext, d.typeNature, d.detailedSourcesBuilder);
            hierarchyDone = hierarchy.done;
            if (hierarchyDone) d.builder.hierarchyIsDone();
        } else {
            hierarchyDone = true;
        }
        if (hierarchyDone
            && d.newContext.typeContext().addSubTypesOfHierarchyReturnAllDefined(d.typeInfo, SUBTYPE_HIERARCHY_PRIORITY)
            && d.typeInfo.compilationUnit().importStatements().stream()
                    .allMatch(is -> !is.isStatic()
                                    || d.context.typeContext().addToStaticImportMap(d.typeInfo.compilationUnit(), is))) {
            return Either.left(continueParsingTypeDeclaration(d.typeInfo, d.builder, d.td, d.context, d.typeNature,
                    d.newContext, d.detailedSourcesBuilder, d.iStart, d.annotations, d.recordComponents));
        }
        return Either.right(d);
    }

    public TypeInfo continueParsingTypeDeclaration(TypeInfo typeInfo, TypeInfo.Builder builder, TypeDeclaration td,
                                                   Context context,
                                                   TypeNature typeNature,
                                                   Context newContext,
                                                   DetailedSources.Builder detailedSourcesBuilder,
                                                   int iStart,
                                                   List<Annotation> annotations,
                                                   List<RecordComponent> recordComponents) {
        parseAnnotations(context, builder, annotations);
        Lombok.Data lombokData = context.isLombok() ? context.lombok().handleType(typeInfo) : null;

        List<RecordField> recordFields;
        if (recordComponents != null) {
            recordFields = new ArrayList<>(recordComponents.size());
            for (RecordComponent rc : recordComponents) {
                recordFields.add(parseRecordField(newContext, typeInfo, rc));
            }
        } else {
            recordFields = null;
        }
        int i = iStart;
        List<Node> permitsCommas = detailedSourcesBuilder == null ? null : new ArrayList<>();
        if (td.get(i) instanceof PermitsList permitsList) {
            if (detailedSourcesBuilder != null) {
                detailedSourcesBuilder.put(DetailedSources.PERMITS, source(permitsList.getFirst()));
            }
            for (int j = 1; j < permitsList.size(); j += 2) {
                if (detailedSourcesBuilder != null && j + 1 < permitsList.size()
                    && permitsList.get(j + 1) instanceof Delimiter d && d.getType() == Token.TokenType.COMMA) {
                    permitsCommas.add(d);
                }
                ParameterizedType pt = parsers.parseType().parse(newContext, permitsList.get(j), detailedSourcesBuilder);
                builder.addPermittedType(pt.typeInfo());
            }
            i++;
        }
        if (detailedSourcesBuilder != null) {
            addCommaList(permitsCommas, detailedSourcesBuilder, DetailedSources.PERMITS_COMMAS);
        }

        Node body = td.get(i);
        if (body instanceof ClassOrInterfaceBody || body instanceof RecordBody || body instanceof EnumBody) {
            Context contextForBody = newContext.newTypeBody();
            if (body instanceof RecordBody) {
                assert recordFields != null;
                for (RecordField rf : recordFields) {
                    typeInfo.builder().addField(rf.fieldInfo);
                    FieldReference fr = runtime.newFieldReference(rf.fieldInfo);
                    contextForBody.variableContext().add(fr);
                }
            } else if (body instanceof EnumBody) {
                List<FieldInfo> enumFields = new ArrayList<>();
                ParameterizedType type = typeInfo.asSimpleParameterizedType();
                for (Node child : body.children()) {
                    if (child instanceof EnumConstant ec) {
                        List<Annotation> enumAnnotations = new ArrayList<>();
                        int j = 0;
                        while (ec.get(j) instanceof Annotation) {
                            enumAnnotations.add((Annotation) ec.get(j));
                            ++j;
                        }
                        Node nameNode = ec.get(j);
                        String name = nameNode.getSource();
                        DetailedSources.Builder dsbuilder = context.newDetailedSourcesBuilder();
                        Source nameSource = source(nameNode);
                        if (dsbuilder != null) dsbuilder.put(name, nameSource);
                        FieldInfo fieldInfo = runtime.newFieldInfo(name, true, type, typeInfo);
                        Source source = source(ec);
                        fieldInfo.builder()
                                .setSynthetic(true) // to distinguish them from normal, non-enum fields
                                .setInitializer(runtime.newEmptyExpression())
                                .addFieldModifier(runtime.fieldModifierFinal())
                                .addFieldModifier(runtime.fieldModifierPublic())
                                .addFieldModifier(runtime.fieldModifierStatic())
                                .setSource(dsbuilder == null ? source : source.withDetailedSources(dsbuilder.build()))
                                .computeAccess();
                        parseAnnotations(context, fieldInfo.builder(), enumAnnotations);
                        // register evaluation of parameters as an object creation for the field
                        builder.addField(fieldInfo);
                        enumFields.add(fieldInfo);
                        contextForBody.variableContext().add(runtime.newFieldReference(fieldInfo));
                        if (ec.size() > j + 1 && ec.get(j + 1) instanceof InvocationArguments ia) {
                            // FIXME pass on detailed sources
                            newContext.resolver().add(fieldInfo, fieldInfo.builder(),
                                    newContext.newForwardType(typeInfo.asSimpleParameterizedType()),
                                    null, ia, newContext, null);
                        } else {
                            fieldInfo.builder().commit();
                        }
                    }
                }
                TypeInfo enumTypeInfo = runtime.getFullyQualified("java.lang.Enum", true);
                builder.setParentClass(runtime.newParameterizedType(enumTypeInfo, List.of(typeInfo.asSimpleParameterizedType())));
                new EnumSynthetics(runtime, typeInfo, builder).create(newContext, enumFields);
            }
            parseBody(contextForBody, body, typeNature, typeInfo, builder, recordFields, lombokData);
        } else if (body instanceof AnnotationTypeBody) {
            for (Node child : body.children()) {
                if (child instanceof TypeDeclaration subTd) {
                    TypeInfo subTypeInfo = parse(newContext, Either.right(typeInfo), subTd, false).getLeft();
                    newContext.typeContext().addToContext(subTypeInfo, SUBTYPE_PRIORITY);
                }
            }
            for (Node child : body.children()) {
                if (child instanceof AnnotationMethodDeclaration amd) {
                    MethodInfo methodInfo = parsers.parseAnnotationMethodDeclaration().parse(newContext, amd);
                    builder.addMethod(methodInfo);
                } else if (child instanceof FieldDeclaration fieldDeclaration) {
                    parsers.parseFieldDeclaration().parse(newContext, fieldDeclaration, lombokData).forEach(builder::addField);
                }
            }
        } else throw new UnsupportedOperationException("node " + td.get(i).getClass());

        // we can only do this when we know all the fields...
        if (lombokData != null) {
            context.lombok().addConstructors(typeInfo, lombokData);
        }
        newContext.resolver().add(builder);

        /*
        Ensure a constructor when the type is a record and there are no compact constructors.
        (those without arguments, as in 'public record SomeRecord(...) { public Record { this.field = } ... }' )
        and also no default constructor override.
        The latter condition is verified in the builder.ensureConstructor() method.
         */
        Source source1 = builder.source();
        assert source1 != null;
        assert detailedSourcesBuilder == null || source1.detailedSources() != null;

        Source source = detailedSourcesBuilder == null ? source1
                : source1.mergeDetailedSources(detailedSourcesBuilder.build());
        builder.setSource(source);

        if (typeNature.isRecord()) {
            RecordSynthetics rs = new RecordSynthetics(runtime, typeInfo);
            assert recordFields != null;
            if (!haveConstructorMatchingFields(builder, recordFields)) {
                MethodInfo cc = rs.createSyntheticConstructor(source, recordFields);
                builder.addConstructor(cc);
            }
            // finally, add synthetic methods if needed
            rs.createAccessors(recordFields).forEach(accessor -> {
                builder.addMethod(accessor);
                context.resolver().addRecordAccessor(accessor);
            });
        }

        if (typeNature.isInterface() || typeNature.isClass() && builder.isAbstract()) {
            createSyntheticFieldsForGetSet.createSyntheticFields(typeInfo);
        }

        return typeInfo;
    }

    private boolean haveConstructorMatchingFields(TypeInfo.Builder builder, List<RecordField> recordFields) {
        return builder.constructors().stream().anyMatch(mi -> {
            if (mi.parameters().size() != recordFields.size()) return false;
            int i = 0;
            for (RecordField recordField : recordFields) {
                ParameterInfo pi = mi.parameters().get(i);
                if (!pi.parameterizedType().equals(recordField.fieldInfo.type())) return false;
                i++;
            }
            return true;
        });
    }

    private String fullyQualifiedName(Either<CompilationUnit, TypeInfo> packageNameOrEnclosing, String simpleName) {
        String prefix;
        if (packageNameOrEnclosing.isLeft()) {
            if (packageNameOrEnclosing.getLeft().packageName().isEmpty()) {
                prefix = "";
            } else {
                prefix = packageNameOrEnclosing.getLeft().packageName() + ".";
            }
        } else {
            prefix = packageNameOrEnclosing.getRight().fullyQualifiedName() + ".";
        }
        return prefix + simpleName;
    }

    private void collectNamesOfSubTypesIntoTypeContext(TypeContext typeContext, TypeInfo typeInfo) {
        typeContext.addToContext(typeInfo, SUBTYPE_PRIORITY);
        // add direct children
        for (TypeInfo subType : typeInfo.subTypes()) {
            typeContext.addToContext(subType, SUBTYPE_PRIORITY);
        }
        if (typeInfo.compilationUnitOrEnclosingType().isRight()) {
            collectNamesOfSubTypesIntoTypeContext(typeContext, typeInfo.compilationUnitOrEnclosingType().getRight());
        }
    }

    private RecordField parseRecordField(Context context, TypeInfo typeInfo, RecordComponent rc) {
        int i = 0;
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();

        List<Annotation> annotations = new ArrayList<>();
        while (true) {
            Node tdi = rc.get(i);
            if (tdi instanceof Annotation a) {
                annotations.add(a);
            } else if (tdi instanceof Modifiers modifiers) {
                for (Node node : modifiers.children()) {
                    if (node instanceof Annotation a) {
                        annotations.add(a);
                    }
                }
            } else break;
            i++;
        }

        ParameterizedType pt;
        if (rc.get(i) instanceof Type type) {
            pt = parsers.parseType().parse(context, type, detailedSourcesBuilder);
            i++;
        } else throw new Summary.ParseException(context, "Expected type in record component");
        boolean varargs;
        ParameterizedType ptWithVarArgs;
        if (rc.get(i) instanceof Delimiter d && Token.TokenType.VAR_ARGS.equals(d.getType())) {
            varargs = true;
            ptWithVarArgs = pt.copyWithArrays(pt.arrays() + 1);
            i++;
        } else {
            varargs = false;
            ptWithVarArgs = pt;
        }
        String name;
        if (rc.get(i) instanceof Identifier identifier) {
            name = identifier.getSource();
            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(name, source(identifier));
        } else {
            throw new Summary.ParseException(context, "Expected identifier in record component");
        }
        FieldInfo fieldInfo = runtime.newFieldInfo(name, false, ptWithVarArgs, typeInfo);
        Source fieldSource = source(rc);
        fieldInfo.builder()
                .addComments(comments(rc, context, fieldInfo, fieldInfo.builder()))
                .setSource(detailedSourcesBuilder == null
                        ? fieldSource : fieldSource.withDetailedSources(detailedSourcesBuilder.build()))
                .setInitializer(runtime.newEmptyExpression())
                .addFieldModifier(runtime.fieldModifierPrivate())
                .addFieldModifier(runtime.fieldModifierFinal())
                .computeAccess();

        // now that we know the type builder, we can parse the annotations
        parseAnnotations(context, fieldInfo.builder(), annotations);

        // not yet commiting! annotations may not have been parsed yet
        context.resolver().addRecordField(fieldInfo);

        return new RecordField(fieldInfo, varargs);
    }

    private MethodInfo createEmptyConstructor(TypeInfo typeInfo, boolean privateEmptyConstructor) {
        MethodInfo methodInfo = runtime.newConstructor(typeInfo, runtime.methodTypeSyntheticConstructor());
        MethodInfo.Builder builder = methodInfo.builder();
        builder.setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor());
        builder.setMethodBody(runtime.emptyBlock());
        builder.addMethodModifier(privateEmptyConstructor
                ? runtime.methodModifierPrivate() : runtime.methodModifierPublic());
        builder.setSynthetic(true);
        builder.computeAccess();
        builder.commit();
        return methodInfo;
    }

    void parseBody(Context newContext,
                   Node body,
                   TypeNature typeNature,
                   TypeInfo typeInfo,
                   TypeInfo.Builder builder,
                   List<RecordField> recordFields,
                   Lombok.Data lombokData) {
        List<TypeDeclaration> typeDeclarations = new ArrayList<>();
        List<FieldDeclaration> fieldDeclarations = new ArrayList<>();
        int countNormalConstructors = 0;
        int countInitializers = 0;

        for (Node child : body.children()) {
            if (!(child instanceof EmptyDeclaration)) {
                if (child instanceof TypeDeclaration cid) typeDeclarations.add(cid);
                else if (child instanceof ConstructorDeclaration) {
                    ++countNormalConstructors;
                } else if (child instanceof FieldDeclaration fd) fieldDeclarations.add(fd);
            }
        }

        // FIRST, parse the subtypes (but do not add them as a subtype, that has already been done in the
        // scan phase

        for (TypeDeclaration typeDeclaration : typeDeclarations) {
            TypeInfo subTypeInfo = parse(newContext, Either.right(typeInfo), typeDeclaration,
                    false).getLeft();
            // for the rest of the body
            newContext.typeContext().addToContext(subTypeInfo, SUBTYPE_PRIORITY);
        }

        // THEN, all sorts of methods and constructors

        for (Node child : body.children()) {
            if (!(child instanceof EmptyDeclaration)) {
                if (child instanceof MethodDeclaration md) {
                    MethodInfo methodInfo = parsers.parseMethodDeclaration().parse(newContext, md, null);
                    if (methodInfo != null) {
                        builder.addMethod(methodInfo);
                    } // else error
                } else if (child instanceof ConstructorDeclaration || child instanceof CompactConstructorDeclaration) {
                    MethodInfo constructor = parsers.parseMethodDeclaration().parse(newContext, child, recordFields);
                    if (constructor != null) {
                        builder.addConstructor(constructor);
                    } // else error
                } else if (child instanceof Initializer i) {
                    int j = 0;
                    boolean staticInitializer = Token.TokenType.STATIC.equals(i.getFirst().getType());
                    String base;
                    MethodInfo.MethodType methodType;
                    if (staticInitializer) {
                        ++j;
                        base = "<static_";
                        methodType = runtime.methodTypeStaticInitializer();
                    } else {
                        base = "<init_";
                        methodType = runtime.methodTypeInstanceInitializer();
                    }
                    if (i.get(j) instanceof CodeBlock cb) {
                        Source cbSource = source(cb);
                        String name = base + (countInitializers++) + ">";
                        MethodInfo method = runtime.newMethod(typeInfo, name, methodType);
                        method.builder()
                                .setSource(cbSource)
                                .setReturnType(runtime.voidParameterizedType())
                                .setAccess(runtime.accessPrivate())
                                .commitParameters();
                        newContext.resolver().add(method, method.builder(),
                                newContext.emptyForwardType(), null, cb, newContext,
                                null);
                        builder.addMethod(method);
                    } else {
                        throw new Summary.ParseException(newContext, "Unknown node in static initializer");
                    }

                }
            }
        }

        if (countNormalConstructors == 0 && (typeNature.isClass() || typeNature.isEnum())) {
            boolean privateEmptyConstructor = typeNature.isEnum();
            builder.addConstructor(createEmptyConstructor(typeInfo, privateEmptyConstructor));
        }

        // FINALLY, do the fields
        for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
            parsers.parseFieldDeclaration().parse(newContext, fieldDeclaration, lombokData).forEach(builder::addField);
        }
    }

}
