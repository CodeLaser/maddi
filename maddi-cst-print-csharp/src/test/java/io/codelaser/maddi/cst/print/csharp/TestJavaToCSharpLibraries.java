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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.print.translate.PrintMessage;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The library mappings besides the JDK's: slf4j, Jackson, JAXB and Micrometer, a client's overrides in Java and as a
 * mapping file, and what a mapping requires of the translated project. Each test writes its samples' translations to {@code build/csharp-samples/}.
 */
public class TestJavaToCSharpLibraries extends CommonJavaToCSharp {

    /**
     * A client's mapping in front of the JDK's overrides single rules: here a UUID is a string, randomUUID()
     * Guid.NewGuid().ToString() and fromString(s) s; what it does not answer, URI, stays the JDK mapping's.
     */
    @Test
    public void libraryOverride() {
        LibraryMapping uuidAsString = new LibraryMapping() {
            @Override
            public String id() {
                return "client:uuid-as-string";
            }

            @Override
            public TypeMapping type(TypeInfo typeInfo) {
                return "java.util.UUID".equals(typeInfo.fullyQualifiedName())
                        ? new TypeMapping("string", null, false, false) : null;
            }

            @Override
            public Rule call(MethodInfo method,
                             MethodCall call) {
                if (!"java.util.UUID".equals(method.typeInfo().fullyQualifiedName())) return null;
                return switch (method.name()) {
                    case "randomUUID" -> new Rule("Guid.NewGuid().ToString()", null, List.of("System"));
                    case "fromString" -> new Rule("$1", null, List.of());
                    // a string already
                    case "toString" -> new Rule("$0", null, List.of());
                    default -> null;
                };
            }
        };
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.DEFAULT.withFirst(uuidAsString));
        String cs = translate("ValuesOverridden", TestJavaToCSharpBcl.VALUES);
        contains(cs, "internal static string Id() => Guid.NewGuid().ToString();");
        contains(cs, "internal static string Parse(string s) => s;");
        contains(cs, "internal static Uri Uri(string s) => new Uri(s, UriKind.RelativeOrAbsolute);");
    }

    @Language("java")
    private static final String ACCESSORS = """
            package org.example.accessors;
            final class Generator {
                static boolean supported() { return false; }
                Object generate(String name) { return null; }
            }
            public class Accessors {
                static String key(String name) { return name + "$Accessor"; }
                static Object accessor(String name, boolean fast) {
                    if (fast && Generator.supported()) return new Generator().generate(key(name));
                    return name;
                }
            }
            """;

    private static final String NO_GENERATOR = """
            mapping example:no-generator
            exclude org.example.accessors.Generator
            member org.example.accessors.Generator.supported/0 -> false
            member org.example.accessors.Generator.generate/1 -> JavaUnsupported.Value<{R}>("the generator") using Maddi.JavaCompat
            """;

    /**
     * A mapping leaves a program's own type out ({@code exclude}): it is not printed, and its uses go through the
     * mapping's rules as a library's do; a call that cannot work in .NET throws, with the type the call has.
     */
    @Test
    public void excludedTypes() {
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.DEFAULT
                .withFirst(CSharpMappingFile.parse(NO_GENERATOR, "no-generator.csmap")));
        String cs = translate("Accessors", ACCESSORS);
        assertFalse(cs.contains("class Generator"), cs);
        contains(cs, "if (fast && false) { return JavaUnsupported.Value<object>(\"the generator\"); }");
        contains(cs, "internal static string Key(string name) => name + \"$Accessor\";");
    }

    private static final String UUID_AS_STRING = """
            # a client's overrides: a UUID is a string
            mapping client:uuid-as-string
            type java.util.UUID -> string
            member java.util.UUID.randomUUID/0 -> Guid.NewGuid().ToString() using System
            member java.util.UUID.fromString/1 -> $1
            member java.util.UUID.toString/0 -> $0                 # a string already
            member java.util.Random.nextInt/1 ?receiver=java.util.Random -> $0.Next(0, $1)   # a guard
            """;

    /** The same override as a mapping file, the way a client writes it without Java; with a guarded rule. */
    @Test
    public void libraryOverrideFile() {
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.DEFAULT
                .withFirst(CSharpMappingFile.parse(UUID_AS_STRING, "uuid.csmap")));
        String cs = translate("ValuesOverriddenByFile", TestJavaToCSharpBcl.VALUES);
        contains(cs, "internal static string Id() => Guid.NewGuid().ToString();");
        contains(cs, "internal static string Parse(string s) => s;");
        contains(cs, "internal static int Roll(Random random) => random.Next(0, 6);");
    }

    @Language("java")
    private static final String BEANS = """
            package org.example.beans;
            import jakarta.xml.bind.annotation.XmlElement;
            import jakarta.xml.bind.annotation.XmlRootElement;
            @XmlRootElement
            public class Person {
                private String name;
                private int age;
                private String nick;
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                @XmlElement(name = "years")
                public int getAge() { return age; }
                public void setAge(int age) { this.age = age; }
                public String getNick() { return nick == null ? "" : nick; }
                public void setNick(String nick) { this.nick = nick; }
                @XmlElement
                public String getGreeting() { return "hi " + name; }
            }
            """;

    /**
     * JAXB's default access binds public JavaBean properties: a plain one through its field, with its getter's
     * annotations; a property that does more, or has no field, is reported, not dropped silently.
     */
    @Test
    public void xmlProperties() {
        String cs = translate("Person", BEANS);
        contains(cs, "[XmlElement(\"name\")]");
        contains(cs, "[XmlElement(\"years\")]");
        List<String> unbound = messages.stream().filter(m -> m.code() == CSharpPrintCode.XML_UNBOUND)
                .map(PrintMessage::detail).toList();
        assertEquals(List.of(
                "getNick(): a property whose getter or setter does more than access its field",
                "getGreeting(): a property without a field of its name and type"), unbound);
    }

    @Language("java")
    private static final String MARKS = """
            package org.example.marks;
            import com.fasterxml.jackson.annotation.JsonProperty;
            public class Marks {
                @JsonProperty(required = true) public String id;
                @JsonProperty(value = "n", required = true) public String name;
                public String strip(String s) { return s.trim(); }
            }
            """;

    /**
     * A mapping file's {@code #} inside a string is no comment; an annotation's rule applies when all its guards hold
     * and the use has the elements it writes; a type mapped twice is an error.
     */
    @Test
    public void mappingFileParsing() {
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.DEFAULT.withFirst(CSharpMappingFile.parse("""
                mapping client:marks
                member java.lang.String.trim/0 -> $0.Replace(" #", "")   # a comment, not the string's #
                annotation com.fasterxml.jackson.annotation.JsonProperty ?value ?required=true -> JsonPropertyName($value) ][ JsonRequired
                annotation com.fasterxml.jackson.annotation.JsonProperty -> JsonPropertyName($value)
                annotation com.fasterxml.jackson.annotation.JsonProperty -> Marked
                """, "marks.csmap")));
        String cs = translate("Marks", MARKS);
        contains(cs, "s.Replace(\" #\", \"\")");
        contains(cs, "[JsonPropertyName(\"n\")] [JsonRequired]");
        contains(cs, "[Marked]\n    public string id;");
        assertFalse(cs.contains("$value"), cs);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CSharpMappingFile.parse("mapping x\ntype a.B -> B\ntype a.B -> C", "twice.csmap"));
        assertEquals("twice.csmap:3: a second type line for a.B", e.getMessage());
    }

    /** A mapping's packages go into the translation's project file; the JDK's compatibility library is its source. */
    @Test
    public void libraryRequirements() {
        CSharpLibraries libraries = CSharpLibraries.choose("slf4j=none,jackson=none,micrometer=none,jaxb=none").withFirst(CSharpMappingFile.parse("""
                mapping slf4j:example
                nuget Microsoft.Extensions.Logging.Abstractions 10.0.0
                type org.slf4j.Logger -> ILogger using Microsoft.Extensions.Logging
                """, "example.csmap"));
        assertEquals(List.of(
                new LibraryMapping.NuGetPackage("Microsoft.Extensions.Logging.Abstractions", "10.0.0")),
                libraries.requirements().packages());
        assertEquals(List.of(CSharpCompat.FILE_NAME),
                libraries.requirements().sources().stream().map(LibraryMapping.Source::fileName).toList());
        String project = libraries.projectFile("net10.0", false);
        contains(project, "<PackageReference Include=\"Microsoft.Extensions.Logging.Abstractions\" Version=\"10.0.0\" />");
        contains(project, "<!-- generated by maddi's Java -> C# translation; libraries: slf4j:example,jdk -->");
    }

    @Language("java")
    private static final String METRICS = """
            package org.example.metrics;
            import io.micrometer.core.instrument.Counter;
            import io.micrometer.core.instrument.Gauge;
            import io.micrometer.core.instrument.Metrics;
            import io.micrometer.core.instrument.Tags;
            import java.util.concurrent.atomic.AtomicInteger;
            public class Meters {
                private final AtomicInteger size = new AtomicInteger();
                private final Counter calls = Metrics.counter("calls", "kind", "test");
                void register(String name) {
                    Metrics.gauge("size", Tags.of("name", name), size, AtomicInteger::get);
                    calls.increment();
                }
                double read(String name) {
                    Gauge g = Metrics.globalRegistry.find("size").tags(Tags.of("name", name)).gauge();
                    return g == null ? 0 : g.value();
                }
            }
            """;

    /**
     * Micrometer is System.Diagnostics.Metrics through the compatibility library's registry, JavaMetrics, which the
     * default libraries bring along as a source.
     */
    @Test
    public void metrics() {
        String cs = translate("Meters", METRICS);
        contains(cs, "using Maddi.JavaCompat;");
        contains(cs, "private readonly JavaCounter calls = JavaMetrics.Counter(\"calls\", \"kind\", \"test\");");
        contains(cs, "JavaMetrics.Gauge(\"size\", JavaTags.Of(\"name\", name), size, (AtomicInteger p0) => p0.Get());");
        contains(cs, "calls.Increment();");
        contains(cs, "JavaGauge g = JavaMetrics.GlobalRegistry.Find(\"size\").Tags(JavaTags.Of(\"name\", name)).Gauge();");
        contains(cs, "return g == null ? 0 : g.Value();");
        assertTrue(CSharpLibraries.DEFAULT.requirements().sources().stream()
                .anyMatch(source -> "JavaMetrics.cs".equals(source.fileName())));
    }

    @Language("java")
    private static final String LOGGING = """
            package org.example.logging;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            public class Worker {
                private static final Logger LOG = LoggerFactory.getLogger(Worker.class);
                void work(String output, int count, Exception e) {
                    LOG.debug("LLM output: {}", output);
                    LOG.warn("{} of {} failed {", count, output.length());
                    LOG.error("Ignored error", e);
                    LOG.warn("Failed {}", output, e);
                    if (LOG.isDebugEnabled()) LOG.info("done");
                }
            }
            """;

    /**
     * slf4j as Microsoft.Extensions.Logging, the default: a message's {} holes are named after their arguments, a
     * trailing exception is the first argument; as Serilog, the option.
     */
    @Test
    public void logging() {
        String cs = translate("Worker", LOGGING);
        contains(cs, "private static readonly ILogger LOG = JavaLogging.GetLogger(typeof(Worker));");
        contains(cs, "LOG.LogDebug(\"LLM output: {Output}\", output);");
        contains(cs, "LOG.LogWarning(\"{Count} of {Length} failed {{\", count, output.Length);");
        contains(cs, "LOG.LogError(e, \"Ignored error\");");
        contains(cs, "LOG.LogWarning(e, \"Failed {Output}\", output);");
        contains(cs, "if (LOG.IsEnabled(LogLevel.Debug)) { LOG.LogInformation(\"done\"); }");
    }

    @Test
    public void loggingSerilog() {
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.choose("slf4j=serilog"));
        String serilog = translate("WorkerSerilog", LOGGING);
        contains(serilog, "using Serilog;");
        contains(serilog, "private static readonly ILogger LOG = Log.ForContext(typeof(Worker));");
        contains(serilog, "LOG.Warning(e, \"Failed {Output}\", output);");
        contains(serilog, "if (LOG.IsEnabled(LogEventLevel.Debug)) { LOG.Information(\"done\"); }");
    }

    @Language("java")
    private static final String JSON = """
            package org.example.json;
            import com.fasterxml.jackson.annotation.JsonInclude;
            import com.fasterxml.jackson.annotation.JsonProperty;
            import com.fasterxml.jackson.annotation.JsonSubTypes;
            import com.fasterxml.jackson.annotation.JsonTypeInfo;
            import com.fasterxml.jackson.core.JsonProcessingException;
            import com.fasterxml.jackson.databind.JsonNode;
            import com.fasterxml.jackson.databind.ObjectMapper;
            public class Codec {
                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
                @JsonSubTypes({@JsonSubTypes.Type(value = Text.class, name = "text"),
                               @JsonSubTypes.Type(value = Image.class, name = "image")})
                public static abstract class Content { }
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class Text extends Content { @JsonProperty("body") public String text; }
                public static class Image extends Content { }
                private final ObjectMapper mapper = new ObjectMapper();
                Content read(String json) throws JsonProcessingException { return mapper.readValue(json, Content.class); }
                String write(Content c) throws JsonProcessingException { return mapper.writeValueAsString(c); }
                int year(JsonNode node) { return node.isObject() ? node.get("year").asInt() : 0; }
            }
            """;

    /** Jackson as System.Text.Json, the default; as Json.NET, the option. */
    @Test
    public void json() {
        String cs = translate("Codec", JSON);
        contains(cs, "[JsonPolymorphic(TypeDiscriminatorPropertyName = \"type\")]");
        contains(cs, "[JsonDerivedType(typeof(Text), \"text\")] [JsonDerivedType(typeof(Image), \"image\")]");
        contains(cs, "[JsonPropertyName(\"body\")]");
        contains(cs, "private readonly JsonSerializerOptions mapper = new JsonSerializerOptions();");
        contains(cs, "JsonSerializer.Deserialize<Content>(json, mapper)");
        contains(cs, "JsonSerializer.Serialize(c, mapper)");
        contains(cs, "(node is JsonObject) ? node[\"year\"].GetValue<int>() : 0");
        contains(cs, "public sealed class Text : Content {");
    }

    @Language("java")
    private static final String XML = """
            package org.example.xml;
            import jakarta.xml.bind.JAXBContext;
            import jakarta.xml.bind.annotation.*;
            import jakarta.xml.bind.annotation.adapters.XmlAdapter;
            import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;
            public class Settings {
                public enum Mode { FAST, SAFE }
                public interface Phase { }
                public static class Search implements Phase { }
                public static class Heuristic implements Phase { }
                @XmlType(namespace = "urn:example")
                public static class Adapted { @XmlElement(name = "property") private List<Entry> entries; }
                @XmlType(namespace = "urn:example")
                public static class Entry { @XmlAttribute private String name; @XmlAttribute private String value; }
                public static class MapAdapter extends XmlAdapter<Adapted, Map<String, String>> {
                    @Override public Map<String, String> unmarshal(Adapted a) { return new HashMap<>(); }
                    @Override public Adapted marshal(Map<String, String> m) { return m == null ? null : new Adapted(); }
                }
                @XmlRootElement(name = "solver", namespace = "urn:example")
                @XmlType(name = "solverConfig", namespace = "urn:example",
                         propOrder = {"mode", "count", "classes", "phases", "properties", "wrapped", "tags"})
                @XmlAccessorType(XmlAccessType.FIELD)
                public static class Solver {
                    @XmlTransient private Object clock;
                    @XmlAttribute private String id;
                    private Mode mode;
                    private Integer count;
                    @XmlElement(name = "entityClass") private List<String> classes;
                    @XmlElements({@XmlElement(name = "search", type = Search.class),
                                  @XmlElement(name = "heuristic", type = Heuristic.class)})
                    private List<Phase> phases;
                    @XmlJavaTypeAdapter(MapAdapter.class) private Map<String, String> properties;
                    @XmlElementWrapper(name = "wraps") @XmlElement(name = "wrap") private List<String> wrapped;
                    @XmlElement(name = "tag") private Set<String> tags;
                    public Mode getMode() { return mode; }
                    public void setMode(Mode mode) { this.mode = mode; }
                }
                static Solver read(java.io.Reader reader) throws Exception {
                    var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(reader));
                    if (document.getDocumentElement().getNamespaceURI() == null) return null;
                    JAXBContext context = JAXBContext.newInstance(Solver.class);
                    return (Solver) context.createUnmarshaller().unmarshal(document);
                }
            }
            """;

    /**
     * JAXB as XmlSerializer: a bound field is a public property with the attributes of its annotations, converted to
     * what XmlSerializer writes; a public member that a getter made of a bound field is {@code [XmlIgnore]}.
     */
    @Test
    public void xml() {
        String cs = translate("Settings", XML);
        contains(cs, "using System.Xml.Serialization;");
        contains(cs, "[XmlRoot(\"solver\", Namespace = \"urn:example\")]\n[XmlType(\"solverConfig\", Namespace = \"urn:example\")]");
        contains(cs, "[XmlAttribute(\"id\")]\npublic string XmlId { get => this.id; set => this.id = value; }");
        contains(cs, "public enum Mode { [XmlEnum(\"FAST\")] Fast, [XmlEnum(\"SAFE\")] Safe }");
        contains(cs, "[XmlElement(\"mode\", Order = 0)]\npublic Mode? XmlMode { get => this.Mode; set => this.Mode = value; }\n"
                     + "public bool ShouldSerializeXmlMode() => this.Mode != null;");
        contains(cs, "public int? XmlCount { get => this.count; set => this.count = value; }");
        contains(cs, "public bool ShouldSerializeXmlCount() => this.count != null;");
        contains(cs, "[XmlElement(\"entityClass\", Order = 2)]\npublic List<string> XmlClasses");
        contains(cs, "[XmlElement(\"search\", typeof(Search), Order = 3)]\n[XmlElement(\"heuristic\", typeof(Heuristic), Order = 3)]");
        contains(cs, "public object[] XmlPhases { get => this.phases?.ToArray(); set => this.phases = value == null ? null : new List<IPhase>(value.Select(e_ => (IPhase) e_)); }");
        contains(cs, "public Adapted XmlProperties { get => new MapAdapter().Marshal(this.properties); set => this.properties = new MapAdapter().Unmarshal(value); }");
        contains(cs, "[XmlArray(\"wraps\", Order = 5)]\n[XmlArrayItem(\"wrap\")]");
        contains(cs, "[XmlElement(\"tag\", Order = 6)]\npublic string[] XmlTags { get => this.tags?.ToArray(); set => this.tags = value == null ? null : value.ToHashSet(); }");
        contains(cs, "[XmlElement(\"property\")]\npublic List<Entry> XmlEntries");
        contains(cs, "public sealed class MapAdapter : JavaXmlAdapter<Adapted, IDictionary<string, string>> {");
        // the runtime: the compatibility library's JAXB, System.Xml's DOM
        contains(cs, "var factory = JavaDocumentBuilderFactory.NewInstance();");
        contains(cs, "XmlDocument document = factory.NewDocumentBuilder().Parse(new JavaInputSource(reader));");
        contains(cs, "if (JavaDom.NamespaceUri(document.DocumentElement) == null) { return null; }");
        contains(cs, "JavaJaxbContext context = JavaJaxbContext.NewInstance(typeof(Solver));");
        contains(cs, "return (Solver) context.CreateUnmarshaller().Unmarshal(document);");
        assertFalse(cs.contains("XmlClock"), cs);
        assertFalse(cs.contains("@Xml"), cs);
    }

    @Test
    public void jsonNewtonsoft() {
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.choose("jackson=newtonsoft"));
        String newtonsoft = translate("CodecNewtonsoft", JSON);
        contains(newtonsoft, "private readonly JsonSerializerSettings mapper = new JsonSerializerSettings();");
        contains(newtonsoft, "JsonConvert.DeserializeObject<Content>(json, mapper)");
        contains(newtonsoft, "[JsonProperty(\"body\")]");
        contains(newtonsoft, "[JsonObject(ItemNullValueHandling = NullValueHandling.Ignore)]");
        contains(newtonsoft, "(node.Type == JTokenType.Object) ? node[\"year\"].Value<int>() : 0");
    }
}
