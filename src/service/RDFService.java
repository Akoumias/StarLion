package service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mapping.GraphBackendMode;
import mapping.ParityComparator;
import mapping.ParityReport;
import mapping.adapter.JenaCanonicalAdapter;
import mapping.adapter.SwkmCanonicalAdapter;
import mapping.canonical.CanonicalRdfSnapshot;
import model.RDFModel;
import model.RDFNamespace;
import model.RDFPropertyInstance;
import model.RDFResource;

/**
 * Service class for RDF data operations, separating business logic from presentation.
 * This class serves as an API for frontend components to interact with RDF data.
 * 
 * @author jakoum
 */
public class RDFService {
    
    private RDFModel rdfModel;
    private List<RDFObserver> observers = new ArrayList<>();
    private GraphBackendMode backendMode = GraphBackendMode.resolveDefault();
    private final SwkmCanonicalAdapter swkmAdapter = new SwkmCanonicalAdapter();
    private final JenaCanonicalAdapter jenaAdapter = new JenaCanonicalAdapter();
    private final ParityComparator parityComparator = new ParityComparator();
    private ParityReport lastParityReport = null;
    private CanonicalRdfSnapshot lastEffectiveSnapshot = null;
    private GraphBackendMode lastEffectiveBackend = GraphBackendMode.SWKM;
    private boolean lastJenaParsable = false;
    private long lastJenaParseMillis = -1L;
    private long lastSwkmReadMillis = -1L;
    private boolean lastSwkmReadExecuted = false;
    
    /**
     * Constructor that initializes with an existing RDFModel
     */
    public RDFService(RDFModel rdfModel) {
        this.rdfModel = rdfModel;
    }
    
    /**
     * Constructor that creates a new RDFModel
     */
    public RDFService() {
        this.rdfModel = new RDFModel();
    }
    
