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
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.element.Keyword;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;

import java.util.Optional;

/** Maps CST access/modifiers to Kotlin keywords (public and final are the Kotlin defaults, hence omitted). */
public class KotlinModifiers {

    /**
     * The Kotlin visibility keyword, or empty for `public` (the default). Package-private has no equivalent, and neither
     * has Java's {@code protected}, which is package-private AND visible in subclasses: Kotlin's {@code protected}
     * refuses a sibling in the package ({@code first.varDefinitions} from CatchAllStatement) and a companion calling a
     * protected constructor. Translating Java, both print as public; only an override of a library's protected member
     * stays protected. A Kotlin declaration's {@code protected} is Kotlin's own.
     */
    public static Optional<Keyword> visibility(Access access) {
        if (access == null || access.isPublic() || access.isPackage()) return Optional.empty();
        if (access.isProtected()) return KotlinContext.translatingJava() ? Optional.empty() : Optional.of(KeywordImpl.PROTECTED);
        if (access.isPrivate()) return Optional.of(KeywordImpl.PRIVATE);
        if (access.isInternal()) return Optional.of(KotlinKeyword.INTERNAL);
        return Optional.empty();
    }

    /** As {@link #visibility(Access, TypeInfo)}; protected, for an override of a protected member Kotlin does not print. */
    public static Optional<Keyword> visibility(MethodInfo methodInfo, TypeInfo owner) {
        if (methodInfo.access() != null && methodInfo.access().isProtected()
            && methodInfo.overrides().stream().anyMatch(m -> !KotlinNullability.translated(m.typeInfo())
                                                             && m.access() != null && m.access().isProtected())) {
            return Optional.of(KeywordImpl.PROTECTED);
        }
        if (KotlinContext.translatingJava() && methodInfo.access() != null && methodInfo.access().isPublic()
            && methodInfo.overrides().stream().anyMatch(m -> m.access() != null && m.access().isProtected()
                                                             && !KotlinNullability.translated(m.typeInfo()))) {
            // a Kotlin override without a modifier inherits the member's visibility: Cloneable.clone() is protected
            return Optional.of(KeywordImpl.PUBLIC);
        }
        return visibility(methodInfo.access(), owner, methodInfo);
    }

    /**
     * As {@link #visibility(Access)}, for a member of {@code owner}. Java lets a class read the private members of the
     * classes nested in it, Kotlin does not: a private member of a nested Java class is {@code internal}.
     */
    public static Optional<Keyword> visibility(Access access, TypeInfo owner) {
        return visibility(access, owner, null);
    }

    /** As {@link #visibility(Access, TypeInfo)}, reporting a protected {@code subject} that prints as public. */
    public static Optional<Keyword> visibility(Access access, TypeInfo owner, io.codelaser.maddi.cst.api.element.Element subject) {
        reportProtected(access, subject);
        if (access != null && access.isPrivate() && !owner.isPrimaryType() && !KotlinTypePrinter.fromKotlinSource(owner)) {
            return Optional.of(KotlinKeyword.INTERNAL);
        }
        return visibility(access);
    }

    /** As {@link #visibility(Access)}, reporting a protected {@code subject} that prints as public. */
    static Optional<Keyword> visibility(Access access, io.codelaser.maddi.cst.api.element.Element subject) {
        reportProtected(access, subject);
        return visibility(access);
    }

    private static void reportProtected(Access access, io.codelaser.maddi.cst.api.element.Element subject) {
        if (subject != null && access != null && access.isProtected() && KotlinContext.translatingJava()) {
            KotlinContext.message(KotlinPrintMessage.Code.PROTECTED_AS_PUBLIC, subject, KotlinContext.describe(subject));
        }
    }
}
