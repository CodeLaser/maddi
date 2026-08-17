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

package org.e2immu.language.inspection.impl.parser;

/**
 * Thrown by type resolution when a referenced type is not found on the (partial) classpath. Distinguished from
 * other resolution failures so the resolver surfaces it as a tolerable <em>warning</em> rather than a fatal error
 * — maddi analyzes with a deliberately partial classpath, so unresolved library types are expected, matching the
 * openjdk front-end (which reports the same as a Summary warning). Extends {@link UnsupportedOperationException}
 * so existing catch sites are unaffected.
 */
public class UnresolvedTypeException extends UnsupportedOperationException {
    public UnresolvedTypeException(String typeNameOrFqn) {
        super("Cannot find type " + typeNameOrFqn);
    }
}
