package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enforces the layering that the package split expresses.
 *
 * <p>Java packages do not constrain dependency direction by themselves — splitting classes into
 * {@code domain}, {@code application} and {@code adapter} only documents an intent that nothing
 * stops the next edit from violating. This test is the thing that actually holds the line, and it
 * does so without adding an architecture-testing dependency: it reads the sources and checks which
 * layers each file names.
 *
 * <p>The allowed direction is inward only:
 * <pre>
 *   adapter      -&gt; application, domain
 *   application  -&gt; domain
 *   domain       -&gt; (nothing)
 * </pre>
 * So the domain cannot reach for a port or an adapter, and the application cannot reach for a
 * concrete adapter. Matching the whole source text rather than just the import block also catches
 * a fully-qualified reference written inline.
 */
@Tag("Unit")
class PackageDependencyTests {
    private static final String BASE = "com.softwareascraft.prescriptionreporter";
    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

    private static final Pattern LAYER_REFERENCE =
            Pattern.compile(Pattern.quote(BASE) + "\\.(domain|application|adapter)\\b");

    private static final Map<String, Set<String>> ALLOWED_DEPENDENCIES = Map.of(
            "domain", Set.of(),
            "application", Set.of("domain"),
            "adapter", Set.of("application", "domain"));

    private static List<Path> sourcesIn(String layer) throws IOException {
        final Path dir = MAIN_SOURCES.resolve(BASE.replace('.', '/')).resolve(layer);
        assertThat(dir).as("source directory for layer '%s'", layer).isDirectory();
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static Set<String> layersReferencedBy(Path source) throws IOException {
        final String text = Files.readString(source, StandardCharsets.UTF_8);
        final Matcher matcher = LAYER_REFERENCE.matcher(text);
        final List<String> found = new ArrayList<>();
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return Set.copyOf(found);
    }

    @ParameterizedTest
    @ValueSource(strings = {"domain", "application", "adapter"})
    void noLayerDependsOutward(String layer) throws IOException {
        final Set<String> allowed = ALLOWED_DEPENDENCIES.get(layer);
        final List<String> violations = new ArrayList<>();

        for (Path source : sourcesIn(layer)) {
            for (String referenced : layersReferencedBy(source)) {
                if (!referenced.equals(layer) && !allowed.contains(referenced)) {
                    violations.add(source.getFileName() + " -> " + referenced);
                }
            }
        }

        assertThat(violations)
                .as("'%s' may only depend on %s", layer, allowed.isEmpty() ? "nothing" : allowed)
                .isEmpty();
    }

    @Test
    void theDomainNamesNoOtherLayerAtAll() throws IOException {
        for (Path source : sourcesIn("domain")) {
            assertThat(layersReferencedBy(source))
                    .as("%s should name no layer but its own", source.getFileName())
                    .isSubsetOf(Set.of("domain"));
        }
    }

    @Test
    void everyLayerActuallyHasSources() throws IOException {
        for (String layer : ALLOWED_DEPENDENCIES.keySet()) {
            assertThat(sourcesIn(layer)).as("sources in '%s'", layer).isNotEmpty();
        }
    }

    /**
     * Guards the test itself: if the sources move and the walk silently finds nothing, the checks
     * above would pass vacuously.
     */
    @Test
    void theSourceTreeIsWhereThisTestExpectsIt() {
        assertThat(MAIN_SOURCES)
                .as("main sources, relative to the build's working directory")
                .isDirectory();
    }
}
