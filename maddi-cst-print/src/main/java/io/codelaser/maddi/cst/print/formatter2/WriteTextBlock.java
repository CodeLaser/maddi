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

package io.codelaser.maddi.cst.print.formatter2;

import io.codelaser.maddi.cst.api.output.element.TextBlockFormatting;

public class WriteTextBlock {
    /*
    starts with """, ends with """, indents according to rules in TextBlockFormatting and given indentation in
    number of spaces.
     */
    public static String write(int indentIn, String string, TextBlockFormatting textBlockFormatting) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"\"\"\n");
        int indent = textBlockFormatting.optOutWhiteSpaceStripping() ? 0 : indentIn;
        sb.append(" ".repeat(indent));
        char[] chars = string.toCharArray();
        int i = 0;
        while (i < chars.length) {
            char c = chars[i];
            if (textBlockFormatting.lineBreaks().contains(i)) {
                sb.append('\\');
                sb.append('\n');
                sb.append(" ".repeat(indent));
                sb.append(c);
            } else if (c == '\n') {
                sb.append('\n');
                sb.append(" ".repeat(indent));
            } else {
                sb.append(c);
            }
            ++i;
        }
        sb.append("\"\"\"");
        return sb.toString();
    }
}
