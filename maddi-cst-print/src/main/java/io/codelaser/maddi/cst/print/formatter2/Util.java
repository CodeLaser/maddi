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

import io.codelaser.maddi.cst.api.output.OutputElement;

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
