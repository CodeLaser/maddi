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

package org.e2immu.language.cst.print.kotlin;

import org.e2immu.language.cst.api.output.element.Keyword;
import org.e2immu.language.cst.impl.output.KeywordImpl;

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
}
