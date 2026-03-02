package model;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import graphs.ConnectivityInfo;
import graphs.Edge;
import graphs.Graph;
import graphs.InstanceCache;
import graphs.InstanceRecord;
import graphs.LayoutMetrics2;
import graphs.Node;
import graphs.Ranker;
import graphs.save.GraphState;
import gui.Project.DEPENDENCIES;

/**
 * Model class for graph data, separating data representation from visual representation.
 * This class contains only data structure and algorithms, with no UI dependencies.
 * 
 * @author jakoum
 */
public class GraphModel {
    
    private Graph graph; // Temporary reference to the original Graph until full refactoring
    
    private Hashtable<String, NodeModel> nodes = new Hashtable<>();
    private Hashtable<String, EdgeModel> edges = new Hashtable<>();
    private ArrayList<String> namespaces = new ArrayList<>();
    private InstanceCache instanceCache;
    private Ranker ranker;
    private LayoutMetrics2 layoutMetrics;
    
    /**
     * Constructor that creates a new GraphModel
     */
    public GraphModel() {
        // In the future, this will create a new graph model without depending on Graph
        // For now, we'll use the existing Graph class
        this.graph = new Graph();
        this.instanceCache = new InstanceCache();
        this.ranker = new Ranker(this);
        this.layoutMetrics = new LayoutMetrics2();
    }
    
    /**
     * Constructor that initializes with an existing Graph
     * This is a temporary constructor for the transition period
     */
    public GraphModel(Graph graph) {
        this.graph = graph;
        // In the future, we'll extract data from the graph and populate our own data structures
    }
    
    /**
     * Get the underlying Graph object
     * This is a temporary method for the transition period
     * @return the Graph object
     */
    public Graph getGraph() {
        return graph;
    }
    
    /**
     * Get the list of nodes in the graph
     * @return a hashtable of nodes
     */
    public Hashtable<String, NodeModel> getNodes() {
        // For now, we'll convert from the original Graph's nodes
        Hashtable<String, NodeModel> result = new Hashtable<>();
        Hashtable<String, Node> originalNodes = graph.getNodeList();
        
        for (String key : originalNodes.keySet()) {
            Node node = originalNodes.get(key);
            NodeModel nodeModel = new NodeModel(node);
            result.put(key, nodeModel);
        }
        
        return result;
    }
    
    /**
     * Get the list of edges in the graph
     * @return a hashtable of edges
     */
    public Hashtable<String, EdgeModel> getEdges() {
        // For now, we'll convert from the original Graph's edges
        Hashtable<String, EdgeModel> result = new Hashtable<>();
        Hashtable<String, Edge> originalEdges = graph.getEdgeList();
        
        for (String key : originalEdges.keySet()) {
            Edge edge = originalEdges.get(key);
            EdgeModel edgeModel = new EdgeModel(edge);
            result.put(key, edgeModel);
        }
        
        return result;
    }
    
    /**
     * Get the state of the graph
     * @return the graph state
     */
    public GraphState getState() {
        return graph.getState();
    }
    
    /**
     * Get the graph namespaces
     * @return the graph namespaces
     */
    public ArrayList<String> getNamespaces() {
        return graph.getGraphNamespaces();
    }
    
    /**
     * Populate the graph with data from an RDF model
     * @param model the RDF model to populate from
     */
    public void populateGraph(RDFModel model) {
        graph.populateGraph(model);
    }
    
    /**
     * Populate the graph from an input stream
     * @param inputStream the input stream containing graph data
     * @param streamURI the URI of the stream
     * @throws IOException if an I/O error occurs
     */
    public void populateGraph(InputStream inputStream, String streamURI) throws IOException {
        graph.populateGraph(inputStream, streamURI);
    }
    
    /**
     * Get the ranker for the graph
     * @return the graph ranker
     */
    public Ranker getRanker() {
        return graph.getGraphRanker();
    }
    
    /**
     * Rank the graph using the specified method
     * @param method the ranking method
     * @param params parameters for the ranking method
     * @param namespace_uri the namespace URI
     * @return a hashtable of connectivity info
     */
    public Hashtable<String, ConnectivityInfo> rank(String method, String params, String namespace_uri) {
        return graph.rank(method, params, namespace_uri);
    }
    
    /**
     * Get the top K nodes
     * @param params parameters for the top K algorithm
     * @param centerOfWindow the center point of the window
     * @return a list of top K nodes
     */
    public ArrayList<String> topKNodes(String params, Point centerOfWindow) {
        return graph.topKNodes(params, centerOfWindow);
    }
    
    /**
     * Get the top K groups
     * @param params parameters for the top K groups algorithm
     * @return a list of top K groups
     */
    public ArrayList<String> topKGroups(String params) {
        return graph.topKGroups(params);
    }
    
    /**
     * Add an instance and its neighbors to the graph
     * @param instanceName the name of the instance
     * @param client the client to use
     * @return the instance record
     */
    public InstanceRecord addInstanceAndNeighbours(String instanceName, gr.forth.ics.swkmclient.Client client) {
        return graph.addInstanceAndNeighbours(instanceName, client);
    }
    
    /**
     * Get the instance cache
     * @return the instance cache
     */
    public InstanceCache getInstanceCache() {
        return graph.getInstanceCache();
    }
    
    /**
     * Set the dependency load type
     * @param dType the dependency type
     */
    public void setDependencyLoad(DEPENDENCIES dType) {
        graph.setDependencyLoad(dType);
    }
    
    /**
     * Set the graph namespaces
     * @param nspaces the namespaces
     */
    public void setNamespaces(String[] nspaces) {
        graph.setGraphNameSpaces(nspaces);
    }
    
    /**
     * Get the layout metrics
     * @return the layout metrics
     */
    public LayoutMetrics2 getLayoutMetrics() {
        return graph.getLayoutMetrics2();
    }
    
    /**
     * Save the graph to a file
     * @param fileName the name of the file to save to
     */
    public void saveGraph(String fileName) {
        graph.saveGraph(fileName);
    }
    
    /**
     * Restore the graph from a file
     * @param model the RDF model to restore to
     * @param fileName the name of the file to restore from
     */
    public void restoreGraph(RDFModel model, String fileName) {
        graph.restoreGraph(model, fileName);
    }
}