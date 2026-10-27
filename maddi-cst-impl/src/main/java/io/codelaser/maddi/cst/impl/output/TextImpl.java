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

package io.codelaser.maddi.cst.impl.output;

import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.element.Text;
import io.codelaser.maddi.cst.api.output.element.TextBlockFormatting;
import io.codelaser.maddi.util.StringUtil;

public record TextImpl(String text, TextBlockFormatting textBlockFormatting) implements Text {

    public TextImpl {
        assert text != null && !text.isBlank();
    }

    public TextImpl(String text) {
        this(text, null);
    }

    @Override
    public String minimal() {
        return text;
    }

    @Override
    public int length(FormattingOptions options) {
        return text.length();
    }

    @Override
    public String write(FormattingOptions options) {
        return text;
    }

    @Override
    public String generateJavaForDebugging() {
        return ".add(new TextImpl(" + StringUtil.quote(text) + "))";
    }

}
