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

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public interface Resources {

    static String stripDotClass(String path) {
        if (path.endsWith(".class")) return path.substring(0, path.length() - 6);
        return path;
    }

    static String stripNameSuffix(String name) {
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? name : name.substring(0, lastDot);
    }

    void addDirectoryFromFileSystem(File base, SourceSet sourceSet);

    String pathToFqn(String name);

    SourceFile sourceFileOfType(TypeInfo subType, String s);

    record JarSize(int entries, int bytes) {
    }

    Map<String, Resources.JarSize> getJarSizes();

    void visit(String[] prefix, BiConsumer<String[], List<SourceFile>> visitor);

    List<String[]> expandPaths(String path);

    void expandPaths(String path, String extension, BiConsumer<String[], List<SourceFile>> visitor);

    void expandLeaves(String path, String extension, BiConsumer<String[], List<SourceFile>> visitor);

    List<SourceFile> expandURLs(String extension);

    URL findJarInClassPath(String prefix) throws IOException;

    void addTestProtocol(SourceFile testProtocol);

    int addJar(SourceFile jarSourceFile) throws IOException;

    int addJmod(SourceFile jmodSourceFile) throws IOException;

    /**
     * Index a JDK module's classes from the runtime image (the {@code jrt} filesystem, backed by {@code lib/modules})
     * rather than from a {@code .jmod} file, so the analyzer runs on JDKs that ship no {@code jmods/} directory
     * (e.g. Eclipse Temurin). Only the running JDK's image is supported; {@code moduleSourceFile.path()} is the
     * module name.
     */
    int addModuleFromRuntimeImage(SourceFile moduleSourceFile) throws IOException;

    SourceFile fqnToPath(String fqn, String s);

    byte[] loadBytes(URI uri);
}
