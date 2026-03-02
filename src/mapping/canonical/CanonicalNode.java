package mapping.canonical;

import java.util.Objects;

/**
 * Canonical node representation independent from RDF backend implementation.
 *
 * @author jakoum
 */
public class CanonicalNode {

    private final String id;
    private final String name;
    private final String uri;
    private final String namespace;
    private final CanonicalElementType type;
    private final boolean visible;
    private final boolean nailed;

    public CanonicalNode(
            String id,
            String name,
            String uri,
            String namespace,
            CanonicalElementType type,
            boolean visible,
            boolean nailed
    ) {
        this.id = id;
        this.name = name;
        this.uri = uri;
        this.namespace = namespace;
        this.type = type;
        this.visible = visible;
        this.nailed = nailed;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    public String getNamespace() {
        return namespace;
    }

    public CanonicalElementType getType() {
        return type;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isNailed() {
        return nailed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CanonicalNode)) {
            return false;
        }
        CanonicalNode that = (CanonicalNode) o;
        return visible == that.visible
                && nailed == that.nailed
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(uri, that.uri)
                && Objects.equals(namespace, that.namespace)
                && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, uri, namespace, type, visible, nailed);
    }
}
