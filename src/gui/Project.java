package gui;

import gr.forth.ics.swkmclient.*;
import gr.forth.ics.rdfsuite.services.*;
import gr.forth.ics.rdfsuite.services.Format;
import gr.forth.ics.rdfsuite.services.RdfDocument;

import gr.forth.ics.rdfsuite.services.util.IOUtils;
import gr.forth.ics.rdfsuite.services.util.ModelUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDesktopPane;

import javax.swing.JPanel;
import mapping.GraphBackendMode;
import mapping.canonical.CanonicalRdfSnapshot;
import model.RDFModel;
import service.RDFService;

/**
 * The user can open multiple frames (using multiple graph visualisations if preferred) for the same RDF Model.
 * A project contains a reference to an RDF_Model and a list with all opened InternalFrames
 * @author leonidis
 */
public class Project {

    public static enum LOCATION_TYPE {

        CLASSPATH, LOCAL, URL, SWKM, TXT
    };
    
    public static enum DEPENDENCIES{
        NONE,DIRECT,TRANSITIVE
    };
    
    private Map<String, RdfDocument> modelDocs = null;
    private Map<String,InputStream>  plainDocs = null;
    private Map<String,Client>       swkmConns = null;
    private Client                   activeClient = null;

    private RDFModel model = null;
    private RDFService rdfService = null;
    private final LinkedHashSet<String> effectiveNamespaces = new LinkedHashSet<String>();
    
    private ArrayList<InternalFrame> frameList = null;
    // private ArrayList <VisualInformationFrame> VframeList =null;
    private InternalFrame activeFrame = null;
    ////// private VisualInformationFrame activeVFrame = null;
    private int totalFrames = 0;
    private int totalInfoFrames = 0;
    private int frameCounter = 0;
    private String name = "";
     
    /**
     * Create new project.
     * Instantiate the ArrayList containing its frames
     * Instantiate RDF_Model
     * @param name : name of project and also the path of RDF file that will be used for reading data
     */
    public Project(String name) {
        this.modelDocs = new HashMap<String, RdfDocument>();
        this.plainDocs = new HashMap<String, InputStream>();
        this.swkmConns = new HashMap<String, Client>();
        
        this.name = name;

        
        frameList = new ArrayList<InternalFrame>();
        model = new RDFModel();
        rdfService = new RDFService(model);
    }//end Project

    /**
     * Adds a new document to the project.According to the type specified
     * does the appropriate actions to retrieve it from its location.Internally
     * creates rdfDocuments and inputStreams
     * 
     * @param doc - the name(path) of the document
     * @param docType - the type of the location of the document
     * @param optionalClient - a client for the case of connection with SWKM
     */
    public void addDocument(String doc, LOCATION_TYPE docType,Client optionalClient) {
        String docUri = null;
        if (doc.charAt(doc.length() - 1) != '#') {
            docUri = doc + '#';
        } else {
            docUri = doc;
        }
        switch (docType) {
            case CLASSPATH:
                addDocumentCLASSPATH(doc,docUri);
                break;
            case LOCAL:
                addDocumentLOCAL(doc,docUri);
                break;
            case SWKM:
                addDocumentSWKM(doc, docUri,optionalClient);
                break;
            case TXT:
                addDocumentTXT(doc,docUri);
                break;
            case URL:
                addDocumentURL(doc,docUri);
                break;
            default:

        }
        //If the document caused an exception to the population of the model
        //remove it and rethrow it to handle the exception from the GUI
        try{
            populateModel();
            //TODO populateTXTModel();
            //Create a parser for the txt files to check if they conform to the
            //grammar specified and do the appropriate actions. Maybe create another
            //model like RDFModel
        }catch(gr.forth.ics.rdfsuite.services.exceptions.SwkmModelException skwme){
            removeDocument(docUri);
            throw skwme;
        }catch(RuntimeException ex){
            removeDocument(docUri);
            throw ex;
        }
    }

    /**
     * Adds an rdfDocument to the project
     * 
     * @param rdfDoc - the rdfDocument that is added
     */
    public void addDocument(RdfDocument rdfDoc){
        modelDocs.put(rdfDoc.getURI(), rdfDoc);
    }
    
    /**
     * Adds an inputStream(plainDocument) to the project
     * 
     * @param docUri - the document uri
     * @param is - the inputStream
     */
    public void addDocument(String docUri,InputStream is){
        plainDocs.put(docUri,is);
    }
    
