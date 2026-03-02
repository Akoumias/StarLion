package mapping.canonical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Canonical edge representation independent from RDF backend implementation.
 *
 * @author jakoum
 */
public class CanonicalEdge {

    private final String id;
    private final String label;
    private final String sourceId;
    private final String targetId;
    private final CanonicalElementType type;
    private final boolean directed;
    private final boolean visible;
    private final List<String> subProperties;

    public CanonicalEdge(
            String id,
            String label,
            String sourceId,
            String targetId,
            CanonicalElementType type,
            boolean directed,
            boolean visible,
            List<String> subProperties
    ) {
        this.id = id;
        this.label = label;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
        this.directed = directed;
        this.visible = visible;
        this.subProperties = new ArrayList<>(subProperties);
        Collections.sort(this.subProperties);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public CanonicalElementType getType() {
        return type;
    }

    public boolean isDirected() {
        return directed;
    }

    public boolean isVisible() {
        return visible;
    }

    public List<String> getSubProperties() {
        return Collections.unmodifiableList(subProperties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CanonicalEdge)) {
            return false;
        }
        CanonicalEdge that = (CanonicalEdge) o;
        return directed == that.directed
                && visible == that.visible
                && Objects.equals(id, that.id)
                && Objects.equals(label, that.label)
                && Objects.equals(sourceId, that.sourceId)
                && Objects.equals(targetId, that.targetId)
                && type == that.type
                && Objects.equals(subProperties, that.subProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, sourceId, targetId, type, directed, visible, subProperties);
    }
}
