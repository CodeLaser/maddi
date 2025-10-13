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
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.ParseResult;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParseResultImpl implements ParseResult {
    private static final Set<TypeInfo> NO_CHILDREN = Set.of();
    private final Set<TypeInfo> types;
    private final Map<String, List<TypeInfo>> typesByFQN;
    private final Map<String, Set<TypeInfo>> primaryTypesOfPackage;
    private final Map<TypeInfo, Set<TypeInfo>> children;
    private final Map<String, List<TypeInfo>> typesBySimpleName;
    private final Map<String, SourceSet> sourceSetsByName;
    private final Map<SourceSet, ModuleInfo> sourceSetToModuleInfo;

    public ParseResultImpl(Set<TypeInfo> types, Map<String, SourceSet> sourceSetsByName, Map<SourceSet, ModuleInfo> sourceSetToModuleInfo) {
        this.sourceSetToModuleInfo = sourceSetToModuleInfo;
        this.sourceSetsByName = sourceSetsByName;
        this.types = types;
        typesByFQN = types.stream()
                .flatMap(TypeInfo::recursiveSubTypeStream)
                .collect(Collectors.groupingBy(Info::fullyQualifiedName, Collectors.toList()));
        Map<String, Set<TypeInfo>> mutableTypesOfPackage = new HashMap<>();
        types.forEach(ti -> mutableTypesOfPackage.computeIfAbsent(ti.packageName(),
                t -> new HashSet<>()).add(ti));
        mutableTypesOfPackage.replaceAll((k, s) -> Set.copyOf(s));
        primaryTypesOfPackage = Map.copyOf(mutableTypesOfPackage);
        Map<TypeInfo, Set<TypeInfo>> children = new HashMap<>();
        Map<String, List<TypeInfo>> typesBySimpleName = new HashMap<>();
        types.stream().flatMap(TypeInfo::recursiveSubTypeStream).forEach(t -> {
            if (t.parentClass() != null && !t.parentClass().typeInfo().isJavaLangObject()) {
                children.computeIfAbsent(t.parentClass().typeInfo(), type -> new HashSet<>()).add(t);
            }
            for (ParameterizedType pt : t.interfacesImplemented()) {
                children.computeIfAbsent(pt.typeInfo(), type -> new HashSet<>()).add(t);
            }
            typesBySimpleName.computeIfAbsent(t.simpleName().toLowerCase(), ti -> new ArrayList<>()).add(t);
        });
        typesBySimpleName.replaceAll((t, ts) -> List.copyOf(ts));
        this.typesBySimpleName = Map.copyOf(typesBySimpleName);
        this.children = Map.copyOf(children);
    }

    @Override
    public Map<String, SourceSet> sourceSetsByName() {
        return sourceSetsByName;
    }

    @Override
    public Set<TypeInfo> descendants(TypeInfo typeInfo, boolean recurse) {
        if (recurse) {
            Set<TypeInfo> all = new HashSet<>();
            recursivelyComputeDescendants(typeInfo, all, new HashSet<>());
            return all;
        }
        return this.children.getOrDefault(typeInfo, NO_CHILDREN);
    }

    private void recursivelyComputeDescendants(TypeInfo typeInfo, Set<TypeInfo> all, Set<Object> seen) {
        if (seen.add(typeInfo)) {
            Set<TypeInfo> descendants = children.getOrDefault(typeInfo, NO_CHILDREN);
            all.addAll(descendants);
            for (TypeInfo child : descendants) {
                recursivelyComputeDescendants(child, all, seen);
            }
        }
    }

    @Override
    public Set<TypeInfo> primaryTypes() {
        return types;
    }

    @Override
    public TypeInfo firstType() {
        return types.stream().findFirst().orElseThrow();
    }

    @Override
    public List<TypeInfo> findMostLikelyType(String name) {
        if (name == null || name.isBlank()) return List.of();
        List<TypeInfo> byFqn = typesByFQN.get(name);
        if (byFqn != null) return byFqn;
        List<TypeInfo> byName = typesBySimpleName.get(name.toLowerCase());
        if (byName != null) {
            return byName;
        }
        int dot = name.lastIndexOf('.');
        if (dot == name.length() - 1) return findMostLikelyType(name.substring(0, name.length() - 1));
        if (dot >= 0) {
            String last = name.substring(dot + 1);
            String prefix = name.substring(0, dot).toLowerCase();
            List<TypeInfo> byLast = typesBySimpleName.get(last.toLowerCase());
            if (byLast != null) {
                return byLast.stream().filter(typeInfo -> {
                    int dot2 = typeInfo.fullyQualifiedName().lastIndexOf('.');
                    if (dot2 > 0) {
                        String typeFqnMinusSimple = typeInfo.fullyQualifiedName().substring(0, dot2);
                        return typeFqnMinusSimple.toLowerCase().endsWith(prefix);
                    }
                    return false;
                }).toList();
            }
        }
        return List.of();
    }

    @Override
    public TypeInfo findType(String fqn) {
        List<TypeInfo> list = typesByFQN.get(fqn);
        if (list == null) return null;
        if (list.size() > 1) throw new UnsupportedOperationException("Use 'typeByFullyQualifiedName'");
        return list.getFirst();
    }

    @Override
    public List<TypeInfo> typeByFullyQualifiedName(String fqn) {
        return typesByFQN.get(fqn);
    }

    @Override
    public Set<TypeInfo> primaryTypesOfPackage(String packageName) {
        return primaryTypesOfPackage.get(packageName);
    }

    @Override
    public int size() {
        return types.size();
    }


    @Override
    public Map<String, Set<TypeInfo>> primaryTypesPerPackage() {
        return primaryTypesOfPackage;
    }

    private static final Pattern METHOD = Pattern.compile("([^(]+)\\.[^(.]+\\(.+");

    @Override
    public MethodInfo findMethod(String methodFqn, boolean complain) {
        Matcher m = METHOD.matcher(methodFqn);
        if (m.matches()) {
            String typeFqn = m.group(1);
            TypeInfo typeInfo = findType(typeFqn);
            if (typeInfo == null) {
                if (complain) {
                    throw new RuntimeException("Cannot find type with fqn '" + typeFqn + "'");
                }
                return null;
            }
            MethodInfo methodInfo = typeInfo.constructorAndMethodStream()
                    .filter(mi -> mi.fullyQualifiedName().equals(methodFqn))
                    .findFirst().orElse(null);
            if (methodInfo != null) return methodInfo;
        }
        if (complain) {
            throw new RuntimeException("Cannot find method with fqn '" + methodFqn + "'");
        }
        return null;
    }

    @Override
    public List<MethodInfo> findMostLikelyMethod(String name) {
        if (name == null || name.isBlank()) return List.of();
        MethodInfo exact = findMethod(name, false);
        if (exact != null) return List.of(exact);
        int open = name.indexOf('(');
        if (open < 0) {
            return noOpenBracket(name);
        }
        List<MethodInfo> candidates = noOpenBracket(name.substring(0, open));
        int close = name.lastIndexOf(')');
        String argsCsv = close < 0 ? name.substring(open + 1) : name.substring(open + 1, close);
        List<String> args = splitByComma(argsCsv);
        int nArgs = args.size();
        return candidates.stream().filter(mi -> mi.parameters().size() == nArgs)
                .filter(mi -> mi.parameters().stream()
                        .allMatch(pi -> someAgreement(pi.parameterizedType(), args.get(pi.index()))))
                .toList();
    }

    private static final Pattern SIMPLE = Pattern.compile(".+\\.([^<.]+)");

    private static boolean someAgreement(ParameterizedType pt, String typeString) {
        if (pt.typeInfo() == null) return typeString.equals(pt.typeParameter().simpleName());
        String typeStringLc = typeString.toLowerCase();
        String typeInfoSimpleName = pt.typeInfo().simpleName().toLowerCase();
        if (typeInfoSimpleName.equals(typeStringLc)) return true;
        Matcher m = SIMPLE.matcher(typeStringLc);
        if (m.find()) {
            String g1 = m.group(1);
            return g1.equals(typeInfoSimpleName);
        }
        if (pt.typeInfo().isPrimitiveExcludingVoid()) return typeStringLc.startsWith(typeInfoSimpleName);
        return false;
    }

    private static List<String> splitByComma(String input) {
        if (input.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int bracketDepth = 0;
        for (char c : input.toCharArray()) {
            if (c == '<') {
                bracketDepth++;
                current.append(c);
            } else if (c == '>') {
                bracketDepth--;
                current.append(c);
            } else if (c == ',' && bracketDepth == 0) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private List<MethodInfo> noOpenBracket(String name) {
        // we still may find a method; # is used in docstrings
        int lastDot = Math.max(name.lastIndexOf('.'), name.lastIndexOf('#'));
        if (lastDot == name.length() - 1) {
            return findMostLikelyMethod(name.substring(0, name.length() - 1));
        }
        if (lastDot < 0) {
            List<TypeInfo> matchingTypes = findMostLikelyType(name);
            if (matchingTypes == null || matchingTypes.isEmpty()) {
                // we have a simple name... this may be expensive
                return this.types.stream().flatMap(TypeInfo::recursiveSubTypeStream)
                        .flatMap(TypeInfo::methodStream).filter(mi -> name.equals(mi.name())).toList();
            }
            // collect all constructors
            return matchingTypes.stream().flatMap(ti -> ti.constructors().stream()).toList();
        }
        String methodName = name.substring(lastDot + 1).toLowerCase();
        String prefix = name.substring(0, lastDot);
        List<TypeInfo> types = findMostLikelyType(prefix);
        return types.stream().flatMap(TypeInfo::methodStream)
                .filter(mi -> mi.name().toLowerCase().equals(methodName)).toList();
    }

    @Override
    public List<FieldInfo> findMostLikelyField(String name) {
        if (name == null || name.isBlank()) return List.of();
        int lastDot = Math.max(name.lastIndexOf('.'), name.lastIndexOf('#'));
        if (lastDot == name.length() - 1) {
            return findMostLikelyField(name.substring(0, name.length() - 1));
        }
        if (lastDot < 0) {
            String nameLc = name.toLowerCase();
            return types.stream().flatMap(TypeInfo::recursiveSubTypeStream).flatMap(ti -> ti.fields().stream())
                    .filter(f -> nameLc.equals(f.name().toLowerCase()))
                    .toList();
        }
        String fieldName = name.substring(lastDot + 1).toLowerCase();
        String prefix = name.substring(0, lastDot);
        List<TypeInfo> types = findMostLikelyType(prefix);
        return types.stream().flatMap(ti -> ti.fields().stream())
                .filter(field -> field.name().toLowerCase().equals(fieldName)).toList();
    }

    @Override
    public List<String> findMostLikelyPackage(String name) {
        if (name == null || name.isBlank()) return List.of();
        if (primaryTypesOfPackage.containsKey(name)) return List.of(name);
        String nameLc = name.toLowerCase();
        return primaryTypesOfPackage.keySet().stream().filter(pkg -> pkg.toLowerCase().contains(nameLc)).toList();
    }

    @Override
    public ModuleInfo moduleInfo(SourceSet sourceSet) {
        return sourceSetToModuleInfo.get(sourceSet);
    }

    @Override
    public Map<SourceSet, ModuleInfo> sourceSetToModuleInfoMap() {
        return sourceSetToModuleInfo;
    }
}
