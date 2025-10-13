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

package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;

import java.net.URI;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CompilationUnitStub implements CompilationUnit {
    private final String packageName;

    public CompilationUnitStub(String candidatePackageName) {
        packageName = candidatePackageName;
    }

    @Override
    public String toString() {
        return "CompilationUnitStub[packageName=" + packageName + "]";
    }

    @Override
    public int complexity() {
        return 0;
    }

    @Override
    public List<Comment> comments() {
        return List.of();
    }

    @Override
    public Source source() {
        return null;
    }

    @Override
    public void visit(Predicate<Element> predicate) {

    }

    @Override
    public void setFingerPrint(FingerPrint fingerPrint) {

    }

    @Override
    public void visit(Visitor visitor) {

    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return null;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        return Stream.empty();
    }

    @Override
    public Stream<Variable> variableStreamDescend() {
        return Stream.empty();
    }

    @Override
    public Stream<TypeReference> typesReferenced() {
        return Stream.empty();
    }

    @Override
    public URI uri() {
        return null;
    }

    @Override
    public String packageName() {
        return packageName;
    }

    @Override
    public List<ImportStatement> importStatements() {
        return List.of();
    }

    @Override
    public SourceSet sourceSet() {
        return null;
    }

    @Override
    public FingerPrint fingerPrintOrNull() {
        return null;
    }

    @Override
    public Element rewire(InfoMap infoMap) {
        return this;
    }
}
