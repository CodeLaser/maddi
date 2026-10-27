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

import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputElement;

public interface Space extends OutputElement {
    ElementarySpace elementarySpace(FormattingOptions options);

    ElementarySpace nice();

    Split split();

    /**
     * True when a line break is not allowed here even where the element before allows one: the formatter withdraws
     * the split point at this position. For a language that ends a statement at a newline in front of an operator.
     */
    default boolean withdrawsSplitBefore() {
        return false;
    }
}
