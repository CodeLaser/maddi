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

import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.io.CodecImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.integration.ToolChain;
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

public class LoadAnalyzedPackageFiles {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadAnalyzedPackageFiles.class);
    private final SourceSet sourceSetOfRequest; // for loading types

    public LoadAnalyzedPackageFiles(SourceSet sourceSetOfRequest) {
        this.sourceSetOfRequest = Objects.requireNonNull(sourceSetOfRequest);
    }

    public int go(JavaInspector javaInspector, List<String> directories) throws IOException {
        Codec codec = new PrepWorkCodec(javaInspector.runtime(), sourceSetOfRequest).codec();
        return go(codec, directories);
    }

    public int go(Codec codec, List<String> directories) throws IOException {
        int countPrimaryTypes = 0;
        for (String dir : directories) {
            if (dir.startsWith(ToolChain.RESOURCE_PROTOCOL)) {
                String path = dir.substring(9);
                URL jarUrl = getClass().getResource(path);
                if (jarUrl == null) {
                    LOGGER.warn("Cannot find resource {}", dir);
                } else {
                    try {
                        countPrimaryTypes += processJsonJar(codec, jarUrl);
                    } catch (Throwable t) {
                        LOGGER.error("Caught an exception processing {}", jarUrl);
                        LOGGER.error("Current jdk: {}", ToolChain.currentJre());
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
        for (JSONObject jo : root.get(0).childrenOfType(JSONObject.class)) {
            processPrimaryType(codec, jo);
            ++countPrimaryTypes;
        }
        return countPrimaryTypes;
    }

    private static void processPrimaryType(Codec codec, JSONObject jo) {
        Codec.Context context = new CodecImpl.ContextImpl();
        processSub(codec, context, jo);
    }

    private static void processSub(Codec codec, Codec.Context context, JSONObject jo) {
        KeyValuePair nameKv = (KeyValuePair) jo.get(1);
        String fullyQualifiedWithType = CodecImpl.unquote(nameKv.get(2).getSource());
        KeyValuePair dataKv = (KeyValuePair) jo.get(3);
        JSONObject dataJo = (JSONObject) dataKv.get(2);

        char type = fullyQualifiedWithType.charAt(0);
        String name = fullyQualifiedWithType.substring(1);
        try {
            Info info = codec.decodeInfoInContext(context, type, name);
            if (info == null) {
                throw new UnsupportedOperationException("Cannot find " + name);
            }
            context.push(info);
            processData(codec, context, info, dataJo);
            if (jo.size() > 5) {
                KeyValuePair subs = (KeyValuePair) jo.get(5);
                String subKey = subs.get(0).getSource();
                if ("\"sub\"".equals(subKey)) {
                    processSub(codec, context, (JSONObject) subs.get(2));
                } else {
                    assert "\"subs\"".equals(subKey);
                    Array array = (Array) subs.get(2);
                    for (int i = 1; i < array.size(); i += 2) {
                        processSub(codec, context, (JSONObject) array.get(i));
                    }
                }
            }
        } catch (RuntimeException re) {
            LOGGER.error("Caught exception destreaming {}", name);
            throw re;
        }
        context.pop();
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
