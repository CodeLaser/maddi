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
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Closes each source set's classpath over the <b>TYPE_USE annotations its dependencies carry in their bytecode</b>.
 *
 * <p>⛔ <b>A COMPILE CLASSPATH IS NOT SUCH A CLOSURE.</b> A build tool computes a source set's classpath from its
 * declared dependencies, and an annotation a dependency used at <i>its</i> compile time is typically declared
 * {@code compileOnly} / {@code provided} — which is not transitive. The class file keeps the annotation; the
 * consumer's classpath does not keep the annotation's class.
 *
 * <p>That is harmless for a DECLARATION annotation: javac notes the reference and moves on, which is why an
 * ordinary parse log carries dozens of {@code class file for ... not found} lines and stays green. A TYPE_USE
 * annotation is <b>part of the type</b> and has to be resolved while attributing, so reading such a signature
 * either loses the annotation silently or fails outright, depending on where the reader stands.
 *
 * <p>▶ THE RULE, stated so it can be falsified:
 * <pre>
 * for every source set S,
 *     for every entry E on S's classpath,
 *         for every class in E carrying a Runtime[In]VisibleTypeAnnotations attribute,
 *             every annotation type it names must be resolvable from S's own classpath.
 * </pre>
 *
 * <p>⚠ <b>WHAT IT ADDS IS DERIVED AND NAMED, NEVER A BUILT-IN LIST.</b> The workaround this replaces (an
 * elasticsearch recipe script) appended one hard-coded jar path to every source set. Measured on a 348-source-set
 * elasticsearch parse, that covers none of the three live cases: {@code org.jetbrains.annotations.NotNull}
 * (carried by {@code orc-core}, provided by a jar on exactly one other source set),
 * {@code org.checkerframework.checker.nullness.qual.Nullable} (carried by {@code jimfs}), and
 * {@code lombok.NonNull} (carried by {@code msal4j} and {@code testcontainers}, and provided by
 * <i>nothing on any classpath</i> — so no amount of re-pointing fixes it and it has to be REPORTED).
 *
 * <p>⚠ <b>AND THE WIDENING IS CHECKED, NOT ASSUMED SAFE.</b> "An annotation jar can neither shadow nor
 * re-resolve another type" is true of the jars people reach for and is not a property of every provider, so a
 * provider is admitted only when it brings nothing the source set already resolves elsewhere. A rejected
 * provider is reported rather than forced.
 */
