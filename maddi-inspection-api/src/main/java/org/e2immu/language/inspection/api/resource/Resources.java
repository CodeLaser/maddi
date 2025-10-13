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

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;

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

    SourceFile fqnToPath(String fqn, String s);

    byte[] loadBytes(URI uri);
}
