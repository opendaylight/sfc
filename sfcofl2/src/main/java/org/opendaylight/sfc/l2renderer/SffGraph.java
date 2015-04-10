package org.opendaylight.sfc.l2renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class stores a simple Connection Graph of how the
 * SFFs (Service Function Forwarders) are interconnected.
 * Additionally, it stores SFF ingress and egress DPLs (Data Plane Locators)
 * for a particular PathId
 *
 * @author Brady Johnson brady.allen.johnson@ericsson.com
 *
 */
public class SffGraph {

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

    /**
     * Internal class to hold the ingress and egress DPLs for an SFF for a particular pathId
     */
    public class SffDataPlaneLocators {
        private String sffName;
        private long pathId;
        private String ingressDplName;
        private String egressDplName;
        // The Ingress DPL info for this hop
        private DataPlaneLocator ingressHopDpl;

        public SffDataPlaneLocators(String sffName, long pathId, String ingressDplName, String egressDplName, DataPlaneLocator hopDpl) {
            this.sffName = sffName;
            this.pathId = pathId;
            this.ingressDplName = ingressDplName;
            this.egressDplName = egressDplName;
            this.ingressHopDpl = hopDpl;
        }
        public String getSffName()        { return sffName;        }
        public long getPathId()           { return pathId;         }
        public String getIngressDplName() { return ingressDplName; }
        public String getEgressDplName()  { return egressDplName;  }
        public DataPlaneLocator getIngressHopDpl() { return ingressHopDpl; }
    }

    public static final String INGRESS = "ingress";
    public static final String EGRESS  = "egress";
    private static final Logger LOG = LoggerFactory.getLogger(SffGraph.class);

    private List<SffGraphEntry> graphEntries;
    private Map<Long, Map<String, SffDataPlaneLocators>> pathIdToSffDataPlaneLocators;
    // Store the RSP egress DPL info
    private Map<Long, DataPlaneLocator> pathIdToPathEgressLocators;

    public SffGraph() {
        this.graphEntries = new ArrayList<SffGraphEntry>();
        this.pathIdToSffDataPlaneLocators = new HashMap<Long, Map<String, SffDataPlaneLocators>>();
        this.pathIdToPathEgressLocators = new HashMap<Long, DataPlaneLocator>();
    }

    //
    // Graph methods
    //
    public void addGraphEntry(final String srcSff, final String dstSff, long pathId, short serviceIndex) {
        addGraphEntry(srcSff, dstSff, null, pathId, serviceIndex);
    }

    public void addGraphEntry(final String srcSff, final String dstSff, final String sf, long pathId, short serviceIndex) {
        SffGraphEntry entry = new SffGraphEntry(srcSff, dstSff, sf, pathId, serviceIndex);
        graphEntries.add(entry);

        LOG.info("SffGraphEntry addEntry srcSff [{}] dstSff [{}] sf [{}] pathId [{}] serviceIndex [{}]",
                srcSff, dstSff, sf, pathId, serviceIndex);
    }

    public Iterator<SffGraphEntry> iterator() {
        return graphEntries.iterator();
    }


    //
    // SFF DPL methods
    //

    public void addSffDpls(final String sffName, final long pathId, final String ingressDpl, final String egressDpl, DataPlaneLocator hopDpl) {
        SffDataPlaneLocators sffDpl = new SffDataPlaneLocators(sffName, pathId, ingressDpl, egressDpl, hopDpl);

        Map<String, SffDataPlaneLocators> sffToDpls = pathIdToSffDataPlaneLocators.get(pathId);

        LOG.info("SffGraphEntry addSffDpls sff [{}] path [{}] ingressDpl [{}] egressDpl [{}]",
                sffName, pathId, ingressDpl, egressDpl);

        if(sffToDpls == null) {
            sffToDpls = new HashMap<String, SffDataPlaneLocators>();
            pathIdToSffDataPlaneLocators.put(pathId, sffToDpls);
        }

        sffToDpls.put(sffName, sffDpl);
    }

    private SffDataPlaneLocators getSffDpl(final String sffName, final long pathId) {
        return getSffDpl(sffName, pathId, false);
    }

    private SffDataPlaneLocators getSffDpl(final String sffName, final long pathId, boolean createEntry) {
        Map<String, SffDataPlaneLocators> sffToDpls = pathIdToSffDataPlaneLocators.get(pathId);
        if(sffToDpls == null) {
            if(!createEntry) {
                LOG.info("SffGraph getSffDpl cant find sffToDpls list for sff [{}] path [{}]", sffName, pathId);

                return null;
            }
            sffToDpls = new HashMap<String, SffDataPlaneLocators>();
            pathIdToSffDataPlaneLocators.put(pathId, sffToDpls);
        }

        return sffToDpls.get(sffName);
    }

    public void setSffIngressDpl(final String sffName, final long pathId, final String ingressDplName) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId, true);
        sffDpl.ingressDplName = ingressDplName;
    }

    public void setSffEgressDpl(final String sffName, final long pathId, final String egressDplName) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId, true);
        sffDpl.egressDplName = egressDplName;
    }

    public String getSffIngressDpl(final String sffName, final long pathId) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId);

        if(sffDpl == null) {
            LOG.info("SffGraph getSffIngressDpl cant find sffDpl for sff [{}] path [{}]", sffName, pathId);
            return null;
        }

        return sffDpl.getIngressDplName();
    }

    public String getSffEgressDpl(final String sffName, final long pathId) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId);

        if(sffDpl == null) {
            LOG.info("SffGraph getSffEgressDpl cant find sffDpl for sff [{}] path [{}]", sffName, pathId);
            return null;
        }

        return sffDpl.getEgressDplName();
    }

    public void setHopIngressDpl(final String sffName, final long pathId, DataPlaneLocator ingressHopDpl) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId, true);
        sffDpl.ingressHopDpl = ingressHopDpl;
    }

    public DataPlaneLocator getHopIngressDpl(final String sffName, final long pathId) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId);

        if(sffDpl == null) {
            LOG.info("SffGraph getSffIngressDpl cant find sffDpl for sff [{}] path [{}]", sffName, pathId);
            return null;
        }

        return sffDpl.getIngressHopDpl();
    }

    //
    // RSP path Egress DPL methods
    //

    public void setPathEgressDpl(long pathId, DataPlaneLocator dpl) {
        this.pathIdToPathEgressLocators.put(pathId, dpl);
    }

    public DataPlaneLocator getPathEgressDpl(long pathId) {
        return this.pathIdToPathEgressLocators.get(pathId);
    }
}