    /**
     * Get the underlying RDFModel object
     * @return the RDFModel object
     */
    public RDFModel getRDFModel() {
        return rdfModel;
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

    public CanonicalRdfSnapshot getCanonicalSnapshot() {
        if (lastEffectiveSnapshot != null) {
            return lastEffectiveSnapshot;
        }
        return swkmAdapter.fromRdfModel(rdfModel);
    }

    public GraphBackendMode getLastEffectiveBackend() {
        return lastEffectiveBackend;
    }

    public boolean wasLastJenaParsable() {
        return lastJenaParsable;
    }

    public long getLastJenaParseMillis() {
        return lastJenaParseMillis;
    }

    public long getLastSwkmReadMillis() {
        return lastSwkmReadMillis;
    }

    public boolean wasLastSwkmReadExecuted() {
        return lastSwkmReadExecuted;
    }
    
    /**
     * Get a namespace by URI
     * @param namespace_uri the URI of the namespace
     * @return the RDFNamespace object
     */
    public RDFNamespace getNamespace(String namespace_uri) {
        return rdfModel.getNamespace(namespace_uri);
    }
    
    /**
     * Get all instances in the model
     * @return a collection of RDFResource objects
     */
    public Collection<RDFResource> getAllInstances() {
        return rdfModel.getAllInstances();
    }
    
    /**
     * Get instances of a specific class
     * @param className the name of the class
     * @return a collection of RDFResource objects
     */
    public Collection<RDFResource> getInstances(String className) {
        return rdfModel.getInstances(className);
    }
    
    /**
     * Get all property instances in the model
     * @return a collection of RDFPropertyInstance objects
     */
    public Collection<RDFPropertyInstance> getAllPropertyInstances() {
        return rdfModel.getAllPropertyInstances();
    }
    
    /**
     * Get all namespaces in the model
     * @return an array of namespace URIs
     */
    public String[] getNamespaces() {
        return rdfModel.getNamespaces();
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
        ParseAttempt jenaAttempt = tryReadJenaFromFile(model_file, base_uri, format);
        CanonicalRdfSnapshot jenaSnapshot = jenaAttempt.snapshot;
        boolean jenaOk = jenaSnapshot != null;
        lastJenaParsable = jenaOk;
        lastJenaParseMillis = jenaAttempt.elapsedMillis;
        lastSwkmReadMillis = -1L;
        lastSwkmReadExecuted = false;

        if (backendMode == GraphBackendMode.JENA) {
            return readUsingPreferredJenaFromFile(model_file, base_uri, format, fetch_all_ns, jenaSnapshot, jenaOk);
        }

        long swkmStart = System.nanoTime();
        boolean swkmResult = rdfModel.read(model_file, base_uri, format, fetch_all_ns);
        lastSwkmReadExecuted = true;
        lastSwkmReadMillis = elapsedMillis(swkmStart);
        if (!swkmResult) {
            return false;
        }
        CanonicalRdfSnapshot swkmSnapshot = swkmAdapter.fromRdfModel(rdfModel);

        if (backendMode == GraphBackendMode.DUAL) {
            if (jenaOk) {
                lastParityReport = parityComparator.compareRdf(swkmSnapshot, jenaSnapshot);
            } else {
                lastParityReport = new ParityReport("rdf");
                lastParityReport.addMismatch("jena_parse_failed");
            }
            logParityReport("RDF file read", lastParityReport);
        } else {
            lastParityReport = null;
        }

        lastEffectiveSnapshot = swkmSnapshot;
        lastEffectiveBackend = GraphBackendMode.SWKM;

        logBackendStatus("RDF file read", jenaOk);
        notifyObservers();
        return true;
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
        try {
            byte[] payload = readAllBytes(input);
            ParseAttempt jenaAttempt = tryReadJenaFromPayload(payload, base_uri, format);
            CanonicalRdfSnapshot jenaSnapshot = jenaAttempt.snapshot;
            boolean jenaOk = jenaSnapshot != null;
            lastJenaParsable = jenaOk;
            lastJenaParseMillis = jenaAttempt.elapsedMillis;
            lastSwkmReadMillis = -1L;
            lastSwkmReadExecuted = false;
            if (backendMode == GraphBackendMode.JENA) {
                return readUsingPreferredJenaFromPayload(payload, base_uri, format, fetch_all_ns, jenaSnapshot, jenaOk);
            }

            long swkmStart = System.nanoTime();
            boolean swkmResult = rdfModel.read(new ByteArrayInputStream(payload), base_uri, format, fetch_all_ns);
            lastSwkmReadExecuted = true;
            lastSwkmReadMillis = elapsedMillis(swkmStart);
            if (!swkmResult) {
                return false;
            }
            CanonicalRdfSnapshot swkmSnapshot = swkmAdapter.fromRdfModel(rdfModel);

            if (backendMode == GraphBackendMode.DUAL) {
                if (jenaOk) {
                    lastParityReport = parityComparator.compareRdf(swkmSnapshot, jenaSnapshot);
                } else {
                    lastParityReport = new ParityReport("rdf");
                    lastParityReport.addMismatch("jena_parse_failed");
                }
                logParityReport("RDF input read", lastParityReport);
            } else {
                lastParityReport = null;
            }

            lastEffectiveSnapshot = swkmSnapshot;
            lastEffectiveBackend = GraphBackendMode.SWKM;

            logBackendStatus("RDF input read", jenaOk);
            notifyObservers();
            return true;
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return false;
        }
    }
    
    /**
     * Write RDF data to a file
     * @param file the file to write to
     * @param Uri the URI of the model
     * @return true if the write was successful
     */
    public boolean write(String file, String Uri) {
        return rdfModel.write(file, Uri);
    }

    private boolean readUsingPreferredJenaFromFile(
            String modelFile,
            String baseUri,
            String format,
            boolean fetchAllNs,
            CanonicalRdfSnapshot jenaSnapshot,
            boolean jenaOk
    ) {
        if (jenaOk) {
            lastEffectiveSnapshot = jenaSnapshot;
            lastEffectiveBackend = GraphBackendMode.JENA;
            lastParityReport = null;
            logBackendStatus("RDF file read (JENA preferred)", jenaOk);
            notifyObservers();
            return true;
        }

        // Fallback to SWKM only when Jena cannot parse.
        long swkmStart = System.nanoTime();
        boolean swkmRead = rdfModel.read(modelFile, baseUri, format, fetchAllNs);
        lastSwkmReadExecuted = true;
        lastSwkmReadMillis = elapsedMillis(swkmStart);
        if (!swkmRead) {
            return false;
        }

        CanonicalRdfSnapshot swkmSnapshot = swkmAdapter.fromRdfModel(rdfModel);
        lastEffectiveSnapshot = swkmSnapshot;
        lastEffectiveBackend = GraphBackendMode.SWKM;
        lastParityReport = null;
        logBackendStatus("RDF file read (JENA preferred)", false);
        notifyObservers();
        return true;
    }

    private boolean readUsingPreferredJenaFromPayload(
            byte[] payload,
            String baseUri,
            String format,
            boolean fetchAllNs,
            CanonicalRdfSnapshot jenaSnapshot,
            boolean jenaOk
    ) {
        if (jenaOk) {
            lastEffectiveSnapshot = jenaSnapshot;
            lastEffectiveBackend = GraphBackendMode.JENA;
            lastParityReport = null;
            logBackendStatus("RDF input read (JENA preferred)", jenaOk);
            notifyObservers();
            return true;
        }

        // Fallback to SWKM only when Jena cannot parse.
        long swkmStart = System.nanoTime();
        boolean swkmRead = rdfModel.read(new ByteArrayInputStream(payload), baseUri, format, fetchAllNs);
        lastSwkmReadExecuted = true;
        lastSwkmReadMillis = elapsedMillis(swkmStart);
        if (!swkmRead) {
            return false;
        }

        CanonicalRdfSnapshot swkmSnapshot = swkmAdapter.fromRdfModel(rdfModel);
        lastEffectiveSnapshot = swkmSnapshot;
        lastEffectiveBackend = GraphBackendMode.SWKM;
        lastParityReport = null;
        logBackendStatus("RDF input read (JENA preferred)", false);
        notifyObservers();
        return true;
    }

    private ParseAttempt tryReadJenaFromFile(String modelFile, String baseUri, String format) {
        long start = System.nanoTime();
        try {
            return new ParseAttempt(jenaAdapter.fromFile(modelFile, baseUri, format), elapsedMillis(start));
        } catch (RuntimeException ignored) {
            return new ParseAttempt(null, elapsedMillis(start));
        }
    }

    private ParseAttempt tryReadJenaFromPayload(byte[] payload, String baseUri, String format) {
        long start = System.nanoTime();
        try {
            return new ParseAttempt(
                    jenaAdapter.fromInputStream(new ByteArrayInputStream(payload), baseUri, format),
                    elapsedMillis(start)
            );
        } catch (RuntimeException ignored) {
            return new ParseAttempt(null, elapsedMillis(start));
        }
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

    private void logBackendStatus(String operation, boolean jenaParsable) {
        String parityStatus;
        if (lastParityReport == null) {
            parityStatus = "N/A";
        } else if (lastParityReport.isEquivalent()) {
            parityStatus = "OK";
        } else {
            parityStatus = "FAIL(" + lastParityReport.getMismatches().size() + ")";
        }

        System.out.println("[BACKEND STATUS] op=" + operation
                + " configured=" + backendMode
                + " effective=" + lastEffectiveBackend
                + " jena_parsable=" + jenaParsable
                + " jena_parse_ms=" + (lastJenaParseMillis >= 0 ? lastJenaParseMillis : "N/A")
                + " swkm_read_executed=" + lastSwkmReadExecuted
                + " swkm_read_ms=" + (lastSwkmReadMillis >= 0 ? lastSwkmReadMillis : "N/A")
                + " parity=" + parityStatus);
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static final class ParseAttempt {
        private final CanonicalRdfSnapshot snapshot;
        private final long elapsedMillis;

        private ParseAttempt(CanonicalRdfSnapshot snapshot, long elapsedMillis) {
            this.snapshot = snapshot;
            this.elapsedMillis = elapsedMillis;
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
    
    /**
     * Register an observer for RDF data changes
     * @param observer the observer to register
     */
    public void registerObserver(RDFObserver observer) {
        observers.add(observer);
    }
    
    /**
     * Unregister an observer
     * @param observer the observer to unregister
     */
    public void unregisterObserver(RDFObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notify all observers of a change
     */
    private void notifyObservers() {
        for (RDFObserver observer : observers) {
            observer.onRDFDataChanged();
        }
    }
    
    /**
     * Interface for observing RDF data changes
     */
    public interface RDFObserver {
        void onRDFDataChanged();
    }
}