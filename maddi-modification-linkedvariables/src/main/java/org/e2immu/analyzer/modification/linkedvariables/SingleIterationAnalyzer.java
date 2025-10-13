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

package org.e2immu.analyzer.modification.linkedvariables;

import org.e2immu.language.cst.api.info.Info;
import org.e2immu.util.internal.graph.G;

import java.util.List;
import java.util.Map;

/*
Phases.

1. Method modification and linking. WaitFor: other methods, both internal and external.
    NOTE: we wait for a method, even if it is the modification or independence of a parameter that we're actually
          waiting for. This is done to reduce the complexity of the eventual 'waitFor' graph.

    NOTE: this waitFor is the ONLY cause of cyclic computation issues in the analyzer.

    NOTE: this analyzer also deals with @Identity, @Fluent, and static values

2. Field modification, linking and independence. WaitFor: internal methods.
    NOTE: when a field is non-final, we don't bother computing any other value.

3. Primary type, modification and independence of all its components.
    WaitFor: nothing. Either the values are there, or they are not. If they're not, they should be covered by the
             waitFor of phases 1 or 2.
   This analyzer can solve internal cycles, but because phase 1 can also inject external methods, this is not a panacea.

4.1. Primary type @Immutable, @Independence

    This waitingFor can cause cycles only for non-private fields, which must be of immutable type for the owner to
    be immutable (one constraint among many).

4.2. Type @Container. Once phase 3 has provided sufficient values, we can compute the @Container property of a type
    from the modification state of its method's parameters.

    NOTE: we cannot enforce the @Container property from its parents' COMPUTED property.
    So either the parent's property is confirmed, in which case we can raise a warning later.
    Or, if it's not confirmed, having modifications in one parameter in this type can invalidate the parent's.

    If the type is not overriding every method, then it can cause external waiting for the modification status
    of the ancestor's parameters' modification status.
    This waitingFor cannot cause cycles, because it is tied to Java's strictly enforced type hierarchy DAG.

5. Type misc. Derivative annotations such as @UtilityClass, @Singleton. They cause no waitFor.

6. abstract types and methods: reverse computation

 */
public interface SingleIterationAnalyzer extends Analyzer {

    interface Output extends Analyzer.Output {

        G<Info> waitFor();

        Map<String, Integer> infoHistogram();
    }

    default Output go(List<Info> analysisOrder, boolean activateCycleBreaking) {
        return go(analysisOrder, activateCycleBreaking, true);
    }

    Output go(List<Info> analysisOrder, boolean activateCycleBreaking, boolean firstIteration);

}
