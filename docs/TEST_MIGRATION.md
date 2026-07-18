# Test migration: maddi-inspection-integration → maddi-java-openjdk

Tests in `maddi-inspection-integration` use a high-level `JavaInspector` API:
```java
TypeInfo typeInfo = javaInspector.parse(INPUT1);
```
Tests in `maddi-java-openjdk` use OpenJDK's `javac` directly via `CommonTest.scan()`:
```java
TypeInfo typeInfo = scan("pkg.ClassName", INPUT1);
```
When moving a test, the FQN (`"pkg.ClassName"`) is extracted from the `package` and primary type declarations inside the INPUT string.

---

## Already in place (moved before this session)

The following tests had counterparts in `maddi-java-openjdk` before this work started.
Note that some were placed in a different subdirectory than the integration original.

| Integration path | Openjdk path |
|---|---|
| `constructor/TestAnonymousType` | `constructor/TestAnonymousType` |
| `constructor/TestConstructor` | `constructor/TestConstructor` |
| `constructor/TestConstructor2` | `constructor/TestConstructor2` |
| `constructor/TestExplicitConstructorInvocation` | `constructor/TestExplicitConstructorInvocation` |
| `constructor/TestExtendedConstructor` | `constructor/TestExtendedConstructor` |
| `method/TestMethodCall0` | `method/TestMethodCall0` |
| `method/TestMethodCall1` | `method/TestMethodCall1` |
| `method/TestMethodCall2` | `method/TestMethodCall2` |
| `method/TestMethodCall3` | `method/TestMethodCall3` |
| `method/TestMethodCall4` | `method/TestMethodCall4` |
| `method/TestMethodCall5` | `method/TestMethodCall5` |
| `method/TestMethodCall6` | `method/TestMethodCall6` |
| `other/TestAnnotations` | `other/TestAnnotations` |
| `other/TestArrayInitializer` | `expression/TestArrayInitializer` |
| `other/TestEnum` | `other/TestEnum` |
| `other/TestInstanceOf` | `expression/TestInstanceOf` |
| `other/TestJavaDoc` | `other/TestJavaDoc` |
| `other/TestLambda` | `expression/TestLambda` |
| `other/TestMethodReference` | `expression/TestMethodReference` |
| `other/TestModuleInfo` | `other/TestModuleInfo` |
| `other/TestPackageInfo` | `other/TestPackageInfo` |
| `other/TestRecord` | `other/TestRecord` |
| `other/TestRecordPattern` | `other/TestRecordPattern` |
| `other/TestTryCatch` | `statement/TestTryCatch` |
| `other/TestTryResource` | `statement/TestTryResource` |
| `other/TestTypeParameter` | `other/TestTypeParameter` |
| `other/TestUnnamedVariable` | `expression/TestUnnamedVariable` |
| `type/TestLocalType` | `statement/TestLocalType` |
| `type/TestSealed` | `other/TestSealed` |

---

## Successfully moved in this session

The following tests were moved with only the mechanical change of adding the FQN first argument to the `scan()` call (and, for `CommonTest2`-based tests, converting `init(Map.of(...))` → `scan(false, ...)` and `ParseResult.findType()` → `Map.get()`).

| Integration path | Openjdk path |
|---|---|
| `constructor/TestInitializers` | `constructor/TestInitializers` |
| `constructor/TestRecordConstructor` | `constructor/TestRecordConstructor` |
| `method/TestMethodCall7` | `method/TestMethodCall7` |
| `method/TestMethodCall10` | `method/TestMethodCall10` |
| `method/TestMethodCall11` | `method/TestMethodCall11` |
| `method/TestMethodCall12` | `method/TestMethodCall12` |
| `method/TestMethodCall13` | `method/TestMethodCall13` |
| `method/TestOverload0` | `method/TestOverload0` |
| `method/TestOverride2` | `method/TestOverride2` |
| `method/TestOverride3` | `method/TestOverride3` |
| `method/TestOverride4` | `method/TestOverride4` |
| `method/TestOverridesOfRecordAccessors` | `method/TestOverridesOfRecordAccessors` |
| `other/TestComments` | `other/TestComments` |
| `other/TestExceptionTypes` | `other/TestExceptionTypes` |
| `other/TestField` | `other/TestField` |
| `other/TestField2` | `other/TestField2` |
| `other/TestFieldAccess` | `other/TestFieldAccess` |
| `other/TestForLoop` | `other/TestForLoop` |
| `other/TestImport` | `other/TestImport` |
| `other/TestImport2` | `other/TestImport2` |
| `other/TestImport3` | `other/TestImport3` |
| `other/TestImport4` | `other/TestImport4` |
| `other/TestJavaDoc2` | `other/TestJavaDoc2` |
| `other/TestJavaDoc3` | `other/TestJavaDoc3` |
| `other/TestNative` | `other/TestNative` |
| `other/TestOperators` | `other/TestOperators` |
| `other/TestParseResult` | `other/TestParseResult` |
| `other/TestStaticInitializer` | `other/TestStaticInitializer` |
| `other/TestSubTypes` | `other/TestSubTypes` |
| `other/TestSwitch` | `other/TestSwitch` |
| `other/TestTypeParameter2` | `other/TestTypeParameter2` |
| `other/TestTypesReferenced` | `other/TestTypesReferenced` |
| `other/TestVar` | `other/TestVar` |
| `print/TestCastAndMemberAccess` | `print/TestCastAndMemberAccess` |
| `print/TestTextBlockFormatting` | `print/TestTextBlockFormatting` |
| `print/TestVariousPrint2Issues` | `print/TestVariousPrint2Issues` |
| `type/TestAnnotation` | `type/TestAnnotation` |
| `type/TestApplyTranslation` | `type/TestApplyTranslation` |
| `type/TestFullyQualified` | `type/TestFullyQualified` |
| `type/TestHierarchy` | `type/TestHierarchy` |
| `type/TestInner2` | `type/TestInner2` |
| `type/TestInnerClass` | `type/TestInnerClass` |
| `type/TestIsAssignableFrom` | `type/TestIsAssignableFrom` |
| `type/TestTypeParameter` | `type/TestTypeParameter` |
| `type/TestVoid` | `type/TestVoid` |
| `lombok/TestBuilder` | `lombok/TestBuilder` |
| `lombok/TestConstructor` | `lombok/TestConstructor` |
| `lombok/TestData` | `lombok/TestData` |
| `lombok/TestGetter` | `lombok/TestGetter` |
| `lombok/TestLogger` | `lombok/TestLogger` |
| `lombok/TestSetter` | `lombok/TestSetter` |
| `stub/TestStub1` | `stub/TestStub1` |
| `stub/TestStub2` | `stub/TestStub2` |
| `translate/TestTranslateAnonymousType` | `translate/TestTranslateAnonymousType` |

