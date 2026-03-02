package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import graphs.Edge;
import graphs.Node;

/**
 * Model class for node data, separating data representation from visual representation.
 * This class contains only data structure, with no UI dependencies.
 * 
 * @author jakoum
 */
public class NodeModel {

    private String name;
    private String uri;
    private String namespace;
    private ObjectType nodeType;
    private RDFResource resource;

    private int x;
    private int y;
    private int z;
    private int width;
    private int height;

    private boolean nailed;
    private boolean visible;

    private LinkedList<Edge> edgesFrom = new LinkedList<>();
    private LinkedList<Edge> edgesTo = new LinkedList<>();

    /**
     * Constructor that creates a new NodeModel
     */
    public NodeModel(String name, String uri, String namespace, ObjectType nodeType, RDFResource resource, 
                    int x, int y, int z, int width, int height) {
        this.name = name;
        this.uri = uri;
        this.namespace = namespace;
        this.nodeType = nodeType;
        this.resource = resource;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.nailed = false;
        this.visible = true;
    }

    /**
     * Constructor that initializes with an existing Node
     * This is a temporary constructor for the transition period
     */
    public NodeModel(Node node) {
        this.name = node.getName();
        this.uri = node.getNodeUri();
        this.namespace = node.getNodeNamespace();

        // Map the RDF object type to our ObjectType enum
        // Since we can't access the type directly, we'll use a default value
        this.nodeType = ObjectType.CLASS; // Default value

        // Since we can't access these properties directly, we'll use default values
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.width = 100;
        this.height = 30;

        this.nailed = node.isNailed();
        this.visible = node.isVisible();

        // For now, we'll keep references to the original edges
        this.edgesFrom = node.getEdgesFrom();
        this.edgesTo = node.getEdgesTo();
    }

    /**
     * Get the name of the node
     * @return the node name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the URI of the node
     * @return the node URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Get the namespace of the node
     * @return the node namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Get the type of the node
     * @return the node type
     */
    public ObjectType getType() {
        return nodeType;
    }

    /**
     * Get the RDF resource associated with the node
     * @return the RDF resource
     */
    public RDFResource getResource() {
        return resource;
    }

    /**
     * Get the X coordinate of the node
     * @return the X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Set the X coordinate of the node
     * @param x the X coordinate
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Get the Y coordinate of the node
     * @return the Y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Set the Y coordinate of the node
     * @param y the Y coordinate
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Get the Z coordinate of the node
     * @return the Z coordinate
     */
    public int getZ() {
        return z;
    }

    /**
     * Set the Z coordinate of the node
     * @param z the Z coordinate
     */
    public void setZ(int z) {
        this.z = z;
    }

    /**
     * Get the width of the node
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Set the width of the node
     * @param width the width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Get the height of the node
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Set the height of the node
     * @param height the height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Check if the node is nailed
     * @return true if the node is nailed
     */
    public boolean isNailed() {
        return nailed;
    }

    /**
     * Set whether the node is nailed
     * @param nailed true if the node should be nailed
     */
    public void setNailed(boolean nailed) {
        this.nailed = nailed;
    }

    /**
     * Check if the node is visible
     * @return true if the node is visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set whether the node is visible
     * @param visible true if the node should be visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Get the edges from this node
     * @return a list of edges
     */
    public LinkedList<Edge> getEdgesFrom() {
        return edgesFrom;
    }

    /**
     * Get the edges to this node
     * @return a list of edges
     */
    public LinkedList<Edge> getEdgesTo() {
        return edgesTo;
    }

    /**
     * Add an edge from this node
     * @param edge the edge to add
     */
    public void addEdgeFrom(Edge edge) {
        edgesFrom.add(edge);
    }

    /**
     * Remove an edge from this node
     * @param edge the edge to remove
     */
    public void removeEdgeFrom(Edge edge) {
        edgesFrom.remove(edge);
    }

    /**
     * Add an edge to this node
     * @param edge the edge to add
     */
    public void addEdgeTo(Edge edge) {
        edgesTo.add(edge);
    }

    /**
     * Remove an edge to this node
     * @param edge the edge to remove
     */
    public void removeEdgeTo(Edge edge) {
        edgesTo.remove(edge);
    }

    /**
     * Get all edges connected to this node
     * @return a list of all edges
     */
    public LinkedList<Edge> getAllEdges() {
        LinkedList<Edge> allEdges = new LinkedList<>();
        allEdges.addAll(edgesFrom);
        allEdges.addAll(edgesTo);
        return allEdges;
    }

    /**
     * Get the total number of edges connected to this node
     * @return the total number of edges
     */
    public int getTotalEdges() {
        return edgesFrom.size() + edgesTo.size();
    }

    /**
     * Check if this node is an instance
     * @return true if the node is an instance
     */
    public boolean isInstance() {
        return nodeType == ObjectType.CLASS_INSTANCE;
    }

    /**
     * Convert to string representation
     * @return a string representation of the node
     */
    @Override
    public String toString() {
        return name;
    }
}
