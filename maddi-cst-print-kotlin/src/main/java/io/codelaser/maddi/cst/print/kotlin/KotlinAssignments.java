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
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.BreakOrContinueStatement;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.IfElseStatement;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.statement.ThrowStatement;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which variables are assigned after their declaration, for {@code val} versus {@code var}. Syntactic, and exact
 * where it is used: a Java local is assigned only in its own method body (a lambda or an inner class may not
 * assign a captured local), and a private field only in its compilation unit.
 */
final class KotlinAssignments {

    /** The fields assigned in the compilation unit of {@code primary}, which is compared by identity. */
    private record AssignedFields(TypeInfo primary, Set<FieldInfo> fields) {
    }

    // one entry, for the file being printed: a TypeInfo equals another of the same name from another parse
    private static final ThreadLocal<AssignedFields> ASSIGNED_FIELDS = new ThreadLocal<>();

    private KotlinAssignments() {
    }

    /** True when an assignment (including {@code ++}, {@code +=}) in {@code scope} targets {@code variable}. */
    static boolean assignedIn(Element scope, Variable variable) {
        boolean[] found = {false};
        scope.visit((Element e) -> {
            if (e instanceof Assignment a && variable.equals(a.variableTarget())) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    /** An assignment to {@code variable} in {@code scope} that is not a statement of its own, but a value. */
    static boolean assignedAsValue(Element scope, Variable variable) {
        Set<io.codelaser.maddi.cst.api.expression.Expression> statements = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        List<Assignment> assignments = new java.util.ArrayList<>();
        scope.visit((Element e) -> {
            if (e instanceof ExpressionAsStatement eas) statements.add(CstQueries.unwrap(eas.expression()));
            if (e instanceof Assignment a && variable.equals(a.variableTarget())) assignments.add(a);
            return true;
        });
        return assignments.stream().anyMatch(a -> !statements.contains(a));
    }

    /**
     * A local declared without a value that each path through the rest of its block assigns at most once: a
     * {@code val}, which Kotlin's definite assignment accepts as javac's does ({@code val data: ByteArray} assigned in
     * both branches of an if). Only an assignment that is a statement of its own counts: one inside an expression is
     * printed in an {@code .also { x = it }} lambda, where a val cannot be assigned. A loop, a switch, a try or any
     * other construct that assigns it keeps the var.
     */
    static boolean assignedOnce(Element scope, Statement declaration, Variable local) {
        if (scope == null) return false;
        Block[] holder = {null};
        scope.visit((Element e) -> {
            if (holder[0] == null && e instanceof Block b && b.statements().stream().anyMatch(st -> st == declaration)) {
                holder[0] = b;
            }
            return holder[0] == null;
        });
        if (holder[0] == null) return false;
        List<Statement> statements = holder[0].statements();
        int index = 0;
        while (statements.get(index) != declaration) index++;
        return sequence(statements.subList(index + 1, statements.size()), local).any() <= 1;
    }

    /** The most assignments on a path that completes normally ({@link #NEVER} when none does), and on any path. */
    private record Flow(int normal, int any) {
    }

    private static final int NEVER = -1_000;
    private static final Flow POISON = new Flow(2, 2);

    private static Flow sequence(List<Statement> statements, Variable local) {
        int normal = 0;
        int any = 0;
        for (Statement statement : statements) {
            Flow flow = flow(statement, local);
            any = Math.max(any, normal + flow.any());
            if (flow.normal() <= NEVER / 2) return new Flow(NEVER, any);
            normal += flow.normal();
        }
        return new Flow(normal, any);
    }

    private static Flow flow(Statement statement, Variable local) {
        boolean jumps = statement instanceof ReturnStatement || statement instanceof ThrowStatement
                        || statement instanceof BreakOrContinueStatement;
        if (!assignedIn(statement, local)) return new Flow(jumps ? NEVER : 0, 0);
        if (statement instanceof ExpressionAsStatement eas && eas.expression() instanceof Assignment a
            && local.equals(a.variableTarget()) && !assignedIn(a.value(), local)
            && a.assignmentOperator() == null && a.prefixPrimitiveOperator() == null) {
            return new Flow(1, 1);
        }
        if (statement instanceof IfElseStatement ife && !assignedIn(ife.expression(), local)) {
            Flow then = sequence(ife.block().statements(), local);
            Flow otherwise = ife.elseBlock() == null ? new Flow(0, 0) : sequence(ife.elseBlock().statements(), local);
            return new Flow(Math.max(then.normal(), otherwise.normal()), Math.max(then.any(), otherwise.any()));
        }
        if (statement instanceof Block b) return sequence(b.statements(), local);
        return POISON;
    }

    /** A private field with an initializer that no code of its compilation unit assigns: a {@code val}. */
    static boolean neverReassigned(FieldInfo fieldInfo) {
        if (fieldInfo.access() == null || !fieldInfo.access().isPrivate()) return false;
        TypeInfo primary = fieldInfo.owner().primaryType();
        AssignedFields cached = ASSIGNED_FIELDS.get();
        if (cached == null || cached.primary() != primary) {
            cached = new AssignedFields(primary, assignedFields(primary));
            ASSIGNED_FIELDS.set(cached);
        }
        return !cached.fields().contains(fieldInfo);
    }

    private static Set<FieldInfo> assignedFields(TypeInfo primary) {
        Set<FieldInfo> assigned = new HashSet<>();
        if (primary.compilationUnit() != null) {
            primary.compilationUnit().types().forEach(t -> collect(t, assigned));
        } else {
            collect(primary, assigned);
        }
        return assigned;
    }

    private static void collect(TypeInfo typeInfo, Set<FieldInfo> assigned) {
        CstQueries.codeWithSubTypes(typeInfo).forEach(e -> collect(e, assigned));
    }

    /** Through lambdas (which {@code visit} enters) and anonymous and local classes (which it does not). */
    private static void collect(Element element, Set<FieldInfo> assigned) {
        element.visit((Element e) -> {
            if (e instanceof Assignment a && a.variableTarget() instanceof FieldReference fr) assigned.add(fr.fieldInfo());
            if (e instanceof ConstructorCall cc && cc.anonymousClass() != null) collect(cc.anonymousClass(), assigned);
            if (e instanceof LocalTypeDeclaration ltd) collect(ltd.typeInfo(), assigned);
            return true;
        });
    }
}
