package model;

import java.util.ArrayList;
import java.util.Collection;

import graphs.Edge;

/**
 * Model class for edge data, separating data representation from visual representation.
 * This class contains only data structure, with no UI dependencies.
 * 
 * @author jakoum
 */
public class EdgeModel {

    private String label;
    private NodeModel source;
    private NodeModel target;
    private ObjectType edgeType;
    private boolean directed;
    private ArrayList<String> subProperties = new ArrayList<>();

    private int x;
    private int y;
    private boolean nailed;
    private boolean visible;
    private boolean labelVisible;

    /**
     * Constructor that creates a new EdgeModel
     */
    public EdgeModel(String label, NodeModel source, NodeModel target, ObjectType edgeType, boolean directed) {
        this.label = label;
        this.source = source;
        this.target = target;
        this.edgeType = edgeType;
        this.directed = directed;
        this.nailed = false;
        this.visible = true;
        this.labelVisible = true;
    }

    /**
     * Constructor that initializes with an existing Edge
     * This is a temporary constructor for the transition period
     * 
     * @author jakoum
     */
    public EdgeModel(Edge edge) {
        // Get the label from the edge
        this.label = edge.toString();

        // Get the source and target nodes
        // Since we can't directly convert Node to NodeModel, we'll create temporary NodeModel objects
        if (edge.getSourceNode() != null) {
            this.source = new NodeModel(edge.getSourceNode());
        } else {
            this.source = null;
        }

        if (edge.getTargetNode() != null) {
            this.target = new NodeModel(edge.getTargetNode());
        } else {
            this.target = null;
        }

        // Since we can't access the RDF object type directly, we'll infer it from the edge's behavior
        // For now, we'll use a default value
        this.edgeType = ObjectType.PROPERTY;

        // Since we can't access the directed property directly, we'll use a default value
        this.directed = true;

        // Since we can't access the coordinates directly, we'll use default values
        this.x = 0;
        this.y = 0;

        // Since we can't access the nailed and visible properties directly, we'll use default values
        this.nailed = false;
        this.visible = true;
        this.labelVisible = true;

        // Get the sub-properties
        if (edge.getSubProperties() != null) {
            this.subProperties.addAll(edge.getSubProperties());
        }
    }

    /**
     * Get the label of the edge
     * @return the edge label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the label of the edge
     * @param label the edge label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Get the source node of the edge
     * @return the source node
     */
    public NodeModel getSource() {
        return source;
    }

    /**
     * Set the source node of the edge
     * @param source the source node
     */
    public void setSource(NodeModel source) {
        this.source = source;
    }

    /**
     * Get the target node of the edge
     * @return the target node
     */
    public NodeModel getTarget() {
        return target;
    }

    /**
     * Set the target node of the edge
     * @param target the target node
     */
    public void setTarget(NodeModel target) {
        this.target = target;
    }

    /**
     * Get the type of the edge
     * @return the edge type
     */
    public ObjectType getType() {
        return edgeType;
    }

    /**
     * Set the type of the edge
     * @param edgeType the edge type
     */
    public void setType(ObjectType edgeType) {
        this.edgeType = edgeType;
    }

    /**
     * Check if the edge is directed
     * @return true if the edge is directed
     */
    public boolean isDirected() {
        return directed;
    }

    /**
     * Set whether the edge is directed
     * @param directed true if the edge should be directed
     */
    public void setDirected(boolean directed) {
        this.directed = directed;
    }

    /**
     * Get the X coordinate of the edge
     * @return the X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Set the X coordinate of the edge
     * @param x the X coordinate
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Get the Y coordinate of the edge
     * @return the Y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Set the Y coordinate of the edge
     * @param y the Y coordinate
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Check if the edge is nailed
     * @return true if the edge is nailed
     */
    public boolean isNailed() {
        return nailed;
    }

    /**
     * Set whether the edge is nailed
     * @param nailed true if the edge should be nailed
     */
    public void setNailed(boolean nailed) {
        this.nailed = nailed;
    }

    /**
     * Check if the edge is visible
     * @return true if the edge is visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set whether the edge is visible
     * @param visible true if the edge should be visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Check if the edge label is visible
     * @return true if the edge label is visible
     */
    public boolean isLabelVisible() {
        return labelVisible;
    }

    /**
     * Set whether the edge label is visible
     * @param labelVisible true if the edge label should be visible
     */
    public void setLabelVisible(boolean labelVisible) {
        this.labelVisible = labelVisible;
    }

    /**
     * Get the sub-properties of the edge
     * @return a list of sub-properties
     */
    public ArrayList<String> getSubProperties() {
        return subProperties;
    }

    /**
     * Set the sub-properties of the edge
     * @param subProperties a list of sub-properties
     */
    public void setSubProperties(Collection<String> subProperties) {
        this.subProperties.clear();
        this.subProperties.addAll(subProperties);
    }

    /**
     * Add a sub-property to the edge
     * @param subProperty the sub-property to add
     */
    public void addSubProperty(String subProperty) {
        this.subProperties.add(subProperty);
    }

    /**
     * Remove a sub-property from the edge
     * @param subProperty the sub-property to remove
     */
    public void removeSubProperty(String subProperty) {
        this.subProperties.remove(subProperty);
    }

    /**
     * Convert to string representation
     * @return a string representation of the edge
     */
    @Override
    public String toString() {
        return label;
    }
}
