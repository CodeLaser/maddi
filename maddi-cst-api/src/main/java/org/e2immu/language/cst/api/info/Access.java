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

package org.e2immu.language.cst.api.info;

public interface Access {

    /*
    returns values starting from 0, with 0 being the most restrictive.
    In the case of Java, this is private==0, package==1, protected==2, public==3
     */
    int level();

    Access combine(Access other);

    Access max(Access other);

    default boolean ge(Access other) {
        return level() >= other.level();
    }

    default boolean le(Access other) {
        return level() <= other.level();
    }

    boolean isPublic();

    boolean isPrivate();

    boolean isProtected();

    boolean isPackage();
}
