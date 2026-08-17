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

package io.codelaser.maddi.modification.prepwork.io;

import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.io.CodecImpl;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import org.parsers.json.JSONParser;
import org.parsers.json.Node;
import org.parsers.json.ast.Array;
import org.parsers.json.ast.JSONObject;
import org.parsers.json.ast.KeyValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

/**
 * Loads analysis-result ("annotated API") archives, applying each stored hint to the corresponding {@link Info}.
 * <p>
 * Loading is deliberately tolerant, so a single unusable hint never aborts a whole archive:
 * <ul>
 *   <li><b>module not on the classpath</b> &mdash; a primary type whose type cannot be resolved (its library is
 *       simply absent from this project's deliberately partial classpath) has all its hints skipped; counted in
 *       {@link #skippedPrimaryTypes()}.</li>
 *   <li><b>unresolvable member reference</b> &mdash; a hint that names a field/method/parameter this reader cannot
 *       resolve against the types actually loaded has that one element's hints dropped, with a warning, while the
 *       rest of the archive still loads; counted in {@link #skippedUnresolvableHints()}. This chiefly covers the
 *       in-flux {@code @GetSet}-on-an-interface analysis, where the writer emits a synthetic interface field (the
 *       backing field lives on the implementation, not the interface) that the reader cannot reconstruct. Resolving
 *       that disagreement is upstream immutability work; until then, dropping the individual hint keeps prep loading
 *       instead of failing every project that references such a type.</li>
 * </ul>
 */
