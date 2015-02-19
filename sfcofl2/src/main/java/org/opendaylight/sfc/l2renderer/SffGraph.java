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
        private String sf;
        private long pathId;
        public String getSrcSff() { return srcSff; }
        public String getDstSff() { return dstSff; }
        public String getSf()     { return sf;     }
        public long   getPathId() { return pathId; }
        public SffGraphEntry(final String srcSff, final String dstSff, final String sf, long pathId) {
            this.srcSff = srcSff;
            this.dstSff = dstSff;
            this.sf = sf;
            this.pathId = pathId;
        }
    }

    private List<SffGraphEntry> graphEntries;

    public SffGraph() {
        graphEntries = new ArrayList<SffGraphEntry>();
    }

    public void addEntry(final String srcSff, final String dstSff, long pathId) {
        addEntry(srcSff, dstSff, null, pathId);
    }

    public void addEntry(final String srcSff, final String dstSff, final String sf, long pathId) {
        SffGraphEntry entry = new SffGraphEntry(srcSff, dstSff, sf, pathId);
        graphEntries.add(entry);
    }

    public Iterator<SffGraphEntry> iterator() {
        return graphEntries.iterator();
    }
}
