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

package io.codelaser.maddi.cst.api.output;

public interface OutputElement {

    default boolean isLeftBlockComment() {
        return false;
    }

    default boolean isNewLine() {
        return false;
    }

    default boolean isRightBlockComment() {
        return false;
    }

    default boolean isSingleLineComment() { return false; }

    String minimal();

    String write(FormattingOptions options);

    default int length(FormattingOptions options) {
        return write(options).length();
    }

    /**
     * used for testing only
     *
     * @return Java-code that can be pasted into a test
     */
    default String generateJavaForDebugging() { throw new UnsupportedOperationException(); }
}
