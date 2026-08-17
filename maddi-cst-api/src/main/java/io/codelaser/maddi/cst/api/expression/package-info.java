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

/**
 * Expression nodes of the common syntax tree (CST).
 *
 * <p>Every node here is an {@link org.e2immu.language.cst.api.expression.Expression}, which extends
 * {@link org.e2immu.language.cst.api.element.Element} (so it has a
 * {@link org.e2immu.language.cst.api.element.Source source}, comments and annotations) and additionally
 * carries a {@link org.e2immu.language.cst.api.type.ParameterizedType type}. This overview documents the
 * high-level base and the mid-level grouping interfaces; the concrete leaves (constants, operators,
 * calls, …) follow these conventions.
 *
 * <h2>Typing and the canonical order</h2>
 * Each expression reports its static {@link org.e2immu.language.cst.api.expression.Expression#parameterizedType()
 * type}. Expressions are {@link java.lang.Comparable}, which the analyzer relies on to keep symbolic
 * values in a canonical form: {@link org.e2immu.language.cst.api.expression.Expression#order()} orders
 * expressions of <em>different</em> kinds, while
 * {@link org.e2immu.language.cst.api.expression.Expression#internalCompareTo(org.e2immu.language.cst.api.expression.Expression)}
 * breaks ties <em>within</em> a single kind.
 *
 * <h2>Naming and printing</h2>
 * As with statements, most concrete expressions declare a {@code String NAME} kind tag and override
 * {@link org.e2immu.language.cst.api.expression.Expression#name()}. For output,
 * {@link org.e2immu.language.cst.api.expression.Expression#precedence()} returns a
 * {@link org.e2immu.language.cst.api.expression.Precedence} so the printer can parenthesise correctly.
 *
 * <h2>The main families</h2>
 * <ul>
 *   <li><b>Constants</b> — {@link org.e2immu.language.cst.api.expression.ConstantExpression
 *       ConstantExpression&lt;T&gt;} wraps a compile-time value (its {@code constant()}). The numeric
 *       constants additionally implement {@link org.e2immu.language.cst.api.expression.Numeric}.</li>
 *   <li><b>Operators</b> — {@link org.e2immu.language.cst.api.expression.BinaryOperator} (such as
 *       {@code Sum}, {@code Product}, {@code Divide}, {@code Equals}) and
 *       {@link org.e2immu.language.cst.api.expression.UnaryOperator} (such as {@code Negation}). Both
 *       identify the operator as a {@link org.e2immu.language.cst.api.info.MethodInfo}, so an operator is
 *       modelled like a method on its operand type(s).</li>
 *   <li><b>Wrappers</b> — {@link org.e2immu.language.cst.api.expression.ExpressionWrapper}, implemented
 *       by nodes that wrap a single other expression (parentheses, negations).</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * Expressions are immutable. They support the same transformations as other elements —
 * {@link org.e2immu.language.cst.api.expression.Expression#translate(org.e2immu.language.cst.api.translate.TranslationMap)
 * translate} (source-to-source rewrite, returning a single expression),
 * {@link org.e2immu.language.cst.api.expression.Expression#rewire(org.e2immu.language.cst.api.info.InfoMap)
 * rewire} (clone into a new {@code Info} graph), and
 * {@link org.e2immu.language.cst.api.expression.Expression#withSource(org.e2immu.language.cst.api.element.Source)
 * withSource} — and are built through fluent builders ({@code Expression.Builder}, inherited from
 * {@link org.e2immu.language.cst.api.element.Element.Builder}).
 */
package org.e2immu.language.cst.api.expression;
