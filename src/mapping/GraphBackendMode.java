package mapping;

/**
 * Backend mode switch for SWKM/Jena migration.
 *
 * @author jakoum
 */
public enum GraphBackendMode {
    SWKM,
    DUAL,
    JENA;

    public static GraphBackendMode resolveDefault() {
        String value = System.getProperty("starlion.backend.mode");
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv("STARLION_BACKEND_MODE");
        }
        if (value == null || value.trim().isEmpty()) {
            return JENA;
        }

        String normalized = value.trim().toUpperCase();
        if ("SWKM".equals(normalized)) {
            return SWKM;
        }
        if ("DUAL".equals(normalized)) {
            return DUAL;
        }
        if ("JENA".equals(normalized)) {
            return JENA;
        }
        return JENA;
    }
}
