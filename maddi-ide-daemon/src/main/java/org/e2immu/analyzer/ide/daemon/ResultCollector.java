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

import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.Summary;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the two maddi output channels — {@code List<Message>} (findings) and the computed properties
 * on each CST {@code Info} (element annotations) — into the plain-JSON {@link DaemonProtocol.Result}
 * shape. No maddi types leak: everything becomes strings/ints.
 * <p>
 * Findings follow {@code ErrorReport.location(Message)}; element annotations come from
 * {@link AnnotationTagger} (the full, polarity/context-tagged set).
 */
public class ResultCollector {
    /** Defensive bound on the recursive why-chain depth. */
    private static final int MAX_CAUSE_DEPTH = 12;

    private final AnnotationTagger tagger;

    public ResultCollector(Runtime runtime, SourceSet sourceSetOfRequest) {
        this.tagger = new AnnotationTagger(runtime, sourceSetOfRequest);
    }

    // ---- findings ----

    public List<DaemonProtocol.Finding> collectFindings(List<Message> messages, Summary summary) {
        List<DaemonProtocol.Finding> findings = new ArrayList<>();
        if (messages != null) {
            for (Message m : messages) findings.add(toFinding(m, MAX_CAUSE_DEPTH));
        }
        findings.addAll(parseFindings(summary));
        return findings;
    }

    /** Parse errors/warnings only — used when the project has parse errors and {@code parseResult()} is unsafe. */
    public List<DaemonProtocol.Finding> parseFindings(Summary summary) {
        List<DaemonProtocol.Finding> findings = new ArrayList<>();
        for (Summary.ParseException pe : summary.parseExceptions()) findings.add(toFinding(pe));
        for (Summary.ParseException pe : summary.parseWarnings()) findings.add(toFinding(pe));
        return findings;
    }

    private DaemonProtocol.Finding toFinding(Message m, int depth) {
        Info info = m.info();
        String uri = uriOf(info);
        Source source = m.source();
        String severity = m.level() != null && m.level().isError() ? "ERROR" : "WARN";
        List<DaemonProtocol.Finding> causes = new ArrayList<>();
        if (depth > 0 && m.causes() != null) {
            for (Message cause : m.causes()) causes.add(toFinding(cause, depth - 1));
        }
        return finding(uri, source, severity, m.category(), m.message(), causes);
    }

    private DaemonProtocol.Finding toFinding(Summary.ParseException pe) {
        String uri = pe.where() instanceof Info info ? uriOf(info) : null;
        String severity = pe.level() != null && pe.level().isError() ? "ERROR" : "WARN";
        return finding(uri, pe.source(), severity, "parse", pe.getMessage(), List.of());
    }

    private static DaemonProtocol.Finding finding(String uri, Source source, String severity,
                                                  String category, String message,
                                                  List<DaemonProtocol.Finding> causes) {
        Integer bl = null, bc = null, el = null, ec = null;
        if (source != null && !source.isNoSource()) {
            bl = source.beginLine();
            bc = source.beginPos();
            el = source.endLine();
            ec = source.endPos();
        }
        return new DaemonProtocol.Finding(uri, bl, bc, el, ec, severity, category, message, causes);
    }

    // ---- element annotations ----

    public List<DaemonProtocol.ElementAnnotation> collectElementAnnotations(Collection<TypeInfo> primaryTypes) {
        List<DaemonProtocol.ElementAnnotation> out = new ArrayList<>();
        for (TypeInfo primaryType : primaryTypes) {
            primaryType.recursiveSubTypeStream().forEach(type -> {
                addElement(out, type);
                type.fields().forEach(field -> addElement(out, field));
                type.constructorAndMethodStream().forEach(method -> {
                    addElement(out, method);
                    for (var parameter : method.parameters()) addElement(out, parameter);
                });
            });
        }
        return out;
    }

    /**
     * Annotations for exactly the elements given, as delivered by {@code AnalysisValueFeed} — which hands
     * over the elements analyzed in a pass, not primary types. A method brings its parameters along: their
     * values are computed with the method but they are separate {@code Info}s, and the feed does not list
     * them.
     * <p>
     * Must be called while the analysis workers are quiescent (the feed's pass boundary): it reads
     * {@code info.analysis()} live.
     */
    public List<DaemonProtocol.ElementAnnotation> collectForElements(Collection<Info> infos) {
        List<DaemonProtocol.ElementAnnotation> out = new ArrayList<>();
        for (Info info : infos) {
            addElement(out, info);
            if (info instanceof MethodInfo method) {
                for (var parameter : method.parameters()) addElement(out, parameter);
            }
        }
        return out;
    }

    private void addElement(List<DaemonProtocol.ElementAnnotation> out, Info info) {
        if (info.isSynthetic()) return;
        Source source = info.source();
        if (source == null || source.isNoSource()) return;
        String uri = uriOf(info);
        if (uri == null) return;

        List<DaemonProtocol.Annotation> annotations = tagger.tag(info);
        List<String> displayAnnotations = annotations.stream().map(DaemonProtocol.Annotation::text).toList();

        Map<String, String> properties = new LinkedHashMap<>();
        info.analysis().propertyValueStream().forEach(pv ->
                properties.put(pv.property().key(), String.valueOf(pv.value())));

        // nothing computed and nothing to show → skip, to keep the payload lean
        if (displayAnnotations.isEmpty() && properties.isEmpty()) return;

        out.add(new DaemonProtocol.ElementAnnotation(
                uri, source.beginLine(), source.beginPos(), source.endLine(), source.endPos(),
                kindOf(info), info.fullyQualifiedName(), displayAnnotations, annotations, properties));
    }

    private static String kindOf(Info info) {
        if (info instanceof TypeInfo) return "TYPE";
        if (info instanceof MethodInfo) return "METHOD";
        if (info instanceof FieldInfo) return "FIELD";
        if (info instanceof ParameterInfo) return "PARAMETER";
        return "OTHER";
    }

    private static String uriOf(Info info) {
        if (info == null || info.compilationUnit() == null) return null;
        URI uri = info.compilationUnit().uri();
        return uri == null ? null : uri.toString();
    }
}
