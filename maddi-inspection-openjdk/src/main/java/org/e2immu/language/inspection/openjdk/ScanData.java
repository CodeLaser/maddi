package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.runtime.Runtime;

import javax.tools.Diagnostic;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public record ScanData(Runtime runtime,
                       LineMap lineMap,
                       SourcePositions sourcePositions,
                       CompilationUnitTree compilationUnitTree,
                       SourceCodeScan.Result scanResult,
                       BlockBuilders blockBuilders,
                       ElementStack elementStack,
                       FlagHelper flagHelper,
                       TypeData typeData,
                       ConvertType convertType,
                       ClassSymbolScanner classSymbolScanner,
                       Map<StatementTree, String> statementLabels) {


    public Source sourceOfIdentifier(String identifier, int pos) {
        long line = lineMap.getLineNumber(pos);
        long begin = lineMap.getColumnNumber(pos);
        return runtime.newParserSource(null, (int) line, (int) begin, (int) line,
                (int) (begin + identifier.length() - 1));
    }

    public Source sourceForNode(Tree node, DetailedSources.Builder dsb) {
        return sourceForNode(node).withDetailedSources(dsb.build());
    }

    public Source sourceForNode(Tree node) {
        return sourceForNode(node, "-");
    }

    public Source statementSourceForNode(Tree node) {
        return sourceForNode(node, blockBuilders.statementIndex());
    }

    public Source statementSourceForNode(Tree node, DetailedSources.Builder dsb) {
        return sourceForNode(node, blockBuilders.statementIndex()).withDetailedSources(dsb.build());
    }


    public Source scanSource(Tree tree) {
        return sourceForNode(tree, "");
    }

    public Source sourceForNode(Tree node, String index) {
        long endPos = sourcePositions.getEndPosition(compilationUnitTree, node);
        if (endPos == Diagnostic.NOPOS) return runtime.noSource(); // synthetic
        long startPos = sourcePositions.getStartPosition(compilationUnitTree, node);
        long startLine = lineMap.getLineNumber(startPos);
        long startCol = lineMap.getColumnNumber(startPos);
        long endLine = lineMap.getLineNumber(endPos);
        long endCol = lineMap.getColumnNumber(endPos) - 1; // we work inclusively
        return runtime.newParserSource(index, (int) startLine, (int) startCol, (int) endLine, (int) endCol);
    }

    public List<Comment> commentsForNode(Source source) {
        return scanResult.findComments(source);
    }

    public List<Comment> trailingCommentsForNode(Source source) {
        return scanResult.findTrailingComments(source);
    }
}
