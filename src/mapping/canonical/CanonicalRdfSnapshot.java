package mapping.canonical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Canonical snapshot of RDF-level data.
 *
 * @author jakoum
 */
public class CanonicalRdfSnapshot {

    private final List<String> namespaces;
    private final List<String> classes;
    private final List<String> properties;
    private final List<String> instances;
    private final List<String> propertyInstances;

    public CanonicalRdfSnapshot(
            List<String> namespaces,
            List<String> classes,
            List<String> properties,
            List<String> instances,
            List<String> propertyInstances
    ) {
        this.namespaces = sortedCopy(namespaces);
        this.classes = sortedCopy(classes);
        this.properties = sortedCopy(properties);
        this.instances = sortedCopy(instances);
        this.propertyInstances = sortedCopy(propertyInstances);
    }

    private static List<String> sortedCopy(List<String> source) {
        List<String> copy = new ArrayList<>(source);
        Collections.sort(copy);
        return copy;
    }

    public List<String> getNamespaces() {
        return Collections.unmodifiableList(namespaces);
    }

    public List<String> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    public List<String> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    public List<String> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    public List<String> getPropertyInstances() {
        return Collections.unmodifiableList(propertyInstances);
    }
}
