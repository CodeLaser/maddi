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
* ∩ _object graphs overlap_: this is the least informative of the operators
  

Making a graph out of these links is done using a fixpoint propagation algorithm, and a binary operator on `linkNature`.
Some noteworthy combinations are:

* a ∋ b ∈ c implies a ~ c; similarly, a ⊇ b ⊆ c implies a ~ c 
* a ∈ b ⊆ c implies a ∈ c; reversing this, a ⊇ b ∋ c implies a ∋ c

See `operator.adoc` for a comprehensive table.

## Virtual fields

All virtual fields' names start with § character, illegal in Java.

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
