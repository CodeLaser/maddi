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
 * <p>Every node here is an {@link io.codelaser.maddi.cst.api.expression.Expression}, which extends
 * {@link io.codelaser.maddi.cst.api.element.Element} (so it has a
 * {@link io.codelaser.maddi.cst.api.element.Source source}, comments and annotations) and additionally
 * carries a {@link io.codelaser.maddi.cst.api.type.ParameterizedType type}. This overview documents the
 * high-level base and the mid-level grouping interfaces; the concrete leaves (constants, operators,
 * calls, …) follow these conventions.
 *
 * <h2>Typing and the canonical order</h2>
 * Each expression reports its static {@link io.codelaser.maddi.cst.api.expression.Expression#parameterizedType()
 * type}. Expressions are {@link java.lang.Comparable}, which the analyzer relies on to keep symbolic
 * values in a canonical form: {@link io.codelaser.maddi.cst.api.expression.Expression#order()} orders
 * expressions of <em>different</em> kinds, while
 * {@link io.codelaser.maddi.cst.api.expression.Expression#internalCompareTo(io.codelaser.maddi.cst.api.expression.Expression)}
 * breaks ties <em>within</em> a single kind.
 *
 * <h2>Naming and printing</h2>
 * As with statements, most concrete expressions declare a {@code String NAME} kind tag and override
 * {@link io.codelaser.maddi.cst.api.expression.Expression#name()}. For output,
 * {@link io.codelaser.maddi.cst.api.expression.Expression#precedence()} returns a
 * {@link io.codelaser.maddi.cst.api.expression.Precedence} so the printer can parenthesise correctly.
 *
 * <h2>The main families</h2>
 * <ul>
 *   <li><b>Constants</b> — {@link io.codelaser.maddi.cst.api.expression.ConstantExpression
 *       ConstantExpression&lt;T&gt;} wraps a compile-time value (its {@code constant()}). The numeric
 *       constants additionally implement {@link io.codelaser.maddi.cst.api.expression.Numeric}.</li>
 *   <li><b>Operators</b> — {@link io.codelaser.maddi.cst.api.expression.BinaryOperator} (such as
 *       {@code Sum}, {@code Product}, {@code Divide}, {@code Equals}) and
 *       {@link io.codelaser.maddi.cst.api.expression.UnaryOperator} (such as {@code Negation}). Both
 *       identify the operator as a {@link io.codelaser.maddi.cst.api.info.MethodInfo}, so an operator is
 *       modelled like a method on its operand type(s).</li>
 *   <li><b>Wrappers</b> — {@link io.codelaser.maddi.cst.api.expression.ExpressionWrapper}, implemented
 *       by nodes that wrap a single other expression (parentheses, negations).</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * Expressions are immutable. They support the same transformations as other elements —
 * {@link io.codelaser.maddi.cst.api.expression.Expression#translate(io.codelaser.maddi.cst.api.translate.TranslationMap)
 * translate} (source-to-source rewrite, returning a single expression),
 * {@link io.codelaser.maddi.cst.api.expression.Expression#rewire(io.codelaser.maddi.cst.api.info.InfoMap)
 * rewire} (clone into a new {@code Info} graph), and
 * {@link io.codelaser.maddi.cst.api.expression.Expression#withSource(io.codelaser.maddi.cst.api.element.Source)
 * withSource} — and are built through fluent builders ({@code Expression.Builder}, inherited from
 * {@link io.codelaser.maddi.cst.api.element.Element.Builder}).
 */
package io.codelaser.maddi.cst.api.expression;
