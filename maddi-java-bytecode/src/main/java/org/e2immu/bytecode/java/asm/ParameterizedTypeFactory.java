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

package org.e2immu.bytecode.java.asm;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.Wildcard;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

// signatures formally defined in https://docs.oracle.com/javase/specs/jvms/se13/html/jvms-4.html

public class ParameterizedTypeFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterizedTypeFactory.class);
    public static final char PLUS_EXTENDS = '+';
    public static final char MINUS_SUPER = '-';
    public static final char ARRAY_BRACKET = '[';
    public static final char CHAR_L = 'L';
    public static final char TYPE_PARAM_T = 'T';
    public static final char WILDCARD_STAR = '*';
    public static final char SEMICOLON_END_NAME = ';';
    public static final char DOT = '.';
    public static final char DOLLAR_SEPARATE_SUBTYPE = '$';
    public static final char GT_END_TYPE_PARAMS = '>';
    public static final char LT_START_TYPE_PARAMS = '<';

    static class Result {
        final ParameterizedType parameterizedType;
        final int nextPos;
        final boolean typeNotFoundError;

        private Result(ParameterizedType parameterizedType, int nextPos, boolean error) {
            this.nextPos = nextPos;
            this.parameterizedType = parameterizedType;
            typeNotFoundError = error;
        }
    }

    static Result from(Runtime runtime,
                       ByteCodeInspector.TypeParameterContext typeContext,
                       LocalTypeMap findType,
                       LocalTypeMap.LoadMode loadMode,
                       String signature,
                       boolean createStub) {
        try {
            int firstCharPos = 0;
            char firstChar = signature.charAt(0);

            // wildcard, <?>
            if (WILDCARD_STAR == firstChar) {
                return new Result(runtime.parameterizedTypeWildcard(), 1, false);
            }

            Wildcard wildCard;
            // extends keyword; NOTE: order is important, extends and super need to come before arrays
            if (PLUS_EXTENDS == firstChar) {
                firstCharPos++;
                firstChar = signature.charAt(firstCharPos);
                wildCard = runtime.wildcardExtends();
            } else if (MINUS_SUPER == firstChar) {
                firstCharPos++;
                firstChar = signature.charAt(firstCharPos);
                wildCard = runtime.wildcardSuper();
            } else wildCard = null;

            // arrays
            int arrays = 0;
            while (ARRAY_BRACKET == firstChar) {
                arrays++;
                firstCharPos++;
                firstChar = signature.charAt(firstCharPos);
            }

            // normal class or interface type
            if (CHAR_L == firstChar) {
                return normalType(runtime, typeContext, findType, loadMode, signature, arrays, wildCard, firstCharPos,
                        createStub);
            }

            // type parameter
            if (TYPE_PARAM_T == firstChar) {
                int semiColon = signature.indexOf(SEMICOLON_END_NAME);
                String typeParamName = signature.substring(firstCharPos + 1, semiColon);
                TypeParameter namedType = typeContext.get(typeParamName);
                if (namedType == null) {
                    // this is possible
                    // <T:Ljava/lang/Object;T_SPLITR::Ljava/util/Spliterator$OfPrimitive<TT;TT_CONS;TT_SPLITR;>;T_CONS:Ljava/lang/Object;>Ljava/util/stream/StreamSpliterators$SliceSpliterator<TT;TT_SPLITR;>;Ljava/util/Spliterator$OfPrimitive<TT;TT_CONS;TT_SPLITR;>;
                    // problem is that T_CONS is used before it is declared
                    ParameterizedType objectParameterizedType = runtime.objectParameterizedType();
                    return new Result(objectParameterizedType, semiColon + 1, true);
                }
                return new Result(runtime.newParameterizedType(namedType, arrays, wildCard), semiColon + 1, false);
            }
            ParameterizedType primitivePt = primitive(runtime, firstChar);
            if (arrays > 0) {
                return new Result(runtime.newParameterizedType(primitivePt.typeInfo(), arrays), arrays + 1, false);
            }
            return new Result(primitivePt, 1, false);
        } catch (RuntimeException e) {
            LOGGER.error("Caught exception parsing type from " + signature);
            throw e;
        }
    }

    // TODO extends, super

    // example with generics AND dot
    // Ljava/util/LinkedHashMap<TK;TV;>.LinkedHashIterator;
    // Ljava/util/TreeMap$NavigableSubMap<TK;TV;>.SubMapIterator<TK;>;
    // shows that we need to make this recursive or get the generics in a while loop

    private static Result normalType(Runtime runtime,
                                     ByteCodeInspector.TypeParameterContext typeContext,
                                     LocalTypeMap localTypeMap,
                                     LocalTypeMap.LoadMode loadMode,
                                     String signature,
                                     int arrays,
                                     Wildcard wildCard,
                                     int firstCharIndex,
                                     boolean createStub) {
        StringBuilder path = new StringBuilder();
        int semiColon = -1;
        int start = firstCharIndex + 1;
        List<ParameterizedType> typeParameters = new ArrayList<>();
        boolean haveDot = true;
        boolean typeNotFoundError = false;

        while (haveDot) {
            semiColon = signature.indexOf(SEMICOLON_END_NAME, start);
            int openGenerics = signature.indexOf(LT_START_TYPE_PARAMS, start);
            boolean haveGenerics = openGenerics >= 0 && openGenerics < semiColon;
            int endOfTypeInfo;
            int unmodifiedStart = start;
            if (haveGenerics) {
                endOfTypeInfo = openGenerics;
                IterativeParsing iterativeParsing = new IterativeParsing();
                iterativeParsing.startPos = openGenerics + 1;
                do {
                    iterativeParsing = iterativelyParseTypes(runtime, typeContext, localTypeMap, loadMode, signature,
                            iterativeParsing, createStub);
                    if (iterativeParsing == null) return null;
                    typeParameters.add(iterativeParsing.result);
                    typeNotFoundError = typeNotFoundError || iterativeParsing.typeNotFoundError;
                } while (iterativeParsing.more);
                haveDot = iterativeParsing.endPos < signature.length() && signature.charAt(iterativeParsing.endPos) == DOT;
                if (haveDot) start = iterativeParsing.endPos + 1;
                semiColon = signature.indexOf(SEMICOLON_END_NAME, iterativeParsing.endPos);
            } else {
                int dot = signature.indexOf(DOT, start);
                haveDot = dot >= 0 && dot < semiColon;
                if (haveDot) start = dot + 1;
                endOfTypeInfo = haveDot ? dot : semiColon;
            }
            if (!path.isEmpty()) path.append(DOLLAR_SEPARATE_SUBTYPE);
            path.append(signature, unmodifiedStart, endOfTypeInfo);
        }
        String fqn = path.toString().replaceAll("[/$]", ".");

        TypeInfo typeInfo1 = localTypeMap.getOrCreate(fqn, loadMode);
        TypeInfo typeInfo;
        if (typeInfo1 == null) {
            if (createStub) {
                int lastDot = fqn.lastIndexOf('.');
                String packageName = lastDot < 0 ? "" : fqn.substring(0, lastDot);
                String simpleName = lastDot < 0 ? fqn : fqn.substring(lastDot + 1);
                CompilationUnit cu = runtime.newCompilationUnitStub(packageName);
                typeInfo = runtime.newTypeInfo(cu, simpleName);
                typeInfo.builder().setTypeNature(runtime.typeNatureStub())
                        .setParentClass(runtime.objectParameterizedType())
                        .setAccess(runtime.accessPublic())
                        .setSource(runtime.noSource())
                        .commit();
                LOGGER.info("Created stub {}", typeInfo);
            } else {
                return null;
            }
        } else {
            typeInfo = typeInfo1;
        }
        ParameterizedType parameterizedType = runtime.newParameterizedType(typeInfo, arrays, wildCard,
                typeParameters);
        return new Result(parameterizedType, semiColon + 1, typeNotFoundError);
    }

    private static ParameterizedType primitive(Runtime runtime, char firstChar) {
        return switch (firstChar) {
            case 'B' -> runtime.byteParameterizedType();
            case 'C' -> runtime.charParameterizedType();
            case 'D' -> runtime.doubleParameterizedType();
            case 'F' -> runtime.floatParameterizedType();
            case 'I' -> runtime.intParameterizedType();
            case 'J' -> runtime.longParameterizedType();
            case 'S' -> runtime.shortParameterizedType();
            case 'V' -> runtime.voidParameterizedType();
            case 'Z' -> runtime.booleanParameterizedType();
            default -> throw new RuntimeException("Char " + firstChar + " does NOT represent a primitive!");
        };
    }

    private static class IterativeParsing {
        int startPos;
        int endPos;
        ParameterizedType result;
        boolean more;
        boolean typeNotFoundError;
    }

    private static IterativeParsing iterativelyParseTypes(Runtime runtime,
                                                          ByteCodeInspector.TypeParameterContext typeContext,
                                                          LocalTypeMap findType,
                                                          LocalTypeMap.LoadMode loadMode,
                                                          String signature,
                                                          IterativeParsing iterativeParsing,
                                                          boolean createStub) {
        ParameterizedTypeFactory.Result result = ParameterizedTypeFactory.from(runtime, typeContext, findType, loadMode,
                signature.substring(iterativeParsing.startPos), createStub);
        if (result == null) return null;
        int end = iterativeParsing.startPos + result.nextPos;
        IterativeParsing next = new IterativeParsing();
        next.result = result.parameterizedType;
        char atEnd = signature.charAt(end);
        if (atEnd == GT_END_TYPE_PARAMS) {
            next.more = false;
            next.endPos = end + 1;
        } else {
            next.more = true;
            next.startPos = end;
        }
        next.typeNotFoundError = iterativeParsing.typeNotFoundError || result.typeNotFoundError;
        return next;
    }
}
