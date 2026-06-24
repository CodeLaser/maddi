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

package org.e2immu.analyzer.aapi.parser;

import org.e2immu.analyzer.modification.common.defaults.AnnotationProvider;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.StringConstant;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.integration.JavaInspectorFactory;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Read the analysis hints of a library/JDK.
 */
public class AnalysisHintsParser implements AnnotationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisHintsParser.class);

    public record Data(List<AnnotationExpression> annotations,
                       List<Comment> comments,
                       boolean explainAnnotationInComment,
                       Integer frequency,
                       Integer overrideHasFrequency) {
    }

    private final List<TypeInfo> typesParsed = new ArrayList<>();
    private final Map<Element, Data> infoMap = new LinkedHashMap<>();
    private final JavaInspectorFactory javaInspectorFactory;
    private int warnings;
    private int annotatedTypes;
    private int annotations;

    public AnalysisHintsParser(JavaInspectorFactory javaInspectorFactory) {
        this.javaInspectorFactory = javaInspectorFactory;
    }

    public JavaInspector go(AnalysisHints analysisHints) throws IOException {
        // construct a java inspector
        SourceSet sourceSet = analysisHints.toSourceSet(javaInspectorFactory.dependencies());
        JavaInspector javaInspector = javaInspectorFactory.withSources(sourceSet);

        // parse the analysis hint files
        ParseResult parseResult = javaInspector.parse(new JavaInspector.ParseOptions.Builder().build()).parseResult();

        // load the analysis results on all (relevant/loaded) library and JDK types
        int loaded = new LoadAnalysisResults(javaInspector.runtime(), javaInspector.mainSources())
                .go(analysisHints.preloadAnalysisResultsDirs());
        LOGGER.info("Loaded {} analysis results file(s)", loaded);

        // then process the hint files
        parseResult.primaryTypes().forEach(pt -> process(javaInspector.compiledTypesManager(), pt));
        LOGGER.info("Finished parsing, annotated {} types, counted {} annotations, issued {} warning(s)",
                annotatedTypes, annotations, warnings);
        return javaInspector;
    }

    private void process(CompiledTypesManager compiledTypesManager, TypeInfo typeInfo) {
        typesParsed.add(typeInfo);
        FieldInfo packageName = typeInfo.getFieldByName("PACKAGE_NAME", false);
        if (packageName == null) {
            LOGGER.info("Ignoring class {}, has no PACKAGE_NAME field", typeInfo);
            return;
        }
        String apiPackage;
        if (packageName.initializer() instanceof StringConstant sc) {
            apiPackage = sc.constant();
        } else {
            LOGGER.info("Ignoring class {}, PACKAGE_NAME field has not been assigned a String literal", typeInfo);
            return;
        }
        LOGGER.debug("Starting AAPI inspection of {}, in API package {}", typeInfo, apiPackage);
        typeInfo.subTypes().forEach(st -> inspect(compiledTypesManager, apiPackage, st));
    }

    private void inspect(CompiledTypesManager compiledTypesManager, String apiPackage, TypeInfo typeInfo) {
        if (typeInfo.simpleName().endsWith("$")) {
            String simpleNameWithoutDollar = typeInfo.simpleName().substring(0, typeInfo.simpleName().length() - 1);
            String fqn = apiPackage + "." + simpleNameWithoutDollar;
            TypeInfo targetType = compiledTypesManager.getOrLoad(fqn, typeInfo.compilationUnit().sourceSet());
            if (targetType != null) {
                annotatedTypes++;
                transferAnnotations(typeInfo, targetType);
            } else {
                warnings++;
                LOGGER.warn("Ignoring type '{}', cannot load it.", fqn);
            }
        } else {
            LOGGER.warn("Ignoring type '{}', name does not end in $.", typeInfo);
            warnings++;
        }
    }

    private static final Pattern EXPLAIN_PATTERN = Pattern.compile("^(\\s*@|<no annotations>)");
    private static final Pattern FREQ_PATTERN = Pattern.compile("^\\s*frequency (\\d+)");
    private static final Pattern FREQ_OVERRIDE_PATTERN = Pattern.compile("^\\s*override.+frequency (\\d+)");

    private Data makeData(Element info) {
        boolean explainAnnotationInComment = false;
        Integer freq = null;
        Integer overrideHasFreq = null;
        List<Comment> commentsToKeep = new ArrayList<>(info.comments().size());
        for (Comment comment : info.comments()) {
            boolean keep;
            String commentString = acceptComment(comment, info);
            if (commentString != null) {
                Matcher m0 = EXPLAIN_PATTERN.matcher(commentString);
                if (m0.find()) {
                    explainAnnotationInComment = true;
                    keep = false;
                } else {
                    Matcher m1 = FREQ_PATTERN.matcher(commentString);
                    if (m1.find()) {
                        freq = Integer.parseInt(m1.group(1));
                        keep = false;
                    } else {
                        Matcher m2 = FREQ_OVERRIDE_PATTERN.matcher(commentString);
                        if (m2.find()) {
                            overrideHasFreq = Integer.parseInt(m2.group(1));
                            keep = false;
                        } else {
                            keep = commentString.startsWith(" ");
                        }
                    }
                }
            } else {
                keep = true;
            }
            if (keep) commentsToKeep.add(comment);
        }
        return new Data(info.annotations(), commentsToKeep, explainAnnotationInComment, freq, overrideHasFreq);
    }

    private static String acceptComment(Comment comment, Element owner) {
        if (owner instanceof ParameterInfo && comment instanceof MultiLineComment mlc) return mlc.comment();
        if (comment instanceof SingleLineComment slc) return slc.comment();
        return null;
    }

    private void transferAnnotations(TypeInfo sourceType, TypeInfo targetType) {
        Data typeData = makeData(sourceType);
        for (TypeParameter typeParameter : sourceType.typeParameters()) {
            TypeParameter targetTypeParameter = targetType.typeParameters().get(typeParameter.getIndex());
            infoMap.put(targetTypeParameter, makeData(typeParameter));
        }
        annotations += sourceType.annotations().size();
        infoMap.put(targetType, typeData);

        for (TypeInfo subType : sourceType.subTypes()) {
            TypeInfo targetSubType = targetType.findSubType(subType.simpleName(), false);
            if (targetSubType != null) {
                transferAnnotations(subType, targetSubType);
            } else {
                warnings++;
                LOGGER.warn("Ignoring subtype '{}', cannot find it in the target type '{}'",
                        subType.simpleName(), targetType);
            }
        }
        // TODO: for now, we don't accept/recognize annotations on method, constructor type parameters
        // TODO 202606: skipping invariants ($ in the name)
        for (MethodInfo sourceMethod : sourceType.methods()) {
            if (!sourceMethod.isSynthetic() && !sourceMethod.name().contains("$")) {
                MethodInfo targetMethod = findTargetMethod(targetType, sourceMethod);
                if (targetMethod != null) {
                    annotations += sourceMethod.annotations().size();
                    Data methodData = makeData(sourceMethod);
                    infoMap.put(targetMethod, methodData);
                    doParameters(sourceMethod, targetMethod);
                } else {
                    LOGGER.warn("Ignoring method '{}', not found in target type '{}'", sourceMethod, targetType);
                    ++warnings;
                }
            }
        }
        for (MethodInfo sourceMethod : sourceType.constructors()) {
            if (!sourceMethod.isSynthetic()) {
                MethodInfo targetMethod = findTargetConstructor(targetType, sourceMethod);
                if (targetMethod != null) {
                    annotations += sourceMethod.annotations().size();
                    Data methodData = makeData(sourceMethod);
                    infoMap.put(targetMethod, methodData);
                    doParameters(sourceMethod, targetMethod);
                } else {
                    LOGGER.warn("Ignoring constructor '{}', not found in target type '{}'", sourceMethod, targetType);
                    ++warnings;
                }
            }
        }
        for (FieldInfo sourceField : sourceType.fields()) {
            FieldInfo targetField = findTargetField(targetType, sourceField);
            if (targetField != null) {
                annotations += sourceField.annotations().size();
                Data fieldData = makeData(sourceField);
                infoMap.put(targetField, fieldData);
            } else {
                LOGGER.warn("Ignoring field '{}', not found in target type '{}'", sourceField, targetType);
                ++warnings;
            }
        }
    }

    private void doParameters(MethodInfo sourceMethod, MethodInfo targetMethod) {
        int i = 0;
        for (ParameterInfo sourceParameter : sourceMethod.parameters()) {
            ParameterInfo targetParameter = targetMethod.parameters().get(i);
            annotations += sourceParameter.annotations().size();
            Data paramData = makeData(sourceParameter);
            infoMap.put(targetParameter, paramData);
            i++;
        }
    }

    private MethodInfo findTargetConstructor(TypeInfo targetType, MethodInfo sourceMethod) {
        int n = sourceMethod.parameters().size();
        for (MethodInfo candidate : targetType.constructors()) {
            if (candidate.parameters().size() == n && sameParameterTypes(candidate, sourceMethod)) {
                return candidate;
            }
        }
        return null;
    }

    private MethodInfo findTargetMethod(TypeInfo targetType, MethodInfo sourceMethod) {
        int n = sourceMethod.parameters().size();
        String sourceMethodName = trimDollar(sourceMethod.name());
        for (MethodInfo candidate : targetType.methods()) {
            if (candidate.parameters().size() == n
                && candidate.name().equals(sourceMethodName)
                && sameParameterTypes(candidate, sourceMethod)) {
                return candidate;
            }
        }
        return null; // cannot find the method, we'll NOT be looking at a supertype, since we cannot add a copy
    }

    private String trimDollar(String name) {
        if (name.endsWith("$")) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }

    private FieldInfo findTargetField(TypeInfo targetType, FieldInfo sourceField) {
        return targetType.getFieldByName(sourceField.name(), false);
    }

    private boolean sameParameterTypes(MethodInfo candidate, MethodInfo sourceMethod) {
        Iterator<ParameterInfo> it = candidate.parameters().iterator();
        for (ParameterInfo pi : sourceMethod.parameters()) {
            assert it.hasNext();
            ParameterInfo pi2 = it.next();
            if (!sameType(pi.parameterizedType(), pi2.parameterizedType())) return false;
        }
        return true;
    }

    private boolean sameType(ParameterizedType pt1, ParameterizedType pt2) {
        if (pt1.arrays() != pt2.arrays()) return false;
        if (pt1.typeInfo() != null) return pt1.typeInfo() == pt2.typeInfo();
        return (pt1.typeParameter() == null) == (pt2.typeParameter() == null);
    }

    @Override
    public List<AnnotationExpression> annotations(Element element) {
        Data data = infoMap.get(element);
        if (data != null) return data.annotations;
        return List.of();
    }

    public int getWarnings() {
        return warnings;
    }

    public List<TypeInfo> types() {
        return infoMap.keySet().stream().filter(i -> i instanceof TypeInfo).map(t -> (TypeInfo) t).toList();
    }

    public Set<Element> infos() {
        return infoMap.keySet();
    }

    public List<TypeInfo> typesParsed() {
        return typesParsed;
    }

    public Data data(Element info) {
        return infoMap.get(info);
    }
}
