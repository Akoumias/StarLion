package service;

import gr.forth.ics.rdfsuite.services.RdfDocument;
import gr.forth.ics.swkmclient.Client;
import gui.Project;
import gui.Project.DEPENDENCIES;
import gui.Project.LOCATION_TYPE;
import model.RDFModel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service class for project operations, separating business logic from presentation.
 * This class serves as an API for frontend components to interact with project data.
 * 
 * @author jakoum
 */
public class ProjectService {

    private Project project;
    private List<ProjectObserver> observers = new ArrayList<>();

    /**
     * Constructor that initializes with an existing Project
     */
    public ProjectService(Project project) {
        this.project = project;
    }

    /**
     * Constructor that creates a new Project
     */
    public ProjectService(String name) {
        this.project = new Project(name);
    }

    /**
     * Get the underlying Project object
     * @return the Project object
     */
    public Project getProject() {
        return project;
    }

    /**
     * Add a document to the project
     * @param doc the document to add
     * @param docType the type of document location
     * @param optionalClient optional client for SWKM documents
     */
    public void addDocument(String doc, LOCATION_TYPE docType, Client optionalClient) {
        project.addDocument(doc, docType, optionalClient);
        notifyObservers();
    }

    /**
     * Add an RDF document to the project
     * @param rdfDoc the RDF document to add
     */
    public void addDocument(RdfDocument rdfDoc) {
        project.addDocument(rdfDoc);
        notifyObservers();
    }

    /**
     * Add a document from an input stream
     * @param docUri the URI of the document
     * @param is the input stream containing the document
     */
    public void addDocument(String docUri, InputStream is) {
        project.addDocument(docUri, is);
        notifyObservers();
    }

    /**
     * Add multiple RDF documents to the project
     * @param rdfDocs the collection of RDF documents to add
     */
    public void addDocuments(Collection<RdfDocument> rdfDocs) {
        project.addDocuments(rdfDocs);
        notifyObservers();
    }

    /**
     * Set the active client for the project
     * @param documentOrUri the document or URI to set the client for
     */
    public void setActiveClient(String documentOrUri) {
        project.setActiveClient(documentOrUri);
    }

    /**
     * Get the active client for the project
     * @return the active client
     */
    public Client getActiveClient() {
        return project.getActiveClient();
    }

    /**
     * Remove a document from the project
     * @param documentOrUri the document or URI to remove
     */
    public void removeDocument(String documentOrUri) {
        project.removeDocument(documentOrUri);
        notifyObservers();
    }

    /**
     * Remove all documents from the project
     */
    public void removeAllDocuments() {
        project.removeAllDocuments();
        notifyObservers();
    }

    /**
     * Populate the model with data from the project's documents
     * Note: This is a workaround since Project.populateModel() is private
     */
    public void populateModel() {
        // Since we can't directly call project.populateModel() as it's private,
        // we'll use the model and documents we have access to
        RDFModel model = project.getModel();
        Collection<InputStream> docStreams = project.getPlainDocsAsStreams();

        // The actual population happens when documents are added to the project
        // This method is mainly for notifying observers
        notifyObservers();
    }

    /**
     * Get the name of the project
     * @return the project name
     */
    public String getName() {
        return project.getName();
    }

    /**
     * Set the name of the project
     * @param name the new project name
     */
    public void setName(String name) {
        project.setName(name);
        notifyObservers();
    }

    /**
     * Get the RDF model for the project
     * @return the RDF model
     */
    public RDFModel getModel() {
        return project.getModel();
    }

    /**
     * Get the plain documents as input streams
     * @return a collection of input streams
     */
    public Collection<InputStream> getPlainDocsAsStreams() {
        return project.getPlainDocsAsStreams();
    }

    /**
     * Get specific plain documents as input streams
     * @param docUri the collection of document URIs
     * @return a collection of input streams
     */
    public Collection<InputStream> getPlainDocsAsStreams(Collection<String> docUri) {
        return project.getPlainDocsAsStreams(docUri);
    }

    /**
     * Get a specific plain document as an input stream
     * @param docUri the document URI
     * @return the input stream
     */
    public InputStream getPlainDocAsStream(String docUri) {
        return project.getPlainDocAsStream(docUri);
    }

    /**
     * Get the namespaces used in the project
     * @return a collection of namespace URIs
     */
    public Collection<String> getProjectNamespaces() {
        return project.getProjectNamespaces();
    }

    /**
     * Register an observer for project changes
     * @param observer the observer to register
     */
    public void registerObserver(ProjectObserver observer) {
        observers.add(observer);
    }

    /**
     * Unregister an observer
     * @param observer the observer to unregister
     */
    public void unregisterObserver(ProjectObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify all observers of a change
     */
    private void notifyObservers() {
        for (ProjectObserver observer : observers) {
            observer.onProjectChanged();
        }
    }

    /**
     * Interface for observing project changes
     */
    public interface ProjectObserver {
        void onProjectChanged();
    }
}
