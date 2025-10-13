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

package org.e2immu.language.cst.print.formatter2;

import org.e2immu.language.cst.api.output.element.TextBlockFormatting;

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
