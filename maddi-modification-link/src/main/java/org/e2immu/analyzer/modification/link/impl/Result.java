package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.LinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.expression.Expression;
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
    private final Set<Variable> modified;
    private final Set<Variable> modifiedFunctionalInterfaceComponents;
    private final List<ExpressionVisitor.WriteMethodCall> writeMethodCalls;
    private final Map<Variable, Set<TypeInfo>> casts;
    private final Set<Variable> erase;
    private final Set<LocalVariable> variablesRepresentingConstants;

    private Expression evaluated;

    public Result(Links links,
                  LinkedVariables extra,
                  Set<Variable> modified,
                  Set<Variable> modifiedFunctionalInterfaceComponents,
                  List<ExpressionVisitor.WriteMethodCall> writeMethodCalls,
                  Map<Variable, Set<TypeInfo>> casts,
                  Set<Variable> erase,
                  Set<LocalVariable> variablesRepresentingConstants) {
        this.links = links;
        this.extra = extra;
        this.modified = modified;
        this.modifiedFunctionalInterfaceComponents = modifiedFunctionalInterfaceComponents;
        this.writeMethodCalls = writeMethodCalls;
        this.casts = casts;
        this.erase = erase;
        this.variablesRepresentingConstants = variablesRepresentingConstants;
    }

    public Result(Links links, LinkedVariables extra) {
        this(links, extra, new HashSet<>(), new HashSet<>(), new ArrayList<>(), new HashMap<>(), new HashSet<>(),
                new HashSet<>());
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
                    modified, modifiedFunctionalInterfaceComponents, writeMethodCalls, casts, erase,
                    variablesRepresentingConstants);
        }
        return this;
    }

    public Set<Variable> modifiedFunctionalInterfaceComponents() {
        return modifiedFunctionalInterfaceComponents;
    }

    public Result addModified(Set<Variable> modified) {
        this.modified.addAll(modified);
        return this;
    }

    public Result addModifiedFunctionalInterfaceComponents(Set<Variable> set) {
        this.modifiedFunctionalInterfaceComponents.addAll(set);
        return this;
    }

    public Result add(ExpressionVisitor.WriteMethodCall writeMethodCall) {
        this.writeMethodCalls.add(writeMethodCall);
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

    public Set<Variable> modified() {
        return modified;
    }

    public Set<LocalVariable> variablesRepresentingConstants() {
        return variablesRepresentingConstants;
    }

    public Result with(Links links) {
        return new Result(links, extra, modified, modifiedFunctionalInterfaceComponents, writeMethodCalls, casts,
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
                new HashSet<>(this.modified),
                new HashSet<>(modifiedFunctionalInterfaceComponents),
                new ArrayList<>(this.writeMethodCalls),
                new HashMap<>(this.casts),
                new HashSet<>(this.erase),
                new HashSet<>(this.variablesRepresentingConstants));
        r.writeMethodCalls.addAll(other.writeMethodCalls);
        r.modified.addAll(other.modified);
        r.modifiedFunctionalInterfaceComponents.addAll(other.modifiedFunctionalInterfaceComponents);
        r.variablesRepresentingConstants.addAll(other.variablesRepresentingConstants);
        other.casts.forEach((v, set) ->
                r.casts.computeIfAbsent(v, _ -> new HashSet<>()).addAll(set));
        r.erase.addAll(other.erase);
        return r.setEvaluated(evaluated);
    }

    public Result moveLinksToExtra() {
        if (links.primary() != null) {
            LinkedVariables newExtra = this.extra.merge(new LinkedVariablesImpl(Map.of(links.primary(), links)));
            return new Result(LinksImpl.EMPTY, newExtra, modified, modifiedFunctionalInterfaceComponents,
                    writeMethodCalls, casts, erase, variablesRepresentingConstants).setEvaluated(evaluated);
        }
        return this;
    }

    public Result copyLinksToExtra() {
        if (links.primary() != null) {
            LinkedVariables newExtra = this.extra.merge(new LinkedVariablesImpl(Map.of(links.primary(), links)));
            return new Result(links, newExtra, modified, modifiedFunctionalInterfaceComponents,
                    writeMethodCalls, casts, erase, variablesRepresentingConstants).setEvaluated(evaluated);
        }
        return this;
    }
}
