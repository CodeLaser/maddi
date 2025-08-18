package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.e2immu.util.internal.util.StringUtil.replaceSlashDollar;

public class ResourcesImpl implements Resources {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesImpl.class);

    static class ResourceAccessException extends RuntimeException {
        public ResourceAccessException(String msg) {
            super(msg);
        }
    }

    private final Trie<SourceFile> data = new Trie<>();
    private final Map<String, JarSize> jarSizes = new HashMap<>();

    private final Path workingDirectory;

    public ResourcesImpl(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    private URI relativeToAbsolute(URI uri) {
        if ("file".equals(uri.getScheme())) {
            String path = uri.getSchemeSpecificPart();
            if (!path.startsWith("/")) {
                Path newPath = workingDirectory.resolve(Path.of(path));
                return newPath.toUri();
            }
        }
        return uri;
    }

    @Override
    public Map<String, JarSize> getJarSizes() {
        return jarSizes;
    }

    @Override
    public void visit(String[] prefix, BiConsumer<String[], List<SourceFile>> visitor) {
        data.visit(prefix, visitor);
    }

    @Override
    public List<String[]> expandPaths(String path) {
        List<String[]> expansions = new LinkedList<>();
        String[] prefix = path.split("\\.");
        data.visit(prefix, (s, list) -> expansions.add(s));
        return expansions;
    }

    @Override
    public void expandPaths(String path, String extension, BiConsumer<String[], List<SourceFile>> visitor) {
        String[] prefix = path.split("\\.");
        data.visit(prefix, (s, list) -> {
            if (s.length > 0 && s[s.length - 1].endsWith(extension)) {
                visitor.accept(s, list);
            }
        });
    }

    @Override
    public void expandLeaves(String path, String extension, BiConsumer<String[], List<SourceFile>> visitor) {
        String[] prefix = path.split("\\.");
        data.visitLeaves(prefix, (s, list) -> {
            if (s.length > 0 && s[s.length - 1].endsWith(extension)) {
                visitor.accept(s, list);
            }
        });
    }

    @Override
    public List<SourceFile> expandURLs(String extension) {
        List<SourceFile> expansions = new LinkedList<>();
        data.visit(new String[0], (s, list) -> {
            if (s[s.length - 1].endsWith(extension)) {
                expansions.addAll(list);
            }
        });
        return expansions;
    }


    /**
     * Mostly used in tests: add a jar from the classpath of the test.
     *
     * @param prefix adds the jars that contain the package denoted by the prefix
     * @return a map containing the number of entries per jar
     * @throws IOException when the jar handling fails somehow
     */
    @Override
    public URL findJarInClassPath(String prefix) throws IOException {
        Enumeration<URL> roots = getClass().getClassLoader().getResources(prefix);
        if (roots.hasMoreElements()) {
            URL url = roots.nextElement();
            String urlString = url.toString();
            int bangSlash = urlString.indexOf("!/");
            String strippedUrlString = urlString.substring(0, bangSlash + 2);
            return new URL(strippedUrlString);
        }
        return null;
    }

    private static final Pattern JAR_FILE = Pattern.compile("/([^/]+\\.jar)");

    @Override
    public void addTestProtocol(SourceFile testProtocol) {
        String s = testProtocol.uri().toString();
        String fullyQualifiedName = s.substring(s.indexOf(':') + 1);
        String[] split = fullyQualifiedName.split("\\.");
        split[split.length - 1] = split[split.length - 1] + ".java";
        data.add(split, testProtocol);
    }

    /**
     * Add a jar to the trie
     *
     * @param jarSourceFile must contain a correct JAR url, as described in the class JarURLConnection
     * @return the number of entries added to the classpath
     * @throws IOException when jar handling fails somehow.
     */
    @Override
    public int addJar(SourceFile jarSourceFile) throws IOException {
        URL url = relativeToAbsolute(jarSourceFile.uri()).toURL();
        JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
        JarFile jarFile = jarConnection.getJarFile();
        AtomicInteger entries = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        jarFile.stream().forEach(je -> {
            String realName = je.getRealName();
            // let's exclude XML files, etc., anything not Java-related
            if (realName.endsWith(".class") || realName.endsWith(".java")) {
                LOGGER.trace("Adding {}", realName);
                String[] split = je.getRealName().split("/");
                try {
                    URI fullUrl = new URL(url, je.getRealName()).toURI();
                    data.add(split, jarSourceFile.withURI(fullUrl));
                    entries.incrementAndGet();
                } catch (MalformedURLException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        String jarName;
        Matcher m = JAR_FILE.matcher(jarFile.getName());
        if (m.find()) {
            jarName = m.group(1);
        } else {
            jarName = url.toString();
        }
        jarSizes.put(jarName, new JarSize(entries.get(), 0));
        if (errors.get() > 0) {
            throw new IOException("Got " + errors.get() + " errors while adding from jar to classpath");
        }
        return entries.get();
    }

    public static URL constructJModURL(String part, Path altJREDirectory) throws MalformedURLException {
        if (part.startsWith("/")) {
            return new URL("jar:file:" + part + "!/");
        }
        String jre;
        if (altJREDirectory == null) {
            jre = System.getProperty("java.home");
        } else {
            jre = altJREDirectory.toString();
        }
        if (!jre.endsWith("/")) jre = jre + "/";
        return new URL("jar:file:" + jre + "jmods/" + part + ".jmod!/");
    }


    @Override
    public int addJmod(SourceFile jmodSourceFile) throws IOException {
        URL jmodUrl = jmodSourceFile.uri().toURL();
        JarURLConnection jarConnection = (JarURLConnection) jmodUrl.openConnection();
        JarFile jarFile = jarConnection.getJarFile();
        AtomicInteger entries = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        jarFile.stream()
                .filter(je -> je.getRealName().startsWith("classes/"))
                .forEach(je -> {
                    String realName = je.getRealName().substring("classes/".length());
                    LOGGER.trace("Adding {}", realName);
                    String[] split = realName.split("/");
                    try {
                        URL fullUrl = new URL(jmodUrl, je.getRealName());
                        data.add(split, jmodSourceFile.withURI(fullUrl.toURI()));
                        entries.incrementAndGet();
                    } catch (MalformedURLException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });
        if (errors.get() > 0) {
            throw new IOException("Got " + errors.get() + " errors while adding from jar to classpath");
        }
        return entries.get();
    }

    @Override
    public SourceFile sourceFileOfType(TypeInfo subType, String suffix) {
        if (subType.compilationUnitOrEnclosingType().isLeft()) {
            String path = subType.fullyQualifiedName().replace(".", "/") + suffix;
            CompilationUnit cu = subType.compilationUnit();
            return new SourceFile(path, cu.uri(), cu.sourceSet(), cu.fingerPrintOrNull());
        }
        SourceFile parentSourceFile = sourceFileOfType(subType.compilationUnitOrEnclosingType().getRight(), suffix);
        String p = parentSourceFile.path();
        String newPath = p.substring(0, p.length() - suffix.length()) + "$" + subType.simpleName() + suffix;
        return parentSourceFile.withPath(newPath);
    }

    @Override
    public SourceFile fqnToPath(String fqn, String extension) {
        String[] splitDot = fqn.split("\\.");
        for (int i = 1; i < splitDot.length; i++) {
            String[] parts = new String[i + 1];
            System.arraycopy(splitDot, 0, parts, 0, i);
            parts[i] = splitDot[i];
            for (int j = i + 1; j < splitDot.length; j++) {
                parts[i] += "$" + splitDot[j];
            }
            parts[i] += extension;
            List<SourceFile> sourceFiles = data.get(parts);
            if (sourceFiles != null && !sourceFiles.isEmpty()) {
                SourceFile sf0 = sourceFiles.getFirst();
                return sf0.withPath(String.join("/", parts));
            }
        }
        LOGGER.debug("Cannot find {} with extension {} in classpath", fqn, extension);
        return null;
    }

    // could have been static, but allows for overrides
    @Override
    public String pathToFqn(String path) {
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
    public byte[] loadBytes(String path) {
        String[] prefix = path.split("/");
        List<SourceFile> sourceFiles = data.get(prefix);
        if (sourceFiles != null) {
            for (SourceFile sourceFile : sourceFiles) {
                URI absolute = relativeToAbsolute(sourceFile.uri());
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                     InputStream inputStream = absolute.toURL().openStream()) {
                    inputStream.transferTo(byteArrayOutputStream);
                    return byteArrayOutputStream.toByteArray();
                } catch (IOException e) {
                    throw new ResourceAccessException("URI = " + absolute + ", from " + workingDirectory
                                                      + " and " + sourceFile.uri() + ", Cannot read? " + e.getMessage());
                }
            }
        }
        LOGGER.debug("{} not found in class path", path);
        return null;
    }

    @Override
    public void addDirectoryFromFileSystem(File base, SourceSet sourceSet) {
        File file = new File("");
        try {
            recursivelyAddFiles(base, file, sourceSet);
        } catch (MalformedURLException e) {
            throw new UnsupportedOperationException("??");
        }
    }

    private void recursivelyAddFiles(File baseDirectory, File dirRelativeToBase, SourceSet sourceSet)
            throws MalformedURLException {
        File dir = new File(baseDirectory, dirRelativeToBase.getPath());
        if (dir.isDirectory()) {
            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) { // 1.0.1.0.0
                    recursivelyAddFiles(baseDirectory, new File(dirRelativeToBase, subDir.getName()), sourceSet);
                }
            }
            File[] files = dir.listFiles(f -> !f.isDirectory());
            if (files != null) { // 1.0.3
                String pathString = dirRelativeToBase.getPath(); // 1.0.3.0.0
                String[] packageParts =
                        pathString.isEmpty() ? new String[0] :
                                (pathString.startsWith("/") ? pathString.substring(1) : pathString)
                                        .split("/");
                for (File file : files) {
                    String name = file.getName();
                    String packageName = String.join(".", packageParts);
                    LOGGER.debug("File {} in package {}", name, packageName);
                    if (sourceSet.acceptSource(packageName, Resources.stripNameSuffix(name))) {
                        data.add(Stream.concat(Arrays.stream(packageParts), Stream.of(name)).toArray(String[]::new),
                                new SourceFile(file.getPath(), file.toURI(), sourceSet, null));
                    }
                }
            }
        }
    }
}
