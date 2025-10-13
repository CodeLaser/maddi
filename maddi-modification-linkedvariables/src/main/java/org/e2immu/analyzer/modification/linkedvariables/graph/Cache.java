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

package org.e2immu.analyzer.modification.linkedvariables.graph;

import java.util.Arrays;
import java.util.function.Function;

public interface Cache {
    interface CacheElement {
        int savings();
    }

    record Hash(byte[] bytes) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            return obj instanceof Hash o && Arrays.equals(bytes, o.bytes);
        }

        @Override
        public int hashCode() {
            return bytes[0] + (bytes[1] << 8) + (bytes[2] << 16) + (bytes[3] << 24);
        }
    }

    Hash createHash(String string);

    CacheElement computeIfAbsent(Hash hash, Function<Hash, CacheElement> elementSupplier);
}
