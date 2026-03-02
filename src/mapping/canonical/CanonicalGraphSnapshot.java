package mapping.canonical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Canonical snapshot of graph data.
 *
 * @author jakoum
 */
public class CanonicalGraphSnapshot {

    private final List<CanonicalNode> nodes;
    private final List<CanonicalEdge> edges;
    private final List<String> namespaces;

    public CanonicalGraphSnapshot(List<CanonicalNode> nodes, List<CanonicalEdge> edges, List<String> namespaces) {
        this.nodes = new ArrayList<>(nodes);
        this.edges = new ArrayList<>(edges);
        this.namespaces = new ArrayList<>(namespaces);
        Collections.sort(this.namespaces);
    }

    public List<CanonicalNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<CanonicalEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public List<String> getNamespaces() {
        return Collections.unmodifiableList(namespaces);
    }
}
