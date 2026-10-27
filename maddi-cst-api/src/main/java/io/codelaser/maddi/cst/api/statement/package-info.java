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

/**
 * Statement nodes of the common syntax tree (CST).
 *
 * <p>Every node here is a {@link io.codelaser.maddi.cst.api.statement.Statement}, which extends
 * {@link io.codelaser.maddi.cst.api.element.Element} and therefore also carries a
 * {@link io.codelaser.maddi.cst.api.element.Source source}, comments and annotations. Two intermediate
 * interfaces group related kinds:
 * {@link io.codelaser.maddi.cst.api.statement.LoopStatement} ({@code while}, {@code do}, {@code for},
 * {@code forEach}) and {@link io.codelaser.maddi.cst.api.statement.BreakOrContinueStatement}
 * ({@code break}, {@code continue}).
 *
 * <p>The package follows a handful of conventions; they are documented here once so the individual
 * interfaces can stay terse.
 *
 * <h2>Naming: the {@code NAME} constant and {@code name()}</h2>
 * Each concrete statement declares a {@code public static final String NAME} holding a short, stable
 * kind identifier (for example {@code "while"}, {@code "ifElse"}, {@code "try"}) and overrides
 * {@link io.codelaser.maddi.cst.api.statement.Statement#name()} to return it. The value is an
 * identity tag for the kind of statement, not a label or a printed representation.
 *
 * <h2>The block model</h2>
 * Statements expose their nested blocks uniformly, so that traversal code does not need to know the
 * concrete kind:
 * <ul>
 *   <li>{@link io.codelaser.maddi.cst.api.statement.Statement#block()} is the <em>primary</em> block
 *       (for example the body of a loop, the {@code try} block, the {@code if} branch), or {@code null}
 *       when the statement has none.</li>
 *   <li>{@link io.codelaser.maddi.cst.api.statement.Statement#otherBlocksStream()} streams every
 *       <em>additional</em> block (for example an {@code else} branch, {@code catch}/{@code finally}
 *       blocks, switch entry blocks).</li>
 *   <li>{@link io.codelaser.maddi.cst.api.statement.Statement#subBlockStream()} is the concatenation of
 *       the two, in order, including empty blocks; {@code subBlocks()} is its {@link Iterable} form.</li>
 *   <li>{@link io.codelaser.maddi.cst.api.statement.Statement#hasSubBlocks()} reports whether the
 *       statement has any nested block at all.</li>
 * </ul>
 *
 * <h2>Builders</h2>
 * Each statement nests a fluent {@code Builder} extending
 * {@link io.codelaser.maddi.cst.api.statement.Statement.Builder}. Mutators are annotated
 * {@link io.codelaser.maddi.annotation.Fluent} and return the builder; {@code build()} returns the concrete
 * statement type (a covariant narrowing of {@code Statement.Builder.build()}). The shared base supplies
 * {@code setLabel(String)}.
 *
 * <h2>Immutable copies: {@code withX(...)}</h2>
 * CST nodes are immutable. Methods of the form {@code withSomething(...)} (for example
 * {@code withBlocks(...)} or {@code withSource(...)}) return a new node with the one aspect changed,
 * leaving the receiver untouched.
 *
 * <h2>Two transformation lifecycles: {@code rewire} and {@code translate}</h2>
 * <ul>
 *   <li>{@link io.codelaser.maddi.cst.api.statement.Statement#rewire(io.codelaser.maddi.cst.api.info.InfoMap)}
 *       clones a node into a new {@code Info} graph, relinking references through the supplied
 *       {@link io.codelaser.maddi.cst.api.info.InfoMap}; structure is preserved one-to-one.</li>
 *   <li>{@link io.codelaser.maddi.cst.api.statement.Statement#translate(io.codelaser.maddi.cst.api.translate.TranslationMap)}
 *       performs a source-to-source rewrite and returns a {@code List<Statement>}, because a single
 *       statement may translate to zero, one, or several statements.</li>
 * </ul>
 */
package io.codelaser.maddi.cst.api.statement;
