package org.e2immu.language.java.openjdk;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * The refusal message for the one shape a parse cannot recover from and could not explain: <b>the same type
 * arriving twice, from source and from a stale artifact, with different members</b>.
 * <p>
 * <b>WHY IT EXISTS — "rung 6 poisons rung 1".</b> A distribution build leaves jars behind. The parse then reads
 * such a jar as a <em>second, stale definition</em> of every type edited afterwards, and the scanner meets the
 * type twice: once committed from the jar, once again from source with a different constructor descriptor. It
 * had all three facts in hand — the type, the member being added, the members already present — logged them,
 * and then threw a <b>bare</b> {@code UnsupportedOperationException}. The top-level verdict was
 * <i>"Can only switch to ParseResult when there are no parse exceptions"</i>, which names neither the type nor
 * the jar and reads as a SOURCE problem. It cost a session: 18 units dropped, <b>10 of them never touched by
 * the edit</b>, and the failure looked exactly like a resolver bug while javac was green.
 * <p>
 * ⚠ <b>The information was never missing — the presentation was.</b> Same disease as {@code #135} (a report
 * that named the thing and was banked unread) and as the {@code r53} sweep printing evidence with no claim
 * beside it: retrievable facts, arranged so that nobody could act on them.
 * <p>
 * On the client build there WILL be jars we did not build, so this is a day-one message, not an ES one.
 */
public class StaleArtifactDiagnosis {

    private StaleArtifactDiagnosis() {
    }

    /**
     * ⚠ <b>THE JAR CAN BE ON EITHER SIDE, and the first version of this message only handled one.</b> It asked
     * whether the COMMITTED definition was jar-backed, and printed <i>"this is NOT the stale-artifact case"</i>
     * while looking straight at a stale jar — because in the reproduction the SOURCE committed first and the jar
     * arrived second. The ES incident happened to be the other polarity (a plugin bundle committed first), which
     * is exactly why one worked example is not a specification. ⇒ both arrivals are inspected.
     *
     * @param typeDescriptor         the type that is already committed
     * @param committedOrigin        where the committed definition came from
     * @param sourceSetName          the source set it was attributed to, or null
     * @param incomingSymbolIsSource whether the SECOND arrival is being parsed from source
     * @param incomingOrigin         the class file behind the second arrival, when it is not source; may be null
     * @param memberBeingAdded       the member that cannot be added any more
     * @param existingMembers        the members already present, so the disagreement is visible
     */
    public static String message(String typeDescriptor,
                                 URI committedOrigin,
                                 String sourceSetName,
                                 boolean incomingSymbolIsSource,
                                 URI incomingOrigin,
                                 String memberBeingAdded,
                                 List<String> existingMembers) {
        Path committedJar = jarFileOf(committedOrigin);
        Path incomingJar = incomingSymbolIsSource ? null : jarFileOf(incomingOrigin);
        StringBuilder sb = new StringBuilder();
        sb.append("Type ").append(typeDescriptor).append(" is already committed, and a second definition of it")
                .append(" is now arriving ").append(incomingSymbolIsSource ? "FROM SOURCE" : "FROM A COMPILED ARTIFACT")
                .append(" with a member the committed one does not have.\n");
        sb.append("  committed definition came from: ").append(committedOrigin == null ? "(unknown)" : committedOrigin);
        if (sourceSetName != null) sb.append("  [source set ").append(sourceSetName).append(']');
        sb.append('\n');
        if (!incomingSymbolIsSource) {
            sb.append("  second definition came from:    ")
                    .append(incomingOrigin == null ? "(a compiled artifact, path unknown)" : incomingOrigin)
                    .append('\n');
        }
        sb.append("  member that cannot be added:    ").append(memberBeingAdded).append('\n');
        if (!existingMembers.isEmpty()) {
            sb.append("  members already present:\n");
            for (String m : existingMembers) sb.append("      ").append(m).append('\n');
        }
        // The discriminating case: ONE side is source and the OTHER is a jar, in either order.
        Path stale = committedJar != null && incomingSymbolIsSource ? committedJar
                : committedJar == null && incomingJar != null ? incomingJar : null;
        if (stale != null) {
            sb.append("⛔ THE JAR IS STALE: ").append(stale);
            String mtime = lastModified(stale);
            if (mtime != null) sb.append(" (last modified ").append(mtime).append(')');
            sb.append("\n   The same type is defined BOTH in source and in that jar, with different members, so")
                    .append(" the jar\n   predates the sources now being parsed.")
                    .append("\n   REBUILD OR DELETE IT and parse again. A distribution/assembly build is the usual")
                    .append(" source of\n   such a jar (\"rung 6 poisons rung 1\"): the parse reads it as a second,")
                    .append(" stale definition of\n   every type edited afterwards, and the units it drops need not")
                    .append(" be the ones you changed.");
        } else if (committedJar != null || incomingJar != null) {
            Path jar = committedJar != null ? committedJar : incomingJar;
            sb.append("⚠ One definition came from a JAR (").append(jar).append("). If that jar predates the")
                    .append(" sources,\n   rebuild or delete it and parse again.");
        } else {
            sb.append("⚠ Neither definition came from a jar, so this is NOT the stale-artifact case; the two")
                    .append("\n   arrivals disagree for another reason and the member lists above are the evidence")
                    .append(" to read.");
        }
        return sb.toString();
    }

    /**
     * The jar file behind a type's origin, or null when it is not jar-backed.
     * <p>
     * ⚠ Deliberately textual and defensive. A jar-backed compilation unit is addressed as
     * {@code jar:file:/path/to/x.jar!/p/q/T.class}, but a type read from an exploded classes directory is a
     * plain {@code file:} URI, and a diagnostic must never be the thing that throws — it runs on the failure
     * path, where the caller has already lost.
     */
    static Path jarFileOf(URI uri) {
        if (uri == null) return null;
        String s = uri.toString();
        int bang = s.indexOf("!/");
        String candidate = bang >= 0 ? s.substring(0, bang) : s;
        if (!candidate.endsWith(".jar")) return null;
        int file = candidate.indexOf("file:");
        if (file < 0) return null;
        try {
            return Path.of(candidate.substring(file + "file:".length()));
        } catch (RuntimeException re) {
            return null;
        }
    }

    /** The artifact's mtime is what lets a reader see for themselves that it predates their edit. */
    static String lastModified(Path path) {
        try {
            if (!Files.exists(path)) return null;
            return Instant.ofEpochMilli(Files.getLastModifiedTime(path).toMillis()).toString();
        } catch (Exception e) {
            return null;
        }
    }
}
