package controller;

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
import graphs.Node;
import graphs.Ranker;
import graphs.VisualGraph;
import gui.Project.DEPENDENCIES;
import mapping.canonical.CanonicalGraphSnapshot;
import model.RDFModel;
import model.RDFNamespace;
import service.GraphService;
import service.GraphService.GraphObserver;

/**
 * Controller class for graph operations, mediating between UI components and the GraphService.
 * This class handles UI events related to graphs and updates the UI when graph data changes.
 * 
 * @author jakoum
 */
public class GraphController implements GraphObserver {
    
    private GraphService graphService;
    private Collection<GraphUIObserver> uiObservers = new ArrayList<>();
    
    /**
     * Constructor that initializes with an existing GraphService
     */
    public GraphController(GraphService graphService) {
        this.graphService = graphService;
        this.graphService.registerObserver(this);
    }
    
    /**
     * Constructor that creates a new GraphService
     */
    public GraphController() {
        this.graphService = new GraphService();
        this.graphService.registerObserver(this);
    }
    
    /**
     * Get the underlying GraphService
     * @return the GraphService
     */
    public GraphService getGraphService() {
        return graphService;
    }
    
    /**
     * Populate the graph with data from an RDF model
     * @param model the RDF model to populate from
     */
    public void populateGraph(RDFModel model) {
        graphService.populateGraph(model);
    }

    /**
     * Populate the graph from canonical snapshot data.
     * @param snapshot canonical graph snapshot
     */
    public void populateGraph(CanonicalGraphSnapshot snapshot) {
        graphService.populateGraph(snapshot);
    }
    
    /**
     * Populate the graph from an input stream
     * @param inputStream the input stream containing graph data
     * @param streamURI the URI of the stream
     * @throws IOException if an I/O error occurs
     */
    public void populateGraph(InputStream inputStream, String streamURI) throws IOException {
        graphService.populateGraph(inputStream, streamURI);
    }
    
    /**
     * Update the layout of the graph
     * @param layout the layout algorithm to use
     * @param params parameters for the layout algorithm
     * @param centerOfWindow the center point of the window
     * @return true if the layout was updated successfully
     */
    public boolean updateLayout(String layout, String params, Point centerOfWindow) {
        return graphService.updateLayout(layout, params, centerOfWindow);
    }
    
    /**
     * Save the graph as an image
     * @param fileName the name of the file to save to
     * @return true if the save was successful
     */
    public boolean saveGraphAsImage(String fileName) {
        return graphService.saveGraphAsImage(fileName);
    }
    
    /**
     * Save the graph to a file
     * @param fileName the name of the file to save to
     */
    public void saveGraph(String fileName) {
        graphService.saveGraph(fileName);
    }
    
    /**
     * Restore the graph from a file
     * @param model the RDF model to restore to
     * @param fileName the name of the file to restore from
     */
    public void restoreGraph(RDFModel model, String fileName) {
        graphService.restoreGraph(model, fileName);
    }
    
    /**
     * Rank the graph using the specified method
     * @param method the ranking method
     * @param params parameters for the ranking method
     * @param namespace_uri the namespace URI
     * @return a hashtable of connectivity info
     */
    public Hashtable<String, ConnectivityInfo> rank(String method, String params, String namespace_uri) {
        return graphService.rank(method, params, namespace_uri);
    }
    
    /**
     * Get the top K nodes
     * @param params parameters for the top K algorithm
     * @param centerOfWindow the center point of the window
     * @return a list of top K nodes
     */
    public ArrayList<String> topKNodes(String params, Point centerOfWindow) {
        return graphService.topKNodes(params, centerOfWindow);
    }
    
    /**
     * Get the top K groups
     * @param params parameters for the top K groups algorithm
     * @return a list of top K groups
     */
    public ArrayList<String> topKGroups(String params) {
        return graphService.topKGroups(params);
    }
    
    /**
     * Add an instance and its neighbors to the graph
     * @param instanceName the name of the instance
     * @param client the client to use
     * @return the instance record
     */
    public InstanceRecord addInstanceAndNeighbours(String instanceName, gr.forth.ics.swkmclient.Client client) {
        return graphService.addInstanceAndNeighbours(instanceName, client);
    }
    
    /**
     * Set the dependency load type
     * @param dType the dependency type
     */
    public void setDependencyLoad(DEPENDENCIES dType) {
        graphService.setDependencyLoad(dType);
    }
    
    /**
     * Set the graph namespaces
     * @param nspaces the namespaces
     */
    public void setGraphNameSpaces(String[] nspaces) {
        graphService.setGraphNameSpaces(nspaces);
    }
    
    /**
     * Get the graph namespaces
     * @return the graph namespaces
     */
    public ArrayList<String> getGraphNamespaces() {
        return graphService.getGraphNamespaces();
    }
    
    /**
     * Set the gradient color for the graph
     * @param GColor the gradient color
     * @param selNameSpace the selected namespace
     */
    public void setGraphGradientColor(Color GColor, String selNameSpace) {
        graphService.setGraphGradientColor(GColor, selNameSpace);
    }
    
    /**
     * Register a UI observer for graph changes
     * @param observer the observer to register
     */
    public void registerUIObserver(GraphUIObserver observer) {
        uiObservers.add(observer);
    }
    
    /**
     * Unregister a UI observer
     * @param observer the observer to unregister
     */
    public void unregisterUIObserver(GraphUIObserver observer) {
        uiObservers.remove(observer);
    }
    
    /**
     * Called when the graph changes
     */
    @Override
    public void onGraphChanged() {
        notifyUIObservers();
    }
    
    /**
     * Notify all UI observers of a change
     */
    private void notifyUIObservers() {
        for (GraphUIObserver observer : uiObservers) {
            observer.onGraphUIUpdate();
        }
    }
    
    /**
     * Interface for observing graph UI updates
     */
    public interface GraphUIObserver {
        void onGraphUIUpdate();
    }
}