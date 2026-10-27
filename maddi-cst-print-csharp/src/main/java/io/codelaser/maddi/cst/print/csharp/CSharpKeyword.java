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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.output.element.Keyword;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;

/** C# keywords not present in the (Java-oriented) {@link KeywordImpl}. */
public final class CSharpKeyword {
    public static final Keyword NAMESPACE = new KeywordImpl("namespace");
    public static final Keyword USING = new KeywordImpl("using");
    public static final Keyword FOREACH = new KeywordImpl("foreach");
    public static final Keyword IN = new KeywordImpl("in");
    public static final Keyword IS = new KeywordImpl("is");
    public static final Keyword NOT = new KeywordImpl("not");
    public static final Keyword LOCK = new KeywordImpl("lock");
    public static final Keyword BASE = new KeywordImpl("base");
    public static final Keyword WHEN = new KeywordImpl("when");

    private CSharpKeyword() {
    }
}
