package service;

import java.awt.Color;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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
import mapping.GraphBackendMode;
import mapping.ParityComparator;
import mapping.ParityReport;
import mapping.adapter.JenaCanonicalAdapter;
import mapping.adapter.SwkmCanonicalAdapter;
import mapping.canonical.CanonicalGraphSnapshot;
import mapping.canonical.CanonicalRdfSnapshot;
import model.RDFModel;

/**
 * Service class for graph operations, separating business logic from presentation.
 * This class serves as an API for frontend components to interact with graph data.
 * 
 * @author jakoum
 */
public class GraphService {

    private Graph graph;
    private List<GraphObserver> observers = new ArrayList<>();
    private GraphBackendMode backendMode = GraphBackendMode.resolveDefault();
    private final SwkmCanonicalAdapter swkmAdapter = new SwkmCanonicalAdapter();
    private final JenaCanonicalAdapter jenaAdapter = new JenaCanonicalAdapter();
    private final ParityComparator parityComparator = new ParityComparator();
    private ParityReport lastParityReport = null;

    /**
     * Constructor that initializes with an existing Graph
     */
    public GraphService(Graph graph) {
        this.graph = graph;
    }

    /**
     * Constructor that creates a new Graph
     */
    public GraphService() {
        this.graph = new Graph();
    }

    /**
     * Get the underlying Graph object
     * @return the Graph object
     */
    public Graph getGraph() {
        return graph;
    }

    public GraphBackendMode getBackendMode() {
        return backendMode;
    }

    public void setBackendMode(GraphBackendMode backendMode) {
        if (backendMode != null) {
            this.backendMode = backendMode;
        }
    }

    public ParityReport getLastParityReport() {
        return lastParityReport;
    }

    public CanonicalGraphSnapshot getCanonicalSnapshot() {
        return swkmAdapter.fromGraph(graph);
    }

    /**
     * Populate the graph with data from an RDF model
     * @param model the RDF model to populate from
     */
    public void populateGraph(RDFModel model) {
        graph.populateGraph(model);
        if (backendMode == GraphBackendMode.DUAL || backendMode == GraphBackendMode.JENA) {
            CanonicalGraphSnapshot swkmGraph = swkmAdapter.fromGraph(graph);
            CanonicalRdfSnapshot jenaRdf = jenaAdapter.fromInputStream(
                    toInputStream(model),
                    null,
                    "RDF/XML"
            );
            CanonicalGraphSnapshot jenaGraph = jenaAdapter.toGraphSnapshot(jenaRdf);
            lastParityReport = parityComparator.compareGraph(swkmGraph, jenaGraph);
            logParityReport("Graph populate from RDFModel", lastParityReport);
        } else {
            lastParityReport = null;
        }
        notifyObservers();
    }

    /**
     * Populate graph directly from canonical snapshot data.
     *
     * @param snapshot canonical graph snapshot
     */
    public void populateGraph(CanonicalGraphSnapshot snapshot) {
        graph.populateGraph(snapshot);
        notifyObservers();
    }

    /**
     * Populate the graph from an input stream
     * @param inputStream the input stream containing graph data
     * @param streamURI the URI of the stream
     * @throws java.io.IOException if an I/O error occurs
     */
    public void populateGraph(InputStream inputStream, String streamURI) throws java.io.IOException {
        byte[] payload = readAllBytes(inputStream);
        graph.populateGraph(new ByteArrayInputStream(payload), streamURI);
        if (isPlainTextGraphStream(streamURI)) {
            lastParityReport = null;
            notifyObservers();
            return;
        }

        if (backendMode == GraphBackendMode.DUAL || backendMode == GraphBackendMode.JENA) {
            CanonicalGraphSnapshot swkmGraph = swkmAdapter.fromGraph(graph);
            CanonicalRdfSnapshot jenaRdf = jenaAdapter.fromInputStream(
                    new ByteArrayInputStream(payload),
                    streamURI,
                    "RDF/XML"
            );
            CanonicalGraphSnapshot jenaGraph = jenaAdapter.toGraphSnapshot(jenaRdf);
            lastParityReport = parityComparator.compareGraph(swkmGraph, jenaGraph);
            logParityReport("Graph populate from InputStream", lastParityReport);
        } else {
            lastParityReport = null;
        }
        notifyObservers();
    }

