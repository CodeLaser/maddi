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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.info.Access;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.element.Keyword;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;

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

    /**
     * As {@link #visibility(Access)}, for a member of {@code owner}. Java lets a class read the private members of the
     * classes nested in it, Kotlin does not: a private member of a nested Java class is {@code internal}.
     */
    public static Optional<Keyword> visibility(Access access, TypeInfo owner) {
        if (access != null && access.isPrivate() && !owner.isPrimaryType() && !KotlinTypePrinter.fromKotlinSource(owner)) {
            return Optional.of(KotlinKeyword.INTERNAL);
        }
        return visibility(access);
    }
}
