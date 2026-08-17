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

import org.e2immu.language.cst.api.output.OutputElement;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static int charactersUntilAndExcludingLastNewline(String string) {
        int nl = string.lastIndexOf('\n');
        if (nl < 0) {
            return string.length();
        }
        return string.length() - (nl + 1);
    }

    public static List<OutputElement> removeComments(List<OutputElement> list) {
        List<OutputElement> elements = new ArrayList<>(list.size());
        int commentDepth = 0;
        boolean dropUntilNewline = false;
        for (OutputElement outputElement : list) {
            if (outputElement.isLeftBlockComment()) {
                commentDepth++;
            } else if (outputElement.isRightBlockComment()) {
                commentDepth--;
            } else if (commentDepth == 0) {
                if (outputElement.isSingleLineComment()) {
                    dropUntilNewline = true;
                } else if (outputElement.isNewLine()) {
                    dropUntilNewline = false;
                }
                if (!dropUntilNewline) {
                    elements.add(outputElement);
                }
            }
        }
        return elements;
    }
}