    private boolean isPlainTextGraphStream(String streamURI) {
        if (streamURI == null) {
            return false;
        }
        String normalized = streamURI.trim().toLowerCase();
        return normalized.endsWith(".txt") || normalized.endsWith(".txt#");
    }

    /**
     * Update the layout of the graph
     * @param layout the layout algorithm to use
     * @param params parameters for the layout algorithm
     * @param centerOfWindow the center point of the window
     * @return true if the layout was updated successfully
     */
    public boolean updateLayout(String layout, String params, Point centerOfWindow) {
        boolean result = graph.updateLayout(layout, params, centerOfWindow);
        notifyObservers();
        return result;
    }

    /**
     * Save the graph as an image
     * @param fileName the name of the file to save to
     * @return true if the save was successful
     */
    public boolean saveGraphAsImage(String fileName) {
        return graph.saveGraphAsImage(fileName);
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
        notifyObservers();
    }

    private InputStream toInputStream(RDFModel model) {
        try {
            java.io.File tmp = java.io.File.createTempFile("starlion-swkm-export", ".rdf");
            tmp.deleteOnExit();
            model.write(tmp.getAbsolutePath(), "");
            java.nio.file.Path path = tmp.toPath();
            byte[] content = java.nio.file.Files.readAllBytes(path);
            return new ByteArrayInputStream(content);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to materialize SWKM model for Jena parity run", ex);
        }
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private void logParityReport(String operation, ParityReport report) {
        if (report == null) {
            return;
        }
        if (report.isEquivalent()) {
            System.out.println("[PARITY OK] " + operation + " -> " + report.getDomain());
            return;
        }

        System.out.println("[PARITY FAIL] " + operation + " -> " + report.getDomain()
                + " mismatches=" + report.getMismatches().size());
        for (String mismatch : report.getMismatches()) {
            System.out.println("[PARITY DIFF] " + mismatch);
        }
    }

    /**
     * Get the state of the graph
     * @return the graph state
     */
    public GraphState getState() {
        return graph.getState();
    }

    /**
     * Get the list of nodes in the graph
     * @return a hashtable of nodes
     */
    public Hashtable<String, Node> getNodeList() {
        return graph.getNodeList();
    }

    /**
     * Get the list of edges in the graph
     * @return a hashtable of edges
     */
    public Hashtable<String, Edge> getEdgeList() {
        return graph.getEdgeList();
    }

    /**
     * Get the ranker for the graph
     * @return the graph ranker
     */
    public Ranker getGraphRanker() {
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
        InstanceRecord record = graph.addInstanceAndNeighbours(instanceName, client);
        notifyObservers();
        return record;
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
    public void setGraphNameSpaces(String[] nspaces) {
        graph.setGraphNameSpaces(nspaces);
    }

    /**
     * Get the graph namespaces
     * @return the graph namespaces
     */
    public ArrayList<String> getGraphNamespaces() {
        return graph.getGraphNamespaces();
    }

    /**
     * Set the gradient color for the graph
     * @param GColor the gradient color
     * @param selNameSpace the selected namespace
     */
    public void setGraphGradientColor(Color GColor, String selNameSpace) {
        graph.setGraphGradientColor(GColor, selNameSpace);
        notifyObservers();
    }

    /**
     * Get the layout metrics
     * @return the layout metrics
     */
    public LayoutMetrics2 getLayoutMetrics2() {
        return graph.getLayoutMetrics2();
    }

    /**
     * Register an observer for graph changes
     * @param observer the observer to register
     */
    public void registerObserver(GraphObserver observer) {
        observers.add(observer);
    }

    /**
     * Unregister an observer
     * @param observer the observer to unregister
     */
    public void unregisterObserver(GraphObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify all observers of a change
     */
    private void notifyObservers() {
        for (GraphObserver observer : observers) {
            observer.onGraphChanged();
        }
    }

    /**
     * Interface for observing graph changes
     */
    public interface GraphObserver {
        void onGraphChanged();
    }
}
