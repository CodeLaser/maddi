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

package org.e2immu.language.inspection.integration.java.example;

import org.e2immu.language.inspection.integration.java.importhelper.a.Resource;
import org.e2immu.language.inspection.integration.java.importhelper.a.Resources;

import static org.e2immu.language.inspection.integration.java.example.Annotations_2.XX;

// this file is here only to show that we can use a static import for XX!
// see TestAnnotations, INPUT3
@Resources({
        @Resource(name = XX, lookup = "yy", type = java.util.TreeMap.class),
        @Resource(name = Annotations_2.ZZ, type = Integer.class)
})
public class Annotations_2 {
    static final String XX = "xx";
    static final String ZZ = "zz";
}
