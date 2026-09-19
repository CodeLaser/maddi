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

package io.codelaser.maddi.inspection.api.parser;

import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/*
result of successful parsing.

computed from a Summary object

 */
public interface ParseResult {

    ModuleInfo moduleInfo(SourceSet sourceSet);

    Map<SourceSet, ModuleInfo> sourceSetToModuleInfoMap();

    Map<String, SourceSet> sourceSetsByName();

    default MethodInfo findMethod(String methodFqn) {
        return findMethod(methodFqn, true);
    }

    MethodInfo findMethod(String methodFqn, boolean complain);

    List<FieldInfo> findMostLikelyField(String name);

    List<String> findMostLikelyPackage(String name);

    List<TypeInfo> findMostLikelyType(String name);

    // primarily intended for testing; use 'typeByFullyQualifiedName' in production
    TypeInfo findType(String typeFqn);

    Set<TypeInfo> primaryTypes();

    TypeInfo firstType();

    List<MethodInfo> findMostLikelyMethod(String name);

    Map<String, Set<TypeInfo>> primaryTypesPerPackage();

    int size();

    List<TypeInfo> typeByFullyQualifiedName(String fqn);

    /**
     * The types whose {@link TypeInfo#binaryName()} is exactly {@code binaryName} — {@code a.b.Outer$Inner}, the
     * form a string literal holds. <b>Never null</b>; empty when nothing matches.
     * <p>
     * ⭐ The inverse of {@link TypeInfo#binaryName()}, and the resolution step of every by-name reference: a name
     * that reaches the parse this way is an edge, one that does not is a blind spot to be counted rather than
     * guessed at. It is a lookup, not a parse of the string — so a simple name that itself contains a {@code $}
     * (legal, and the reason splitting on the first {@code $} is wrong) resolves like any other, and an anonymous
     * or local type's JVM name ({@code Outer$1}) resolves to nothing, because maddi does not number those the way
     * the class file does.
     * <p>
     * ⚠ A list, for the same reason {@link #typeByFullyQualifiedName} returns one: the same name can be declared
     * in two source sets.
     */
    List<TypeInfo> typeByBinaryName(String binaryName);

    Set<TypeInfo> primaryTypesOfPackage(String packageName);

    /**
     * Given type A, return all B which implement or extend A, either directly (children) or recursively (descendants).
     * Only available (precomputed) for source types.
     *
     * @param typeInfo the starting type (A).
     * @param recurse  If false, only direct children are returned. if true, all descendants are returned recursively.
     * @return all descendants, never null.
     */
    Set<TypeInfo> descendants(TypeInfo typeInfo, boolean recurse);

    boolean isPackagePart(String candidate);

}
