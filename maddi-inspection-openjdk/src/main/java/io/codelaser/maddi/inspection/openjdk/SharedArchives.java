package io.codelaser.maddi.inspection.openjdk;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One read-only archive file system per jar (and multi-release version), shared by all the javac file managers of one
 * inspector, and closed with them.
 * <p>
 * ⛔ WHY. Every source set gets a {@link StandardJavaFileManager} of its own, kept open for as long as its task can be
 * driven (see {@code JavaInspectorImpl.openFileManagers}), and javac opens a {@code ZipFileSystem} per archive per
 * file manager. On laser1's refactor server (maddi-audit, 244 source sets, 992 class path parts) one model held
 * 10,479 of them for 908 distinct jars, 12.8 million index nodes, about 0.6 GB. Shared, it is one per jar.
 * <p>
 * javac takes the jar provider from the {@code FSInfo} in its file manager's context; {@link SharedArchivesJavac}
 * builds the file manager as {@code JavacTool.getStandardFileManager} does, with an {@code FSInfo} whose provider
 * hands out the shared file systems. That needs {@code com.sun.tools.javac.file}, which a launcher must export
 * ({@code --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED}); without it the file managers are made
 * as before, one archive file system each, and this says so once.
 */
final class SharedArchives implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedArchives.class);
    private static final String PACKAGE = "com.sun.tools.javac.file";
    private static final boolean AVAILABLE = computeAvailable();

    private final SharedArchivesJavac.Cache cache = AVAILABLE ? new SharedArchivesJavac.Cache() : null;

    private static boolean computeAvailable() {
        if (Boolean.getBoolean("maddi.unsharedArchives")) return false;
        Module javac = ModuleLayer.boot().findModule("jdk.compiler").orElse(null);
        boolean exported = javac != null && javac.isExported(PACKAGE, SharedArchives.class.getModule());
        if (!exported) {
            LOGGER.warn("{} is not exported to maddi: every javac file manager opens its own copy of every jar."
                        + " Add --add-exports jdk.compiler/{}=ALL-UNNAMED to share one per jar.", PACKAGE, PACKAGE);
        }
        return exported;
    }

    static boolean available() {
        return AVAILABLE;
    }

    StandardJavaFileManager newFileManager(JavaCompiler javaCompiler,
                                           DiagnosticListener<? super JavaFileObject> diagnostics,
                                           Locale locale, Charset charset) {
        if (cache == null) return javaCompiler.getStandardFileManager(diagnostics, locale, charset);
        return SharedArchivesJavac.newFileManager(cache, diagnostics, locale, charset);
    }

    /** The number of archive file systems open, for tests and diagnostics; -1 when not sharing. */
    int openArchives() {
        return cache == null ? -1 : cache.size();
    }

    @Override
    public void close() throws IOException {
        if (cache != null) cache.close();
    }
}
