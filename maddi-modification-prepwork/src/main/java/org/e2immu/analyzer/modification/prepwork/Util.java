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

package org.e2immu.analyzer.modification.prepwork;

import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.*;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.prepwork.StatementIndex.*;

public class Util {

    public static boolean atSameLevel(String i0, String i1) {
        int d0 = i0.lastIndexOf(DOT);
        int d1 = i1.lastIndexOf(DOT);
        return d0 == -1 && d1 == -1
               || d0 > 0 && d1 > 0 && i0.substring(0, d0).equals(i1.substring(0, d1));
    }

    public static String endOf(String index) {
        int i = index.lastIndexOf('.');
        if (i < 0) return "~";
        return index.substring(0, i) + ".~";
    }

    public static Stream<FieldInfo> fieldsOf(Variable v) {
        if (v instanceof FieldReference fr) {
            Stream<FieldInfo> sub = fr.scopeVariable() != null ? fieldsOf(fr.scopeVariable()) : Stream.empty();
            return Stream.concat(sub, Stream.of(fr.fieldInfo()));
        }
        return Stream.of();
    }

    public static Stream<TypeInfo> realTypeStream(Variable v) {
        return switch (v) {
            case null -> Stream.empty();
            case FieldReference fr -> {
                TypeInfo owner;
                if (virtual(fr.fieldInfo())) {
                    owner = null;
                } else {
                    TypeParameter typeParameter = fr.fieldInfo().type().typeParameter();
                    if (typeParameter != null
                        && typeParameter.getIndex() < fr.scope().parameterizedType().parameters().size()) {
                        // return the concrete value for types like SetOnce<T>
                        owner = fr.scope().parameterizedType().parameters().get(typeParameter.getIndex()).typeInfo();
                    } else {
                        owner = fr.fieldInfo().owner();
                    }
                }
                yield Stream.concat(Stream.ofNullable(owner), realTypeStream(fr.scopeVariable()));
            }
            case DependentVariable dv -> realTypeStream(dv.arrayVariable());
            default -> Stream.of();
        };
    }

    public static Iterable<Variable> goUp(Variable variable) {
        return new Iterable<>() {
            @Override
            public @NotNull Iterator<Variable> iterator() {
                return new Iterator<>() {
                    Variable v = variable;

                    @Override
                    public boolean hasNext() {
                        return v != null;
                    }

                    @Override
                    public Variable next() {
                        Variable rv = v;
                        if (v instanceof FieldReference fr && fr.scopeVariable() != null) {
                            v = fr.scopeVariable();
                        } else if (v instanceof DependentVariable dv) {
                            v = dv.arrayVariable();
                        } else {
                            v = null;
                        }
                        return rv;
                    }
                };
            }
        };
    }

    public static boolean hasVirtualFields(Variable v) {
        if (v instanceof ReturnVariable rv) {
            return rv.methodInfo().isAbstract() || rv.methodInfo().typeInfo().compilationUnit().externalLibrary();
        }
        if (v instanceof ParameterInfo pi) {
            return pi.methodInfo().isAbstract() || pi.methodInfo().typeInfo().compilationUnit().externalLibrary();
        }
        TypeInfo typeInfo;
        if (v.parameterizedType().typeInfo() != null) {
            typeInfo = v.parameterizedType().typeInfo();
        } else if (v.parameterizedType().typeParameter() != null) {
            typeInfo = v.parameterizedType().typeParameter().typeInfo();
        } else {
            return false; // wildcard type
        }
        return typeInfo.isAbstract() || typeInfo.compilationUnit().externalLibrary();
    }

