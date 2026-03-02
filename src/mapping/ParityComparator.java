package mapping;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mapping.canonical.CanonicalEdge;
import mapping.canonical.CanonicalGraphSnapshot;
import mapping.canonical.CanonicalNode;
import mapping.canonical.CanonicalRdfSnapshot;

/**
 * Deterministic comparator for canonical SWKM/Jena outputs.
 *
 * @author jakoum
 */
public class ParityComparator {

    /**
     * Compares canonical RDF snapshots as unordered sets per logical field.
     */
    public ParityReport compareRdf(CanonicalRdfSnapshot swkm, CanonicalRdfSnapshot jena) {
        ParityReport report = new ParityReport("rdf");
        compareStringLists(
                "namespaces",
                filterNamespaces(swkm.getNamespaces()),
                filterNamespaces(jena.getNamespaces()),
                report
        );
        compareStringLists("classes", swkm.getClasses(), jena.getClasses(), report);
        compareStringLists("properties", swkm.getProperties(), jena.getProperties(), report);
        compareStringLists("instances", swkm.getInstances(), jena.getInstances(), report);
        compareStringLists(
                "propertyInstances",
                filterSchemaPropertyInstances(swkm.getPropertyInstances()),
                filterSchemaPropertyInstances(jena.getPropertyInstances()),
                report
        );
        return report;
    }

    /**
     * Compares canonical graph snapshots (namespaces, nodes, edges).
     */
    public ParityReport compareGraph(CanonicalGraphSnapshot swkm, CanonicalGraphSnapshot jena) {
        ParityReport report = new ParityReport("graph");

        compareStringLists(
                "graph.namespaces",
                filterNamespaces(swkm.getNamespaces()),
                filterNamespaces(jena.getNamespaces()),
                report
        );
        compareStringLists("graph.nodes", nodeKeys(swkm.getNodes()), nodeKeys(jena.getNodes()), report);
        compareStringLists("graph.edges", edgeKeys(swkm.getEdges()), edgeKeys(jena.getEdges()), report);
        return report;
    }

    /**
     * Computes set differences and records deterministic mismatch messages.
     */
    private void compareStringLists(String field, List<String> left, List<String> right, ParityReport report) {
        Set<String> leftSet = new HashSet<>(left);
        Set<String> rightSet = new HashSet<>(right);

        Set<String> missingInJena = new HashSet<>(leftSet);
        missingInJena.removeAll(rightSet);
        for (String key : missingInJena) {
            report.addMismatch(field + " missing_in_jena: " + key);
        }

        Set<String> extraInJena = new HashSet<>(rightSet);
        extraInJena.removeAll(leftSet);
        for (String key : extraInJena) {
            report.addMismatch(field + " extra_in_jena: " + key);
        }
    }

    private List<String> nodeKeys(List<CanonicalNode> nodes) {
        java.util.ArrayList<String> keys = new java.util.ArrayList<>();
        for (CanonicalNode node : nodes) {
            keys.add(node.getId() + "|" + node.getName() + "|" + node.getUri() + "|" + node.getType());
        }
        return keys;
    }

    private List<String> edgeKeys(List<CanonicalEdge> edges) {
        java.util.ArrayList<String> keys = new java.util.ArrayList<>();
        for (CanonicalEdge edge : edges) {
            keys.add(edge.getLabel()
                    + "|" + edge.getSourceId()
                    + "|" + edge.getTargetId()
                    + "|" + edge.getType()
                    + "|" + edge.isDirected()
                    + "|" + edge.getSubProperties());
        }
        return keys;
    }

    /**
     * Removes schema triples reconstructed by adapters to avoid false diffs.
     */
    private List<String> filterSchemaPropertyInstances(List<String> source) {
        List<String> filtered = new java.util.ArrayList<>();
        for (String item : source) {
            String[] parts = item.split("\\|", 3);
            if (parts.length < 2) {
                filtered.add(item);
                continue;
            }
            String predicate = parts[1];
            if ("http://www.w3.org/2000/01/rdf-schema#subClassOf".equals(predicate)
                    || "http://www.w3.org/2000/01/rdf-schema#domain".equals(predicate)
                    || "http://www.w3.org/2000/01/rdf-schema#range".equals(predicate)
                    || "http://www.w3.org/2000/01/rdf-schema#subPropertyOf".equals(predicate)) {
                continue;
            }
            filtered.add(item);
        }
        return filtered;
    }

    /**
     * Filters synthetic/local namespaces that are environment-specific noise.
     */
    private List<String> filterNamespaces(List<String> namespaces) {
        List<String> filtered = new java.util.ArrayList<>();
        for (String ns : namespaces) {
            if (ns == null) {
                continue;
            }
            if ("SampleRDFFiles/".equals(ns)) {
                continue;
            }
            filtered.add(ns);
        }
        return filtered;
    }
}
