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

package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SummaryImpl implements Summary {
    private final static Logger LOGGER = LoggerFactory.getLogger(SummaryImpl.class);

    private final Set<TypeInfo> types = new HashSet<>();
    private final List<ParseException> parseExceptions = new LinkedList<>();
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
            throw new UnsupportedOperationException("Can only switch to ParseResult when there are no parse exceptions");
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
        this.parseExceptions.add(parseException);
    }

    @Override
    public List<ParseException> parseExceptions() {
        return parseExceptions;
    }

    @Override
    public boolean haveErrors() {
        return !parseExceptions.isEmpty();
    }
}
