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

package io.codelaser.maddi.cst.impl.type;


import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.output.TypeNameRequired;
import io.codelaser.maddi.cst.api.type.Diamond;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.type.Wildcard;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ParameterizedTypePrinter {

    /**
     * It is important not to use the inspection provider too eagerly. During bootstrap of the java.lang classes,
     * there are a lot of interdependencies, and this printer does not have an auto-inspect system.
     * <p>
     * Default: no explicit type parameter definitions.
     *
     * @param parameterizedType to be printed
     * @param varargs           in a context where [] becomes ... ?
     * @param withoutArrays     don't print []
     * @return printed result
     */
    public static OutputBuilder print(Qualification qualification,
                                      ParameterizedType parameterizedType,
                                      boolean varargs,
                                      Diamond diamond,
                                      boolean withoutArrays) {
        return print(qualification, parameterizedType, varargs, diamond, withoutArrays, false);
    }

    /**
     * @param qualification     fully qualified, partially, simple...?
     * @param parameterizedType the type to print
     * @param varargs           print, or don't print ...
     * @param diamond           print, or don't print the diamond operator < ... >
     * @param withoutArrays     don't print or print []
     * @param printTypeBounds   print "extends ..."
     * @return printed result
     */
    public static OutputBuilder print(Qualification qualification,
                                      ParameterizedType parameterizedType,
                                      boolean varargs,
                                      Diamond diamond,
                                      boolean withoutArrays,
                                      boolean printTypeBounds) {
        return print(qualification, parameterizedType, varargs, diamond, withoutArrays, printTypeBounds, true);
    }

    /**
     * @param printAnnotations include the type's TYPE-USE annotations. TRUE when rendering SOURCE; FALSE when
     *                         rendering a NAME -- {@code fullyQualifiedName()} and {@code printForMethodFQN()}
     *                         identify a type and must not vary with what is written on a particular use of
     *                         it, or '@NonNull a.b.Factory' starts turning up where 'a.b.Factory' is expected
     *                         and every name-keyed lookup misses.
     */
    public static OutputBuilder print(Qualification qualification,
                                      ParameterizedType parameterizedType,
                                      boolean varargs,
                                      Diamond diamond,
                                      boolean withoutArrays,
                                      boolean printTypeBounds,
                                      boolean printAnnotations) {
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        if (parameterizedType.isIntersectionType()) {
            return intersectionType(qualification, parameterizedType, printTypeBounds);
        }
        Wildcard w = parameterizedType.wildcard();
        if (w != null) {
            if (w.isUnbound()) {
                outputBuilder.add(new TextImpl("?"));
            } else if (w.isExtends()) {
                outputBuilder.add(new TextImpl("?")).add(SpaceEnum.ONE).add(KeywordImpl.EXTENDS).add(SpaceEnum.ONE);
            } else if (w.isSuper()) {
                outputBuilder.add(new TextImpl("?")).add(SpaceEnum.ONE).add(KeywordImpl.SUPER).add(SpaceEnum.ONE);
            }
        }
        /*
         ⭐ TYPE-USE ANNOTATIONS, printed where they were written: after any wildcard bound and immediately
         before the type. '? extends @Nullable Object', 'List<@Nullable String>', '@Nullable String m()'.
         Modelled on TypeParameterImpl's own annotation printing so the two agree on spacing.

         ⛔ WITHOUT THIS the parse can carry them and nothing shows it. That is worse than not carrying them
         at all: every generated file would silently state something weaker than its source, and only a
         nullness checker at the consumer's build would ever say so.
         */
        /*
         On an array the annotations before the type name are the ELEMENTS' (the innermost component type's); the
         array's own, and each inner dimension's, are printed before its '[]': 'String @Nullable []' (JLS 9.7.4).
         */
        List<ParameterizedType> dimensions = new ArrayList<>(); // the array, then each component that is an array
        ParameterizedType element = parameterizedType;
        while (element.arrays() > 0) {
            dimensions.add(element);
            element = element.componentType();
        }
        List<AnnotationExpression> typeAnnotations = printAnnotations ? element.annotations() : List.of();
        TypeParameter tp = parameterizedType.typeParameter();
        TypeInfo typeInfo = parameterizedType.typeInfo();
        boolean singleName = typeInfo != null && (parameterizedType.parameters().isEmpty()
                                                  || typeInfo.isPrimaryType() || typeInfo.isStatic());
        /*
         JLS 9.7.4: on a qualified type name a type-use annotation goes immediately before the simple name,
         'A.M.@NN P<T>'; '@NN A.M.P<T>' does not compile ("to annotate a qualified type, write A.M.@NN P<T>").
         When the name prints qualified, the qualifier is written first and the name itself simple
         (CodeLaser/maddi#113). Not for a generic inner class, whose type arguments are distributed over its
         outer types (distributeTypeParameters).
         */
        String qualifier = !typeAnnotations.isEmpty() && tp == null && singleName
                ? qualifierBeforeSimpleName(qualification, typeInfo) : null;
        // no space between the qualifier and the annotation: the formatter separates two words by default, which
        // printed 'java.util. @Nullable List'
        if (qualifier != null) outputBuilder.add(new TextImpl(qualifier)).add(SpaceEnum.NONE);
        if (!typeAnnotations.isEmpty()) {
            OutputBuilder ab = typeAnnotations.stream().map(ae -> ae.print(qualification))
                    .collect(OutputBuilderImpl.joining(SpaceEnum.ONE));
            outputBuilder.add(ab).add(SpaceEnum.ONE);
        }
        if (tp != null) {
            outputBuilder.add(tp.print(qualification, printTypeBounds));
        } else if (typeInfo != null) {
            if (parameterizedType.parameters().isEmpty()) {
                outputBuilder.add(TypeNameImpl.typeName(typeInfo, qualifier != null ? TypeNameImpl.Required.SIMPLE
                        : qualification.qualifierRequired(typeInfo), false));
                if (diamond.isYes()) {
                    outputBuilder.add(SymbolEnum.DIAMOND);
                }
            } else {
                OutputBuilder sub;
                if (singleName) { // shortcut
                    sub = singleType(qualification, typeInfo, diamond, qualifier != null,
                            parameterizedType.parameters(), printTypeBounds);
                } else {
                    sub = distributeTypeParameters(qualification, parameterizedType,
                            printTypeBounds, diamond);
                }
                outputBuilder.add(sub);
            }
        } else if (w == null) {
            // all null, this is a JLO marker
            String jlo = qualification == QualificationImpl.DESCRIPTORS ? "java.lang.Object" : "Object";
            outputBuilder.add(new TextImpl(jlo));
        }
        if (!withoutArrays) {
            if (varargs) {
                if (parameterizedType.arrays() == 0) {
                    throw new UnsupportedOperationException("Varargs parameterized types must have arrays>0!");
                }
            }
            for (int i = 0; i < dimensions.size(); i++) {
                List<AnnotationExpression> dimensionAnnotations = printAnnotations ? dimensions.get(i).annotations()
                        : List.of();
                if (!dimensionAnnotations.isEmpty()) {
                    outputBuilder.add(dimensionAnnotations.stream().map(ae -> ae.print(qualification))
                            .collect(OutputBuilderImpl.joining(SpaceEnum.ONE))).add(SpaceEnum.ONE);
                }
                boolean last = i == dimensions.size() - 1;
                if (dimensionAnnotations.isEmpty()) {
                    // unannotated dimensions print as before: '[][]', 'String[]...'
                    int j = i;
                    while (j < dimensions.size() - 1 && (!printAnnotations
                                                         || dimensions.get(j + 1).annotations().isEmpty())) j++;
                    int count = j - i + 1;
                    boolean reachesEnd = j == dimensions.size() - 1;
                    outputBuilder.add(new TextImpl(varargs && reachesEnd ? "[]".repeat(count - 1) + "..."
                            : "[]".repeat(count)));
                    i = j;
                } else {
                    outputBuilder.add(new TextImpl(varargs && last ? "..." : "[]"));
                }
            }
        }
        return outputBuilder;
    }

    /*
     The part of the type's printed name before its simple name ('a.b.A.M.' for 'a.b.A.M.P', 'A.M.' when qualified
     from the primary type), or null when the name prints simple, or in a form that is not Java source.
     */
    private static String qualifierBeforeSimpleName(Qualification qualification, TypeInfo typeInfo) {
        TypeNameRequired required = qualification.qualifierRequired(typeInfo);
        if (required != TypeNameImpl.Required.FQN && required != TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE
            && required != TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE_FOLLOW_EXISTING) {
            return null;
        }
        String name = TypeNameImpl.typeName(typeInfo, required, false).minimal();
        String simple = typeInfo.simpleName();
        return name.length() > simple.length() && name.endsWith("." + simple)
                ? name.substring(0, name.length() - simple.length()) : null;
    }

    // if a type is a subtype, the type parameters may belong to any of the intermediate types
    // we should write them there
    private static OutputBuilder distributeTypeParameters(Qualification qualification,
                                                          ParameterizedType parameterizedType,
                                                          boolean printTypeBounds,
                                                          Diamond diamond) {
        TypeInfo typeInfo = parameterizedType.typeInfo();
        assert typeInfo != null;
        List<TypeAndParameters> taps = new LinkedList<>();
        int offset = parameterizedType.parameters().size();
        // see TestByteCodeInspectorCommonPool for the offset>0 test
        while (typeInfo != null && offset > 0) {
            List<ParameterizedType> typesForTypeInfo = new ArrayList<>();
            int numTypeParameters = typeInfo.typeParameters().size();
            offset -= numTypeParameters;
            if (offset < 0) {
                throw new UnsupportedOperationException();
            }
            for (int i = 0; i < numTypeParameters; i++) {
                typesForTypeInfo.add(parameterizedType.parameters().get(offset + i));
            }
            TypeInfo next;
            if (typeInfo.compilationUnitOrEnclosingType().isRight()) {
                next = typeInfo.compilationUnitOrEnclosingType().getRight();
            } else {
                next = null;
            }
            taps.add(0, new TypeAndParameters(typeInfo, next == null, typesForTypeInfo));
            typeInfo = next;
        }
        return taps.stream().map(tap -> singleType(qualification,
                        tap.typeInfo, diamond, !tap.isPrimaryType, tap.typeParameters, printTypeBounds))
                .collect(OutputBuilderImpl.joining(SymbolEnum.DOT));
    }

    record TypeAndParameters(TypeInfo typeInfo, boolean isPrimaryType, List<ParameterizedType> typeParameters) {
    }

    /**
     * The class of a qualified instance creation, {@code outer.new Inner<String>(...)}: named by its simple name,
     * relative to the outer instance's type ({@code outer.new X.Inner(1)} does not parse: GitHub #107), with only
     * its own type arguments (the outer type's come first in {@code parameters()}).
     */
    public static OutputBuilder printAfterQualifiedNew(Qualification qualification,
                                                      ParameterizedType parameterizedType,
                                                      Diamond diamond) {
        TypeInfo typeInfo = parameterizedType.typeInfo();
        List<ParameterizedType> parameters = parameterizedType.parameters();
        int own = typeInfo.typeParameters().size();
        List<ParameterizedType> ownParameters = parameters.size() >= own
                ? parameters.subList(parameters.size() - own, parameters.size()) : parameters;
        OutputBuilder outputBuilder = singleType(qualification, typeInfo, diamond, true, ownParameters, false);
        if (ownParameters.isEmpty() && own > 0 && diamond.isYes()) outputBuilder.add(SymbolEnum.DIAMOND);
        return outputBuilder;
    }

    private static OutputBuilder singleType(Qualification qualification,
                                            TypeInfo typeInfo,
                                            Diamond diamond,
                                            boolean forceSimple, // when constructing an qualified with distributed type parameters
                                            List<ParameterizedType> typeParameters,
                                            boolean printTypeBounds) {
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        if (forceSimple) {
            outputBuilder.add(new TextImpl(typeInfo.simpleName()));
        } else {
            outputBuilder.add(TypeNameImpl.typeName(typeInfo, qualification.qualifierRequired(typeInfo), false));
        }
        if (!typeParameters.isEmpty() && diamond != DiamondEnum.NO) {
            if (diamond == DiamondEnum.SHOW_ALL) {
                outputBuilder.add(SymbolEnum.LEFT_ANGLE_BRACKET);
                outputBuilder.add(typeParameters.stream().map(tp -> print(qualification,
                                tp, false, DiamondEnum.SHOW_ALL, false, printTypeBounds))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
                outputBuilder.add(SymbolEnum.RIGHT_ANGLE_BRACKET);
            } else {
                // diamond YES: emit the dedicated <> token (no surrounding space), matching the no-parameters
                // path above; building it from separate < and > leaves a stray space before a following '('
                outputBuilder.add(SymbolEnum.DIAMOND);
            }
        }
        return outputBuilder;
    }

    private static OutputBuilder intersectionType(Qualification qualification,
                                                  ParameterizedType parameterizedType,
                                                  boolean printTypeBounds) {
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        if (parameterizedType.typeParameter() != null) {
            outputBuilder
                    .add(parameterizedType.typeParameter().print(qualification, printTypeBounds))
                    .add(SpaceEnum.ONE);
        }
        Wildcard wildcard = parameterizedType.wildcard();
        if (wildcard != null && !wildcard.isUnbound()) {
            /*
            ⛔⛔ THIS USED TO BE `assert wildcard.isExtendsIntersection();`, AND IT TOOK A WHOLE REFACTORING
            DOWN. Cassandra design G1, 2026-09-21: a read-only extractInterfaceSuggestion on db.Keyspace died
            with "AssertionError with no message". The caller is not a rendering at all -- CommonAnalyze's
            getOrCreateReplaceCandidate uses a type's toString() AS A MAP KEY -- and an Error walks past every
            catch(RuntimeException) between here and the response, so the run reports nothing actionable.
            ⭐ THE SHAPE IS ONE THIS MODEL ITSELF CREATES: ParameterizedTypeImpl puts a plain EXTENDS wildcard
            on a bound that is a type parameter (documented there); when that bound is an INTERSECTION the
            result is exactly the state the assert denied. EXTENDS_INTERSECTION describes what JAVA can write;
            it is not an invariant of what the model can hold.
            ⇒ Print what we are given, for every wildcard kind. A printer's job is to have an answer.
             */
            outputBuilder
                    .add(new TextImpl("?"))
                    .add(SpaceEnum.ONE)
                    .add(wildcard.isSuper() ? KeywordImpl.SUPER : KeywordImpl.EXTENDS)
                    .add(SpaceEnum.ONE);
        }
        outputBuilder.add(parameterizedType.parameters().stream()
                .map(pt -> print(qualification, pt, false, DiamondEnum.SHOW_ALL,
                        false, false))
                .collect(OutputBuilderImpl.joining(SymbolEnum.AND_TYPES)));
        return outputBuilder;
    }
}
