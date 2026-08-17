package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.impl.localvar.FunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.LinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/*
primary = end result
links = additional links for parts of the result
extra = link information about unrelated variables
 */
public class Result {
    private final Links links;
    private final LinkedVariables extra;
    private final Map<Variable, Set<MethodInfo>> modified;
    private final List<ExpressionVisitor.WriteMethodCall> writeMethodCalls;
    private final Map<Variable, Set<TypeInfo>> casts;
    private final Set<Variable> erase;
    private final Set<LocalVariable> variablesRepresentingConstants;

    private Expression evaluated;

    public Result(Links links,
                  LinkedVariables extra,
                  Map<Variable, Set<MethodInfo>> modified,
                  List<ExpressionVisitor.WriteMethodCall> writeMethodCalls,
                  Map<Variable, Set<TypeInfo>> casts,
                  Set<Variable> erase,
                  Set<LocalVariable> variablesRepresentingConstants) {
        this.links = links;
        this.extra = extra;
        this.modified = modified;
        this.writeMethodCalls = writeMethodCalls;
        this.casts = casts;
        this.erase = erase;
        this.variablesRepresentingConstants = variablesRepresentingConstants;
    }

    public Result(Links links, LinkedVariables extra) {
        this(links, extra, new HashMap<>(), new ArrayList<>(), new HashMap<>(), new HashSet<>(), new HashSet<>());
    }

    @Override
    public String toString() {
        return "Result{" +
               "links=" + links +
               (extra.isEmpty() ? "" : ", extra=" + extra) +
               (modified.isEmpty() ? "" : ", modified=" + modified) +
               (writeMethodCalls.isEmpty() ? "" : ", writeMethodCalls=" + writeMethodCalls) +
               (casts.isEmpty() ? "" : ", casts=" + casts) +
               (erase.isEmpty() ? "" : ", erase=" + erase) +
               (variablesRepresentingConstants.isEmpty() ? "" : ", variablesRepresentingConstants=" + variablesRepresentingConstants) +
               ", evaluated=" + evaluated +
               '}';
    }

    public Expression getEvaluated() {
        return evaluated;
    }

    public Result setEvaluated(Expression evaluated) {
        this.evaluated = evaluated;
        return this;
    }

    public void addErase(Variable variable) {
        this.erase.add(variable);
    }

    public @NotNull Result addExtra(Map<Variable, Links> linkedVariables) {
        if (!linkedVariables.isEmpty()) {
            return new Result(links, extra.merge(new LinkedVariablesImpl(linkedVariables)),
                    modified, writeMethodCalls, casts, erase,
                    variablesRepresentingConstants);
        }
        return this;
    }

    public Result addModified(Set<Variable> variables, MethodInfo methodThatCausesModification) {
        for (Variable variable : variables) {
            Set<MethodInfo> methodInfos = this.modified.computeIfAbsent(variable, _ -> new HashSet<>());
            if (methodThatCausesModification != null) methodInfos.add(methodThatCausesModification);
        }
        return this;
    }

    public Result add(ExpressionVisitor.WriteMethodCall writeMethodCall) {
        this.writeMethodCalls.add(writeMethodCall);
        return this;
    }

    public Result addCasts(Map<Variable, Set<TypeInfo>> map) {
        map.forEach((v, tiSet) -> this.casts.computeIfAbsent(v, _ -> new HashSet<>())
                .addAll(tiSet));
        return this;
    }

    public Result addCast(Variable variable, TypeInfo pt) {
        this.casts.computeIfAbsent(variable, _ -> new HashSet<>()).add(pt);
        return this;
    }

    public Result addVariableRepresentingConstant(LocalVariable lv) {
        this.variablesRepresentingConstants.add(lv);
        return this;
    }

    public Result addVariablesRepresentingConstant(List<Result> params) {
        params.forEach(p -> this.variablesRepresentingConstants.addAll(p.variablesRepresentingConstants));
        return this;
    }

    public Result addVariablesRepresentingConstant(Result object) {
        this.variablesRepresentingConstants.addAll(object.variablesRepresentingConstants);
        return this;
    }

    public Set<Variable> erase() {
        return erase;
    }

    public Map<Variable, Set<TypeInfo>> casts() {
        return casts;
    }

    public LinkedVariables extra() {
        return extra;
    }

    public Links links() {
        return links;
    }

    public List<ExpressionVisitor.WriteMethodCall> writeMethodCalls() {
        return writeMethodCalls;
    }

    public Map<Variable, Set<MethodInfo>> modified() {
        return modified;
    }

    public Set<LocalVariable> variablesRepresentingConstants() {
        return variablesRepresentingConstants;
    }

    public Result with(Links links) {
        return new Result(links, extra, modified, writeMethodCalls, casts,
                erase, variablesRepresentingConstants).setEvaluated(evaluated);
    }

    public Result merge(Result other) {
        if (this == ExpressionVisitor.EMPTY) return other;
        if (other == ExpressionVisitor.EMPTY) return this;
        LinkedVariables combinedExtra = extra.isEmpty() ? other.extra : extra.merge(other.extra);
        if (other.links != null && other.links.primary() != null) {
            combinedExtra = combinedExtra.merge(new LinkedVariablesImpl(Map.of(other.links.primary(), other.links)));
        }
        Result r = new Result(this.links, combinedExtra,
                new HashMap<>(this.modified),
                new ArrayList<>(this.writeMethodCalls),
                new HashMap<>(this.casts),
                new HashSet<>(this.erase),
                new HashSet<>(this.variablesRepresentingConstants));
        r.writeMethodCalls.addAll(other.writeMethodCalls);
        other.modified.forEach((v, set) ->
                r.modified.computeIfAbsent(v, _ -> new HashSet<>()).addAll(set));
        r.variablesRepresentingConstants.addAll(other.variablesRepresentingConstants);
        other.casts.forEach((v, set) ->
                r.casts.computeIfAbsent(v, _ -> new HashSet<>()).addAll(set));
        r.erase.addAll(other.erase);
        return r.setEvaluated(evaluated);
    }

    public Result moveLinksToExtra() {
        if (links.primary() != null) {
            LinkedVariables newExtra = this.extra.merge(new LinkedVariablesImpl(Map.of(links.primary(), links)));
            return new Result(LinksImpl.EMPTY, newExtra, modified, writeMethodCalls, casts, erase,
                    variablesRepresentingConstants).setEvaluated(evaluated);
        }
        return this;
    }

    public Result copyLinksToExtra() {
        if (links.primary() != null) {
            LinkedVariables newExtra = this.extra.merge(new LinkedVariablesImpl(Map.of(links.primary(), links)));
            return new Result(links, newExtra, modified, writeMethodCalls, casts, erase,
                    variablesRepresentingConstants).setEvaluated(evaluated);
        }
        return this;
    }

    public Result expandFunctionalInterfaceVariables() {
        if (links.primary() instanceof FunctionalInterfaceVariable fiv) {
            // TestSupplier, 1
            return fiv.result().setEvaluated(evaluated);
        }
        for (Link link : links) {
            if (link.from().equals(links.primary())
                && link.to() instanceof FunctionalInterfaceVariable fiv && link.linkNature().isAssignedFrom()) {
                // 3 cases in TestSupplier (1b, 5method2, 7)
                return fiv.result().setEvaluated(evaluated);
            }
        }
        return this;
    }

}