    /**
     * Adds a collection of rdfDocuments to the project
     * @param rdfDocs - a collection of rdfDocuments to be added to the project
     */
    public void addDocuments(Collection<RdfDocument> rdfDocs){
        for(RdfDocument rdfDoc:rdfDocs){
            modelDocs.put(rdfDoc.getURI(), rdfDoc);
        }
    }
    
    /**
     * Sets the active client corresponding to the document or uri passed as parameter
     * or null if such document doesn't exist 
     * 
     * @param documentOrUri - the document that we want to retrieve the client
     */
    public void setActiveClient(String documentOrUri){
         this.activeClient = swkmConns.get(documentOrUri);
    }

    /**
     * Retrieve the active client  
     * 
     * @return the active client of a swkm connection
     */
    public Client getActiveClient(){
         return this.activeClient;
    }
    
    /**
     * Given a document name or a uri of a document it removes the appropriate 
     * document from the project
     * 
     * @param docUri - the document's uri that we want to remove
     */
    public void removeDocument(String documentOrUri){
        String docUri = null;
        if (documentOrUri.charAt(documentOrUri.length() - 1) != '#') {
            docUri = documentOrUri + '#';
        } else {
            docUri = documentOrUri;
        }
        
        if(modelDocs.containsKey(docUri)){
            modelDocs.remove(docUri);
            //if a document is removed we should update the model
            //To do this we create a new model and populate it
            model = new RDFModel();
            rdfService = new RDFService(model);
            populateModel();
        }
        if(plainDocs.containsKey(docUri)){
            plainDocs.remove(docUri);            
        }
        if(swkmConns.containsKey(docUri)){
            swkmConns.remove(docUri);
        }
    }
    
    /**
     * Removes all the documents from the project
     */
    public void removeAllDocuments(){
        modelDocs.clear();
        model = new RDFModel();
        rdfService = new RDFService(model);
        populateModel();
        plainDocs.clear();
        swkmConns.clear();
    }
    /*
     * Create an rdfDocument from the classpath and add it to the project
     */
    private boolean addDocumentCLASSPATH(String doc,String docUri) {
        System.out.println(doc);
        InputStream in = Project.class.getResourceAsStream(doc);
        boolean success = true;
        if (in != null) {
            RdfDocument rdfDoc = new RdfDocument(docUri, IOUtils.readStreamAsString(in), Format.RDF_XML);
            modelDocs.put(docUri, rdfDoc);
        } else {
            System.out.println("CLASSPATH ERR");
            success = false;
        }

        return success;
    }

    /*
     * Create an rdfDocument from a local file and add it to the project
     */
    private boolean addDocumentLOCAL(String doc,String docUri) {
        File f = new File(doc);
        boolean success = true;
        if (f.exists()) {
            RdfDocument rdfDoc = new RdfDocument(docUri, IOUtils.readFileAsString(f), Format.RDF_XML);
            modelDocs.put(docUri, rdfDoc);
        } else {
            success = false;
        }
        return success;
    }

    /*
     * Create an rdfDocument from SWKM connection and add it to the project
     */
    private boolean addDocumentSWKM(String doc, String docUri,Client client) {
        boolean success = true;
        String[] namespaces = new String[1];
        namespaces[0] = doc;
        System.out.println("Trying to fetch : " + name);

        Set<String> defaultNameSpaces = ModelUtils.getDefaultNamespaces();
        for (String dNSpace : defaultNameSpaces) {
            if (dNSpace.equals(namespaces[0])) {
                System.out.println("Cannot export default namespace");
            }
        }
        Map<String, RdfDocument> results = client.exporter().fetch(Arrays.asList(namespaces), Format.RDF_XML, Deps.WITHOUT, Data.WITHOUT);
        if (results != null) {
            modelDocs.putAll(results);
            swkmConns.put(docUri, client);
        } else {
            success = false;
        }

        return success;
    }

    /*
     * Create an rdfDocument from a URL add it to the project
     */
    private boolean addDocumentURL(String doc,String docUri) {
        boolean success = true;
        URL theURL;
        try {
            theURL = new URL(doc);
            InputStream in = theURL.openStream();
            RdfDocument rdfdoc = new RdfDocument(docUri, IOUtils.readStreamAsString(in), Format.RDF_XML);
            modelDocs.put(docUri, rdfdoc);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block

            e.printStackTrace();
            success = false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            success = false;
        }
        
        return success;
    }

