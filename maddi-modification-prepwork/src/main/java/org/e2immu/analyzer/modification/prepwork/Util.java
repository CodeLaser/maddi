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

package org.e2immu.analyzer.modification.prepwork;

import java.util.regex.Matcher;

import static org.e2immu.analyzer.modification.prepwork.StatementIndex.*;

public class Util {

    public static boolean atSameLevel(String i0, String i1) {
        int d0 = i0.lastIndexOf(DOT);
        int d1 = i1.lastIndexOf(DOT);
        return d0 == -1 && d1 == -1
               || d0 > 0 && d1 > 0 && i0.substring(0, d0).equals(i1.substring(0, d1));
    }

    public static String endOf(String index) {
        int i = index.lastIndexOf('.');
        if (i < 0) return "~";
        return index.substring(0, i) + ".~";
    }

    /**
     * all
     *
     * @param scope an index designating the scope (of a variable)
     * @param index an index
     * @return true when the index is in the scope
     */
    public static boolean inScopeOf(String scope, String index) {
        if (BEFORE_METHOD.equals(scope)) return true;
        int dashScope = Math.max(scope.lastIndexOf(DASH), scope.lastIndexOf(PLUS));
        if (dashScope >= 0) {
            // 0-E -> in scope means starting with 0
            String sub = scope.substring(0, dashScope);
            return index.startsWith(sub);
        }
        int lastDotScope = scope.lastIndexOf(DOT);
        if (lastDotScope < 0) {
            // scope = 3 --> 3.0.0 ok, 3 ok, 4 ok
            return index.compareTo(scope) >= 0;
        }
        // scope = 3.0.2 --> 3.0.3 ok, 3.0.2.0.0 OK,  but 3.1.3 is not OK; 4 is not OK
        String withoutDot = scope.substring(0, lastDotScope);
        if (!index.startsWith(withoutDot)) return false;
        return index.compareTo(scope) >= 0;
    }


    // 3.0.0-E, +I
    public static String stage(String assignmentId) {
        Matcher m = StatementIndex.STAGE_PATTERN.matcher(assignmentId);
        if (m.matches()) return m.group(2);
        throw new UnsupportedOperationException();
    }

    public static String stripStage(String index) {
        Matcher m = StatementIndex.STAGE_PATTERN.matcher(index);
        if (m.matches()) return m.group(1);
        return index;
    }

    // add a character so that we're definitely beyond this index
    public static String beyond(String index) {
        return index + END;
    }
}
