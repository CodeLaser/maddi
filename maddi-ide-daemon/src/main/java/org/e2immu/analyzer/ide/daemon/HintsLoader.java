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

package org.e2immu.analyzer.ide.daemon;

import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.analyzer.modification.prepwork.io.PrepWorkCodec;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Preloads maddi's bundled analysis hints (analyzed annotated-API results) so the daemon knows the
 * modification/immutability/independence of JDK and common-library types instead of falling back to
 * shallow defaults. The hints ship in {@code maddi-aapi-archive}, on the daemon's own classpath.
 * <p>
 * Two sets are loaded: the JDK hints (loose {@code jdk/*.json}) and {@code libs.jar} (slf4j, junit).
 * Works whether the archive is on the classpath as a jar (installed daemon) or as a resources
 * directory (running from the repo). Best-effort: a failure is logged and analysis proceeds.
 */
public class HintsLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(HintsLoader.class);

    private static final String BASE = "/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles";
    private static final String LIBS_JAR_RESOURCE = BASE + "/libs.jar";
    private static final String JDK_ENTRY_PREFIX = BASE.substring(1) + "/jdk/"; // no leading slash, for jar entries

    /**
     * JDK packages to preload before decoding their hints. The hints reference overloaded methods by a
     * positional index into the type's method list, so those types must already be parsed (with faithful
     * descriptors) for the index to resolve — otherwise the codec fails with "ambiguous method … stale index".
     * Mirrors the analyzer's own test harness.
     */
    private static final List<String> PRELOAD_PACKAGES = List.of(
            "java.base::java.util.",
            "java.base::java.io",
            "java.base::java.net",
            "java.base::java.nio.",
            "java.base::java.time.",
            "java.base::java.security",
            "java.base::java.lang.annotation",
            "java.base::java.lang.reflect",
            "java.base::java.lang.constant",
            "org.e2immu.annotation.");

    /**
     * Register the JDK packages to eagerly parse. MUST be called BEFORE {@code inspector.initialize(...)} so
     * the types are parsed (with faithful descriptors) and the hint codec can resolve method indices.
     */
    public void preload(JavaInspector inspector) {
        for (String pkg : PRELOAD_PACKAGES) {
            try {
                inspector.preload(pkg);
            } catch (Throwable t) {
                LOGGER.warn("Could not preload {} before hints: {}", pkg, t.toString());
            }
        }
    }

    /** Load the hint files. Call AFTER initialize (and after {@link #preload}). @return primary types loaded. */
    public int loadHints(Runtime runtime, SourceSet sourceSetOfRequest) {
        LoadAnalysisResults loader = new LoadAnalysisResults(runtime, sourceSetOfRequest);
        Codec codec = new PrepWorkCodec(runtime, sourceSetOfRequest).codec();
        int count = 0;
        count += guarded("JDK", () -> loadJdk(loader, codec));
        // libs.jar is a real jar (nested resource); LoadAnalysisResults handles the resource: form directly
        count += guarded("libs", () -> loader.go(List.of("resource:" + LIBS_JAR_RESOURCE)));
        LOGGER.info("Preloaded {} primary types of analysis hints", count);
        return count;
    }

    private interface Load {
        int run() throws Exception;
    }

    private int guarded(String what, Load load) {
        try {
            return load.run();
        } catch (Throwable t) {
            LOGGER.warn("Could not preload {} analysis hints (analysis will use shallow defaults): {}", what, t.toString());
            return 0;
        }
    }

    private int loadJdk(LoadAnalysisResults loader, Codec codec) throws Exception {
        // Anchor on libs.jar (guaranteed to exist) to locate the archive, then load the sibling jdk hints.
        URL anchor = getClass().getResource(LIBS_JAR_RESOURCE);
        if (anchor == null) {
            LOGGER.warn("Analysis hints archive not found on classpath ({})", LIBS_JAR_RESOURCE);
            return 0;
        }
        return switch (anchor.getProtocol()) {
            case "file" -> loadJdkFromDir(loader, codec, anchor);
            case "jar" -> loadJdkFromJar(loader, codec, anchor);
            default -> {
                LOGGER.warn("Unsupported classpath protocol '{}' for {}", anchor.getProtocol(), anchor);
                yield 0;
            }
        };
    }

    /** Repo / resources-dir case: the jdk hints are a sibling directory of libs.jar on disk. */
    private int loadJdkFromDir(LoadAnalysisResults loader, Codec codec, URL libsJar) throws Exception {
        File libsFile = new File(libsJar.toURI());
        File jdkDir = new File(libsFile.getParentFile(), "jdk");
        File[] jsons = jdkDir.listFiles((d, n) -> n.endsWith(".json"));
        if (jsons == null) {
            LOGGER.warn("JDK hints directory not found: {}", jdkDir);
            return 0;
        }
        int count = 0;
        for (File json : jsons) {
            count += loadOne(loader, codec, json.getName(), Files.readString(json.toPath(), StandardCharsets.UTF_8));
        }
        return count;
    }

    /** Installed-daemon case: enumerate jdk/*.json entries inside the archive jar. */
    private int loadJdkFromJar(LoadAnalysisResults loader, Codec codec, URL libsJar) throws Exception {
        JarURLConnection conn = (JarURLConnection) libsJar.openConnection();
        File archiveJar = new File(conn.getJarFileURL().toURI()); // our own handle; safe to close
        int count = 0;
        try (JarFile jar = new JarFile(archiveJar)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(JDK_ENTRY_PREFIX) || !name.endsWith(".json")) continue;
                try (InputStream is = jar.getInputStream(entry)) {
                    count += loadOne(loader, codec, name, new String(is.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        return count;
    }

    /** Load one hint file, isolating failures: a single bad file must not abort the whole JDK hint set. */
    private int loadOne(LoadAnalysisResults loader, Codec codec, String name, String content) {
        try {
            return loader.go(codec, content);
        } catch (Throwable t) {
            LOGGER.warn("Skipping analysis hint file {} ({})", name, t.toString());
            return 0;
        }
    }
}
