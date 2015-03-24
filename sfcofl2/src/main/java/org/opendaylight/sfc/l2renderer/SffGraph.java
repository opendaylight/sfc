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

    /**
     * Internal class to hold the ingress and egress DPLs for an SFF for a particular pathId
     */
    public class SffDataPlaneLocators {
        private String sffName;
        private long pathId;
        private String ingressDplName;
        private String egressDplName;
        private DataPlaneLocator hopIngressDpl;

        public SffDataPlaneLocators(String sffName, long pathId, String ingressDplName, String egressDplName, DataPlaneLocator hopDpl) {
            this.sffName = sffName;
            this.pathId = pathId;
            this.ingressDplName = ingressDplName;
            this.egressDplName = egressDplName;
            this.hopIngressDpl = hopDpl;
        }
        public String getSffName()        { return sffName;        }
        public long getPathId()           { return pathId;         }
        public String getIngressDplName() { return ingressDplName; }
        public String getEgressDplName()  { return egressDplName;  }
        public DataPlaneLocator getHopIngressDpl() { return hopIngressDpl; }
    }

    public static final String INGRESS = "ingress";
    public static final String EGRESS  = "egress";
    private static final Logger LOG = LoggerFactory.getLogger(SffGraph.class);

    private List<SffGraphEntry> graphEntries;
    private Map<Long, Map<String, SffDataPlaneLocators>> pathIdTosffDataPlaneLocators;
    private Map<Long, DataPlaneLocator> pathIdToPathEgressLocators;

    public SffGraph() {
        this.graphEntries = new ArrayList<SffGraphEntry>();
        this.pathIdTosffDataPlaneLocators = new HashMap<Long, Map<String, SffDataPlaneLocators>>();
        this.pathIdToPathEgressLocators = new HashMap<Long, DataPlaneLocator>();
    }

    public void setPathEgressDpl(long pathId, DataPlaneLocator dpl) {
        this.pathIdToPathEgressLocators.put(pathId, dpl);
    }

    public DataPlaneLocator getPathEgressDpl(long pathId) {
        return this.pathIdToPathEgressLocators.get(pathId);
    }

    //
    // Graph methods
    //

    public void addGraphEntry(final String srcSff, final String dstSff, long pathId) {
        addGraphEntry(srcSff, dstSff, null, pathId);
    }

    public void addGraphEntry(final String srcSff, final String dstSff, final String sf, long pathId) {
        SffGraphEntry entry = new SffGraphEntry(srcSff, dstSff, sf, pathId);
        graphEntries.add(entry);

        LOG.info("SffGraphEntry addEntry srcSff [{}] dstSff [{}] sf [{}] pathId [{}]",
                srcSff, dstSff, sf, pathId);
    }

    public Iterator<SffGraphEntry> iterator() {
        return graphEntries.iterator();
    }

    //
    // SFF DPL methods
    //

    public void addSffDpls(final String sffName, final long pathId, final String ingressDpl, final String egressDpl, DataPlaneLocator hopDpl) {
        SffDataPlaneLocators sffDpl = new SffDataPlaneLocators(sffName, pathId, ingressDpl, egressDpl, hopDpl);

        Map<String, SffDataPlaneLocators> sffToDpls = pathIdTosffDataPlaneLocators.get(pathId);

        LOG.info("SffGraphEntry addSffDpls sff [{}] path [{}] ingressDpl [{}] egressDpl [{}]",
                sffName, pathId, ingressDpl, egressDpl);

        if(sffToDpls == null) {
            sffToDpls = new HashMap<String, SffDataPlaneLocators>();
            pathIdTosffDataPlaneLocators.put(pathId, sffToDpls);
        }

        sffToDpls.put(sffName, sffDpl);
    }

    private SffDataPlaneLocators getSffDpl(final String sffName, final long pathId) {
        Map<String, SffDataPlaneLocators> sffToDpls = pathIdTosffDataPlaneLocators.get(pathId);
        if(sffToDpls == null) {
            LOG.info("SffGraph getSffDpl cant find sffToDpls list for sff [{}] path [{}]", sffName, pathId);

            return null;
        }

        return sffToDpls.get(sffName);
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

    public DataPlaneLocator getHopIngressDpl(final String sffName, final long pathId) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId);

        if(sffDpl == null) {
            LOG.info("SffGraph getSffIngressDpl cant find sffDpl for sff [{}] path [{}]", sffName, pathId);
            return null;
        }

        return sffDpl.getHopIngressDpl();
    }
}
