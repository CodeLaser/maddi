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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * C# has no local classes. A local class that captures nothing of its method (no local variable, no parameter, no
 * instance member of the enclosing object) is lifted: printed as a private nested type of the enclosing type, where
 * the method's code names it as before. One that captures is not translated ({@code LOCAL_CLASS}).
 */
final class CSharpLocalTypes {

    private CSharpLocalTypes() {
    }

    /** The local types declared in the methods and constructors of {@code typeInfo} that are lifted into it. */
    static List<TypeInfo> lifted(TypeInfo typeInfo) {
        List<TypeInfo> lifted = new ArrayList<>();
        Stream.concat(typeInfo.constructors().stream(), typeInfo.methods().stream()).forEach(m -> {
            if (m.methodBody() == null) return;
            m.methodBody().visit((Element e) -> {
                if (e instanceof LocalTypeDeclaration ltd && liftable(ltd.typeInfo())) lifted.add(ltd.typeInfo());
                return true;
            });
        });
        return lifted;
    }

    static boolean liftable(TypeInfo local) {
        MethodInfo method = local.enclosingMethod();
        if (method == null) return false;
        Set<String> outside = new HashSet<>();
        if (method.methodBody() != null) declared(method.methodBody(), outside);
        Set<String> inside = new HashSet<>();
        List<Element> elements = CstQueries.codeWithSubTypes(local);
        elements.forEach(e -> declared(e, inside));
        boolean[] captures = {false};
        for (Element element : elements) {
            element.visit((Element e) -> {
                if (e instanceof VariableExpression ve) {
                    switch (ve.variable()) {
                        case LocalVariable lv when outside.contains(lv.simpleName())
                                                   && !inside.contains(lv.simpleName()) -> captures[0] = true;
                        case ParameterInfo pi when method.parameters().contains(pi) -> captures[0] = true;
                        case FieldReference fr when !fr.fieldInfo().isStatic() && CSharpNames.foreign(fr.fieldInfo().owner(), local)
                                                    && CstQueries.onThis(fr) -> captures[0] = true;
                        default -> {
                        }
                    }
                } else if (e instanceof MethodCall mc && !mc.methodInfo().isStatic() && mc.objectIsImplicit()
                           && CSharpNames.foreign(mc.methodInfo().typeInfo(), local)) {
                    captures[0] = true;
                }
                return !captures[0];
            });
            if (captures[0]) return false;
        }
        return true;
    }

    private static void declared(Element element, Set<String> names) {
        element.visit((Element e) -> {
            if (e instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().map(LocalVariable::simpleName).forEach(names::add);
            } else if (e instanceof RecordPattern rp && rp.localVariable() != null) {
                names.add(rp.localVariable().simpleName());
            }
            return true;
        });
    }
}
