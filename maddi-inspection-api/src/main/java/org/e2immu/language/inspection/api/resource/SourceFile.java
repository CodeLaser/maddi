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

package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;

import java.net.URI;
import java.util.Objects;

import static org.e2immu.util.internal.util.StringUtil.replaceSlashDollar;

public record SourceFile(String path, URI uri, SourceSet sourceSet, FingerPrint fingerPrint) {

    public SourceFile {
        assert !(path.endsWith(".class") || path.endsWith(".java")) ||
               !path.startsWith("/");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SourceFile that)) return false;
        return Objects.equals(uri(), that.uri()) && Objects.equals(sourceSet(), that.sourceSet());
    }

    public String fullyQualifiedNameFromPath() {
        assert !path.startsWith("/");
        if (path.endsWith(".java")) {
            String stripDotJava = Resources.stripNameSuffix(path);
            return stripDotJava.replaceAll("[/$]", ".");
        }
        String stripDotClass = Resources.stripDotClass(path);
        if (stripDotClass.endsWith("$")) {
            // scala
            return stripDotClass.substring(0, stripDotClass.length() - 1).replaceAll("[/$]", ".") + ".object";
        }
        if (stripDotClass.endsWith("$class")) {
            // scala; keep it as is, ending in .class
            return stripDotClass.replaceAll("[/$]", ".");
        }
        int anon;
        if ((anon = stripDotClass.indexOf("$$anonfun")) > 0) {
            // scala
            String random = Integer.toString(Math.abs(stripDotClass.hashCode()));
            return stripDotClass.substring(0, anon).replaceAll("[/$]", ".") + "." + random;
        }
        return replaceSlashDollar(stripDotClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri(), sourceSet());
    }

    public static String ensureDotClass(String substring) {
        if (!substring.endsWith(".class")) {
            return substring + ".class";
        }
        return substring;
    }

    public String stripDotClass() {
        return Resources.stripDotClass(path);
    }

    public SourceFile withFingerprint(FingerPrint fingerPrint) {
        return new SourceFile(path, uri, sourceSet, fingerPrint);
    }

    public SourceFile withPath(String path) {
        return new SourceFile(path, uri, sourceSet, fingerPrint);
    }

    public SourceFile withPathURI(String path, URI uri) {
        return new SourceFile(path, uri, sourceSet, fingerPrint);
    }
}
