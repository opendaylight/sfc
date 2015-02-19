package org.opendaylight.sfc.l2renderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class stores a simple Connection Graph of how the
 * SFFs (Service Function Forwarders) are interconnected.
 * 
 * @author Brady Johnson brady.allen.johnson@ericsson.com
 *
 */
public class SffGraph {
    public static final String INGRESS = "ingress";
    public static final String EGRESS  = "egress";

    /**
     * Internal class to hold each SffGraph entry
     */
    public class SffGraphEntry {
        private String srcSff;
        private String dstSff;
        private String toSf;
        public String getSrcSff() { return srcSff; }
        public String getDstSff() { return dstSff; }
        public String getToSf()   { return toSf;   }
        public SffGraphEntry(final String srcSff, final String dstSff, final String toSf) {
            this.srcSff = srcSff;
            this.dstSff = dstSff;
            this.toSf = toSf;
        }
    }

    private List<SffGraphEntry> graphEntries;

    public SffGraph() {
        graphEntries = new ArrayList<SffGraphEntry>();
    }

    public void addEntry(final String srcSff, final String dstSff) {
        addEntry(srcSff, dstSff, null);
    }

    public void addEntry(final String srcSff, final String dstSff, final String toSf) {
        SffGraphEntry entry = new SffGraphEntry(srcSff, dstSff, toSf);
        graphEntries.add(entry);
    }

    public Iterator<SffGraphEntry> iterator() {
        return graphEntries.iterator();
    }
}
