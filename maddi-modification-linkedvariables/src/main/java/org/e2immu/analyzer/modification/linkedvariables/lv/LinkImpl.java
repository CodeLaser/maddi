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

package org.e2immu.analyzer.modification.linkedvariables.lv;

import org.e2immu.analyzer.modification.prepwork.variable.Indices;
import org.e2immu.analyzer.modification.prepwork.variable.Link;

import java.util.Map;


public record LinkImpl(Indices to, boolean mutable) implements Link {
    public Link correctTo(Map<Indices, Indices> correctionMap) {
        return new LinkImpl(correctionMap.getOrDefault(to, to), mutable);
    }

    @Override
    public Link merge(Link l2) {
        return new LinkImpl(to.merge(l2.to()), mutable || l2.mutable());
    }

    @Override
    public Link prefixTheirs(int index) {
        return new LinkImpl(to.prefix(index), mutable);
    }

    @Override
    public String toString() {
        String toStr = to == null ? "NULL" : to.isAll() ? "*" : to.toString();
        return "LinkImpl[" + toStr + ",mutable=" + mutable + "]";
    }
}
