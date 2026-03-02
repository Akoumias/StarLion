package mapping;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import gr.forth.ics.rdfsuite.swkm.model.impl.RDF_Model;
import mapping.adapter.JenaCanonicalAdapter;
import model.RDFModel;

/**
 * Benchmark Jena vs SWKM RDF load times across bundled samples.
 *
 * Run with:
 * ./gradlew test --tests "*BackendLoadBenchmarkTest" \
 *   -Dstarlion.benchmark.warmup=1 -Dstarlion.benchmark.iterations=3
 *
 * @author jakoum
 */
public class BackendLoadBenchmarkTest {

    private static final String[] JENA_FORMATS = new String[]{"RDF/XML", "TURTLE", "TRIG", "N3", "N-TRIPLE"};

    @Test
    public void benchmarkJenaVsSwkmAcrossSampleFiles() {
        File sampleDir = new File("SampleRDFFiles");
        File[] files = sampleDir.listFiles((dir, name) -> name.endsWith(".rdf") || name.endsWith(".rdfs"));
        assertTrue("No sample RDF files found under SampleRDFFiles", files != null && files.length > 0);

        Arrays.sort(files);

        int warmupIterations = Integer.getInteger("starlion.benchmark.warmup", 1);
        int measureIterations = Integer.getInteger("starlion.benchmark.iterations", 3);
        if (warmupIterations < 0) {
            warmupIterations = 0;
        }
        if (measureIterations < 1) {
            measureIterations = 1;
        }

        JenaCanonicalAdapter jenaAdapter = new JenaCanonicalAdapter();
        List<String> failures = new ArrayList<String>();
        List<String> rows = new ArrayList<String>();

        long totalJenaMs = 0L;
        long totalSwkmMs = 0L;
        int supportedCount = 0;
        int unsupportedCount = 0;

        for (File file : files) {
            String path = file.getPath();
            String baseUri = path + "#";

            try {
                boolean jenaSupported = true;
                String unsupportedReason = "";

                for (int i = 0; i < warmupIterations; i++) {
                    ParseTiming warmupJena = benchmarkJenaParse(jenaAdapter, path, baseUri);
                    benchmarkSwkmRead(path, baseUri);
                    if (!warmupJena.supported) {
                        jenaSupported = false;
                        unsupportedReason = warmupJena.error;
                        break;
                    }
                }

                if (!jenaSupported) {
                    unsupportedCount++;
                    rows.add(file.getName() + " | UNSUPPORTED_BY_JENA | reason=" + unsupportedReason);
                    continue;
                }

                long jenaSum = 0L;
                long swkmSum = 0L;
                boolean supportedInMeasured = true;
                for (int i = 0; i < measureIterations; i++) {
                    ParseTiming jenaTiming = benchmarkJenaParse(jenaAdapter, path, baseUri);
                    if (!jenaTiming.supported) {
                        supportedInMeasured = false;
                        unsupportedReason = jenaTiming.error;
                        break;
                    }
                    long swkmMs = benchmarkSwkmRead(path, baseUri);
                    jenaSum += jenaTiming.elapsedMillis;
                    swkmSum += swkmMs;
                }

                if (!supportedInMeasured) {
                    unsupportedCount++;
                    rows.add(file.getName() + " | UNSUPPORTED_BY_JENA | reason=" + unsupportedReason);
                    continue;
                }

                long jenaAvg = jenaSum / measureIterations;
                long swkmAvg = swkmSum / measureIterations;
                double speedup = jenaAvg == 0 ? Double.POSITIVE_INFINITY : (double) swkmAvg / (double) jenaAvg;

                supportedCount++;
                totalJenaMs += jenaAvg;
                totalSwkmMs += swkmAvg;

                rows.add(file.getName()
                        + " | jena_ms=" + jenaAvg
                        + " | swkm_ms=" + swkmAvg
                        + " | speedup(swkm/jena)=" + formatDouble(speedup));
            } catch (Exception ex) {
                failures.add(file.getName() + " -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }

        System.out.println("=== Backend Load Benchmark (Jena vs SWKM) ===");
        System.out.println("warmup_iterations=" + warmupIterations + ", measure_iterations=" + measureIterations);
        for (String row : rows) {
            System.out.println(row);
        }

        if (supportedCount > 0) {
            double avgJena = (double) totalJenaMs / supportedCount;
            double avgSwkm = (double) totalSwkmMs / supportedCount;
            double avgSpeedup = avgJena == 0.0 ? Double.POSITIVE_INFINITY : avgSwkm / avgJena;
            System.out.println("SUMMARY supported_files=" + supportedCount
                    + ", unsupported_files=" + unsupportedCount
                    + ", avg_jena_ms=" + formatDouble(avgJena)
                    + ", avg_swkm_ms=" + formatDouble(avgSwkm)
                    + ", avg_speedup(swkm/jena)=" + formatDouble(avgSpeedup));
        } else {
            System.out.println("SUMMARY supported_files=0, unsupported_files=" + unsupportedCount);
        }

        assertTrue("Benchmark hard failures found:\n" + String.join("\n", failures), failures.isEmpty());
    }

    private ParseTiming benchmarkJenaParse(JenaCanonicalAdapter jenaAdapter, String path, String baseUri) {
        long start = System.nanoTime();
        RuntimeException last = null;
        for (String format : JENA_FORMATS) {
            try {
                jenaAdapter.fromFile(path, baseUri, format);
                long elapsed = (System.nanoTime() - start) / 1_000_000L;
                return new ParseTiming(true, elapsed, "");
            } catch (RuntimeException ex) {
                last = ex;
            }
        }
        long elapsed = (System.nanoTime() - start) / 1_000_000L;
        return new ParseTiming(false, elapsed, normalizeError(last));
    }

    private long benchmarkSwkmRead(String path, String baseUri) {
        RDFModel swkmModel = new RDFModel();
        long start = System.nanoTime();
        boolean readOk = swkmModel.read(path, baseUri, RDF_Model.RDF_XML, true);
        long elapsed = (System.nanoTime() - start) / 1_000_000L;
        if (!readOk) {
            throw new IllegalStateException("SWKM read failed");
        }
        return elapsed;
    }

    private String formatDouble(double value) {
        if (Double.isInfinite(value)) {
            return "INF";
        }
        if (Double.isNaN(value)) {
            return "NaN";
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String normalizeError(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        String message = String.valueOf(throwable.getMessage()).replace('\n', ' ').replace('\r', ' ').trim();
        if (message.length() > 120) {
            return message.substring(0, 120) + "...";
        }
        return message;
    }

    private static final class ParseTiming {
        private final boolean supported;
        private final long elapsedMillis;
        private final String error;

        private ParseTiming(boolean supported, long elapsedMillis, String error) {
            this.supported = supported;
            this.elapsedMillis = elapsedMillis;
            this.error = error;
        }
    }
}
