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

package org.e2immu.language.java.openjdk;

/**
 * A referenced type, constructor or method could not be resolved during the openjdk scan — typically because
 * maddi runs on a deliberately partial classpath, so a library symbol simply is not present (javac hands us an
 * error/NIL symbol). It extends {@link UnsupportedOperationException} — the historical throw type at these sites,
 * so existing catch clauses are unaffected — but it is <em>typed</em> so the compilation-unit-level fault
 * isolation in {@link ScanCompilationUnits} can classify it as a tolerable warning rather than a hard error.
 */
public class UnresolvedSymbolException extends UnsupportedOperationException {
    public UnresolvedSymbolException(String message) {
        super(message);
    }
}
