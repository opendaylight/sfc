package org.opendaylight.sfc.l2renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
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
        private String prevSf;
        private String sf;
        private String sfg;
        private long pathId;
        private short serviceIndex;
        public String getSrcSff() { return srcSff; }
        public String getDstSff() { return dstSff; }
        public String getSf()     { return sf;     }
        public String getPrevSf() { return prevSf; }
        public String getSfg()     { return sfg;   }
        public long   getPathId() { return pathId; }
        public short  getServiceIndex() { return serviceIndex; }
        public void   setPrevSf(String prevSf) { this.prevSf = prevSf; }
        public SffGraphEntry(final String srcSff, final String dstSff, final String sf, final String sfg, long pathId, short serviceIndex) {
            this.srcSff = srcSff;
            this.dstSff = dstSff;
            this.sf = sf;
            this.sfg = sfg;
            this.pathId = pathId;
            this.serviceIndex = serviceIndex;
            this.prevSf = null;
        }
        @Override
        public String toString() {
            return "SffGraphEntry [srcSff=" + srcSff +
                    ", dstSff=" + dstSff +
                    ", sf=" + sf +
                    ", prevSf=" + prevSf +
                    ", sfg=" + sfg +
                    ", pathId=" + pathId +
                    ", serviceIndex=" + serviceIndex +
                    "]";
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
    public SffGraphEntry addGraphEntry(final String srcSff, final String dstSff, long pathId, short serviceIndex) {
        return addGraphEntry(srcSff, dstSff, null, null, pathId, serviceIndex);
    }

    public SffGraphEntry addGraphEntry(final String srcSff, final String dstSff, final String sf, final String sfg, long pathId, short serviceIndex) {
        SffGraphEntry entry = new SffGraphEntry(srcSff, dstSff, sf, sfg, pathId, serviceIndex);
        graphEntries.add(entry);

        LOG.info("SffGraphEntry addEntry srcSff [{}] dstSff [{}] sf [{}] sfg [{}] pathId [{}] serviceIndex [{}]",
                srcSff, dstSff, sf, sfg, pathId, serviceIndex);

        return entry;
    }

    public Iterator<SffGraphEntry> getGraphEntryIterator() {
        return graphEntries.iterator();
    }

    public Set<Long> getSffDplKeys() {
        return pathIdToSffDataPlaneLocators.keySet();
    }

    public Map<String, SffDataPlaneLocators> getSffDplsForPath(long pathId) {
        return pathIdToSffDataPlaneLocators.get(pathId);
    }

    public Set<Long> getEgressLocatorKeys() {
        return pathIdToPathEgressLocators.keySet();
    }

    //
    // SFF DPL methods
    //

    public void addSffDpls(final String sffName, final long pathId, final String ingressDpl, final String egressDpl, DataPlaneLocator hopDpl) {
        SffDataPlaneLocators sffDpl = new SffDataPlaneLocators(sffName, pathId, ingressDpl, egressDpl, hopDpl);

        Map<String, SffDataPlaneLocators> sffToDpls = pathIdToSffDataPlaneLocators.get(pathId);

        LOG.debug("SffGraphEntry addSffDpls sff [{}] path [{}] ingressDpl [{}] egressDpl [{}]",
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
                LOG.debug("SffGraph getSffDpl cant find sffToDpls list for sff [{}] path [{}]", sffName, pathId);

                return null;
            }

            sffToDpls = new HashMap<String, SffDataPlaneLocators>();
            pathIdToSffDataPlaneLocators.put(pathId, sffToDpls);
        }

        SffDataPlaneLocators sffDpl = sffToDpls.get(sffName);
        if(sffDpl == null && createEntry) {
            sffDpl = new SffDataPlaneLocators(sffName, pathId, null, null, null);
            sffToDpls.put(sffName, sffDpl);
        }

        return sffDpl;
    }

    public void setSffIngressDpl(final String sffName, final long pathId, final String ingressDplName) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId, true);
        LOG.debug("setSffIngressDpl sff [{}] pathId [{}] dpl [{}]", sffName, pathId, ingressDplName);
        sffDpl.ingressDplName = ingressDplName;
    }

    public void setSffEgressDpl(final String sffName, final long pathId, final String egressDplName) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId, true);
        LOG.debug("setSffEgressDpl sff [{}] pathId [{}] dpl [{}]", sffName, pathId, egressDplName);
        sffDpl.egressDplName = egressDplName;
    }

    public String getSffIngressDpl(final String sffName, final long pathId) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId);

        if(sffDpl == null) {
            LOG.debug("SffGraph getSffIngressDpl cant find sffDpl for sff [{}] path [{}]", sffName, pathId);
            return null;
        }

        return sffDpl.getIngressDplName();
    }

    public String getSffEgressDpl(final String sffName, final long pathId) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId);

        if(sffDpl == null) {
            LOG.debug("SffGraph getSffEgressDpl cant find sffDpl for sff [{}] path [{}]", sffName, pathId);
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
            LOG.debug("SffGraph getSffIngressDpl cant find sffDpl for sff [{}] path [{}]", sffName, pathId);
            return null;
        }

        return sffDpl.getIngressHopDpl();
    }

    public void logDpls() {
        // Print the SFF DPLs
        Set<Long> dplKeys = this.getSffDplKeys();
        for(Long key : dplKeys) {
            Map<String, SffDataPlaneLocators> sffDpls = this.getSffDplsForPath(key);
            Set<String> sffDplKeys = sffDpls.keySet();
            for(String sffDplKey : sffDplKeys) {
                SffDataPlaneLocators sffDpl = sffDpls.get(sffDplKey);
                // TODO need to get the locator details for any/all transport
                LOG.info("SFF [{}] pathId [{}] IngressDpl [{}] EgressDpl [{}] IngressHopDpl Transport [{}] Vlan ID [{}]",
                        sffDpl.getSffName(), sffDpl.getPathId(), sffDpl.getIngressDplName(), sffDpl.getEgressDplName(),
                        ((sffDpl.getIngressHopDpl() == null) ? "null" : sffDpl.getIngressHopDpl().getTransport().getName()),
                        //((Mpls)sffDpl.getIngressHopDpl().getLocatorType()).getMplsLabel());
                        ((sffDpl.getIngressHopDpl() == null) ? "null" : ((Mac)sffDpl.getIngressHopDpl().getLocatorType()).getVlanId()));
            }
        }

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

    public void logEgressDpls() {
        // Print the Path Egress DPLs
        Set<Long> egressDplKeys = this.getEgressLocatorKeys();
        for(Long pathId : egressDplKeys) {
            DataPlaneLocator dpl = this.getPathEgressDpl(pathId);
            // TODO need to get the locator details for any/all transport
            LOG.info("Path Egress DPL pathId [{}] Dpl Transport [{}] Vlan ID [{}]",
                     pathId,
                     dpl.getTransport().getName(),
                     ((Mac)dpl.getLocatorType()).getVlanId());
        }

    }
}
