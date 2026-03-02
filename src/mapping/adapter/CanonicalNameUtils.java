package mapping.adapter;

/**
 * URI and label normalization helpers for canonical mapping.
 *
 * @author jakoum
 */
final class CanonicalNameUtils {

    private CanonicalNameUtils() {
    }

    /**
     * Normalizes URIs/paths so SWKM and Jena produce comparable identifiers.
     */
    static String normalizeUri(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        if (trimmed.startsWith("file:")) {
            try {
                java.net.URI uri = java.net.URI.create(trimmed);
                String path = uri.getPath();
                if (path != null && !path.isEmpty()) {
                    String fragment = uri.getFragment();
                    String normalizedPath = normalizeLocalPath(path);
                    return fragment == null ? normalizedPath : normalizedPath + "#" + fragment;
                }
            } catch (IllegalArgumentException ignored) {
                // Fall back to raw input when URI parsing fails.
            }
        }
        return normalizeLocalPath(trimmed);
    }

    private static String normalizeLocalPath(String value) {
        String normalized = value.replace('\\', '/');

        // Align absolute local paths with SWKM-relative project URIs for parity.
        int sampleIndex = normalized.indexOf("SampleRDFFiles/");
        if (sampleIndex >= 0) {
            return normalized.substring(sampleIndex);
        }

        String cwd = System.getProperty("user.dir");
        if (cwd != null && !cwd.isEmpty()) {
            String cwdUnix = cwd.replace('\\', '/');
            if (normalized.startsWith(cwdUnix + "/")) {
                return normalized.substring(cwdUnix.length() + 1);
            }
        }
        return normalized;
    }

    /**
     * Extracts a stable local token from a URI/path for node/edge ids.
     */
    static String localName(String value) {
        String normalized = normalizeUri(value);
        if (normalized.isEmpty()) {
            return "";
        }
        int hashIndex = normalized.lastIndexOf('#');
        int slashIndex = normalized.lastIndexOf('/');
        int index = Math.max(hashIndex, slashIndex);
        if (index >= 0 && index + 1 < normalized.length()) {
            return normalized.substring(index + 1);
        }
        return normalized;
    }

    /**
     * Returns the namespace portion of a URI/path (including trailing separator).
     */
    static String namespace(String value) {
        String normalized = normalizeUri(value);
        if (normalized.isEmpty()) {
            return "";
        }
        int hashIndex = normalized.lastIndexOf('#');
        if (hashIndex >= 0) {
            return normalized.substring(0, hashIndex + 1);
        }
        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0) {
            return normalized.substring(0, slashIndex + 1);
        }
        return "";
    }
}
