package mapping;

import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import graphs.Graph;
import gr.forth.ics.rdfsuite.swkm.model.impl.RDF_Model;
import mapping.adapter.JenaCanonicalAdapter;
import mapping.canonical.CanonicalGraphSnapshot;
import mapping.canonical.CanonicalRdfSnapshot;
import model.RDFModel;
import service.RDFService;

/**
 * Smoke test all bundled sample files through current visualization strategy:
 * Jena-first graph snapshot path with SWKM fallback.
 *
 * @author jakoum
 */
public class VisualizationSamplesSmokeTest {

    @Test
    public void allBundledSamplesCanBuildGraphForVisualization() {
        File sampleDir = new File("SampleRDFFiles");
        File[] files = sampleDir.listFiles((dir, name) -> {
            String normalized = name.toLowerCase();
            return normalized.endsWith(".rdf")
                    || normalized.endsWith(".rdfs")
                    || normalized.endsWith(".ttl")
                    || normalized.endsWith(".owl");
        });
        assertTrue("No sample RDF/TTL/OWL files found under SampleRDFFiles", files != null && files.length > 0);

        Arrays.sort(files);
        List<String> failures = new ArrayList<String>();
        List<String> outcomes = new ArrayList<String>();

        for (File file : files) {
            String path = file.getPath();
            String baseUri = path + "#";

            try {
                RDFService rdfService = new RDFService();
                rdfService.setBackendMode(GraphBackendMode.JENA);
                boolean readOk = rdfService.read(path, baseUri, RDF_Model.RDF_XML, true);
                if (!readOk) {
                    failures.add(file.getName() + " -> read failed in JENA mode");
                    continue;
                }

                Graph graph = new Graph();
                GraphBackendMode effective = rdfService.getLastEffectiveBackend();
                if (effective == GraphBackendMode.JENA) {
                    JenaCanonicalAdapter jenaAdapter = new JenaCanonicalAdapter();
                    CanonicalRdfSnapshot rdfSnapshot = rdfService.getCanonicalSnapshot();
                    CanonicalGraphSnapshot graphSnapshot = jenaAdapter.toGraphSnapshot(rdfSnapshot);
                    graph.populateGraph(graphSnapshot);
                    outcomes.add(file.getName() + " -> JENA_PATH");
                } else {
                    RDFModel swkmModel = rdfService.getRDFModel();
                    graph.setGraphNameSpaces(swkmModel.getNamespaces());
                    graph.populateGraph(swkmModel);
                    outcomes.add(file.getName() + " -> SWKM_FALLBACK");
                }

                // Basic visualization smoke path (same core operations used by UI flow)
                graph.createGraphRanker();
                graph.visualizeGraphElements();
                graph.updateLayout("random", null, new Point(500, 350));
            } catch (Exception ex) {
                failures.add(file.getName() + " -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }

        System.out.println("Visualization smoke outcomes:");
        for (String outcome : outcomes) {
            System.out.println("  - " + outcome);
        }

        assertTrue("Visualization smoke failures:\n" + String.join("\n", failures), failures.isEmpty());
    }
}
