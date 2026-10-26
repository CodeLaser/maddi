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

package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.print.translate.PrintMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The state of printing one Java compilation unit as one Rust module: the facts, the messages, the {@code use}
 * declarations the printed text needs, and where the printer is. Passed along explicitly; one per file.
 */
final class RustContext {
    final RustFacts facts;
    /** The dependent fields printed as borrows with a lifetime. */
    final RustBorrows borrows;
    /** The fields that methods with {@code @IgnoreModifications} modify, printed as {@code RefCell}s. */
    final RustCells cells;
    /** The methods that are an {@code unsafe fn}: they touch a {@code static mut}, or call one that does. */
    final RustUnsafety unsafety = new RustUnsafety();
    final RustBoxes boxes;
    final RustBounds bounds;
    // the getters a call site modifies through: they get an exclusive twin (RustGetters)
    Set<MethodInfo> mutableGetters = Set.of();
    // the function parameters a call site passes a closure that writes what it captures: FnMut (RustClosures)
    Set<ParameterInfo> mutatingFunctions = Set.of();
    /** The module being printed: a type in it needs no {@code use}. */
    final String module;
    private final List<PrintMessage<RustPrintCode>> messages = new ArrayList<>();
    private final Set<String> uses = new TreeSet<>();

    /** The type whose members are being printed. */
    TypeInfo type;
    /** The method whose body is being printed. */
    MethodInfo method;
    /**
     * How the body names the object under construction: {@code self} in a method, {@code this} in a constructor
     * body, which builds a {@code Self} into a local of that name before returning it.
     */
    String receiver = "self";

    RustContext(RustFacts facts, String module) {
        this.facts = facts;
        this.module = module;
        this.borrows = new RustBorrows(facts);
        this.cells = new RustCells(facts);
        this.boxes = new RustBoxes(facts, borrows);
        this.bounds = new RustBounds(facts);
    }

    /**
     * How a parameter is passed: as the facts say, except a constructor parameter stored in a borrowed field, which is
     * that borrow (see {@link RustBorrows}), and a {@code dyn Fn} closure, which is called through a shared borrow:
     * the analysis marks a functional parameter modified because applying it may modify (the applied marker its
     * callers resolve, maddi-mod#69), which says nothing about the borrow.
     */
    RustFacts.Passing passing(ParameterInfo parameter) {
        RustTypes.Form borrowed = borrows.parameter(parameter);
        if (borrowed != null) {
            return borrowed == RustTypes.Form.SHARED ? RustFacts.Passing.SHARED : RustFacts.Passing.EXCLUSIVE;
        }
        RustFacts.Passing passing = facts.passing(parameter);
        if (passing == RustFacts.Passing.EXCLUSIVE && RustTypes.isFn(parameter.parameterizedType())
            && !mutatingFunctions.contains(parameter)) {
            return RustFacts.Passing.SHARED;
        }
        if (passing == RustFacts.Passing.SHARED && mutatingFunctions.contains(parameter)) {
            return RustFacts.Passing.EXCLUSIVE; // an FnMut is called through &mut
        }
        if (passing == RustFacts.Passing.SHARED && parameter.parameterizedType().isTypeParameter()
            && parameter.parameterizedType().arrays() == 0 && stored(parameter)) {
            return RustFacts.Passing.OWNED;
        }
        return passing;
    }

    private final Map<ParameterInfo, Boolean> stored = new HashMap<>();

    /*
     A T parameter's independence says nothing about keeping it: an unbounded T has no content to modify, and
     independence is measured against 'this' and the result, not against the list the value goes into. So the body
     decides: returned, assigned, or handed to a JDK method's type-variable parameter (add(E), put(K, V), List.of) or
     to a source callee that takes it owned, it is moved in ('list.add(t)' was 'list.push(t)' with 't: &T', E0308).
     */
    private boolean stored(ParameterInfo parameter) {
        Boolean known = stored.get(parameter);
        if (known != null) return known;
        stored.put(parameter, false); // a recursive call passing it on is not evidence
        var body = parameter.methodInfo().methodBody();
        boolean[] keeps = {false};
        if (body != null) {
            body.visit(e -> {
                switch (e) {
                    case ReturnStatement rs
                            when is(rs.expression(), parameter) -> keeps[0] = true;
                    case Assignment a when is(a.value(), parameter) -> keeps[0] = true;
                    case MethodCall mc ->
                            keeps[0] |= handedOn(mc.methodInfo(), mc.parameterExpressions(), parameter);
                    case ConstructorCall cc when cc.constructor() != null ->
                            keeps[0] |= handedOn(cc.constructor(), cc.parameterExpressions(), parameter);
                    default -> { }
                }
                return !keeps[0];
            });
        }
        stored.put(parameter, keeps[0]);
        return keeps[0];
    }

    private boolean handedOn(MethodInfo callee, List<Expression> args,
                             ParameterInfo parameter) {
        var formals = callee.parameters();
        for (int i = 0; i < args.size(); i++) {
            if (!is(args.get(i), parameter) || formals.isEmpty()) continue;
            var formal = formals.get(Math.min(i, formals.size() - 1));
            if (callee.typeInfo().compilationUnit().externalLibrary()) {
                if (formal.parameterizedType().isTypeParameter()) return true;
            } else if (formal != parameter && passing(formal) == RustFacts.Passing.OWNED) {
                return true;
            }
        }
        return false;
    }

    private static boolean is(Expression e,
                              ParameterInfo parameter) {
        return e != null && CstQueries.unwrap(e)
                instanceof VariableExpression ve && parameter.equals(ve.variable());
    }

    void use(String path) {
        uses.add(path);
    }

    Set<String> uses() {
        return uses;
    }

    List<PrintMessage<RustPrintCode>> messages() {
        return messages;
    }

    void message(RustPrintCode code, Element element, String detail) {
        Source source = element == null ? null : element.source();
        String typeName = type == null ? "" : type.fullyQualifiedName();
        messages.add(new PrintMessage<>(code, typeName, source == null ? -1 : source.beginLine(),
                source == null ? -1 : source.beginPos(), detail));
    }
}
