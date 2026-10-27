/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.cst.api.element.*;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.support.SetOnce;

import java.net.URI;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CompilationUnitStub implements CompilationUnit {
    private final String packageName;
    private final SetOnce<List<TypeInfo>> types = new SetOnce<>();

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
    public List<Comment> trailingComments() {
        return List.of();
    }

    @Override
    public List<TypeInfo> types() {
        return types.get();
    }

    @Override
    public void setTypes(List<TypeInfo> types) {
        this.types.set(types);
    }

    @Override
    public ModuleInfo moduleInfo() {
        return null; // a stub stands in for a type's compilation unit, never for a module declaration
    }

    @Override
    public void setModuleInfo(ModuleInfo moduleInfo) {
        throw new UnsupportedOperationException();
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
    public Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
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
    public Element rewire(InfoMapView infoMap) {
        return this;
    }

    @Override
    public CompilationUnit copy() {
        return new CompilationUnitStub(packageName);
    }
}
