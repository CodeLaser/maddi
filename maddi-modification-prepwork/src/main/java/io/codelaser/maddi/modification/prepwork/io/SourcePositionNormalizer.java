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

package org.e2immu.analyzer.modification.prepwork.io;

import java.util.regex.Pattern;

/**
 * Erases {@code compact2} source coordinates from the dump. {@code ExpressionCodec.encodeExpression} embeds every
 * expression's source as element [1] of its encoding, a quoted {@code "beginLine-beginPos:endLine-endPos"} string
 * (e.g. {@code "4-23:4-26"}, see {@code Source.compact2()}). Those coordinates leak into link summaries, so a purely
 * cosmetic, line-shifting edit (a comment added above, a reformat) changes them and would flip the fingerprint of an
 * otherwise-unchanged type. No analysis reads a callee's source coordinates, so blanking them is sound and only
 * widens the invariance.
 * <p>
 * String-level and anchored on the distinctive four-number, quote-delimited shape, which does not collide with
 * statement indices ({@code "1.0.0"}), fqns, or link markers. Blanks to {@code ""} (a stable placeholder) rather
 * than deleting, so the surrounding list structure is untouched.
 */
public class SourcePositionNormalizer implements FingerprintNormalizer {

    // "beginLine-beginPos:endLine-endPos", quote-delimited; e.g. "4-23:4-26". noSource() yields "0-0:0-0".
    private static final Pattern COMPACT2 = Pattern.compile("\"\\d+-\\d+:\\d+-\\d+\"");

    @Override
    public String name() {
        return "source-positions";
    }

    @Override
    public String normalize(String encoded) {
        return COMPACT2.matcher(encoded).replaceAll("\"\"");
    }
}