    /**
     * all
     *
     * @param scope an index designating the scope (of a variable)
     * @param index an index
     * @return true when the index is in the scope
     */
    public static boolean inScopeOf(String scope, String index) {
        if (BEFORE_METHOD.equals(scope)) return true;
        int dashScope = Math.max(scope.lastIndexOf(DASH), scope.lastIndexOf(PLUS));
        if (dashScope >= 0) {
            // 0-E -> in scope means starting with 0
            String sub = scope.substring(0, dashScope);
            return index.startsWith(sub);
        }
        int lastDotScope = scope.lastIndexOf(DOT);
        if (lastDotScope < 0) {
            // scope = 3 --> 3.0.0 ok, 3 ok, 4 ok
            return index.compareTo(scope) >= 0;
        }
        // scope = 3.0.2 --> 3.0.3 ok, 3.0.2.0.0 OK,  but 3.1.3 is not OK; 4 is not OK
        String withoutDot = scope.substring(0, lastDotScope);
        if (!index.startsWith(withoutDot)) return false;
        return index.compareTo(scope) >= 0;
    }

    public static boolean isContainerType(TypeInfo typeInfo) {
        return typeInfo.simpleName().startsWith("§");
    }

    public static boolean isPrimary(Variable variable) {
        return variable == primary(variable);
    }

    public static boolean isSlice(Variable v) {
        return v instanceof DependentVariable dv && dv.indexExpression() instanceof IntConstant ic && ic.constant() < 0;
    }

    public static boolean isVirtualModification(Variable variable) {
        return variable instanceof FieldReference fr && isVirtualModificationField(fr.fieldInfo());
    }

    public static boolean isVirtualModificationField(FieldInfo fieldInfo) {
        return fieldInfo.name().startsWith("§m") && fieldInfo.type().typeInfo() != null
               && "java.util.concurrent.atomic.AtomicBoolean".equals(fieldInfo.type().typeInfo().fullyQualifiedName());
    }

    public static LocalVariable lvPrimaryOrNull(Variable variable) {
        if (variable instanceof LocalVariable lv) return lv;
        if (primary(variable) instanceof LocalVariable lv) return lv;
        return null;
    }

    public static ParameterInfo parameterPrimaryOrNull(Variable variable) {
        if (primary(variable) instanceof ParameterInfo pi) return pi;
        return null;
    }

    public static Variable oneBelowThis(Variable v) {
        if (v instanceof FieldReference fr && fr.scopeVariable() != null && !fr.scopeIsThis()) {
            return oneBelowThis(fr.scopeVariable());
        }
        if (v instanceof DependentVariable dv) {
            return oneBelowThis(dv.arrayVariable());
        }
        return v;
    }

    public static Variable primary(Variable variable) {
        if (variable instanceof FieldReference fr) {
            if (fr.scopeVariable() != null
                // accept this.§xs, but not this.v.§xs
                // see e.g. TestPrefix,3 for the this.§xs situation
                && (!(fr.scopeVariable() instanceof This) || Util.virtual(fr.fieldInfo()))) {
                return primary(fr.scopeVariable());
            }
        }
        if (variable instanceof DependentVariable dv) {
            return primary(dv.arrayVariable());
        }
        return variable;
    }

    public static Variable firstRealVariable(Variable variable) {
        if (variable instanceof FieldReference fr
            && fr.scopeVariable() != null
            && Util.virtual(fr.fieldInfo())) {
            return firstRealVariable(fr.scopeVariable());
        }
        if (variable instanceof DependentVariable dv
            && dv.indexExpression() instanceof IntConstant ic
            && ic.constant() < 0) {
            // slices
            return firstRealVariable(dv.arrayVariable());
        }
        return variable;
    }

    // to avoid TimSort problems
    public static int isPartOfComparator(Variable v1, Variable v2) {
        if (v1 instanceof FieldReference fr1 && v2 instanceof FieldReference fr2) {
            int c = fr1.fieldInfo().simpleName().compareTo(fr2.fieldInfo().simpleName());
            if (c != 0) return c;
            if (fr1.scopeVariable() != null && fr2.scopeVariable() != null) {
                return isPartOfComparator(fr1.scopeVariable(), fr2.scopeVariable());
            }
        }
        if (v1 instanceof DependentVariable dv1 && v2 instanceof DependentVariable dv2) {
            int c = isPartOfComparator(dv1.arrayVariable(), dv2.arrayVariable());
            if (c != 0) return c;
        }
        return v1.fullyQualifiedName().compareTo(v2.fullyQualifiedName());
    }

