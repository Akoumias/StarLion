package mapping.adapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import graphs.Edge;
import graphs.Graph;
import graphs.Node;
import mapping.canonical.CanonicalEdge;
import mapping.canonical.CanonicalElementType;
import mapping.canonical.CanonicalGraphSnapshot;
import mapping.canonical.CanonicalNode;
import mapping.canonical.CanonicalRdfSnapshot;
import model.RDFClass;
import model.RDFModel;
import model.RDFNamespace;
import model.RDFProperty;
import model.RDFPropertyInstance;
import model.RDFResource;

/**
 * Maps SWKM-backed wrappers to canonical DTO snapshots.
 *
 * @author jakoum
 */
public class SwkmCanonicalAdapter {

    /**
     * Flattens SWKM RDF model content into canonical RDF lists.
     * This is the SWKM counterpart to Jena canonical extraction.
     */
    public CanonicalRdfSnapshot fromRdfModel(RDFModel model) {
        List<String> namespaces = new ArrayList<>();
        List<String> classes = new ArrayList<>();
        List<String> properties = new ArrayList<>();
        List<String> instances = new ArrayList<>();
        List<String> propertyInstances = new ArrayList<>();

        String[] nsArray = model.getNamespaces();
        if (nsArray != null) {
            for (String nsUri : nsArray) {
                namespaces.add(CanonicalNameUtils.namespace(nsUri));
                RDFNamespace ns = model.getNamespace(nsUri);
                if (ns == null) {
                    continue;
                }
                for (Object classObj : ns.getClasses()) {
                    RDFClass rdfClass = (RDFClass) classObj;
                    classes.add(CanonicalNameUtils.normalizeUri(rdfClass.getURI()));
                }
                for (Object propertyObj : ns.getProperties()) {
                    RDFProperty rdfProperty = (RDFProperty) propertyObj;
                    properties.add(CanonicalNameUtils.normalizeUri(rdfProperty.getURI()));
                }
            }
        }

        for (RDFResource resource : model.getAllInstances()) {
            instances.add(CanonicalNameUtils.normalizeUri(resource.getURI()));
        }

        for (RDFPropertyInstance propertyInstance : model.getAllPropertyInstances()) {
            String subject = CanonicalNameUtils.normalizeUri(propertyInstance.getSubject().getURI());
            String predicate = CanonicalNameUtils.normalizeUri(propertyInstance.getPredicate().getURI());
            Object object = propertyInstance.getObject();
            propertyInstances.add(subject + "|" + predicate + "|" + CanonicalNameUtils.normalizeUri(String.valueOf(object)));
        }

        return new CanonicalRdfSnapshot(namespaces, classes, properties, instances, propertyInstances);
    }

    /**
     * Converts an already built runtime Graph into canonical graph DTOs.
     * Useful for parity checks against Jena-produced graph snapshots.
     */
    public CanonicalGraphSnapshot fromGraph(Graph graph) {
        List<CanonicalNode> nodes = new ArrayList<>();
        List<CanonicalEdge> edges = new ArrayList<>();
        List<String> namespaces = new ArrayList<>();
        for (String ns : graph.getGraphNamespaces()) {
            namespaces.add(CanonicalNameUtils.namespace(ns));
        }

        Hashtable<String, Node> nodeList = graph.getNodeList();
        for (String key : nodeList.keySet()) {
            Node node = nodeList.get(key);
            CanonicalElementType type = node.isInstance()
                    ? CanonicalElementType.CLASS_INSTANCE
                    : CanonicalElementType.CLASS;
            nodes.add(new CanonicalNode(
                    key,
                    node.getName(),
                    CanonicalNameUtils.normalizeUri(node.getNodeUri()),
                    CanonicalNameUtils.namespace(node.getNodeNamespace()),
                    type,
                    node.isVisible(),
                    node.isNailed()
            ));
        }

        Hashtable<String, Edge> edgeList = graph.getEdgeList();
        for (String key : edgeList.keySet()) {
            Edge edge = edgeList.get(key);
            Node source = edge.getSourceNode();
            Node target = edge.getTargetNode();
            CanonicalElementType type = inferEdgeType(edge.toString());

            edges.add(new CanonicalEdge(
                    key,
                    edge.toString(),
                    source == null ? "" : source.getName(),
                    target == null ? "" : target.getName(),
                    type,
                    true,
                    true,
                    edge.getSubProperties() == null ? java.util.Collections.<String>emptyList() : edge.getSubProperties()
            ));
        }

        return new CanonicalGraphSnapshot(nodes, edges, namespaces);
    }

    /**
     * Maps legacy edge labels to canonical semantic edge categories.
     */
    private CanonicalElementType inferEdgeType(String label) {
        if (label == null) {
            return CanonicalElementType.UNKNOWN;
        }
        String normalized = label.trim().toLowerCase();
        if ("isa".equals(normalized)) {
            return CanonicalElementType.SUBCLASS_OF;
        }
        if ("instanceof".equals(normalized)) {
            return CanonicalElementType.INSTANCE_OF;
        }
        return CanonicalElementType.PROPERTY;
    }

    /**
     * Builds a minimal graph snapshot from RDF canonical data.
     * This path currently emits nodes only and keeps edges empty.
     */
    public CanonicalGraphSnapshot fromRdfSnapshot(CanonicalRdfSnapshot rdfSnapshot) {
        Set<String> nodeIds = new HashSet<>();
        List<CanonicalNode> nodes = new ArrayList<>();
        List<CanonicalEdge> edges = new ArrayList<>();

        for (String classUri : rdfSnapshot.getClasses()) {
            String className = CanonicalNameUtils.localName(classUri);
            if (nodeIds.add(className)) {
                nodes.add(new CanonicalNode(
                        className,
                        className,
                        classUri,
                        CanonicalNameUtils.namespace(classUri),
                        CanonicalElementType.CLASS,
                        true,
                        false
                ));
            }
        }
        for (String instanceUri : rdfSnapshot.getInstances()) {
            String instanceName = CanonicalNameUtils.localName(instanceUri);
            if (nodeIds.add(instanceName)) {
                nodes.add(new CanonicalNode(
                        instanceName,
                        instanceName,
                        instanceUri,
                        CanonicalNameUtils.namespace(instanceUri),
                        CanonicalElementType.CLASS_INSTANCE,
                        true,
                        false
                ));
            }
        }

        return new CanonicalGraphSnapshot(nodes, edges, rdfSnapshot.getNamespaces());
    }
}
