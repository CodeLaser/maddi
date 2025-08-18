package org.e2immu.language.inspection.impl.parser;

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

    public SummaryImpl(boolean failFast) {
        this.failFast = failFast;
    }

    @Override
    public synchronized void ensureSourceSet(SourceSet sourceSet) {
        sourceSetsByName.putIfAbsent(sourceSet.name(), sourceSet);
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
        return new ParseResultImpl(types, sourceSetsByName);
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
