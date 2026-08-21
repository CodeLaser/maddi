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
package io.codelaser.maddi.run.config.compile;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A compile classpath is not a closure over the TYPE_USE annotations its dependencies carry.
 *
 * <p>⚠ THE FIXTURE IS REAL BYTECODE, COMPILED HERE. An assertion about what a class file carries, written
 * against a hand-made class file, tests the fixture's author. These class files come out of {@code javac},
 * which is the only party whose opinion about a {@code RuntimeInvisibleTypeAnnotations} attribute counts.
 *
 * <p>⭐ AND THE CONTROL THAT CARRIES THE MOST WEIGHT IS {@link #declarationAnnotationIsNotClosedOver()}: on a
 * real 348-source-set elasticsearch parse the log named <b>85</b> distinct missing classes and this closure
 * finds <b>3</b>. The difference is exactly the declaration/TYPE_USE distinction — a check that closed over
 * both would add most of the world to every classpath and call it a fix.
 */
public class TestTypeUseAnnotationClosure {

    @TempDir
    static Path root;

    private static Path annotations;      // provides tu.TU, a TYPE_USE annotation
    private static Path annotationsModular;   // the SAME tu.TU, plus a module-info -- the published artifact
    private static Path aModule;              // any module at all: one class and a descriptor
    private static Path declAnnotations;  // provides decl.Decl, a METHOD annotation -- DELIBERATELY SEPARATE
    private static Path carrier;          // lib.Carrier: carries BOTH, on the same method
    private static Path declarationOnly;  // lib.DeclarationOnly: carries only the declaration annotation
    private static Path consumerOutput;   // the source set doing the reading

    /**
     * ⭐ THE TWO ANNOTATIONS LIVE IN SEPARATE OUTPUT DIRECTORIES ON PURPOSE, and {@code Carrier} uses BOTH.
     * The first version of this fixture put them in one directory and gave {@code Carrier} only the TYPE_USE
     * one — so the declaration control passed for the wrong reason: the byte prefilter saw no type-annotation
     * attribute at all, and the visitor was never asked to tell the two kinds apart. A deliberate red check
     * (make the visitor record declaration annotations too) left the suite GREEN, which is rank 14's rule
     * again: A CONTROL-SHAPED ASSERTION THAT CANNOT FAIL IS A HALF-MATCH WEARING A TEST'S CLOTHES. With
     * {@code decl.Decl} unprovided and carried by the same class, that red check now fails as it must.
     */
    @BeforeAll
    public static void compileTheFixture() throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull(javac, "this test needs a JDK, not a JRE");

        annotations = compile(javac, "annotations", null, """
                package tu;
                import java.lang.annotation.*;
                @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.CLASS) public @interface TU {}
                """);
        declAnnotations = compile(javac, "declAnnotations", null, """
                package decl;
                import java.lang.annotation.*;
                @Target(ElementType.METHOD) @Retention(RetentionPolicy.CLASS) public @interface Decl {}
                """);
        carrier = compile(javac, "carrier", annotations + java.io.File.pathSeparator + declAnnotations, """
                package lib;
                public class Carrier { @decl.Decl public @tu.TU String hello() { return ""; } }
                """);
        declarationOnly = compile(javac, "declarationOnly", declAnnotations.toString(), """
                package lib;
                public class DeclarationOnly { @decl.Decl public String hello() { return ""; } }
                """);
        // ⭐ THE PAIR THE DEFECT WAS FOUND ON, reduced to its shape: the same annotation class, once with a
        // descriptor and once without. A real IDE distribution's annotations.jar and the published
        // annotations-<version>.jar differ by EXACTLY that one entry.
        annotationsModular = compileModule(javac, "annotationsModular", null,
                "module tu.mod { exports tu; }", """
                package tu;
                import java.lang.annotation.*;
                @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.CLASS) public @interface TU {}
                """);
        // ⚠ NOTHING TO DO WITH THE ANNOTATION. Its only job is to be A MODULE on the consumer's classpath,
        // which is all it took to veto every modular provider.
        aModule = compileModule(javac, "aModule", null, "module other.mod { exports other; }", """
                package other;
                public class Other {}
                """);
        consumerOutput = Files.createDirectories(root.resolve("consumer/classes"));
    }

    /**
     * javac into {@code <root>/<name>/classes}, with {@code cp} on the classpath if given.
     * ⚠ The file is named after the PUBLIC type it declares; javac rejects any other name, which is how the
     * first version of this fixture failed.
     */
    private static final java.util.regex.Pattern DECLARES =
            java.util.regex.Pattern.compile("public (?:@interface|class|interface|record|enum) (\\w+)");

    private static Path compile(JavaCompiler javac, String name, String cp, String... sources) throws IOException {
        Path src = Files.createDirectories(root.resolve(name).resolve("src"));
        Path out = Files.createDirectories(root.resolve(name).resolve("classes"));
        List<String> files = new java.util.ArrayList<>();
        for (String source : sources) {
            java.util.regex.Matcher m = DECLARES.matcher(source);
            assertTrue(m.find(), "cannot see what this source declares: " + source);
            Path f = src.resolve(m.group(1) + ".java");
            Files.writeString(f, source);
            files.add(f.toString());
        }
        List<String> args = new java.util.ArrayList<>(List.of("-d", out.toString()));
        if (cp != null) args.addAll(List.of("-classpath", cp));
        args.addAll(files);
        assertEquals(0, javac.run(null, null, System.err, args.toArray(String[]::new)),
                "the fixture itself must compile");
        return out;
    }

    /**
     * Like {@link #compile}, with a {@code module-info.java} beside the sources, so the output directory
     * carries a real descriptor. ⚠ {@code --module-path} rather than {@code -classpath}: a module cannot read
     * the class path, and getting that wrong makes the fixture fail to compile rather than fail silently.
     */
    private static Path compileModule(JavaCompiler javac, String name, String modulePath, String moduleInfo,
                                      String... sources) throws IOException {
        Path src = Files.createDirectories(root.resolve(name).resolve("src"));
        Path out = Files.createDirectories(root.resolve(name).resolve("classes"));
        List<String> files = new java.util.ArrayList<>();
        Path mi = src.resolve("module-info.java");
        Files.writeString(mi, moduleInfo);
        files.add(mi.toString());
        for (String source : sources) {
            java.util.regex.Matcher m = DECLARES.matcher(source);
            assertTrue(m.find(), "cannot see what this source declares: " + source);
            Path f = src.resolve(m.group(1) + ".java");
            Files.writeString(f, source);
            files.add(f.toString());
        }
        List<String> args = new java.util.ArrayList<>(List.of("-d", out.toString()));
        if (modulePath != null) args.addAll(List.of("--module-path", modulePath));
        args.addAll(files);
        assertEquals(0, javac.run(null, null, System.err, args.toArray(String[]::new)),
                "the fixture itself must compile");
        assertTrue(Files.isRegularFile(out.resolve("module-info.class")),
                "the point of this fixture is the descriptor; it is not there");
        return out;
    }

    private static SourceSet library(String name, Path dir) {
        return new SourceSetImpl.Builder().setName(name).setSourceDirectories(List.of())
                .setUri(URI.create("file:" + dir)).setLibrary(true).setExternalLibrary(true).build();
    }

    private static SourceSet consumer(List<SourceSet> dependencies) {
        return new SourceSetImpl.Builder().setName("app/main").setSourceDirectories(List.of())
                .setUri(URI.create("file:" + consumerOutput)).setDependencies(dependencies).build();
    }

    /**
     * ⛔⛔ THE HEADLINE. The consumer reads {@code lib.Carrier}, whose return type carries {@code @tu.TU}; the
     * annotation's own class is on no classpath of its own — exactly what {@code compileOnly} does to a
     * transitive consumer. The provider is declared, so the closure must add it.
     */
    @DisplayName("a TYPE_USE annotation carried by a dependency's bytecode pulls in its provider")
    @Test
    public void typeUseAnnotationIsClosedOver() {
        SourceSet carrierPart = library("carrier", carrier);
        SourceSet annotationPart = library("annotations", annotations);
        SourceSet app = consumer(List.of(carrierPart));

        TypeUseAnnotationClosure.Result result =
                new TypeUseAnnotationClosure().close(List.of(app), List.of(carrierPart, annotationPart));

        assertEquals(List.of("annotations"), result.added().get("app/main").stream().map(SourceSet::name).toList());
        assertTrue(result.sourceSets().getFirst().dependencies().contains(annotationPart),
                "the returned source set must actually carry the new dependency");
        // ⭐ AND THE NARROWING, IN THE SAME CLASS FILE. `Carrier.hello` also carries @decl.Decl, whose provider
        // is on NO classpath here. A closure that did not distinguish the two kinds would report it as
        // unresolvable, so this assertion is what makes the declaration/TYPE_USE line load-bearing.
        assertTrue(result.unresolved().isEmpty(),
                "a DECLARATION annotation must not be closed over: " + result.unresolved());
        assertEquals(1, result.distinctAnnotations(), "exactly tu.TU, not decl.Decl");
    }

    /**
     * ⚠ CONTROL, AND THE ONE THAT DEFINES THE SCOPE. {@code @tu.Decl} is a DECLARATION annotation in exactly
     * the same jar, unresolvable in exactly the same way — and javac tolerates it, so closing over it would be
     * a widening with no defect behind it. Without this control the check could fire on every annotation
     * anywhere and still look correct.
     */
    @DisplayName("CONTROL: a declaration annotation is NOT closed over")
    @Test
    public void declarationAnnotationIsNotClosedOver() {
        SourceSet declPart = library("declarationOnly", declarationOnly);
        SourceSet declProvider = library("declAnnotations", declAnnotations);
        SourceSet app = consumer(List.of(declPart));

        TypeUseAnnotationClosure.Result result =
                new TypeUseAnnotationClosure().close(List.of(app), List.of(declPart, declProvider));

        assertTrue(result.added().isEmpty(), "" + result.added());
        assertTrue(result.unresolved().isEmpty(), "" + result.unresolved());
        assertEquals(0, result.carrierLocations(), "no location carries a TYPE annotation here");
    }

    /** ⚠ CONTROL: a consumer that can already resolve the annotation must not be touched. */
    @DisplayName("CONTROL: a provider already on the classpath is not added twice")
    @Test
    public void alreadyResolvableIsLeftAlone() {
        SourceSet carrierPart = library("carrier", carrier);
        SourceSet annotationPart = library("annotations", annotations);
        SourceSet app = consumer(List.of(carrierPart, annotationPart));

        TypeUseAnnotationClosure.Result result =
                new TypeUseAnnotationClosure().close(List.of(app), List.of(carrierPart, annotationPart));

        assertTrue(result.added().isEmpty(), "" + result.added());
        assertTrue(result.unresolved().isEmpty(), "" + result.unresolved());
        assertEquals(1, result.carrierLocations(), "the carrier is still found; it simply needs nothing");
    }

    /**
     * ⛔ THE CASE NO RE-POINTING CAN FIX, and the reason this reports rather than only repairs: on
     * elasticsearch, {@code lombok.NonNull} is carried by {@code msal4j} and {@code testcontainers} and
     * provided by nothing on any classpath. A closure that silently did nothing here would be indistinguishable
     * from one that found nothing.
     */
    @DisplayName("an annotation nothing declares is REPORTED, not silently skipped")
    @Test
    public void unprovidedAnnotationIsReported() {
        SourceSet carrierPart = library("carrier", carrier);
        SourceSet app = consumer(List.of(carrierPart));

        TypeUseAnnotationClosure.Result result =
                new TypeUseAnnotationClosure().close(List.of(app), List.of(carrierPart));

        assertTrue(result.added().isEmpty(), "" + result.added());
        assertEquals(1, result.unresolved().size(), "" + result.unresolved());
        TypeUseAnnotationClosure.Unresolved u = result.unresolved().getFirst();
        assertEquals("tu.TU", u.annotation());
        assertEquals("app/main", u.sourceSet());
        assertEquals("carrier", u.carrier());
        assertEquals("lib.Carrier", u.example(), "the class that carries it, so the report can be checked");
        assertTrue(u.why().contains("no classpath part declares it"), u.why());
    }

    /**
     * ⛔⛔ THE REGRESSION. Two providers of the same annotation, identical but for a {@code module-info}: the
     * one WITH the descriptor must win. It used to lose, twice over, and both losses came from counting
     * {@code module-info} as a type it provides — here the size comparison, "smallest wins", which the
     * descriptor puts the modular copy one entry the wrong side of.
     *
     * <p>▶ WHY IT MATTERS BEYOND TIDINESS: what this closure adds lands on a real source set's classpath. A
     * provider that is not a module is one the JDK can only name from its FILE NAME, so choosing it turns a
     * source set that resolved cleanly on the module path into one that no longer does — inside the very
     * configuration that is used to measure exactly that.
     */
    @DisplayName("between two providers alike but for a module-info, the MODULE is chosen")
    @Test
    public void aModularProviderBeatsAnIdenticalNonModularOne() {
        SourceSet carrierPart = library("carrier", carrier);
        SourceSet plain = library("annotations", annotations);
        SourceSet modular = library("annotationsModular", annotationsModular);
        SourceSet app = consumer(List.of(carrierPart));

        TypeUseAnnotationClosure.Result result = new TypeUseAnnotationClosure()
                .close(List.of(app), List.of(carrierPart, plain, modular));

        assertEquals(List.of("annotationsModular"),
                result.added().get("app/main").stream().map(SourceSet::name).toList(),
                "the provider carrying a descriptor must be preferred");
        assertTrue(result.unresolved().isEmpty(), "" + result.unresolved());
    }

    /**
     * ⛔ THE MECHANISM, ISOLATED — and the half that no size comparison explains. The consumer's own
     * dependency is a module, so {@code module-info} was in the set of names "already resolved here". Every
     * OTHER module then looked like it would shadow a resolved type, and the modular provider was discarded
     * before its real classes were ever compared. One modular jar anywhere on a classpath was enough to make
     * every modular provider unpickable.
     *
     * <p>⚠ {@code other.mod} shares not one class with either provider, so the only thing it can contribute
     * to the shadow test is the descriptor — which is the whole point.
     */
    @DisplayName("a module on the classpath does not make every other module look like a shadow")
    @Test
    public void anAlreadyVisibleModuleInfoDoesNotVetoModularProviders() {
        SourceSet carrierPart = library("carrier", carrier);
        SourceSet unrelatedModule = library("aModule", aModule);
        SourceSet plain = library("annotations", annotations);
        SourceSet modular = library("annotationsModular", annotationsModular);
        SourceSet app = consumer(List.of(carrierPart, unrelatedModule));

        TypeUseAnnotationClosure.Result result = new TypeUseAnnotationClosure()
                .close(List.of(app), List.of(carrierPart, unrelatedModule, plain, modular));

        assertEquals(List.of("annotationsModular"),
                result.added().get("app/main").stream().map(SourceSet::name).toList(),
                "the shadow test must not fire on module-info, which is not a type anyone can reference");
        assertTrue(result.unresolved().isEmpty(), "" + result.unresolved());
    }
}