public class LoadAnalysisResults {
    public static final String ANALYZED_RESULTS_JDK = "../maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/jdk";
    public static final String ANALYZED_RESULTS_LIBS = "../maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/libs";
    public static final List<String> ANALYZED_RESULTS = List.of(ANALYZED_RESULTS_JDK, ANALYZED_RESULTS_LIBS + "/test",
            ANALYZED_RESULTS_LIBS+"/log");
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadAnalysisResults.class);
    private final SourceSet sourceSetOfRequest; // for loading types
    private final Runtime runtime;

    public LoadAnalysisResults(Runtime runtime, SourceSet sourceSetOfRequest) {
        this.runtime = runtime;
        this.sourceSetOfRequest = Objects.requireNonNull(sourceSetOfRequest);
    }

    public int go(List<String> directories) throws IOException {
        Codec codec = new PrepWorkCodec(runtime, sourceSetOfRequest).codec();
        return go(codec, directories);
    }

    public int go(Codec codec, List<String> directories) throws IOException {
        int countPrimaryTypes = 0;
        for (String dir : directories) {
            if (dir.startsWith("resource:")) {
                String path = dir.substring(9);
                URL jarUrl = getClass().getResource(path);
                if (jarUrl == null) {
                    LOGGER.warn("Cannot find resource {}", dir);
                } else {
                    try {
                        countPrimaryTypes += processJsonJar(codec, jarUrl);
                    } catch (Throwable t) {
                        LOGGER.error("Caught an exception processing {}", jarUrl);
                        throw t;
                    }
                }
            } else {
                File directory = new File(dir);
                if (directory.canRead()) {
                    countPrimaryTypes += goDir(codec, directory);
                    LOGGER.info("Finished reading all json files in AAAPI {}", directory.getAbsolutePath());
                } else {
                    LOGGER.warn("Path '{}' is not a directory containing analyzed annotated API files", directory);
                }
            }
        }
        if (skippedPrimaryTypes > 0) {
            LOGGER.info("Skipped analysis hints for {} primary types whose module is not on the classpath",
                    skippedPrimaryTypes);
        }
        if (skippedUnresolvableHints > 0) {
            LOGGER.info("Skipped {} analysis hint(s) referencing an element not resolvable on the classpath",
                    skippedUnresolvableHints);
        }
        return countPrimaryTypes;
    }

    private int processJsonJar(Codec codec, URL jarUrl) {
        int countPrimaryTypes = 0;
        try (InputStream inputStream = jarUrl.openStream();
             JarInputStream jis = new JarInputStream(inputStream)) {
            JarEntry jarEntry;
            while ((jarEntry = jis.getNextJarEntry()) != null) {
                String realName = jarEntry.getRealName();
                if (realName.endsWith(".json")) {
                    LOGGER.debug("Adding {}", realName);
                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        byte[] bytes = new byte[1024];
                        int read;
                        while ((read = jis.read(bytes, 0, bytes.length)) > 0) {
                            os.write(bytes, 0, read);
                        }
                        String content = os.toString();
                        countPrimaryTypes += go(codec, content);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Caught exception", e);
            throw new RuntimeException(e);
        }
        LOGGER.info("Loaded {} primary types from {}", countPrimaryTypes, jarUrl);
        return countPrimaryTypes;
    }

    public int goDir(JavaInspector javaInspector, File directory) throws IOException {
        Codec codec = new PrepWorkCodec(javaInspector.runtime(), sourceSetOfRequest).codec();
        return goDir(codec, directory);
    }

    public int goDir(Codec codec, File directory) throws IOException {
        if (!directory.isDirectory()) throw new UnsupportedEncodingException(directory + " is not a directory");
        try (Stream<Path> jsonFiles = Files.walk(directory.toPath(), 3)
                .filter(p -> p.toString().endsWith(".json"))) {
            int countPrimaryTypes = 0;
            for (Path jsonFile : jsonFiles.toList()) {
                countPrimaryTypes += go(codec, jsonFile);
            }
            return countPrimaryTypes;
        }
    }

    /**
     * checkpoint-restore variant (task #34): an unparseable or undecodable file (crash mid-write,
     * codec drift) is skipped and counted, never fatal — the resumed run re-analyzes what is missing.
     */
    public int goDirTolerant(Codec codec, File directory) throws IOException {
        if (!directory.isDirectory()) throw new UnsupportedEncodingException(directory + " is not a directory");
        try (Stream<Path> jsonFiles = Files.walk(directory.toPath(), 3)
                .filter(p -> p.toString().endsWith(".json"))) {
            int countPrimaryTypes = 0;
            int skippedFiles = 0;
            for (Path jsonFile : jsonFiles.toList()) {
                try {
                    countPrimaryTypes += go(codec, jsonFile);
                } catch (IOException | RuntimeException | AssertionError | StackOverflowError e) {
                    ++skippedFiles;
                    LOGGER.warn("Skipping unreadable analysis file {}: {}", jsonFile, e.toString());
                }
            }
            if (skippedFiles > 0) {
                LOGGER.warn("Skipped {} unreadable analysis file(s) in {}", skippedFiles, directory);
            }
            return countPrimaryTypes;
        }
    }

    public int go(Codec codec, Path jsonFile) throws IOException {
        LOGGER.info("Parsing {}", jsonFile);
        String s = Files.readString(jsonFile);
        return go(codec, s);
    }

    public int go(Codec codec, String content) {
        JSONParser parser = new JSONParser(content);
        parser.Root();
        Node root = parser.rootNode();
        int countPrimaryTypes = 0;
        for (JSONObject jo : root.getFirst().childrenOfType(JSONObject.class)) {
            if (processPrimaryType(codec, jo)) ++countPrimaryTypes;
            else ++skippedPrimaryTypes;
        }
        return countPrimaryTypes;
    }

    // number of primary types whose module is not on the classpath, so their hints were skipped
    private int skippedPrimaryTypes;

    public int skippedPrimaryTypes() {
        return skippedPrimaryTypes;
    }

    // number of individual element hints dropped because they reference something this reader cannot resolve
    // (chiefly the in-flux @GetSet synthetic interface field); the surrounding archive still loads. See the class note.
    private int skippedUnresolvableHints;

    public int skippedUnresolvableHints() {
        return skippedUnresolvableHints;
    }

    // returns false if the primary type's hints are skipped whole (its module is not on the classpath)
    private boolean processPrimaryType(Codec codec, JSONObject jo) {
        Codec.Context context = new CodecImpl.ContextImpl();
        return processSub(codec, context, jo, true);
    }

    // Applies the hints for one element and recurses into its children. Returns false only for a primary type
    // (topLevel) whose own type is not on the classpath, so the caller counts it as skipped; a nested element that
    // cannot be applied is dropped in place (see the class note on tolerance) without failing its siblings.
    private boolean processSub(Codec codec, Codec.Context context, JSONObject jo, boolean topLevel) {
        KeyValuePair nameKv = (KeyValuePair) jo.get(1);
        String fullyQualifiedWithType = CodecImpl.unquote(nameKv.get(2).getSource());
        KeyValuePair dataKv = (KeyValuePair) jo.get(3);
        JSONObject dataJo = (JSONObject) dataKv.get(2);

        char type = fullyQualifiedWithType.charAt(0);
        String name = fullyQualifiedWithType.substring(1);

        Info info;
        try {
            info = codec.decodeInfoInContext(context, type, name);
        } catch (Codec.DecoderException de) {
            // The element itself names something this reader cannot resolve against the loaded types (chiefly a
            // synthetic @GetSet interface field written by the in-flux immutability analyzer). Drop its hint and
            // carry on rather than aborting the whole archive. See the class note.
            LOGGER.warn("Skipping analysis hint for unresolvable element '{}': {}", name, de.getMessage());
            ++skippedUnresolvableHints;
            return !topLevel;
        }
        if (info == null) {
            if (topLevel) {
                // the module carrying this type is not on the classpath; skip its analysis hints entirely
                LOGGER.debug("Skipping analysis hints for {}: type not on the classpath", name);
                return false;
            }
            throw new UnsupportedOperationException("Cannot find " + name);
        }
        context.push(info);
        try {
            processData(codec, context, info, dataJo);
            if (jo.size() > 5) {
                KeyValuePair subs = (KeyValuePair) jo.get(5);
                String subKey = subs.get(0).getSource();
                if ("\"sub\"".equals(subKey)) {
                    processSub(codec, context, (JSONObject) subs.get(2), false);
                } else {
                    assert "\"subs\"".equals(subKey);
                    Array array = (Array) subs.get(2);
                    for (int i = 1; i < array.size(); i += 2) {
                        processSub(codec, context, (JSONObject) array.get(i), false);
                    }
                }
            }
        } catch (Codec.DecoderException de) {
            // A property of this element references something unresolvable (e.g. its @GetSet field). The property
            // group is decoded as a unit, so this element's hints are dropped; its siblings still load. See the
            // class note.
            LOGGER.warn("Skipping analysis hints for '{}': {}", name, de.getMessage());
            ++skippedUnresolvableHints;
            context.pop();
            return !topLevel;
        } catch (RuntimeException re) {
            LOGGER.error("Caught exception destreaming {}", name);
            throw re;
        }
        context.pop();
        return true;
    }

    private static void processData(Codec codec, Codec.Context context, Info info, JSONObject dataJo) {
        List<Codec.EncodedPropertyValue> epvs = new ArrayList<>();
        for (int i = 1; i < dataJo.size(); i += 2) {
            if (dataJo.get(i) instanceof KeyValuePair kvp2) {
                String key = CodecImpl.unquote(kvp2.get(0).getSource());
                epvs.add(new Codec.EncodedPropertyValue(key, new CodecImpl.D(kvp2.get(2))));
            }
        }
        // the decoder writes directly into info.analysis()! we must do this, because to properly
        // decode HCS, we need the value of HCT which occurs earlier in the same list
        codec.decode(context, info.analysis(), epvs.stream());
    }
}
