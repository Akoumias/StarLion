package controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import model.RDFModel;
import model.RDFNamespace;
import model.RDFPropertyInstance;
import model.RDFResource;
import service.RDFService;
import service.RDFService.RDFObserver;

/**
 * Controller class for RDF data operations, mediating between UI components and the RDFService.
 * This class handles UI events related to RDF data and updates the UI when RDF data changes.
 * 
 * @author jakoum
 */
public class RDFController implements RDFObserver {
    
    private RDFService rdfService;
    private Collection<RDFUIObserver> uiObservers = new ArrayList<>();
    
    /**
     * Constructor that initializes with an existing RDFService
     */
    public RDFController(RDFService rdfService) {
        this.rdfService = rdfService;
        this.rdfService.registerObserver(this);
    }
    
    /**
     * Constructor that creates a new RDFService
     */
    public RDFController() {
        this.rdfService = new RDFService();
        this.rdfService.registerObserver(this);
    }
    
    /**
     * Get the underlying RDFService
     * @return the RDFService
     */
    public RDFService getRDFService() {
        return rdfService;
    }
    
    /**
     * Get the underlying RDFModel object
     * @return the RDFModel object
     */
    public RDFModel getRDFModel() {
        return rdfService.getRDFModel();
    }
    
    /**
     * Get a namespace by URI
     * @param namespace_uri the URI of the namespace
     * @return the RDFNamespace object
     */
    public RDFNamespace getNamespace(String namespace_uri) {
        return rdfService.getNamespace(namespace_uri);
    }
    
    /**
     * Get all instances in the model
     * @return a collection of RDFResource objects
     */
    public Collection<RDFResource> getAllInstances() {
        return rdfService.getAllInstances();
    }
    
    /**
     * Get instances of a specific class
     * @param className the name of the class
     * @return a collection of RDFResource objects
     */
    public Collection<RDFResource> getInstances(String className) {
        return rdfService.getInstances(className);
    }
    
    /**
     * Get all property instances in the model
     * @return a collection of RDFPropertyInstance objects
     */
    public Collection<RDFPropertyInstance> getAllPropertyInstances() {
        return rdfService.getAllPropertyInstances();
    }
    
    /**
     * Get all namespaces in the model
     * @return an array of namespace URIs
     */
    public String[] getNamespaces() {
        return rdfService.getNamespaces();
    }
    
    /**
     * Read RDF data from a file
     * @param model_file the file to read from
     * @param base_uri the base URI
     * @param format the format of the file
     * @param fetch_all_ns whether to fetch all namespaces
     * @return true if the read was successful
     */
    public boolean read(String model_file, String base_uri, String format, boolean fetch_all_ns) {
        return rdfService.read(model_file, base_uri, format, fetch_all_ns);
    }
    
    /**
     * Read RDF data from an input stream
     * @param input the input stream to read from
     * @param base_uri the base URI
     * @param format the format of the data
     * @param fetch_all_ns whether to fetch all namespaces
     * @return true if the read was successful
     */
    public boolean read(InputStream input, String base_uri, String format, boolean fetch_all_ns) {
        return rdfService.read(input, base_uri, format, fetch_all_ns);
    }
    
    /**
     * Write RDF data to a file
     * @param file the file to write to
     * @param Uri the URI of the model
     * @return true if the write was successful
     */
    public boolean write(String file, String Uri) {
        return rdfService.write(file, Uri);
    }
    
    /**
     * Register a UI observer for RDF data changes
     * @param observer the observer to register
     */
    public void registerUIObserver(RDFUIObserver observer) {
        uiObservers.add(observer);
    }
    
    /**
     * Unregister a UI observer
     * @param observer the observer to unregister
     */
    public void unregisterUIObserver(RDFUIObserver observer) {
        uiObservers.remove(observer);
    }
    
    /**
     * Called when the RDF data changes
     */
    @Override
    public void onRDFDataChanged() {
        notifyUIObservers();
    }
    
    /**
     * Notify all UI observers of a change
     */
    private void notifyUIObservers() {
        for (RDFUIObserver observer : uiObservers) {
            observer.onRDFUIUpdate();
        }
    }
    
    /**
     * Interface for observing RDF UI updates
     */
    public interface RDFUIObserver {
        void onRDFUIUpdate();
    }
}