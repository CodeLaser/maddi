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

package org.e2immu.language.cst.api.element;

/**
 * Represents any object that can be serialized using the cst-io system that can be used to determine
 * whether a source or set of sources has changed.
 * Typical implementation is an MD-5 hash.
 * <p>
 * Only relevant methods are <code>Object.equals()</code> and <code>Object.toString()</code>.
 */
public interface FingerPrint {

    boolean isNoFingerPrint();

}