---

## Moved with corrections

The following tests required changes beyond the mechanical conversion. Each correction is also documented with a comment at the affected location in the openjdk test file.

### `invalidate/TestInvalidate`

Original extended `CommonTest2` and used `javaInspector.reloadSources()`, `makeInputConfiguration()`, and `sourcesByURIString()` to simulate incremental re-parsing after a source change (`ISOURCE` → `ISOURCE_CHANGED`). None of those methods exist in this module's `CommonTest`. Each test was simplified to a single `scan(false, ...)` of all three sources; the invalidation/reload behaviour is not covered.

### `other/TestCompilationProblem`

Original did not extend `CommonTest`; it instantiated `JavaInspectorImpl` directly and read source files from `src/test/resources/compilationError/`, a directory that only exists in `maddi-inspection-integration`. No equivalent file-based resource setup is available here, so both test methods are disabled stubs.

### `other/TestCompiledTypesManager`

Original `test1` called `javaInspector.compiledTypesManager().isPackagePart(...)` to assert that scanning `INPUT1` registers `"veryunlikely"` as a known package part. `compiledTypesManager` is not available here; the `scan()` call is kept to verify the parse succeeds.

### `type/TestByteCode`

Every test called `javaInspector.compiledTypesManager().getOrLoad(<JdkClass>.class, ...)` to load and inspect byte-code-based type info (e.g. `AbstractMockMvcBuilder`, `FileOutputStream`, `Long`, `ArrayList`). `compiledTypesManager` is not available here; `classSymbolScanner.getType()` is the closest equivalent but only works for types already encountered during a prior `scan()` call. Since these tests did not scan any source — they only loaded compiled types — there is no viable replacement and the test bodies are left empty.

### `print/TestDetailedSources` — `test1`

`javaInspector.importComputer(6, sourceSet)` is a factory method on `JavaInspector` that configures the import-depth limit and source set. Replaced with `new ImportComputerImpl()` (direct instantiation with default configuration). `javaInspector.runtime()` replaced with the `runtime` field directly.

### `print/TestTypeQualification` — `test1`

Same correction as `TestDetailedSources`: `javaInspector.importComputer(4, null)` → `new ImportComputerImpl()`; `javaInspector.runtime()` → `runtime` field.

### `method/TestMethodCall8` — `test3`

After parsing `INPUT3`, the original called `javaInspector.compiledTypesManager().getOrLoad("org.assertj.core.api.AbstractCollectionAssert", null)` and `javaInspector.print2()` to assert the printed form of the assertj byte-code type. `compiledTypesManager` is not available here; the `scan()` call is kept to verify the parse succeeds.

### `method/TestMethodCall9` — `test4b`

One line used `javaInspector.runtime().qualificationSimpleNames()`. Replaced with `runtime.qualificationSimpleNames()` — the `runtime` field is the same `RuntimeImpl` instance, accessed directly from `CommonTest`.

### `method/TestOverload1` — `test4`

After parsing `INPUT4`, the original called `javaInspector.print2(typeInfo.compilationUnit())` and asserted the printed output matched an expected string with formatted type parameters. The assertion was dropped: `javaInspector.print2()` on the integration `JavaInspector` uses a different formatter configuration than `CommonTest.print2()` here, so the output strings are not comparable.

### `translate/TestTranslate` — `test1`

`javaInspector.compiledTypesManager().getOrLoad(Class.class)` and `getOrLoad(TypeDescriptor.class)` load JDK types from bytecode. Replaced with `classSymbolScanner.getType("java.lang.Class")` and `classSymbolScanner.getType("java.lang.invoke.TypeDescriptor")`, which retrieve types that javac resolved during the preceding `scan()` call.

### `genericshelper/TestGenericsHelper` — `test1`, `test2`

`javaInspector.compiledTypesManager().getOrLoad(List.class)` loads `java.util.List` from bytecode without a prior scan. Replaced with `classSymbolScanner.getType("java.util.List")` after a minimal `scan()` that causes javac to resolve `java.util.List`.
