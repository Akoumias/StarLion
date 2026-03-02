package mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import service.RDFService;

/**
 * Verifies Jena-only behavior with SWKM fallback on unsupported input.
 *
 * @author jakoum
 */
public class RDFServiceJenaOnlyFallbackTest {

    @Test
    public void jenaModeSkipsSwkmWhenParsable() {
        RDFService service = new RDFService();
        service.setBackendMode(GraphBackendMode.JENA);

        boolean readOk = service.read("SampleRDFFiles/testFile.rdf", "SampleRDFFiles/testFile.rdf#", "RDF/XML", true);
        assertTrue("Expected supported RDF file to load", readOk);
        assertEquals("Expected effective backend to remain JENA", GraphBackendMode.JENA, service.getLastEffectiveBackend());
        assertFalse("SWKM should not execute when Jena can parse", service.wasLastSwkmReadExecuted());
    }

    @Test
    public void jenaModeFallsBackToSwkmWhenUnsupported() {
        RDFService service = new RDFService();
        service.setBackendMode(GraphBackendMode.JENA);

        boolean readOk = service.read("SampleRDFFiles/hybrid.rdf", "SampleRDFFiles/hybrid.rdf#", "RDF/XML", true);
        assertTrue("Expected fallback to SWKM for Jena-unsupported RDF", readOk);
        assertEquals("Expected effective backend to switch to SWKM fallback", GraphBackendMode.SWKM, service.getLastEffectiveBackend());
        assertTrue("SWKM should execute for Jena-unsupported input", service.wasLastSwkmReadExecuted());
    }
}