    public static boolean isPartOf(Variable base, Variable sub) {
        if (base.equals(sub)) return true;
        if (sub instanceof FieldReference fr) {
            if (fr.scopeVariable() != null) {
                return isPartOf(base, fr.scopeVariable());
            }
        }
        if (sub instanceof DependentVariable dv) {
            return isPartOf(base, dv.arrayVariable());
        }
        return false;
    }

    public static Set<Variable> scopeVariables(Variable variable) {
        if (variable instanceof FieldReference fr && fr.scopeVariable() != null) {
            return Stream.concat(scopeVariables(fr.scopeVariable()).stream(), Stream.of(fr.scopeVariable()))
                    .collect(Collectors.toUnmodifiableSet());
        }
        if (variable instanceof DependentVariable dv && dv.arrayVariable() != null) {
            return Stream.concat(scopeVariables(dv.arrayVariable()).stream(), Stream.of(dv.arrayVariable()))
                    .collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }

    public static Stream<Variable> variableAndScopes(Variable variable) {
        if (variable instanceof FieldReference fr && fr.scopeVariable() != null) {
            return Stream.concat(variableAndScopes(fr.scopeVariable()), Stream.of(variable));
        }
        if (variable instanceof DependentVariable dv) {
            return Stream.concat(variableAndScopes(dv.arrayVariable()), Stream.of(variable));
        }
        return Stream.of(variable);
    }

    public static String simpleName(Variable variable) {
        return simpleName(variable, Set.of());
    }

    public static String simpleName(Variable variable, Set<Variable> modified) {
        assert modified != null;
        assert variable != null;
        return switch (variable) {
            case ParameterInfo pi -> pi.index() + ":" + pi.name() + (modified.contains(pi) ? "*" : "");
            case ReturnVariable rv -> rv.methodInfo().name();
            case FieldReference fr -> {
                boolean frModified = modified.contains(fr);
                String scope = fr.scopeVariable() != null
                        ? simpleName(fr.scopeVariable(), frModified ? Set.of() : modified)
                        : fr.scope().toString();
                yield scope + "." + fr.fieldInfo().name() + (frModified ? "*" : "");
            }
            case DependentVariable dv -> {
                boolean dvModified = modified.contains(dv);
                String index = dv.indexVariable() != null
                        ? simpleName(dv.indexVariable(), dvModified ? Set.of() : modified)
                        : dv.indexExpression().toString();
                String simpleArrayVar;
                if (dv.arrayVariable() != null) simpleArrayVar = simpleName(dv.arrayVariable(), modified);
                else simpleArrayVar = dv.arrayExpression().toString();
                yield simpleArrayVar + "[" + index + "]" + (dvModified ? "*" : "");
            }
            default -> variable + (modified.contains(variable) ? "*" : "");
        };
    }


    // 3.0.0-E, +I
    public static String stage(String assignmentId) {
        Matcher m = StatementIndex.STAGE_PATTERN.matcher(assignmentId);
        if (m.matches()) return m.group(2);
        throw new UnsupportedOperationException();
    }

    public static String stripStage(String index) {
        Matcher m = StatementIndex.STAGE_PATTERN.matcher(index);
        if (m.matches()) return m.group(1);
        return index;
    }

    // add a character so that we're definitely beyond this index
    public static String beyond(String index) {
        return index + END;
    }

    public static boolean virtual(FieldInfo fieldInfo) {
        return fieldInfo.name().startsWith("§");
    }

    public static boolean virtual(Variable v) {
        if (v instanceof FieldReference fr) {
            return virtual(fr.fieldInfo());
        }
        if (v instanceof DependentVariable dv) {
            return virtual(dv.arrayVariable()) ||
                   dv.indexExpression() instanceof IntConstant ic && ic.constant() < 0;
        }
        return false;
    }

    public static boolean needsVirtual(ParameterizedType pt) {
        if (pt.typeParameter() != null && pt.arrays() > 0) return true;
        if (pt.isFunctionalInterface()) return false;
        TypeInfo best = pt.bestTypeInfo();
        return best != null && (best.isAbstract() || best.compilationUnit().externalLibrary());
    }
}
