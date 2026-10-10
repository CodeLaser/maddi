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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.output.element.Keyword;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;

/** C# keywords not present in the (Java-oriented) {@link KeywordImpl}. */
public final class CSharpKeyword {
    public static final Keyword NAMESPACE = new KeywordImpl("namespace");
    public static final Keyword USING = new KeywordImpl("using");
    public static final Keyword READONLY = new KeywordImpl("readonly");
    public static final Keyword CONST = new KeywordImpl("const");
    public static final Keyword VIRTUAL = new KeywordImpl("virtual");
    public static final Keyword OVERRIDE = new KeywordImpl("override");
    public static final Keyword FOREACH = new KeywordImpl("foreach");
    public static final Keyword IN = new KeywordImpl("in");
    public static final Keyword IS = new KeywordImpl("is");
    public static final Keyword NOT = new KeywordImpl("not");
    public static final Keyword LOCK = new KeywordImpl("lock");
    public static final Keyword BASE = new KeywordImpl("base");
    public static final Keyword PARAMS = new KeywordImpl("params");
    public static final Keyword WHERE = new KeywordImpl("where");
    public static final Keyword WHEN = new KeywordImpl("when");

    private CSharpKeyword() {
    }
}
