package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.io.File;
import java.io.IOException;
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

    byte[] loadBytes(String path);
}
