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


package io.codelaser.maddi.cst.api.output.element;


/**
 * Marks a blank line in the source in front of the element that follows: a statement, a member, the imports or a
 * type declaration. A printer adds it when the sources of two neighbours say so; the formatter writes it only when
 * {@link io.codelaser.maddi.cst.api.output.FormattingOptions#blankLinesFromSource()} is set, and then writes no
 * blank line of its own (CodeLaser/maddi#49). It is empty otherwise.
 * <p>
 * It is a {@link Space}: no space, never a split. A client that walks an output and knows nothing of blank lines
 * (a tokenizer) treats it as the empty space it is to them, rather than meeting an element type it does not know.
 * A formatter that does know must test for it BEFORE it tests for a space.
 */
public interface BlankLine extends Space {
}