    /*
     * Open a stream from a simple txt file and add it to the project as a stream
     */
    private boolean addDocumentTXT(String doc,String docUri) {
        boolean success = false;
        File f = new File(doc);
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            plainDocs.put(docUri, is);
            success = true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
            success = false;
        }

        return success;
    }


    /*
     * Every time a document is added to the project the model is populated
     * to reflect the changes
     */
     private void populateModel(){
         model = new RDFModel();
         rdfService = new RDFService(model);
         if (requiresJenaMode(modelDocs.values())) {
             rdfService.setBackendMode(GraphBackendMode.JENA);
         }
         effectiveNamespaces.clear();

         for (RdfDocument document : modelDocs.values()) {
             if (document == null || document.getContent() == null) {
                 continue;
             }
             String format = normalizeFormat(document.getFormat());
             InputStream payload = new ByteArrayInputStream(document.getContent().getBytes(StandardCharsets.UTF_8));
             boolean loaded = rdfService.read(payload, document.getURI(), format, true);
             if (!loaded) {
                 throw new IllegalStateException("Failed to load RDF document: " + document.getURI());
             }
             CanonicalRdfSnapshot snapshot = rdfService.getCanonicalSnapshot();
             effectiveNamespaces.addAll(snapshot.getNamespaces());
             logDocumentBackendSummary(document.getURI(), snapshot.getNamespaces().size());
         }
     }

     private void logDocumentBackendSummary(String documentUri, int namespaceCount) {
         System.out.println("[DOC BACKEND] file=" + documentUri
                 + " jena_parsable=" + rdfService.wasLastJenaParsable()
                 + " effective=" + rdfService.getLastEffectiveBackend()
                 + " namespaces_count=" + namespaceCount);
     }

     private String normalizeFormat(Object format) {
         if (format == null) {
             return "RDF/XML";
         }
         String normalized = String.valueOf(format).trim();
         if ("RDF_XML".equalsIgnoreCase(normalized) || "RDFXML".equalsIgnoreCase(normalized)) {
             return "RDF/XML";
         }
         return normalized;
     }

     private boolean requiresJenaMode(Collection<RdfDocument> documents) {
         for (RdfDocument document : documents) {
             if (document == null || document.getURI() == null) {
                 continue;
             }
             String uri = document.getURI().toLowerCase();
             if (uri.contains(".ttl") || uri.contains(".owl")) {
                 return true;
             }
         }
         return false;
     }
     

    /**
     * Return the name of this project
     * @return name
     */
    public String getName(){
        return name;
    }//end getName

    /**
     * Returns the InternalFrame in position index in frameList
     * @param index
     * @return frame
     */
    public InternalFrame getFrameAt(int index) {
        return frameList.get(index);
    }//end getFrameAt

    /**
     * Returns the InternalFrame with the specified name
     * @param name
     * @return frame
     */
    public InternalFrame getFrameWithName(String name) {
        InternalFrame frame = null;
        for (int i = 0; i < frameList.size(); i++) {
            frame = frameList.get(i);
            if (frame.getFrameName().equals(name)) {
                return frame;
            }
        }//end for i < frameList.size
        return null;
    }//end getFrameWithName

    /**
     * Return the active frame of this project
     * @return activeFrame
     */
    public InternalFrame getActiveFrame() {
        return frameList.get(frameList.indexOf(activeFrame));
    }//end getActiveFrame

    /**
     * Set the given frame as the active frame of this project 
     * @param frame : new active frame
     */
    public void setActiveFrame(InternalFrame frame) {
        activeFrame = frame;
        ProjectManager.getSingleton().setActiveProject(this);
    }//end setActiveFrame

    public void setName(String name){
        this.name = name;
    }
    /**
     * Create a new visualization frame for this project and add it to its frameList
     * Increment totalFrames variable
     * @param name : title name of new frame 
     */
    public void addVisualizationFrame(String name) {
        GraphInternalFrame f = new GraphInternalFrame(name, this);

        frameList.add(totalFrames, f);
        setActiveFrame(f);
        totalFrames++;
        frameCounter++;
        ProjectTree.getSingleton().createFrameNode(this, f);

    //f.createGraph(layout,params,createIsa);
    }//end addVisualisationFrame

    public void addInfoVisFrame(String name, JPanel panel) {
        VisualInformationFrame f = new VisualInformationFrame(name, this, panel);
        frameList.add(totalFrames, f);
        setActiveFrame(f);
        totalFrames++;
        totalInfoFrames++;
        frameCounter++;
        ProjectTree.getSingleton().createFrameNode(this, f);
    }

    public void addGraphInFrame(String layout, String params) {
        ((GraphInternalFrame) getActiveFrame()).createGraph(layout, params);
    }

    /**
     * Restore an InternalFrame for this project, using the saved data in file with name the parameter fileName 
     * @param fileName : the name of the file which contains the saved data 
     */
    public void restoreVisualizationFrame(String name, String fileName, boolean createIsa) {
        GraphInternalFrame f = new GraphInternalFrame(name, this);
        System.out.println("FILENAME GRAPH =" + fileName);
        ((GraphInternalFrame) f).restoreGraph(fileName, createIsa);
        frameList.add(totalFrames, f);
        setActiveFrame(f);
        totalFrames++;
        frameCounter++;
        ProjectTree.getSingleton().createFrameNode(this, f);
    }//end restoreVisualisationFrame

    /**
     * Remove the given frame from this project
     * Decrement totalFrames variable
     * @param frame : frame to be removed from project
     */
    public void removeFrame(InternalFrame frame) {
        frameList.remove(frameList.indexOf(frame));
        totalFrames--;

        ProjectTree.getSingleton().removeFrameNode(this, frame);
        if (!frameList.isEmpty()) {
            setActiveFrame(frameList.get(0));
        } else {
            setActiveFrame(null);
            
        }
    }//end removeFrame
	

    /**
     * Return the frames' number of this project
     * @return totalFrames : total frames number
     */
    public int getTotalFramesNo() {
        return totalFrames;
    }//end getTotalFramesNo

    /**
     * Returns the number of the info frames of this project
     * @return totalInfoFrames : the total number of info frames
     */
    public int getTotalInfoFramesNo() {
        return totalInfoFrames;
    }

    /**
     * Remove all frames from this project. Usually called in project closing
     * @param desktop : main desktop in which all frames are laid in
     */
    public void closeAllFrames(JDesktopPane desktop) {
        for (int i = 0; i < totalFrames; i++) {
            //If some frames are iconified they are already removed from JDesktopPane and we only have to remove their icons from the desktop
            if (frameList.get(i).isIcon()) {
                desktop.remove(frameList.get(i).getDesktopIcon());
            } else {
                desktop.remove(frameList.get(i));
            }
        }//end for
    }//end closeAllFrames

    /**
     * Overrides default toString function and return the name of this project
     */
    @Override
    public String toString() {
        return name;
    }//end toString

    /**
     * Return the RDF_Model of this project
     * @return model : RDF_Model reference
     */
    public RDFModel getModel() {
        return model;
    }//end getModel

    /**
     * Returns all RDF documents currently loaded in this project.
     */
    public Collection<RdfDocument> getModelDocuments() {
        return modelDocs.values();
    }

    /**
     * Get all txt documents that are loaded in the project as InputStreams
     * 
     * @return a collection of inputStreams(stream of simple txt files)
     */
    public Collection<InputStream> getPlainDocsAsStreams(){
        return plainDocs.values();
    }

    /**
     * Get the txt documents with the corresponding URIs 
     * that are loaded in the project as InputStreams
     * 
     * @return a collection of inputStreams(stream of simple txt files)
     */
    public Collection<InputStream> getPlainDocsAsStreams(Collection<String> docUri){
        ArrayList<InputStream> streams = new ArrayList<InputStream>();
        for(String s:docUri){
           streams.add(plainDocs.get(s));
        }
        return streams;
    }

    public InputStream getPlainDocAsStream(String docUri){
        return plainDocs.get(docUri);
    }
    /**
     * Get all the namespaces that the project contains depending on the containing
     * documents
     * 
     * @return all the namespaces that are contained-related with the documents
     */
    public Collection<String> getProjectNamespaces(){
        String[] modelNs = getModel().getNamespaces();
        Set<String> plainDocNs = plainDocs.keySet();
        
        ArrayList<String> allNs = new ArrayList<String>();

        if (!effectiveNamespaces.isEmpty()) {
            allNs.addAll(effectiveNamespaces);
        } else {
            for (String s : modelNs) {
                allNs.add(s);
            }
        }
        for(String s:plainDocNs){
            allNs.add(s);
        }
        
        return allNs;
    }
    
    /**
     * Return the frame counter that represents the total number of frames opened in this project (including those that are closed)
     * @return frameCounter
     */
    public int getFrameCounter() {
        return frameCounter;
    }//end getFrameCounter

    
}//end class Project