package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.util.internal.util.StringUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class BlockBuilders {

    public Block.Builder removeLast() {
        return blockBuilders.removeLast().blockBuilder;
    }

    private record BlockData(Block.Builder blockBuilder, String index, int numberOfStatements) {
    }
    private Deque<BlockData> blockBuilders = new ArrayDeque<>();


    void replaceLastStatement(Statement statement) {
        List<Statement> statements = blockBuilders.getLast().blockBuilder.statements();
        statements.removeLast();
        statements.add(statement);
    }

    Statement lastStatement() {
        BlockData bd = blockBuilders.getLast();
        if (bd.blockBuilder.statements().isEmpty()) return null;
        return bd.blockBuilder.statements().getLast();
    }

    void addStatement(Statement statement) {
        blockBuilders.getLast().blockBuilder.addStatement(statement);
    }

    String statementIndex() {
        if (blockBuilders.isEmpty()) return "-";
        BlockData bd = blockBuilders.getLast();
        String padded = StringUtil.pad(bd.blockBuilder.statements().size(), bd.numberOfStatements);
        return ("-".equals(bd.index) ? "" : bd.index + ".") + padded;
    }


}
