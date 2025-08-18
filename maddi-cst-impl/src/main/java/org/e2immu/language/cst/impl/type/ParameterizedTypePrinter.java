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

package org.e2immu.language.cst.impl.type;


import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.type.Diamond;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.Wildcard;
import org.e2immu.language.cst.impl.output.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ParameterizedTypePrinter {

    /**
     * It is important not too use the inspection provider too eagerly. During bootstrap of the java.lang classes,
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
        OutputBuilder outputBuilder = new OutputBuilderImpl();
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
        TypeParameter tp = parameterizedType.typeParameter();
        if (tp != null) {
            outputBuilder.add(tp.print(qualification, printTypeBounds));
        } else if (parameterizedType.typeInfo() != null) {
            if (parameterizedType.parameters().isEmpty()) {
                outputBuilder.add(TypeNameImpl.typeName(parameterizedType.typeInfo(),
                        qualification.qualifierRequired(parameterizedType.typeInfo()), false));
                if (diamond.isYes()) {
                    outputBuilder.add(SymbolEnum.DIAMOND);
                }
            } else {
                OutputBuilder sub;
                if (parameterizedType.typeInfo().isPrimaryType() || parameterizedType.typeInfo().isStatic()) { // shortcut
                    sub = singleType(qualification, parameterizedType.typeInfo(), diamond, false,
                            parameterizedType.parameters(), printTypeBounds);
                } else {
                    sub = distributeTypeParameters(qualification, parameterizedType,
                            printTypeBounds, diamond);
                }
                outputBuilder.add(sub);
            }
        }
        if (!withoutArrays) {
            if (varargs) {
                if (parameterizedType.arrays() == 0) {
                    throw new UnsupportedOperationException("Varargs parameterized types must have arrays>0!");
                }
                outputBuilder.add(new TextImpl(("[]".repeat(parameterizedType.arrays() - 1) + "...")));
            } else if (parameterizedType.arrays() > 0) {
                outputBuilder.add(new TextImpl("[]".repeat(parameterizedType.arrays())));
            }
        }
        return outputBuilder;
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
            outputBuilder.add(SymbolEnum.LEFT_ANGLE_BRACKET);
            if (diamond == DiamondEnum.SHOW_ALL) {
                outputBuilder.add(typeParameters.stream().map(tp -> print(qualification,
                                tp, false, DiamondEnum.SHOW_ALL, false, printTypeBounds))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
            }
            outputBuilder.add(SymbolEnum.RIGHT_ANGLE_BRACKET);
        }
        return outputBuilder;
    }
}
