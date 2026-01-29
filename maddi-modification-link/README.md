# Linking

A link is a triple `(from, linkNature, to)`, where `from` and `to` are variables, and `linkNature` is one of:

* ≡ _is identical to_
* ⊆ _is subset of_: `from` is of an array type, and its elements are all contained in `to`
* ⊇ _is superset of_: `to` is of an array type, and its elements are all contained in `from`
* ~ _shares elements with_: both `from` and `to` are of an array type, and they (may) share elements
* ∈ _is element of_: `from` is an element of `to`, which should be an array and have a higher dimension.
* ∋ _contains as member_: `to` is an element of `from`, which should be an array and have a higher dimension.
* ≺ _is field of_
* ≻ _contains as field_
* ≈ _shares fields with_
* ≤ _is part of the object graph of_
* ≥ _contains in its object graph_
* ∩ _object graphs overlap_: this is the least informative of the relations.

Making a graph out of these links is done using a fixpoint propagation algorithm, and a binary operator on `linkNature`.
Some noteworthy combinations are:

* a ∋ b ∈ c implies a ~ c; similarly, a ⊇ b ⊆ c implies a ~ c
* a ∈ b ⊆ c implies a ∈ c; reversing this, a ⊇ b ∋ c implies a ∋ c

See `operator.adoc` for a comprehensive table.

## Virtual fields

All virtual fields' names start with § character, illegal in Java.
They are created when a type is either abstract, or seen as abstract by the shallow link analyzer, with the purpose of
being able to link variables with compatible types.

Each abstract type gets up to two virtual fields:

* the mutation field `§m`
* the hidden content field
  The combination of these virtual fields are sufficient to express linking.

Hidden content fields are of twe different types:

* a type parameter in some array dimension
* a container of type parameters in some array dimension

There is no distinction between one-dimensional arrays and the collection types `Colletion`, `List`, `Set` from the
point of view of virtual fields. Containers are used for types with multiple type parameters, such as `Map`.

The name of a hidden content field is a reflection of its type: all type parameters are represented in lowercase, and
and `s` is added for each extra dimension.

Hidden content fields appear in abstract and in concrete form. Their type and name changes accordingly.
Any non-type parameter is represented by `$`.

For example, `List<E>` gets two virtual fields: `§m` and `§es`. The latter is of type `E[]` and therefore its name
consists of the `e` followed by one `s`.
In a concrete situation, the `§es` changes names to become

* `§xs` for `List<X>`
* `§$s` for `List<Object>`
* `§tss` for `List<T[]>` or `List<List<T>>`.

### Elements

The relations ~, ⊆, ⊇ require virtual fields of at least dimension 1 (1 `s`) on either side. LHS and RHS must be of the
same dimension.
The relation ∈ requires a virtual field on the RHS; the LHS is either

* a variable of a type that corresponds to the virtual field on the RHS, without the arrays
* a virtual field of a variable that corresponds to the virtual field on the RHS, with fewer arrays (but at least one)

Examples:

```
a[0] ∈ a.§xs         X[] a
a[0][0] ∈∈ a.§xss    X[][] a
a[0].§xs ∈ a.§xs     X[][] a
```

The ∈ relation is written multiple times for additional dimensions to be spanned.

### Indexing, slicing

The hidden content field of `Map<K,V>` is `§kvs`, i.e., a container of `k` and `v`, with a single array dimension.
A `Map.Entry<K, V>` has a hidden content field `§kv`, without the `s`.
One can say that the first entry `entry ∈ map.§kvs`.

All `k` elements in a map can be expressed with the slice `§kvs[-1].§k`.
Indexing in `entry`, we can write

```
entry.§kv.§v ∈ map.§kvs[-2].§v
```

### Concrete situations

solving problems in TestStaticValuesRecord,4.

## Shallow method analysis

In the case of methods, from parameter into object, array types use ~.
All other combinations use ⊆ or ⊇:

* from the parameter into the object, in case of constructors (⊇)
* from the return value into the object or parameter (⊆)
* from the object into a functional interface parameter (consumer, function, ⊆)
* between parameters, especially from a varargs ⊆ a collection

## Notes

Modifications change ⊆ and ⊇ to ~, as shown in `TestConstructor,1`. This is unfortunate in the case of the terminal
operations of `Stream`, which are technically modifications that do not touch the hidden content of the stream object
being finalized. For that reason, as shown in `TestStreamBasics,1`, the object of a `@Finalizer` method is not marked
modified. Streams benefit from the directional flow of hidden content (⊆, ⊇) because if a ∈ b ⊆ c, then a ∈ c,
while a ∈ b ~ c does not imply a ∈ c.

A finalizer method changes the internal state of the object but does not touch its hidden content. After the call, no
modifying methods are allowed anymore. Other examples of `@Finalizer` methods are builders that cannot be used anymore
after a `build()` call, or objects that are mode immutable after a `freeze()` call.

## Special variables

Next to the virtual fields, we use special variables of 2 different classes:

1. intermediate variables (`$__` prefix), for variables that cannot escape local linking
    1. `$__rv` return value of method call
    2. `$__c` newly created object
    3. `$__ic` inline condition
    4. `$__l` ? created temporarily for argument values
2. marker variables: (`$_` prefix)
    1. `$_fi` functional interface variable. Is either of class `FunctionalInterfaceVariable` or of class
       `AppliedFunctionalInterfaceVariable`. Represents the functional interface or the result of applying the function
       interface SAM.
    2. `$_ce` constant expression; useful for array indexing
    3. `$_v` some value; used to help identify `@Identity`

The marker variables should definitely go into the linkedVariables of VariableInfo; there is a case to be made for
allowing them in the method links.
