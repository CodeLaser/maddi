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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.output.element.Keyword;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;

/** Kotlin-specific keywords not present in the (Java-oriented) {@link KeywordImpl}. */
public class KotlinKeyword {
    public static final Keyword FUN = new KeywordImpl("fun");
    public static final Keyword VAL = new KeywordImpl("val");
    public static final Keyword VAR = new KeywordImpl("var");
    public static final Keyword OBJECT = new KeywordImpl("object");
    public static final Keyword OVERRIDE = new KeywordImpl("override");
    public static final Keyword OPEN = new KeywordImpl("open");
    public static final Keyword CONSTRUCTOR = new KeywordImpl("constructor");
    public static final Keyword COMPANION = new KeywordImpl("companion");
    public static final Keyword INTERNAL = new KeywordImpl("internal");
    public static final Keyword AS = new KeywordImpl("as");
    public static final Keyword IS = new KeywordImpl("is");
    public static final Keyword IF = new KeywordImpl("if");
    public static final Keyword ELSE = new KeywordImpl("else");
    public static final Keyword RETURN = new KeywordImpl("return");
    public static final Keyword WHILE = new KeywordImpl("while");
    public static final Keyword DO = new KeywordImpl("do");
    public static final Keyword FOR = new KeywordImpl("for");
    public static final Keyword IN = new KeywordImpl("in");
    public static final Keyword THROW = new KeywordImpl("throw");
    public static final Keyword WHEN = new KeywordImpl("when");
    public static final Keyword TRY = new KeywordImpl("try");
    public static final Keyword CATCH = new KeywordImpl("catch");
    public static final Keyword FINALLY = new KeywordImpl("finally");
    public static final Keyword ELSE_ARROW = new KeywordImpl("else"); // the `else` arm of a `when`
    public static final Keyword DATA = new KeywordImpl("data");
}
