package org.opendaylight.sfc.l2renderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(SffGraph.class);

    /**
     * Internal class to hold each SffGraph entry
     */
    public class SffGraphEntry {
        private String srcSff;
        private String dstSff;
        private String sf;
        private long pathId;
        private short serviceIndex;
        public String getSrcSff() { return srcSff; }
        public String getDstSff() { return dstSff; }
        public String getSf()     { return sf;     }
        public long   getPathId() { return pathId; }
        public short  getServiceIndex() { return serviceIndex; }
        public SffGraphEntry(final String srcSff, final String dstSff, final String sf, long pathId, short serviceIndex) {
            this.srcSff = srcSff;
            this.dstSff = dstSff;
            this.sf = sf;
            this.pathId = pathId;
            this.serviceIndex = serviceIndex;
        }
    }

    private List<SffGraphEntry> graphEntries;

    public SffGraph() {
        graphEntries = new ArrayList<SffGraphEntry>();
    }

    public void addEntry(final String srcSff, final String dstSff, long pathId, short serviceIndex) {
        addEntry(srcSff, dstSff, null, pathId, serviceIndex);
    }

    public void addEntry(final String srcSff, final String dstSff, final String sf, long pathId, short serviceIndex) {
        SffGraphEntry entry = new SffGraphEntry(srcSff, dstSff, sf, pathId, serviceIndex);
        graphEntries.add(entry);

        LOG.info("SffGraphEntry addEntry srcSff [{}] dstSff [{}] sf [{}] pathId [{}] serviceIndex [{}]",
                srcSff, dstSff, sf, pathId, serviceIndex);
    }

    public Iterator<SffGraphEntry> iterator() {
        return graphEntries.iterator();
    }
}
