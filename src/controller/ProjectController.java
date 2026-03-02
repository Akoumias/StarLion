package controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import gr.forth.ics.rdfsuite.services.RdfDocument;
import gr.forth.ics.swkmclient.Client;
import gui.InternalFrame;
import gui.Project;
import gui.Project.LOCATION_TYPE;
import model.RDFModel;
import service.ProjectService;
import service.ProjectService.ProjectObserver;

/**
 * Controller class for project operations, mediating between UI components and the ProjectService.
 * This class handles UI events related to projects and updates the UI when project data changes.
 * 
 * @author jakoum
 */
public class ProjectController implements ProjectObserver {
    
    private ProjectService projectService;
    private Collection<ProjectUIObserver> uiObservers = new ArrayList<>();
    
    /**
     * Constructor that initializes with an existing ProjectService
     */
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
        this.projectService.registerObserver(this);
    }
    
    /**
     * Constructor that creates a new ProjectService
     */
    public ProjectController(String name) {
        this.projectService = new ProjectService(name);
        this.projectService.registerObserver(this);
    }
    
    /**
     * Get the underlying ProjectService
     * @return the ProjectService
     */
    public ProjectService getProjectService() {
        return projectService;
    }
    
    /**
     * Get the underlying Project object
     * @return the Project object
     */
    public Project getProject() {
        return projectService.getProject();
    }
    
    /**
     * Add a document to the project
     * @param doc the document to add
     * @param docType the type of document location
     * @param optionalClient optional client for SWKM documents
     */
    public void addDocument(String doc, LOCATION_TYPE docType, Client optionalClient) {
        projectService.addDocument(doc, docType, optionalClient);
    }
    
    /**
     * Add an RDF document to the project
     * @param rdfDoc the RDF document to add
     */
    public void addDocument(RdfDocument rdfDoc) {
        projectService.addDocument(rdfDoc);
    }
    
    /**
     * Add a document from an input stream
     * @param docUri the URI of the document
     * @param is the input stream containing the document
     */
    public void addDocument(String docUri, InputStream is) {
        projectService.addDocument(docUri, is);
    }
    
    /**
     * Add multiple RDF documents to the project
     * @param rdfDocs the collection of RDF documents to add
     */
    public void addDocuments(Collection<RdfDocument> rdfDocs) {
        projectService.addDocuments(rdfDocs);
    }
    
    /**
     * Set the active client for the project
     * @param documentOrUri the document or URI to set the client for
     */
    public void setActiveClient(String documentOrUri) {
        projectService.setActiveClient(documentOrUri);
    }
    
    /**
     * Get the active client for the project
     * @return the active client
     */
    public Client getActiveClient() {
        return projectService.getActiveClient();
    }
    
    /**
     * Remove a document from the project
     * @param documentOrUri the document or URI to remove
     */
    public void removeDocument(String documentOrUri) {
        projectService.removeDocument(documentOrUri);
    }
    
    /**
     * Remove all documents from the project
     */
    public void removeAllDocuments() {
        projectService.removeAllDocuments();
    }
    
    /**
     * Populate the model with data from the project's documents
     */
    public void populateModel() {
        projectService.populateModel();
    }
    
    /**
     * Get the name of the project
     * @return the project name
     */
    public String getName() {
        return projectService.getName();
    }
    
    /**
     * Set the name of the project
     * @param name the new project name
     */
    public void setName(String name) {
        projectService.setName(name);
    }
    
    /**
     * Get the RDF model for the project
     * @return the RDF model
     */
    public RDFModel getModel() {
        return projectService.getModel();
    }
    
    /**
     * Get the plain documents as input streams
     * @return a collection of input streams
     */
    public Collection<InputStream> getPlainDocsAsStreams() {
        return projectService.getPlainDocsAsStreams();
    }
    
    /**
     * Get specific plain documents as input streams
     * @param docUri the collection of document URIs
     * @return a collection of input streams
     */
    public Collection<InputStream> getPlainDocsAsStreams(Collection<String> docUri) {
        return projectService.getPlainDocsAsStreams(docUri);
    }
    
    /**
     * Get a specific plain document as an input stream
     * @param docUri the document URI
     * @return the input stream
     */
    public InputStream getPlainDocAsStream(String docUri) {
        return projectService.getPlainDocAsStream(docUri);
    }
    
    /**
     * Get the namespaces used in the project
     * @return a collection of namespace URIs
     */
    public Collection<String> getProjectNamespaces() {
        return projectService.getProjectNamespaces();
    }
    
    /**
     * Register a UI observer for project changes
     * @param observer the observer to register
     */
    public void registerUIObserver(ProjectUIObserver observer) {
        uiObservers.add(observer);
    }
    
    /**
     * Unregister a UI observer
     * @param observer the observer to unregister
     */
    public void unregisterUIObserver(ProjectUIObserver observer) {
        uiObservers.remove(observer);
    }
    
    /**
     * Called when the project changes
     */
    @Override
    public void onProjectChanged() {
        notifyUIObservers();
    }
    
    /**
     * Notify all UI observers of a change
     */
    private void notifyUIObservers() {
        for (ProjectUIObserver observer : uiObservers) {
            observer.onProjectUIUpdate();
        }
    }
    
    /**
     * Interface for observing project UI updates
     */
    public interface ProjectUIObserver {
        void onProjectUIUpdate();
    }
}