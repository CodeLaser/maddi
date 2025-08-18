# e2immu/e2immu-support

This project produces a small JAR containing the _e2immu_ annotations and support classes for inclusion in your project.
Please refer to https://www.e2immu.org for more information about the _e2immu_ project.

To publish a new version to your local Maven repository, execute:

```
./gradlew publishToMavenLocal
```

A jar with reference `org.e2immu:e2immu-support:0.5.0` will be available for inclusion.

A note on Java versions: Two methods in the support classes make use of the Java 10+ API: `AddOnceSet.toImmutableSet`
and `SetOnceMap.toImmutableMap`. Please remove them to obtain Java 1.8 compatibility. If you then also replace
the `Stream` calls in the same classes, you can go down to Java 1.7.

## Current version

As of the end of June 2022, version 0.2.0 is the version still associated with the `main` branch, and version 0.5.0 is the version that goes with the `development` branch.
The _Road to Immutability_ document corresponds to the development branch.

Version 0.6.2 is of July 2023. The one addition of 2023 is the `@Commutable` annotation.