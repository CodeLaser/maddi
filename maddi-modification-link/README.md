# Linking

## Notes

Modifications change ⊆ and ⊇ to ~, as shown in `TestConstructor,1`. This is unfortunate in the case of the terminal
operations of `Stream`, which are technically modifications that do not touch the hidden content of the stream object
being finalized. For that reason, as shown in `TestStreamBasics,1`, the object of a `@Finalizer` method is not marked
modified. Streams benefit from the directional flow of hidden content (⊆, ⊇) because if a ∈ b ⊆ c, then a ∈ c,
while a ∈ b ~ c does not imply a ∈ c.

A finalizer method changes the internal state of the object but does not touch its hidden content. After the call, no
modifying methods are allowed anymore. Other examples of `@Finalizer` methods are builders that cannot be used anymore
after a `build()` call, or objects that are mode immutable after a `freeze()` call. 
