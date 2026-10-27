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

package io.codelaser.maddi.cst.api.expression;

import io.codelaser.maddi.cst.api.output.element.TextBlockFormatting;

/**
 * A text block (triple-quoted multi-line string literal). It is a {@link StringConstant} whose
 * {@code constant()} is the resolved string value; {@link #textBlockFormatting()} preserves the original
 * formatting (indentation, line breaks) for faithful printing.
 */
public interface TextBlock extends StringConstant {

    /**
     * @return the original formatting details, used to reproduce the literal on output.
     */
    TextBlockFormatting textBlockFormatting();

}
