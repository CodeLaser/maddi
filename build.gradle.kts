// The root build script exists for exactly one reason: to declare `org.jreleaser` ONCE, so that
// every subproject applying it shares a single plugin classloader.
//
// ⛔ Three subprojects deploy to Maven Central -- maddi-annotation, maddi-support and
// maddi-mvnplugin -- and declaring `id("org.jreleaser") version "..."` in each of them does NOT
// give one classloader. The first two apply plain `java-library`, so they land in the same scope
// and coexist; maddi-mvnplugin arrives through buildSrc's `java-library-conventions`, a different
// parent scope, and JReleaser is then loaded twice. The build fails at configuration time with
//
//     class org.jreleaser.gradle.plugin.Banner$Inject_ cannot be cast to
//     class org.jreleaser.gradle.plugin.Banner   (... two VisitableURLClassLoaders ...)
//
// which names neither the plugin's version nor the subproject that introduced the clash. Declaring
// it here with `apply false` puts it in the root scope, which every subproject inherits, and the
// subprojects apply it without a version.
plugins {
    id("org.jreleaser") version "1.19.0" apply false
}
