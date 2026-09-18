package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.util.function.Function;

/**
 * A second front end's share of the parse of ONE source set, when that set's Java sources and another language's
 * sources reference each other in both directions (a mixed Java+Kotlin module: javalin's {@code Handler.java}
 * takes a Kotlin {@code Context}, and 28 Kotlin files take a {@code Handler}).
 *
 * <p>Neither front end can go first. javac cannot attribute the Java sources until the other language's
 * declarations exist as class files it can read (stubs, generated from their CST); those declarations name Java
 * types, which must then already be the very {@link TypeInfo} instances this scan will fill; and the other
 * language's bodies call Java members, which exist only once this scan has run. So the scan stops twice:
 * <ol>
 *     <li>{@link #beforeAttribution}: the compilation units are built and registered, nothing is attributed. The
 *     other front end declares its types and signatures, asking for the Java types it names through
 *     {@code sourceTypes}; then it writes the stubs javac is about to read.</li>
 *     <li>{@link #afterCommit}: every Java type of the set is committed; the other front end converts its bodies.</li>
 * </ol>
 * The scan in between is the ordinary one: {@code ScanCompilationUnit.visitClass} adopts a type that was
 * registered for its own source set, as it already does for one a forward reference loaded from its symbol.
 */
public interface SourceSetInterleave {

    /**
     * @param sourceTypes the uncommitted, memberless {@link TypeInfo} of one of this set's Java source types, by
     *                    fully qualified name, registered on first request (with every type nested in the same
     *                    primary type); {@code null} when the set declares no such type
     */
    void beforeAttribution(SourceSet sourceSet, Function<String, TypeInfo> sourceTypes);

    /** Every Java type of [sourceSet] is committed, and in the {@code CompiledTypesManager}. */
    void afterCommit(SourceSet sourceSet);
}
