Different phases in the analyzer.
"WaitFor" scheme to delay decisions.

### Phase 1, methods, linking, method modification

Method modification and linking, by `LinkComputerImpl` in the `link` project.

Writes:

- `MethodLinkedVariablesImpl.METHOD_LINKS`: a composite object that describes the linking and modification of a method (
  return variable, parameters, modified variables). It is the marker for the method-based link computer.
- `VariableInfoImpl.DOWNCAST_VARIABLES` internal to method
- `VariableInfoImpl.UNMODIFIED_VARIABLE`: internal to the method
- linked variables in `VariableInfo`
- `DOWNCAST_PARAMETER` copied from `DOWNCAST_VARIABLE` for parameters
- `NON_MODIFYING_METHOD`, copied from internal data
- `UNMODIFIED_PARAMETER`, copied from internal data, __unless the parameter is linked to a field of the method's type__,
  in which case it is phase 3 that writes the final value.

- `LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT`: used by CodeLaser ExtractInterface
- `DEFAULTS_ANALYZER` when it calls the `ShallowMethodAnalyzer` before entering the `ShallowMethodLinkComputer`

Reads:

- virtual field computer, for abstract methods: `IMMUTABLE_TYPE`, to determine the presence of `§m` when a type is
  mutable
- shallow: reads the annotated values for `INDEPENDENT_PARAMETER`, `INDEPENDENT_METHOD` using `getOrDefault`
- both read `GET_SET_FIELD` is read, but that one is not part of the analysis system

The current set-up is such that the link computer runs

- single pass
- on-demand

That means that it is vulnerable to

- method call cycles: when it "bites its tail", it uses the shallow link analyzer instead
- abstract methods: it uses the shallow link analyzer
- decisions from the phase 2+3 analyzers to decide on modification of parameters. A parameter may become "unmodified".

As stated, the shallow link analyzer requires information about the immutability of types and the independence of
parameters and methods. This information is highly order-dependent in the case of cyclic code (type, method)
dependencies.

Solutions to the problem are

- avoid cycles (maybe wishful thinking)
- annotate abstract methods and types (a lot of work)
- copy the information from implementation to abstract. This works well when the implementation comes before the use of
  the abstract type in the dependency graph.

Phase 6 will copy from implementation to abstract, using property overwrite (values can get better, but never worse).

NOTE that the shallow link analyzer does not set any properties; it only computes `METHOD_LINKS` for the link computer
to set. `NON_MODIFYING_METHOD` and `UNMODIFIED_PARAMETER` must either come from annotations, or from the copying of
values from implementations.


### Phase 2, fields

Writes

- `LinksImpl.LINKS`, the linked variables of a field
- `UNMODIFIED_FIELD`
- `INDEPENDENT_FIELD`

Reads

- `UNMODIFIED_VARIABLE` from `VariableInfo` set by phase 1
- linked variables from `VariableInfo`, set by phase 1
- `GET_SET_FIELD`, `PART_OF_CONSTRUCTION` from the prep analyzer

This analyzer produces a final value when all methods have been analyzed, i.e., when phase 1 is completed for the
field's owner type.

### Phase 3, more modification and independence

Classes: `TypeModIndyAnalyzer`, `TypeModIndyAnalyzerImpl`.

Writes

- `IDENTITY`, `FLUENT` for methods
- `INDEPENDENT_METHOD`
- `INDEPENDENT_PARAMETER`
- `UNMODIFIED_PARAMETER` when not already set by phase 1

Reads

- `METHOD_LINKS` from phase 1
- `UNMODIFIED_FIELD` from phase 2
- `UNMODIFIED_PARAMETER`, to see if phase 1 handled it
- `IMMUTABLE_TYPE` __from later phases__

Primary type, modification and independence of all its components.
WaitFor: nothing. Either the values are there, or they are not. If they're not, they should be covered by the
waitFor of phases 1 or 2.
This analyzer can solve internal cycles, but because phase 1 can also inject external methods, this is not a panacea.

Does modification and independence of parameters, and independence, fluent, identity of methods.
Also breaks internal cycles.

Modification of methods and linking of variables is done in Phase 1.
Linking, modification and independence of fields is done in Phase 2.
Immutable, independence of types is done in Phase 4.1.

Strategy:
method independence, parameter independence can directly be read from linked variables computed in field analyzer.
parameter modification is computed as the combination of links to fields and local modifications.

### Phase 4

4.1 Primary type @Immutable, @Independence

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

### Phase 5

5. Type misc. Derivative annotations such as @UtilityClass, @Singleton. They cause no waitFor.

### Phase 6

abstract types and methods: reverse computation
