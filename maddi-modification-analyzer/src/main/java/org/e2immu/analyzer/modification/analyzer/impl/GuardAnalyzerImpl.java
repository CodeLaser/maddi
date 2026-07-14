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

package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.analyzer.GuardAnalyzer;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.common.defaults.ContractReader;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.MessageImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.List;
import java.util.Map;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;

/**
 * First iteration of the guard: verifies the two contracts most central to the container story.
 * <ul>
 *   <li>{@code @Container} on a type: no non-private method of the type, nor any implementation of its
 *       abstract methods, may modify a parameter. The contract binds implementations only through the
 *       methods they inherit from the contracted type (a subtype may add modifying methods of its own).</li>
 *   <li>{@code @NotModified} on an abstract method: no implementation may be modifying.</li>
 * </ul>
 * A violation is only reported on a decided FALSE — an undecided value (cycle, external code) stays silent,
 * so the guard never reports on incomplete information.
 */
public class GuardAnalyzerImpl extends CommonAnalyzerImpl implements GuardAnalyzer {
    public static final String CONTRACT_VIOLATION = "contract-violation";

    private final ContractReader contractReader;

    public GuardAnalyzerImpl(Runtime runtime, IteratingAnalyzer.Configuration configuration, List<Message> messages) {
        super(configuration, null, messages);
        this.contractReader = new ContractReader(runtime);
    }

    @Override
    public void go(List<Info> analysisOrder) {
        for (Info info : analysisOrder) {
            if (info instanceof TypeInfo typeInfo) {
                guardType(typeInfo);
            } else if (info instanceof MethodInfo methodInfo) {
                guardMethod(methodInfo);
            }
        }
    }

    private void guardType(TypeInfo typeInfo) {
        Map<Property, Value> contracts = contractReader.contracts(typeInfo);
        if (contracts.get(CONTAINER_TYPE) instanceof Value.Bool container && container.isTrue()) {
            guardContainer(typeInfo);
        }
    }

    private void guardContainer(TypeInfo contractHolder) {
        contractHolder.constructorAndMethodStream()
                .filter(mi -> !mi.access().isPrivate())
                .forEach(mi -> {
                    if (mi.isAbstract()) {
                        Value.SetOfMethodInfo implementations = mi.analysis().getOrDefault(IMPLEMENTATIONS,
                                ValueImpl.SetOfMethodInfoImpl.EMPTY);
                        for (MethodInfo implementation : implementations.methodInfoSet()) {
                            checkParametersUnmodified(contractHolder, mi, implementation);
                        }
                    } else {
                        // the contracted type's own concrete (default, static) methods and constructors
                        checkParametersUnmodified(contractHolder, mi, mi);
                    }
                });
    }

    private void checkParametersUnmodified(TypeInfo contractHolder, MethodInfo declaration, MethodInfo target) {
        for (ParameterInfo pi : target.parameters()) {
            Value.Bool unmodified = pi.analysis().getOrNull(UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
            if (unmodified != null && unmodified.isFalse()) {
                Message contractLocation = MessageImpl.cause(contractHolder,
                        "@Container contracted on " + contractHolder.simpleName());
                Message via = target == declaration ? contractLocation
                        : MessageImpl.cause(declaration, target.simpleName() + " implements "
                                                         + declaration.fullyQualifiedName()
                                                         + ", declared in the @Container type", contractLocation);
                analyzerMessages.add(MessageImpl.error(pi, CONTRACT_VIOLATION,
                        "parameter '" + pi.simpleName() + "' of " + target.fullyQualifiedName()
                        + " is modified, violating the @Container contract on "
                        + contractHolder.fullyQualifiedName(), via));
            }
        }
    }

    private void guardMethod(MethodInfo methodInfo) {
        if (!methodInfo.isAbstract()) return;
        Map<Property, Value> contracts = contractReader.contracts(methodInfo);
        if (contracts.get(NON_MODIFYING_METHOD) instanceof Value.Bool nonModifying && nonModifying.isTrue()) {
            Value.SetOfMethodInfo implementations = methodInfo.analysis().getOrDefault(IMPLEMENTATIONS,
                    ValueImpl.SetOfMethodInfoImpl.EMPTY);
            for (MethodInfo implementation : implementations.methodInfoSet()) {
                Value.Bool implNonModifying = implementation.analysis().getOrNull(NON_MODIFYING_METHOD,
                        ValueImpl.BoolImpl.class);
                if (implNonModifying != null && implNonModifying.isFalse()) {
                    analyzerMessages.add(MessageImpl.error(implementation, CONTRACT_VIOLATION,
                            implementation.fullyQualifiedName()
                            + " is modifying, violating the @NotModified contract on "
                            + methodInfo.fullyQualifiedName(),
                            MessageImpl.cause(methodInfo, "@NotModified contracted here")));
                }
            }
        }
    }
}
