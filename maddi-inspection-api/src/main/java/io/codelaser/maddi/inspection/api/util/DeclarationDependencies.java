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

package io.codelaser.maddi.inspection.api.util;

import java.util.HashSet;
import java.util.Set;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.ImportStatement;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;

/**
 * The primary types whose declarations ({@link DeclarationSignature}) the code of a primary type relies on: when one
 * of them changes, javac may resolve a name in this type differently, and a copy of it that keeps its old resolution
 * (a rewire) is wrong. The re-parse asks this of every type it is about to rewire, so that a declaration change
 * elsewhere forces a re-parse only of the types it can reach.
 * <p>
 * A type's resolution can depend on another type without naming it, so the names written in the source
 * ({@link Element#typesReferenced}) are not enough. Collected, over the type, its nested, anonymous and local types:
 * <ul>
 *     <li>every type written in the source, and the imports;</li>
 *     <li>the type of every expression: in {@code x.b().c()} the call {@code c()} resolves in the type {@code b()}
 *     returns, which the source never names;</li>
 *     <li>the owner of every method, constructor and field used;</li>
 *     <li>the type arguments and type-parameter bounds of these types, recursively;</li>
 * </ul>
 * closed over the supertypes of every collected type (an inherited member resolves through its subtype; an
 * assignability that changed moves an overload resolution), and projected onto primary types.
 * <p>
 * Over-approximating costs a re-parse; missing a type keeps a stale resolution, silently. When in doubt, add.
 */
public final class DeclarationDependencies {

    private DeclarationDependencies() {
    }

    /**
     * @return the fully qualified names of the primary types the code of {@code primaryType} depends on, its own
     * included
     */
    public static Set<String> of(TypeInfo primaryType) {
        Collector collector = new Collector();
        collector.importsOf(primaryType);
        collector.type(primaryType);
        return collector.closeOverSupertypes();
    }

    private static final class Collector {
        private final Set<TypeInfo> types = new HashSet<>();
        private final Set<TypeInfo> walked = new HashSet<>();
        private final Set<TypeParameter> typeParameters = new HashSet<>();
        private final Set<String> names = new HashSet<>();

        void importsOf(TypeInfo primaryType) {
            if (primaryType.compilationUnit() == null) return;
            for (ImportStatement is : primaryType.compilationUnit().importStatements()) {
                String s = is.importString();
                if (s == null) continue;
                if (s.endsWith(".*")) s = s.substring(0, s.length() - 2);
                // 'a.C', 'a.C.Inner', 'a.C.member' (static), 'a' (on demand): each prefix may name a type
                names.add(s);
                for (int dot = s.lastIndexOf('.'); dot > 0; dot = s.lastIndexOf('.', dot - 1)) {
                    names.add(s.substring(0, dot));
                }
            }
        }

        void type(TypeInfo typeInfo) {
            if (typeInfo == null || !walked.add(typeInfo)) return;
            add(typeInfo);
            typeInfo.typesReferenced(_ -> true).forEach(tr -> add(tr.typeInfo()));
            if (typeInfo.parentClass() != null) add(typeInfo.parentClass());
            typeInfo.interfacesImplemented().forEach(this::add);
            typeInfo.permittedWhenSealed().forEach(this::add);
            typeInfo.typeParameters().forEach(tp -> tp.typeBounds().forEach(this::add));
            typeInfo.subTypes().forEach(this::type);
            typeInfo.constructorAndMethodStream().forEach(this::method);
            for (FieldInfo fi : typeInfo.fields()) {
                add(fi.type());
                if (fi.initializer() != null && !fi.initializer().isEmpty()) fi.initializer().visit(this::element);
            }
        }

        private void method(MethodInfo mi) {
            if (mi.hasReturnValue()) add(mi.returnType());
            mi.parameters().forEach(pi -> add(pi.parameterizedType()));
            mi.exceptionTypes().forEach(this::add);
            mi.typeParameters().forEach(tp -> tp.typeBounds().forEach(this::add));
            mi.overrides().forEach(o -> add(o.typeInfo()));
            if (mi.methodBody() != null) mi.methodBody().visit(this::element);
        }

        private boolean element(Element e) {
            if (e instanceof Expression expression) add(expression.parameterizedType());
            switch (e) {
                case MethodCall mc -> {
                    add(mc.methodInfo().typeInfo());
                    mc.typeArguments().forEach(this::add);
                }
                case MethodReference mr -> add(mr.methodInfo().typeInfo());
                case VariableExpression ve when ve.variable() instanceof FieldReference fr ->
                        add(fr.fieldInfo().owner());
                case ConstructorCall cc -> {
                    if (cc.constructor() != null) add(cc.constructor().typeInfo());
                    cc.typeArguments().forEach(this::add);
                    if (cc.anonymousClass() != null) {
                        // the visit does not descend into an anonymous body; its arguments are not part of it
                        type(cc.anonymousClass());
                        if (cc.object() != null) cc.object().visit(this::element);
                        cc.parameterExpressions().forEach(p -> p.visit(this::element));
                        return false;
                    }
                }
                case LocalTypeDeclaration ltd -> {
                    type(ltd.typeInfo());
                    return false;
                }
                default -> {
                }
            }
            // a statement's own types (a local variable, a catch clause, a pattern): typesReferenced has them
            return true;
        }

        private void add(ParameterizedType pt) {
            if (pt == null) return;
            add(pt.typeInfo());
            pt.parameters().forEach(this::add);
            TypeParameter tp = pt.typeParameter();
            if (tp != null && typeParameters.add(tp)) tp.typeBounds().forEach(this::add);
        }

        private void add(TypeInfo typeInfo) {
            if (typeInfo != null) types.add(typeInfo);
        }

        Set<String> closeOverSupertypes() {
            Set<TypeInfo> done = new HashSet<>();
            for (TypeInfo t : Set.copyOf(types)) supertypes(t, done);
            for (TypeInfo t : done) {
                // a nested type's declarations are part of its primary type's signature
                names.add(t.primaryType().fullyQualifiedName());
            }
            return names;
        }

        private static void supertypes(TypeInfo typeInfo, Set<TypeInfo> done) {
            if (typeInfo == null || !done.add(typeInfo)) return;
            if (typeInfo.parentClass() != null) supertypes(typeInfo.parentClass().typeInfo(), done);
            typeInfo.interfacesImplemented().forEach(pt -> supertypes(pt.typeInfo(), done));
            // an inner type resolves names in its enclosing types too
            if (typeInfo.compilationUnitOrEnclosingType().isRight()) {
                supertypes(typeInfo.compilationUnitOrEnclosingType().getRight(), done);
            }
        }
    }
}
