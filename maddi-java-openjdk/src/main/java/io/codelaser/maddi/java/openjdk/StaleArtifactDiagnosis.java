package io.codelaser.maddi.java.openjdk;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return message(typeDescriptor, committedOrigin, sourceSetName, incomingSymbolIsSource, incomingOrigin,
                memberBeingAdded, existingMembers, List.of());
    }

    /**
     * ⛔ <b>THE SECOND CAUSE, AND THE JAR/NO-JAR DICHOTOMY COULD NOT SEE IT.</b> When neither side is a jar the
     * message used to end <i>"the member lists above are the evidence to read"</i> — and that sentence sent two
     * independent readers, within 24 hours, to the same wrong conclusion: that a compact record constructor is
     * modelled without its canonical parameters. It is not ({@code TestCompactConstructorProbe} pins it). The
     * committed source had simply failed to compile: {@code jackson-annotations} was missing from that source
     * set's parse classpath, javac reported {@code package com.fasterxml.jackson.annotation does not exist} 50
     * times in the one file, and its ERROR RECOVERY truncated the annotated record header — so the canonical
     * constructor was modelled with 1 of its 5 parameters and disagreed with the class file.
     * <p>
     * ⚠ <b>The member lists are then an ARTIFACT OF THE ERRORS, not evidence about the member being added.</b>
     * Acting on them is expensive: synthesising the canonical constructor "to fix it" collides with the one this
     * path already builds from javac's {@code JCMethodDecl}, 62 failures in this module alone. So the errors are
     * hoisted ABOVE the jar verdict and the reader is steered to the classpath first.
     * <p>
     * Same disease as the rest of this class: retrievable facts (the scanner is holding the diagnostics when it
     * throws), arranged so that nobody could act on them.
     *
     * @param javacErrorsInCommittedSource javac's own errors for the committed definition's file, each already
     *                                     rendered as one line. Empty when that file compiled cleanly.
     */
    public static String message(String typeDescriptor,
                                 URI committedOrigin,
                                 String sourceSetName,
                                 boolean incomingSymbolIsSource,
                                 URI incomingOrigin,
                                 String memberBeingAdded,
                                 List<String> existingMembers,
                                 List<String> javacErrorsInCommittedSource) {
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
        // ⛔ ABOVE the jar verdict on purpose: when the committed file did not compile, the member lists just
        // printed are an artifact of javac's error recovery, and the reader must be told before they read them.
        appendJavacErrors(sb, javacErrorsInCommittedSource, sourceSetName);
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
        } else if (!javacErrorsInCommittedSource.isEmpty()) {
            // ⚠ NOT "read the member lists": that steer is what produced two wrong diagnoses in 24 hours.
            sb.append("⚠ Neither definition came from a jar, so this is NOT the stale-artifact case — and the")
                    .append(" member\n   lists above are NOT the evidence to read either, because the committed")
                    .append(" file did not compile.\n   START WITH THE JAVAC ERRORS.");
        } else {
            sb.append("⚠ Neither definition came from a jar, so this is NOT the stale-artifact case; the two")
                    .append("\n   arrivals disagree for another reason and the member lists above are the evidence")
                    .append(" to read.");
        }
        return sb.toString();
    }

    /** How many distinct javac errors to print before summarising; a truncated list must still say what it dropped. */
    private static final int MAX_ERRORS_SHOWN = 5;

    /**
     * ⚠ Deduplicated and capped, but never SILENTLY: one missing package produces one error per use site (50, in
     * the case this was written for), and a reader who is shown five of them without the total cannot tell whether
     * the file has a typo or no classpath at all. The count is the signal — "50 errors in one file" IS the
     * diagnosis, and printing all 50 would bury the verdict below.
     */
    private static void appendJavacErrors(StringBuilder sb, List<String> errors, String sourceSetName) {
        if (errors.isEmpty()) return;
        // ⚠ Grouped by the error TEXT, not the whole line. One missing package produces the SAME message at 50
        // different line numbers, so deduplicating whole lines would collapse nothing and the reader would see
        // five near-identical lines with no idea that a single import is behind all of them. "50 errors, 1
        // distinct" is the sentence that says "this file has no classpath", and it only appears if the line
        // number is off the key. The first line carrying each text is the one shown, so a line number survives.
        LinkedHashMap<String, String> firstLineByText = new LinkedHashMap<>();
        for (String e : errors) firstLineByText.putIfAbsent(textOf(e), e);
        List<String> distinct = List.copyOf(firstLineByText.values());
        sb.append("⛔ THE COMMITTED DEFINITION'S FILE DID NOT COMPILE: ").append(errors.size())
                .append(" javac error(s)");
        if (distinct.size() < errors.size()) sb.append(", ").append(distinct.size()).append(" distinct");
        sb.append('\n');
        for (String e : distinct.stream().limit(MAX_ERRORS_SHOWN).toList()) {
            sb.append("      ").append(e).append('\n');
        }
        if (distinct.size() > MAX_ERRORS_SHOWN) {
            sb.append("      ... and ").append(distinct.size() - MAX_ERRORS_SHOWN)
                    .append(" further distinct error(s)\n");
        }
        sb.append("   javac's ERROR RECOVERY models a TRUNCATED type from a file it could not resolve: a record")
                .append("\n   header whose annotations do not resolve loses components, so a constructor can be")
                .append(" modelled\n   with FEWER PARAMETERS than the source declares, and then disagrees with the")
                .append(" class file.\n   ⇒ THE MEMBER LISTS ABOVE ARE AN ARTIFACT OF THESE ERRORS, not evidence")
                .append(" about the member\n   being added. A missing dependency is the usual cause: fix the parse")
                .append(" classpath of source set\n   ").append(sourceSetName == null ? "(unknown)" : sourceSetName)
                .append(", then parse again.\n");
    }

    /** The error text with a leading {@code "line N: "} stripped, so the same complaint groups across line numbers. */
    private static String textOf(String renderedError) {
        Matcher m = LINE_PREFIX.matcher(renderedError);
        return m.lookingAt() ? renderedError.substring(m.end()) : renderedError;
    }

    private static final Pattern LINE_PREFIX = Pattern.compile("line \\d+: ");

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
