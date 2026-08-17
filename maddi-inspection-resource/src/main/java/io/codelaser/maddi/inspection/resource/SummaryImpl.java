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

package io.codelaser.maddi.inspection.resource;

import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.parser.Summary;

import java.util.*;

public class SummaryImpl implements Summary {
    private final Set<TypeInfo> types = new HashSet<>();
    // one list; errors vs warnings distinguished by ParseException.level() (single source of truth)
    private final List<ParseException> messages = new LinkedList<>();
    private final boolean failFast;
    private final Map<String, SourceSet> sourceSetsByName = new HashMap<>();
    private final Map<SourceSet, ModuleInfo> sourceSetToModuleInfo = new HashMap<>();

    public SummaryImpl(boolean failFast) {
        this.failFast = failFast;
    }

    @Override
    public synchronized void ensureSourceSet(SourceSet sourceSet) {
        sourceSetsByName.putIfAbsent(sourceSet.name(), sourceSet);
    }

    @Override
    public Map<SourceSet, ModuleInfo> sourceSetToModuleInfoMap() {
        return sourceSetToModuleInfo;
    }

    @Override
    public synchronized void putSourceSetToModuleInfo(SourceSet sourceSet, ModuleInfo moduleInfo) {
        sourceSetToModuleInfo.put(sourceSet, moduleInfo);
    }

    @Override
    public Iterable<SourceSet> sourceSets() {
        return sourceSetsByName.values();
    }

    @Override
    public Set<TypeInfo> types() {
        return types;
    }

    @Override
    public ParseResult parseResult() {
        if (haveErrors()) {
            throw new UnsupportedOperationException(refusalMessage());
        }
        return new ParseResultImpl(types, sourceSetsByName, Map.copyOf(sourceSetToModuleInfo));
    }

    @Override
    public synchronized void addType(TypeInfo typeInfo) {
        types.add(typeInfo);
    }

    @Override
    public synchronized void addParseException(ParseException parseException) {
        //LOGGER.error("Register parser error", parseException);
        if (failFast) {
            throw new Summary.FailFastException(parseException);
        }
        this.messages.add(parseException);
    }

    @Override
    public synchronized List<ParseException> parseExceptions() {
        return messages.stream().filter(m -> m.level().isError()).toList();
    }

    @Override
    public synchronized void addParseWarning(ParseException parseWarning) {
        // warnings never fail-fast and never contribute to haveErrors()
        this.messages.add(parseWarning);
    }

    @Override
    public synchronized List<ParseException> parseWarnings() {
        return messages.stream().filter(m -> m.level().isWarning()).toList();
    }

    @Override
    public synchronized boolean haveErrors() {
        return messages.stream().anyMatch(m -> m.level().isError());
    }

    /**
     * GAP #12's residual. This refusal used to read <i>"Can only switch to ParseResult when there are no parse
     * exceptions"</i> — the count, the units and the causes were all held right here in {@code messages}, and it
     * named none of them. That is how a stale jar on the classpath (a distribution build's leftover, "rung 6
     * poisons rung 1") presented as a SOURCE problem for a whole session: 18 units dropped, 10 of them never
     * touched by the edit, and nothing in the verdict pointed at the artifact.
     * <p>
     * ▶ <b>A REFUSAL THAT DOES NOT NAME ITS CAUSE SENDS THE READER TO THE WRONG PLACE.</b> The individual
     * {@link ParseException}s were logged as they happened, but by the time the run failed they were thousands
     * of lines up, and this is the line the caller actually sees.
     * <p>
     * ⚠ Capped, and it says so when it caps: an unbounded dump of a parse's every error is its own way of
     * hiding the first one.
     */
    private String refusalMessage() {
        List<ParseException> errors = parseExceptions();
        StringBuilder sb = new StringBuilder("Cannot switch to ParseResult: ").append(errors.size())
                .append(" parse error(s)");
        long units = errors.stream().map(ParseException::uri).filter(Objects::nonNull).distinct().count();
        if (units > 0) sb.append(" in ").append(units).append(" compilation unit(s)");
        sb.append(". ⚠ The units that fail need not be the ones you edited — if a type is defined BOTH in your"
                  + " sources and in a jar on the classpath, the jar may be stale (rebuild or delete it).");
        int max = 10;
        int shown = 0;
        for (ParseException pe : errors) {
            if (shown++ == max) {
                sb.append("\n  ... and ").append(errors.size() - max).append(" more; the full set is in")
                        .append(" parseExceptions().");
                break;
            }
            sb.append("\n  - ").append(pe.getMessage());
        }
        return sb.toString();
    }
}
