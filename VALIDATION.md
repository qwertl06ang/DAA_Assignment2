# Execution record

Date: 28 September 2026. All measurements and test results in this package
come from executions of the submitted Java source on this machine.

## Completed checks

- Production sources compiled successfully with Java `javac --release 17`.
- All test sources compiled successfully against JUnit Jupiter 5.14.0.
- Maven Surefire 3.5.4 executed **39 JUnit 5 tests**: 39 passed, 0 failures,
  0 errors and 0 skipped. The output is in `results/tests.txt`.
- Maven JAR Plugin 3.4.2 packaged the compiled classes with the manifest entry
  `Main-Class: edu.daa.Benchmark`.
- The complete benchmark ran with `-Xms256m -Xmx1g` on Java 25.0.4.
- All 36 required cases were measured, each after warm-up and with five saved
  samples. `scripts/check_results.py` verified every reported median against
  the original nanosecond timings and checked deterministic counters.
- The 16 bonus construction cases also passed the five-sample validation.
- The PDF was rendered and visually checked: exactly five pages.

## Environment-specific compilation limitation

In the restricted automation environment, the regular Maven compiler path
encountered `java.nio.file.AccessDeniedException` while resolving parent
directories in the Windows user profile. This affected Java's path
canonicalization, including valid classpath entries; it was not a Java source
diagnostic. Granting read access did not resolve that environment issue.

For this verification, production sources were compiled together with `javac`.
JUnit class files were read from the normal Maven dependencies, and test sources
were compiled through the standard `JavaCompiler` API with a file manager that
reads those authorized workspace files directly. Maven then ran its actual
JUnit provider and JAR packaging goals using those compiled classes:

```sh
mvn surefire:test jar:jar
```

Therefore the tests and packaging are verified; a complete `mvn clean verify`
run could not be verified end-to-end in this sandbox. The standard `pom.xml`
retains ordinary Maven compilation for a normal JDK environment. Run
`mvn clean verify` locally before submission. A copy of the successfully
packaged runnable JAR is included under `bin/`.

No GitHub repository URL was supplied, so there is no remote publication or
Moodle upload in this execution record. The ZIP preserves the local Git history
as `repository.bundle`.
