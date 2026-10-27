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

package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation;
import io.codelaser.maddi.cst.api.variable.FieldReference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which methods are an {@code unsafe fn}. Java's mutable static state is a {@code static mut}, and Rust lets only
 * {@code unsafe} code touch one, reading as well as writing. So a method is unsafe when its body (its lambdas
 * included, and for a constructor its type's instance field initializers) reads or writes a static field printed
 * as a {@code static mut}, of any type; or calls, constructs or references ({@code Counter::next}) an analysed
 * method that is unsafe, transitively; or has the {@code @StaticSideEffects} verdict, which is how a library
 * method's contract ({@code System.setOut}) reaches its callers. A call of an unsafe method from a method that is
 * not is rustc's E0133, so the closure has to be the printer's own: the verdict counts only the modifications of
 * another type's static state (CodeLaser/maddi-mod#65), not a read ({@code Counter.peek()}), nor a type's own state.
 * One per module; the same method may be decided again in another module.
 */
final class RustUnsafety {
    private final Map<MethodInfo, Boolean> decided = new HashMap<>();

    boolean unsafe(MethodInfo method, RustContext context) {
        return visit(method, context, new HashSet<>(), new boolean[1]);
    }

    /*
     Depth first, along the calls. A method on the current path counts as safe for now: a result that relied on that
     (cycle[0]) is not kept unless it is unsafe, which no later finding can undo.
     */
    private boolean visit(MethodInfo method, RustContext context, Set<MethodInfo> path, boolean[] cycle) {
        Boolean known = decided.get(method);
        if (known != null) return known;
        if (!path.add(method)) {
            cycle[0] = true;
            return false;
        }
        boolean[] ownCycle = {false};
        boolean unsafe = context.facts.staticSideEffects(method) || touches(method, context, path, ownCycle);
        path.remove(method);
        if (unsafe || !ownCycle[0]) decided.put(method, unsafe);
        if (ownCycle[0]) cycle[0] = true;
        return unsafe;
    }

    private boolean touches(MethodInfo method, RustContext context, Set<MethodInfo> path, boolean[] cycle) {
        if (method.methodBody() == null) return false;
        boolean[] found = {false};
        java.util.function.Predicate<Element> visitor = e -> {
            if (e instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
                && RustTypePrinter.mutableStaticValue(fr.fieldInfo(), context) != null) {
                found[0] = true;
            } else {
                MethodInfo callee = switch (e) {
                    case MethodCall mc -> mc.methodInfo();
                    case ConstructorCall cc -> cc.constructor();
                    case ExplicitConstructorInvocation eci -> eci.methodInfo();
                    case MethodReference mr -> mr.methodInfo();
                    default -> null;
                };
                if (callee != null && unsafeCallee(callee, context, path, cycle)) found[0] = true;
            }
            return !found[0];
        };
        method.methodBody().visit(visitor);
        if (!found[0] && method.isConstructor()) {
            for (FieldInfo f : method.typeInfo().fields()) {
                if (found[0]) break;
                if (!f.isStatic() && f.initializer() != null && !f.initializer().isEmpty()) f.initializer().visit(visitor);
            }
        }
        return found[0];
    }

    // a library method has no static mut of ours: only its contract counts
    private boolean unsafeCallee(MethodInfo callee, RustContext context, Set<MethodInfo> path, boolean[] cycle) {
        if (callee.typeInfo().compilationUnit().externalLibrary()) return context.facts.staticSideEffects(callee);
        return visit(callee, context, path, cycle);
    }
}
