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

import org.e2immu.language.cst.api.info.Access;
import org.e2immu.language.cst.api.output.element.Keyword;
import org.e2immu.language.cst.impl.output.KeywordImpl;

import java.util.Optional;

/** Maps CST access/modifiers to Kotlin keywords (public and final are the Kotlin defaults, hence omitted). */
public class KotlinModifiers {

    /** The Kotlin visibility keyword, or empty for `public` (the default) and package-private (no equivalent). */
    public static Optional<Keyword> visibility(Access access) {
        if (access == null || access.isPublic() || access.isPackage()) return Optional.empty();
        if (access.isPrivate()) return Optional.of(KeywordImpl.PRIVATE);
        if (access.isProtected()) return Optional.of(KeywordImpl.PROTECTED);
        if (access.isInternal()) return Optional.of(KotlinKeyword.INTERNAL);
        return Optional.empty();
    }
}
