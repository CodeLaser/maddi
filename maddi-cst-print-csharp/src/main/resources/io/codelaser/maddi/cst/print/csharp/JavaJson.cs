// Maddi.JavaCompat: what code translated from Java with a Jackson mapping needs beside its JSON library.

using System;

namespace Maddi.JavaCompat;

/// <summary>
/// Jackson's TypeReference, new TypeReference&lt;Map&lt;String, Object&gt;&gt;() { }: a type as a value. The
/// translated reads take their type as a type argument, so this is only the type the reference names.
/// </summary>
public abstract class JavaTypeReference<T>
{
    public new Type GetType() => typeof(T);
}
