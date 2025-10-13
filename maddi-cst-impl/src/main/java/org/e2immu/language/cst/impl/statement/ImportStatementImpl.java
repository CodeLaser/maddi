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

package org.e2immu.language.cst.impl.statement;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ImportStatementImpl extends StatementImpl implements ImportStatement {
    private final String importString;
    private final boolean isStatic;

    public ImportStatementImpl(List<Comment> comments, Source source, String importString, boolean isStatic) {
        super(comments, source, List.of(), 1, null);
        this.importString = importString;
        this.isStatic = isStatic;
    }

    public static class Builder extends ElementImpl.Builder<ImportStatement.Builder> implements ImportStatement.Builder {
        private String importString;
        private boolean isStatic;

        @Override
        public ImportStatement build() {
            return new ImportStatementImpl(comments, source, importString, isStatic);
        }

        @Override
        public Builder setImport(String importString) {
            this.importString = importString;
            return this;
        }

        @Override
        public Builder setIsStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImportStatementImpl that)) return false;
        return Objects.equals(importString, that.importString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(importString);
    }

    @Override
    public String importString() {
        return importString;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeStatement(this);
        visitor.afterStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return outputBuilder(qualification).add(new TextImpl("import")).add(SpaceEnum.ONE)
                .add(new TextImpl(importString)).add(SymbolEnum.SEMICOLON).add(SpaceEnum.NEWLINE);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.empty();
    }

    @Override
    public boolean hasSubBlocks() {
        return false;
    }

    @Override
    public String name() {
        return "import";
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;
        if (translationMap.isClearAnalysis()) {
            return List.of(new ImportStatementImpl(comments(), source(), importString, isStatic));
        }
        return List.of(this);
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return this;// no blocks
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return this;
    }
}
