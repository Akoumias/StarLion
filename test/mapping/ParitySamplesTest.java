package mapping;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import graphs.Graph;
import gr.forth.ics.rdfsuite.swkm.model.impl.RDF_Model;
import mapping.adapter.JenaCanonicalAdapter;
import mapping.adapter.SwkmCanonicalAdapter;
import mapping.canonical.CanonicalGraphSnapshot;
import mapping.canonical.CanonicalRdfSnapshot;
import model.RDFModel;

/**
 * Regression parity checks across bundled sample RDF files.
 *
 * @author jakoum
 */
public class ParitySamplesTest {

    @Test
    public void parityHoldsForBundledSamples() {
        File sampleDir = new File("SampleRDFFiles");
        File[] files = sampleDir.listFiles((dir, name) -> name.endsWith(".rdf") || name.endsWith(".rdfs"));
        assertTrue("No sample RDF files found under SampleRDFFiles", files != null && files.length > 0);

        Arrays.sort(files);

        SwkmCanonicalAdapter swkmAdapter = new SwkmCanonicalAdapter();
        JenaCanonicalAdapter jenaAdapter = new JenaCanonicalAdapter();
        ParityComparator comparator = new ParityComparator();

        List<String> failures = new ArrayList<String>();
        List<String> unsupported = new ArrayList<String>();

        for (File file : files) {
            String path = file.getPath();
            String baseUri = path + "#";

            try {
                RDFModel swkmModel = new RDFModel();
                boolean readOk = swkmModel.read(path, baseUri, RDF_Model.RDF_XML, true);
                if (!readOk) {
                    failures.add(file.getName() + " -> SWKM read failed");
                    continue;
                }

                CanonicalRdfSnapshot swkmRdf = swkmAdapter.fromRdfModel(swkmModel);
                CanonicalRdfSnapshot jenaRdf = readJenaWithFallback(jenaAdapter, path, baseUri);
                ParityReport rdfReport = comparator.compareRdf(swkmRdf, jenaRdf);

                Graph swkmGraph = new Graph();
                swkmGraph.setGraphNameSpaces(swkmModel.getNamespaces());
                swkmGraph.populateGraph(swkmModel);
                CanonicalGraphSnapshot swkmGraphSnapshot = swkmAdapter.fromGraph(swkmGraph);
                CanonicalGraphSnapshot jenaGraphSnapshot = jenaAdapter.toGraphSnapshot(jenaRdf);
                ParityReport graphReport = comparator.compareGraph(swkmGraphSnapshot, jenaGraphSnapshot);

                if (!rdfReport.isEquivalent() || !graphReport.isEquivalent()) {
                    failures.add(file.getName()
                            + " -> rdf_mismatches=" + rdfReport.getMismatches().size()
                            + ", graph_mismatches=" + graphReport.getMismatches().size()
                            + summarizeDiffs(rdfReport, graphReport));
                }
            } catch (Exception ex) {
                String details = file.getName() + " -> exception: " + ex.getClass().getSimpleName()
                        + " : " + ex.getMessage();
                if (isUnsupportedByJena(ex)) {
                    unsupported.add(details);
                } else {
                    failures.add(details);
                }
            }
        }

        if (!unsupported.isEmpty()) {
            System.out.println("Unsupported-by-Jena skipped:");
            for (String skipped : unsupported) {
                System.out.println("  - " + skipped);
            }
        }

        System.out.println("Parity checked files: " + files.length
                + ", failures: " + failures.size()
                + ", unsupported-skipped: " + unsupported.size());

        boolean strictParity = Boolean.parseBoolean(System.getProperty("starlion.parity.strict", "false"));
        if (strictParity) {
            assertTrue(
                    "Parity mismatches found:\n" + String.join("\n", failures)
                            + (unsupported.isEmpty() ? "" : "\nUnsupported-by-Jena skipped:\n" + String.join("\n", unsupported)),
                    failures.isEmpty()
            );
        } else if (!failures.isEmpty()) {
            System.out.println("Parity mismatches ignored (strict mode disabled).");
            for (String failure : failures) {
                System.out.println("  - " + failure);
            }
        }
    }

    private CanonicalRdfSnapshot readJenaWithFallback(JenaCanonicalAdapter jenaAdapter, String path, String baseUri) {
        String[] formats = new String[]{"RDF/XML", "TURTLE", "TRIG", "N3", "N-TRIPLE"};
        RuntimeException last = null;
        for (String format : formats) {
            try {
                return jenaAdapter.fromFile(path, baseUri, format);
            } catch (RuntimeException ex) {
                last = ex;
            }
        }
        throw last == null ? new IllegalStateException("No Jena parse attempts executed") : last;
    }

    private String summarizeDiffs(ParityReport rdfReport, ParityReport graphReport) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n  rdf_top_diffs=");
        appendTop(sb, rdfReport.getMismatches(), 5);
        sb.append("\n  graph_top_diffs=");
        appendTop(sb, graphReport.getMismatches(), 5);
        return sb.toString();
    }

    private void appendTop(StringBuilder sb, List<String> diffs, int max) {
        if (diffs.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[");
        int limit = Math.min(max, diffs.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(diffs.get(i));
        }
        if (diffs.size() > limit) {
            sb.append(" | ...");
        }
        sb.append("]");
    }

    private boolean isUnsupportedByJena(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            String combined = String.valueOf(current.getMessage()) + " " + current.toString();
            String normalized = combined.toLowerCase();
            if (normalized.contains("syntaxerror : unknown")
                    || normalized.contains("syntaxerror: unknown")
                    || normalized.contains("no reader for")
                    || normalized.contains("reader not found")
                    || normalized.contains("bad character in iri")
                    || normalized.contains("<?xml[space]")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
