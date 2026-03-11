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

package org.e2immu.analyzer.run.config.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.Version;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleAbstractTypeResolver;
import tools.jackson.databind.module.SimpleModule;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfiguration;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfigurationImpl;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.LanguageConfiguration;
import org.e2immu.language.cst.impl.runtime.LanguageConfigurationImpl;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ser.std.StdSerializer;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonStreaming {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonStreaming.class);

    public static SimpleModule configModule() {
        SimpleModule module = new SimpleModule("e2immuConfig", Version.unknownVersion());

        SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
        resolver.addMapping(SourceSet.class, SourceSetImpl.class);
        resolver.addMapping(InputConfiguration.class, InputConfigurationImpl.class);
        resolver.addMapping(AnnotatedAPIConfiguration.class, AnnotatedAPIConfigurationImpl.class);
        resolver.addMapping(LanguageConfiguration.class, LanguageConfigurationImpl.class);

        module.setAbstractTypes(resolver);

        // only because we want to get the order straight: a correct linearization of the dependencies between the
        // source sets
        module.addSerializer(InputConfigurationImpl.class, new InputConfigurationSerializer());
        module.addSerializer(SourceSetImpl.class, new SourceSetSerializer());
        module.addDeserializer(SourceSetImpl.class, new SourceSetDeserializer());
        //FIXME at the moment, AAPIConfig does not have a @JsonProperty in Configuration, so it is skipped
        return module;
    }

    public static ObjectMapper objectMapper() {
        return JsonMapper.builder().addModule(configModule()).build();
    }

    private static boolean getBoolean(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null && value.asBoolean();
    }

    private static String getString(JsonNode node, String key, String defaultValue) {
        JsonNode value = node.get(key);
        return value == null ? defaultValue : value.asString();
    }

    static class SourceSetDeserializer extends ValueDeserializer<SourceSetImpl> {

        @Override
        public SourceSetImpl deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
            JsonNode node = ctxt.readTree(jp);
            String name = node.get("name").asString();
            List<Path> sourceDirectories = new ArrayList<>();
            JsonNode sourceDirs = node.get("sourceDirectories");
            if (sourceDirs != null) {
                for (JsonNode sourceDir : sourceDirs) {
                    sourceDirectories.add(Path.of(sourceDir.asString()));
                }
            }
            String uriString = node.get("uri").asString("");
            URI uri = uriString.isBlank() ? null : URI.create(uriString);
            String sourceEncodingString = getString(node, "sourceEncoding", StandardCharsets.UTF_8.toString());
            Charset sourceEncoding = sourceEncodingString.isBlank() ? StandardCharsets.UTF_8 :
                    Charset.forName(sourceEncodingString);
            boolean test = getBoolean(node, "test");
            boolean library = getBoolean(node, "library");
            boolean externalLibrary = getBoolean(node, "externalLibrary");
            boolean partOfJdk = getBoolean(node, "partOfJdk");
            boolean runtimeOnly = getBoolean(node, "runtimeOnly");
            Set<String> restrictToPackages = new HashSet<>();
            JsonNode restrictToPackagesNode = node.get("restrictToPackages");
            if (restrictToPackagesNode != null) {
                for (JsonNode jsonNode : restrictToPackagesNode) {
                    restrictToPackages.add(jsonNode.asString());
                }
            }
            Set<SourceSet> dependencies = new HashSet<>();
            JsonNode dependenciesNode = node.get("dependencies");
            if (dependenciesNode != null) {
                for (JsonNode subNode : dependenciesNode) {
                    String key = subNode.asString();
                    SourceSet dependency = (SourceSet) ctxt.getAttribute(key);
                    if (dependency != null) {
                        dependencies.add(dependency);
                    } else {
                        LOGGER.warn("dependency named '{}' unknown", key);
                    }
                }
            }
            SourceSetImpl ssi = new SourceSetImpl(name, sourceDirectories, uri, sourceEncoding, test, library, externalLibrary,
                    partOfJdk, runtimeOnly, Set.copyOf(restrictToPackages), Set.copyOf(dependencies));
            String fingerPrintToString = getString(node, "fingerPrint", "");
            if (!fingerPrintToString.isBlank()) {
                ssi.setFingerPrint(MD5FingerPrint.from(fingerPrintToString));
            }
            String analysisFingerPrintToString = getString(node, "analysisFingerPrint", "");
            if (!analysisFingerPrintToString.isBlank()) {
                ssi.setAnalysisFingerPrint(MD5FingerPrint.from(analysisFingerPrintToString));
            }
            ctxt.setAttribute(name, ssi);

            return ssi;
        }
    }

    static class SourceSetSerializer extends ValueSerializer<SourceSetImpl> {

        @Override
        public void serialize(SourceSetImpl value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            if (value.sourceEncoding() != null) {
                gen.writeStringProperty("sourceEncoding", value.sourceEncoding().name());
            }
            gen.writeStringProperty("name", value.name());
            if (value.sourceDirectories() != null && !value.sourceDirectories().isEmpty()) {
                gen.writeArrayPropertyStart("sourceDirectories");
                for (Path dir : value.sourceDirectories()) gen.writeString(dir.toString());
                gen.writeEndArray();
            }
            gen.writeStringProperty("uri", value.uri().toString());
            if (value.test()) gen.writeBooleanProperty("test", value.test());
            if (value.library()) gen.writeBooleanProperty("library", value.library());
            if (value.externalLibrary()) gen.writeBooleanProperty("externalLibrary", value.externalLibrary());
            if (value.partOfJdk()) gen.writeBooleanProperty("partOfJdk", value.partOfJdk());
            if (value.runtimeOnly()) gen.writeBooleanProperty("runtimeOnly", value.runtimeOnly());
            if (value.restrictToPackages() != null) {
                gen.writeArrayPropertyStart("restrictToPackages");
                for (String pkg : value.restrictToPackages()) gen.writeString(pkg);
                gen.writeEndArray();
            }
            if (value.dependencies() != null && !value.dependencies().isEmpty()) {
                gen.writeArrayPropertyStart("dependencies");
                for (SourceSet d : value.dependencies()) gen.writeString(d.name());
                gen.writeEndArray();
            }
            if (value.fingerPrintOrNull() != null && !value.fingerPrintOrNull().isNoFingerPrint()) {
                gen.writeStringProperty("fingerPrint", value.fingerPrintOrNull().toString());
            }
            if (value.analysisFingerPrintOrNull() != null && !value.analysisFingerPrintOrNull().isNoFingerPrint()) {
                gen.writeStringProperty("analysisFingerPrint", value.analysisFingerPrintOrNull().toString());
            }
            gen.writeEndObject();
        }
    }

    static class InputConfigurationSerializer extends ValueSerializer<InputConfigurationImpl> {

        @Override
        public void serialize(InputConfigurationImpl value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            gen.writeStringProperty("workingDirectory", value.workingDirectory() == null ? null :
                    value.workingDirectory().toString());
            gen.writeArrayPropertyStart("classPathParts");
            for (SourceSet cpp : value.classPathParts()) {
                provider.writeValue(gen, cpp);
            }
            gen.writeEndArray();
            gen.writeArrayPropertyStart("sourceSets");
            for (SourceSet cpp : value.sourceSets()) {
                provider.writeValue(gen, cpp);
            }
            gen.writeEndArray();
            gen.writeStringProperty("alternativeJREDirectory", value.alternativeJREDirectory() == null ? null
                    : value.alternativeJREDirectory().toString());
            gen.writeEndObject();
        }
    }
}