public class TypeUseAnnotationClosure {
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeUseAnnotationClosure.class);

    /** The attribute names, as they appear in a class file's constant pool. Used as a cheap prefilter. */
    private static final byte[] VISIBLE = "RuntimeVisibleTypeAnnotations".getBytes();
    private static final byte[] INVISIBLE = "RuntimeInvisibleTypeAnnotations".getBytes();

    /**
     * @param sourceSets the source sets, each with the dependencies it should now have
     * @param added      per source set name, the classpath parts this closure added
     * @param unresolved a type annotation that stays unresolvable: nothing declared provides it, or the only
     *                   provider would shadow
     */
    public record Result(List<SourceSet> sourceSets,
                         Map<String, List<SourceSet>> added,
                         List<Unresolved> unresolved,
                         int carrierLocations,
                         int distinctAnnotations) {
    }

    public record Unresolved(String sourceSet, String annotation, String carrier, String example, String why) {
    }

    /**
     * @param sourceSets     the compiled source sets, in the order they will be parsed
     * @param classPathParts the libraries, jmod entries included (those are skipped: they hold no class files
     *                       this can read, and {@code java.*}/{@code jdk.*} annotations are resolvable anyway)
     */
    public Result close(List<SourceSet> sourceSets, List<SourceSet> classPathParts) {
        // ⚠ IDENTITY, NOT NAME. A library and a source set can carry the same name -- that collision is a
        // separate defect, and keying this on names would make this code inherit it.
        Map<SourceSet, Set<String>> provides = new IdentityHashMap<>();
        Map<SourceSet, Map<String, String>> carries = new IdentityHashMap<>();
        Set<SourceSet> modular = Collections.newSetFromMap(new IdentityHashMap<>());
        List<SourceSet> locations = Stream.concat(sourceSets.stream(), classPathParts.stream())
                .filter(s -> !s.partOfJdk() && "file".equals(s.uri().getScheme()))
                .toList();
        for (SourceSet location : locations) {
            index(location, provides, carries, modular);
        }
        int distinct = (int) carries.values().stream().flatMap(m -> m.keySet().stream()).distinct().count();
        LOGGER.info("Type-use annotations: {} of {} classpath location(s) carry one, {} distinct annotation type(s)",
                carries.size(), locations.size(), distinct);

        List<SourceSet> result = new ArrayList<>(sourceSets.size());
        Map<String, List<SourceSet>> added = new LinkedHashMap<>();
        List<Unresolved> unresolved = new ArrayList<>();
        for (SourceSet sourceSet : sourceSets) {
            Set<String> visible = new HashSet<>(provides.getOrDefault(sourceSet, Set.of()));
            for (SourceSet dependency : sourceSet.dependencies()) {
                visible.addAll(provides.getOrDefault(dependency, Set.of()));
            }
            List<SourceSet> extra = new ArrayList<>();
            for (SourceSet dependency : sourceSet.dependencies()) {
                Map<String, String> annotations = carries.get(dependency);
                if (annotations == null) continue;
                for (Map.Entry<String, String> e : annotations.entrySet()) {
                    String annotation = e.getKey();
                    if (visible.contains(annotation) || resolvableFromTheJdk(annotation)) continue;
                    SourceSet provider = pickProvider(annotation, provides, visible, modular);
                    if (provider == null) {
                        unresolved.add(new Unresolved(sourceSet.name(), annotation, dependency.name(), e.getValue(),
                                provides.values().stream().noneMatch(p -> p.contains(annotation))
                                        ? "no classpath part declares it"
                                        : "its only provider(s) would shadow types already resolved here"));
                        continue;
                    }
                    extra.add(provider);
                    visible.addAll(provides.get(provider));
                }
            }
            if (extra.isEmpty()) {
                result.add(sourceSet);
            } else {
                added.put(sourceSet.name(), List.copyOf(extra));
                List<SourceSet> dependencies = new ArrayList<>(sourceSet.dependencies());
                dependencies.addAll(extra);
                result.add(sourceSet.withDependencies(List.copyOf(dependencies)));
                LOGGER.info("Type-use annotation closure: {} += {}", sourceSet.name(),
                        extra.stream().map(SourceSet::name).toList());
            }
        }
        // NO SILENT CAPS IN EITHER DIRECTION. A closure that quietly widens every classpath is as bad as one
        // that quietly widens none, so what could NOT be closed is named too.
        for (Unresolved u : unresolved) {
            LOGGER.warn("Type-use annotation {} stays unresolvable in {} (carried by {}, e.g. {}): {}",
                    u.annotation(), u.sourceSet(), u.carrier(), u.example(), u.why());
        }
        return new Result(List.copyOf(result), Map.copyOf(added), List.copyOf(unresolved), carries.size(), distinct);
    }

    /** {@code java.*} and {@code jdk.*} come from the jmod parts, which hold no class files to index here. */
    private static boolean resolvableFromTheJdk(String annotation) {
        return annotation.startsWith("java.") || annotation.startsWith("jdk.");
    }

    /**
     * The smallest provider that brings nothing already resolved. Smallest first because a narrow annotation
     * jar is the intended answer and a fat library that happens to shadow-freely contain the class is not.
     *
     * <p>⭐ <b>TIES ARE BROKEN TOWARDS A PROVIDER THAT IS ITSELF A MODULE</b>, and that is not cosmetic. This
     * closure widens a source set's classpath; a jar carrying neither a {@code module-info} nor an
     * {@code Automatic-Module-Name} is one the JDK can only name from its FILE NAME, so admitting one turns a
     * source set that resolved cleanly on the module path into one that no longer does. The closure exists to
     * make an annotation resolvable and must not pay for that in module resolution, which is a property the
     * configuration it produces is used to MEASURE.
     *
     * <p>⚠ Size still decides first, so a modular fat library never beats a narrow annotation jar — that is
     * the rule above and it is unchanged. Modularity only separates candidates that are equally narrow, which
     * is exactly the case that occurs in practice: an IDE distribution ships the same annotation classes as
     * the published artifact, minus its descriptor. The name is the last resort, so the answer cannot depend
     * on {@link IdentityHashMap} iteration order.
     */
    private static SourceSet pickProvider(String annotation, Map<SourceSet, Set<String>> provides,
                                          Set<String> visible, Set<SourceSet> modular) {
        SourceSet best = null;
        int bestSize = Integer.MAX_VALUE;
        boolean bestModular = false;
        for (Map.Entry<SourceSet, Set<String>> e : provides.entrySet()) {
            if (!e.getValue().contains(annotation) || e.getValue().size() > bestSize) continue;
            boolean isModular = modular.contains(e.getKey());
            if (e.getValue().size() == bestSize && best != null
                && !betterOnATie(isModular, bestModular, e.getKey().name(), best.name())) continue;
            boolean shadows = e.getValue().stream().anyMatch(c -> !c.equals(annotation) && visible.contains(c));
            if (shadows) continue;
            best = e.getKey();
            bestSize = e.getValue().size();
            bestModular = isModular;
        }
        return best;
    }

    /** Modular beats non-modular; otherwise the lower name, so the choice is stable across runs. */
    private static boolean betterOnATie(boolean candidateModular, boolean bestModular, String candidateName,
                                        String bestName) {
        if (candidateModular != bestModular) return candidateModular;
        return candidateName.compareTo(bestName) < 0;
    }

    /**
     * ⛔⛔ <b>{@code module-info} AND {@code package-info} ARE NOT TYPES, AND MUST NOT ENTER {@code provides}.</b>
     * Nothing can reference them, so they can never be the annotation being looked for — and letting them in
     * corrupts both tests that {@link #pickProvider} runs, in the same direction:
     *
     * <ul>
     *   <li><b>The shadow test rejects modular jars outright.</b> Every multi-release modular jar contributes
     *       the same pseudo-type {@code META-INF.versions.9.module-info}, and a plain one contributes
     *       {@code module-info}. One such jar anywhere on a source set's classpath therefore puts that name
     *       into {@code visible}, and every OTHER modular jar then looks like it would shadow a type already
     *       resolved there. The better provider is discarded before its real classes are ever compared.</li>
     *   <li><b>The size test is off by one, always against the module.</b> A descriptor adds an entry, so a
     *       modular jar counts one class more than a byte-identical non-modular copy of itself and loses the
     *       "smallest wins" comparison.</li>
     * </ul>
     *
     * <p>▶ Both fire together on a real pair: an IDE distribution's {@code annotations.jar} and the published
     * {@code annotations-<version>.jar} differ by <b>exactly one entry</b>, the descriptor. The IDE copy won
     * every time, and every source set the closure touched then carried a jar the JDK can only name from its
     * file name. Measured on a 55-unit tree: 31 units reported a filename-derived module for this reason and
     * 18 had no other reason at all — while the real compile classpath had the modular artifact on 79 source
     * sets and the IDE copy on 2.
     */
    private static boolean isNotAType(String binaryName) {
        int lastDot = binaryName.lastIndexOf('.');
        String simple = lastDot < 0 ? binaryName : binaryName.substring(lastDot + 1);
        return "module-info".equals(simple) || "package-info".equals(simple);
    }

    private void index(SourceSet location, Map<SourceSet, Set<String>> provides,
                       Map<SourceSet, Map<String, String>> carries, Set<SourceSet> modular) {
        Path path = Path.of(location.uri().getPath());
        if (path.toString().endsWith(".jar")) {
            if (!Files.isRegularFile(path)) return;
            try (ZipFile zip = new ZipFile(path.toFile())) {
                if (hasAutomaticModuleName(zip)) modular.add(location);
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.getName().endsWith(".class")) continue;
                    String binaryName = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                    if (isNotAType(binaryName)) {
                        if (binaryName.endsWith("module-info")) modular.add(location);
                        continue;
                    }
                    provides.computeIfAbsent(location, s -> new HashSet<>()).add(binaryName);
                    try (var in = zip.getInputStream(entry)) {
                        scan(in.readAllBytes(), binaryName, location, carries);
                    }
                }
            } catch (IOException io) {
                // A classpath entry that does not exist, or is not a zip, is the NORMAL STATE of a real build:
                // a working elasticsearch parse has 117 of them. Nothing to index is not an error.
                LOGGER.debug("Cannot read jar {}: {}", path, io.getMessage());
            }
            return;
        }
        if (!Files.isDirectory(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".class")).toList()) {
                String binaryName = path.relativize(file).toString()
                        .replace(java.io.File.separatorChar, '.');
                binaryName = binaryName.substring(0, binaryName.length() - 6);
                if (isNotAType(binaryName)) {
                    if (binaryName.endsWith("module-info")) modular.add(location);
                    continue;
                }
                provides.computeIfAbsent(location, s -> new HashSet<>()).add(binaryName);
                scan(Files.readAllBytes(file), binaryName, location, carries);
            }
        } catch (IOException io) {
            throw new UncheckedIOException(io);
        }
    }

    /**
     * A jar with {@code Automatic-Module-Name} has an identity of its own too — the JDK does not have to guess
     * one from the file name — so it counts as modular for the tie-break, even though it carries no descriptor.
     */
    private static boolean hasAutomaticModuleName(ZipFile zip) throws IOException {
        ZipEntry manifest = zip.getEntry("META-INF/MANIFEST.MF");
        if (manifest == null) return false;
        try (var in = zip.getInputStream(manifest)) {
            return new java.util.jar.Manifest(in).getMainAttributes().getValue("Automatic-Module-Name") != null;
        }
    }

    /**
     * ⚠ THE PREFILTER CAN ONLY BE WRONG IN THE SAFE DIRECTION. A class file cannot carry the attribute without
     * its NAME in the constant pool, so a false negative is impossible; a false positive costs one ASM parse.
     */
    private void scan(byte[] bytes, String binaryName, SourceSet location,
                      Map<SourceSet, Map<String, String>> carries) {
        if (indexOf(bytes, VISIBLE) < 0 && indexOf(bytes, INVISIBLE) < 0) return;
        Collector collector = new Collector();
        try {
            // SKIP_CODE: a type annotation inside a method BODY is not part of any signature a reader has to
            // attribute, and skipping the code attribute is most of the parse cost.
            new ClassReader(bytes).accept(collector, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG
                                                     | ClassReader.SKIP_FRAMES);
        } catch (RuntimeException re) {
            LOGGER.debug("ASM cannot read {} in {}: {}", binaryName, location.name(), re.getMessage());
            return;
        }
        for (String annotation : collector.annotations) {
            carries.computeIfAbsent(location, s -> new HashMap<>()).putIfAbsent(annotation, binaryName);
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    /** Every position a type annotation can occupy in a SIGNATURE; method bodies are skipped by SKIP_CODE. */
    private static class Collector extends ClassVisitor {
        private final Set<String> annotations = new LinkedHashSet<>();

        Collector() {
            super(Opcodes.ASM9);
        }

        private AnnotationVisitor record(String descriptor) {
            annotations.add(Type.getType(descriptor).getClassName());
            return null;
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor,
                                                     boolean visible) {
            return record(descriptor);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc,
                                                             boolean visible) {
                    return record(desc);
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc,
                                                             boolean visible) {
                    return record(desc);
                }
            };
        }

        @Override
        public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
            return new RecordComponentVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc,
                                                             boolean visible) {
                    return record(desc);
                }
            };
        }
    }
}
