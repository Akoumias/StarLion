package mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Report for SWKM/Jena parity checks.
 *
 * @author jakoum
 */
public class ParityReport {

    private final String domain;
    private final List<String> mismatches = new ArrayList<>();

    public ParityReport(String domain) {
        this.domain = domain;
    }

    public void addMismatch(String mismatch) {
        mismatches.add(mismatch);
    }

    public boolean isEquivalent() {
        return mismatches.isEmpty();
    }

    public String getDomain() {
        return domain;
    }

    public List<String> getMismatches() {
        return Collections.unmodifiableList(mismatches);
    }
}
