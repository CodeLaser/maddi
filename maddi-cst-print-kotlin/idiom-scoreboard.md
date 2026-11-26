# Kotlin printer: idiom scoreboard

What the translation reads like, beyond whether it compiles (that is the J2K ratchets' job, see
`kotlin-printing.md`). Each J2K run reports these counts at the end of its `build/j2k/<name>/report.txt`
(`JavaToKotlinRatchet.IDIOMS`, CodeLaser/maddi#230). They are counted in the text of the translated **main**
sources, comments and string literals included: a measure to compare one commit with another, not an exact
census. Reported, not ratcheted: some go up as the output improves (templates), some down (`!!`, `open`), and a
better translation may move one the wrong way for a good reason.

Add a column when a change moves them, with the commits it was measured at. Newest right.

## langchain4j-core, with nullability verdicts (maddi-mod `TestJavaToKotlinLangchain4jNullability`)

346 main source files.

| idiom                          | 2026-10-15 (#140) | 2026-11-26 before | 2026-11-26 after |
|--------------------------------|------------------:|------------------:|-----------------:|
| `?.` (safe calls)              |                   |                 9 |                9 |
| `?:` (elvis)                   |                 6 |                10 |               10 |
| `!!` (assertions)              |               218 |               203 |              203 |
| `.equals(`                     |                 9 |                 9 |                9 |
| string templates (`$x`, `${…}`)|                 8 |                 6 |              276 |
| `+` next to a string literal   |                   |               478 |                0 |
| `val`                          |                   |              1022 |             1022 |
| `var`                          |                   |               287 |              287 |
| `lateinit`                     |                   |                 1 |                1 |
| `open` (declarations)          |                   |               998 |               10 |
| `data class`                   |                   |                 4 |                4 |
| `.stream()`                    |                61 |                40 |               40 |
| `Optional`                     |               101 |                86 |               86 |
| `.get(`                        |                   |                93 |               75 |

## fernflower, with nullability verdicts (maddi-mod `TestJavaToKotlinFernflowerNullability`)

199 main source files.

| idiom                          | 2026-10-15 (#140) | 2026-11-26 before | 2026-11-26 after |
|--------------------------------|------------------:|------------------:|-----------------:|
| `?.` (safe calls)              |                11 |                39 |               39 |
| `?:` (elvis)                   |                16 |                21 |               21 |
| `!!` (assertions)              |              2880 |              2825 |             2825 |
| `.equals(`                     |               152 |               156 |              156 |
| string templates (`$x`, `${…}`)|                   |                 0 |              310 |
| `+` next to a string literal   |                   |               391 |               22 |
| `val`                          |                   |              4857 |             4857 |
| `var`                          |                   |              1573 |             1573 |
| `lateinit`                     |                   |                15 |               15 |
| `open` (declarations)          |                   |               863 |               43 |
| `data class`                   |                   |                13 |               13 |
| `.stream()`                    |                   |                77 |               77 |
| `Optional`                     |                   |                 9 |                9 |
| `.get(`                        |                   |              1349 |              328 |

## guava, with nullability verdicts (maddi-mod `TestJavaToKotlinGuavaNullability`)

610 main source files; first measured 2026-11-26 after.

| idiom | after | idiom | after |
|---|---:|---|---:|
| `?.` | 114 | `var` | 1811 |
| `?:` | 139 | `lateinit` | 36 |
| `!!` | 1457 | `open` | 361 |
| `.equals(` | 107 | `data class` | 1 |
| templates | 502 | `.stream()` | 30 |
| `+` next to a literal | 38 | `Optional` | 253 |
| `val` | 6481 | `.get(` | 593 |

## Where the columns were measured

- **2026-10-15 (#140)**: the #140 review's numbers, from its issue comments and the langchain4j-nullability
  ratchet's log (maddi af913cce6); counted by hand then, with other patterns, so only the rows they share are
  filled.
- **2026-11-26 before**: maddi devel df2515a90, maddi-mod devel 46cf4ece, on the Mac; counted afterwards with the
  same patterns as the report.
- **2026-11-26 after**: maddi 86548b08e (#227 templates, #228 `open` in a closed world, #229 `list[i]`, one-line
  blocks, declarations with their next-statement assignment, `@BeforeEach` fixtures), maddi-mod 46cf4ece, on the
  Mac. What is left of `+` next to a literal is mostly what a text count cannot tell apart: Java code in comments
  (`// System.out.println("…" + x)`) and literals that hold a `+` (`"\\s+"`, `" + "`). `lateinit` fixtures live in the test sources, which these counts do not cover. Without verdicts
  (maddi-run-openjdk's `TestJavaToKotlin{Fernflower,Langchain4j,Guava}`) the same run counts 310, 276 and 502
  templates, and 43, 10 and 361 `open`.
