package org.e2immu.language.cst.api.element;

public interface Source extends Comparable<Source> {

    boolean isNoSource();

    int beginLine();

    int beginPos();

    int endLine();

    int endPos();

    // for statements only
    String index();

    boolean isCompiledClass();

    DetailedSources detailedSources();

    Source withBeginPos(int beginPos);

    Source withEndPos(int endPos);

    Source withDetailedSources(DetailedSources detailedSources);

    Source mergeDetailedSources(DetailedSources detailedSources);

    // computations: override if you need to use these frequently

    default String compact() {
        return beginLine() + "-" + beginPos();
    }

    default String compact2() {
        return beginLine() + "-" + beginPos() + ":" + endLine() + "-" + endPos();
    }

    Source withIndex(String newIndex);

    default boolean isContainedIn(int line, int pos) {
        if (line > beginLine() && line < endLine()) return true;
        if (line < beginLine() || line > endLine()) return false;
        if (line == beginLine() && line == endLine()) return beginPos() <= pos && pos <= endPos();
        if (line == beginLine()) return beginPos() <= pos;
        return pos <= endPos();
    }
}
