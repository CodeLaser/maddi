/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.language.cst.impl.output;


import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.element.Qualifier;
import org.e2immu.language.cst.api.output.element.ThisName;
import org.e2immu.util.internal.util.StringUtil;

public record ThisNameImpl(boolean isSuper, Qualifier qualifier, boolean qualifierRequired) implements ThisName {

    private String thisOrSuper() {
        return (isSuper ? KeywordImpl.SUPER : KeywordImpl.THIS).minimal();
    }

    @Override
    public String minimal() {
        return qualifierRequired ? qualifier.minimal() + "." + thisOrSuper() : thisOrSuper();
    }

    @Override
    public String fullyQualifiedName() {
        return qualifier.fullyQualifiedName() + "." + thisOrSuper();
    }

    @Override
    public String write(FormattingOptions options) {
        return qualifierRequired ? qualifier.write(options) + "." + thisOrSuper() : thisOrSuper();
    }

    @Override
    public String generateJavaForDebugging() {
        String q = qualifier == null ? "null" : StringUtil.quote(qualifier.minimal());
        return ".add(new ThisNameImpl(" + isSuper + ", " + q + ", " + qualifierRequired + "))";
    }
}
