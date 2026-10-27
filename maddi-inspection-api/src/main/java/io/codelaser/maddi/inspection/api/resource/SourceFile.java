/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.api.resource;

import io.codelaser.maddi.cst.api.element.FingerPrint;
import io.codelaser.maddi.cst.api.element.SourceSet;

import java.net.URI;
import java.util.Objects;

import static io.codelaser.maddi.util.StringUtil.replaceSlashDollar;

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
