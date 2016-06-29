/*
 * Copyright (c) 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
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
 */
public class SffGraph {

    /**
     * Internal class to hold each SffGraph entry
     */
    public class SffGraphEntry {

        private SffName srcSff;
        private SffName dstSff;
        private SfName prevSf;
        private SfName sf;
        private String sfg;
        private long pathId;
        private short serviceIndex;

        public SffName getSrcSff() {
            return srcSff;
        }

        public SffName getDstSff() {
            return dstSff;
        }

        public SfName getSf() {
            return sf;
        }

        public SfName getPrevSf() {
            return prevSf;
        }

        public String getSfg() {
            return sfg;
        }

        public long getPathId() {
            return pathId;
        }

        public short getServiceIndex() {
            return serviceIndex;
        }

        public void setPrevSf(SfName prevSf) {
            this.prevSf = prevSf;
        }

        public SffGraphEntry(final SffName srcSff, final SffName dstSff, final SfName sf, final String sfg, long pathId,
                short serviceIndex) {
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
            return "SffGraphEntry [srcSff=" + srcSff + ", dstSff=" + dstSff + ", sf=" + sf + ", prevSf=" + prevSf
                    + ", sfg=" + sfg + ", pathId=" + pathId + ", serviceIndex=" + serviceIndex + "]";
        }
    }

    /**
     * Internal class to hold the ingress and egress DPLs for an SFF for a particular pathId
     */
    public class SffDataPlaneLocators {

        private SffName sffName;
        private long pathId;
        private SffDataPlaneLocatorName ingressDplName;
        private SffDataPlaneLocatorName egressDplName;
        // The Ingress DPL info for this hop
        private DataPlaneLocator ingressHopDpl;

        public SffDataPlaneLocators(SffName sffName, long pathId, SffDataPlaneLocatorName ingressDplName,
                SffDataPlaneLocatorName egressDplName, DataPlaneLocator hopDpl) {
            this.sffName = sffName;
            this.pathId = pathId;
            this.ingressDplName = ingressDplName;
            this.egressDplName = egressDplName;
            this.ingressHopDpl = hopDpl;
        }

        public SffName getSffName() {
            return sffName;
        }

        public long getPathId() {
            return pathId;
        }

        public SffDataPlaneLocatorName getIngressDplName() {
            return ingressDplName;
        }

        public SffDataPlaneLocatorName getEgressDplName() {
            return egressDplName;
        }

        public DataPlaneLocator getIngressHopDpl() {
            return ingressHopDpl;
        }
    }

    public static final SffName INGRESS = new SffName("ingress");
    public static final SffName EGRESS = new SffName("egress");
    private static final Logger LOG = LoggerFactory.getLogger(SffGraph.class);

    private List<SffGraphEntry> graphEntries;
    private Map<Long, Map<SffName, SffDataPlaneLocators>> pathIdToSffDataPlaneLocators;
    // Store the RSP egress DPL info
    private Map<Long, DataPlaneLocator> pathIdToPathEgressLocators;

    public SffGraph() {
        this.graphEntries = new ArrayList<SffGraphEntry>();
        this.pathIdToSffDataPlaneLocators = new HashMap<Long, Map<SffName, SffDataPlaneLocators>>();
        this.pathIdToPathEgressLocators = new HashMap<Long, DataPlaneLocator>();
    }

    //
    // Graph methods
    //
    public SffGraphEntry addGraphEntry(final SffName srcSff, final SffName dstSff, long pathId, short serviceIndex) {
        return addGraphEntry(srcSff, dstSff, null, null, pathId, serviceIndex);
    }

    public SffGraphEntry addGraphEntry(final SffName srcSff, final SffName dstSff, final SfName sf, final String sfg,
            long pathId, short serviceIndex) {
        SffGraphEntry entry = new SffGraphEntry(srcSff, dstSff, sf, sfg, pathId, serviceIndex);
        graphEntries.add(entry);

        LOG.info("SffGraphEntry addEntry srcSff [{}] dstSff [{}] sf [{}] sfg [{}] pathId [{}] serviceIndex [{}]",
                srcSff.getValue(), dstSff.getValue(), sf.getValue(), sfg, pathId, serviceIndex);

        return entry;
    }

    public Iterator<SffGraphEntry> getGraphEntryIterator() {
        return graphEntries.iterator();
    }

    public Set<Long> getSffDplKeys() {
        return pathIdToSffDataPlaneLocators.keySet();
    }

    public Map<SffName, SffDataPlaneLocators> getSffDplsForPath(long pathId) {
        return pathIdToSffDataPlaneLocators.get(pathId);
    }

    public Set<Long> getEgressLocatorKeys() {
        return pathIdToPathEgressLocators.keySet();
    }

    //
    // SFF DPL methods
    //

    public void addSffDpls(final SffName sffName, final long pathId, final SffDataPlaneLocatorName ingressDpl,
            final SffDataPlaneLocatorName egressDpl, DataPlaneLocator hopDpl) {
        SffDataPlaneLocators sffDpl = new SffDataPlaneLocators(sffName, pathId, ingressDpl, egressDpl, hopDpl);

        Map<SffName, SffDataPlaneLocators> sffToDpls = pathIdToSffDataPlaneLocators.get(pathId);

        LOG.debug("SffGraphEntry addSffDpls sff [{}] path [{}] ingressDpl [{}] egressDpl [{}]",
                sffName.getValue(), pathId, ingressDpl.getValue(), egressDpl.getValue());

        if (sffToDpls == null) {
            sffToDpls = new HashMap<SffName, SffDataPlaneLocators>();
            pathIdToSffDataPlaneLocators.put(pathId, sffToDpls);
        }

        sffToDpls.put(sffName, sffDpl);
    }

    private SffDataPlaneLocators getSffDpl(final SffName sffName, final long pathId) {
        return getSffDpl(sffName, pathId, false);
    }

    private SffDataPlaneLocators getSffDpl(final SffName sffName, final long pathId, boolean createEntry) {
        Map<SffName, SffDataPlaneLocators> sffToDpls = pathIdToSffDataPlaneLocators.get(pathId);
        if (sffToDpls == null) {
            if (!createEntry) {
                LOG.debug("SffGraph getSffDpl cant find sffToDpls list for sff [{}] path [{}]",
                        sffName.getValue(), pathId);

                return null;
            }

            sffToDpls = new HashMap<SffName, SffDataPlaneLocators>();
            pathIdToSffDataPlaneLocators.put(pathId, sffToDpls);
        }

        SffDataPlaneLocators sffDpl = sffToDpls.get(sffName);
        if (sffDpl == null && createEntry) {
            sffDpl = new SffDataPlaneLocators(sffName, pathId, null, null, null);
            sffToDpls.put(sffName, sffDpl);
        }

        return sffDpl;
    }

    public void setSffIngressDpl(final SffName sffName, final long pathId,
            final SffDataPlaneLocatorName ingressDplName) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId, true);
        LOG.debug("setSffIngressDpl sff [{}] pathId [{}] dpl [{}]", sffName.getValue(), pathId, ingressDplName);
        sffDpl.ingressDplName = ingressDplName;
    }

    public void setSffEgressDpl(final SffName sffName, final long pathId, final SffDataPlaneLocatorName egressDplName) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId, true);
        LOG.debug("setSffEgressDpl sff [{}] pathId [{}] dpl [{}]", sffName.getValue(), pathId, egressDplName);
        sffDpl.egressDplName = egressDplName;
    }

    public SffDataPlaneLocatorName getSffIngressDpl(final SffName sffName, final long pathId) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId);

        if (sffDpl == null) {
            LOG.debug("SffGraph getSffIngressDpl cant find sffDpl for sff [{}] path [{}]", sffName.getValue(), pathId);
            return null;
        }

        return sffDpl.getIngressDplName();
    }

    public SffDataPlaneLocatorName getSffEgressDpl(final SffName sffName, final long pathId) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId);

        if (sffDpl == null) {
            LOG.debug("SffGraph getSffEgressDpl cant find sffDpl for sff [{}] path [{}]", sffName.getValue(), pathId);
            return null;
        }

        return sffDpl.getEgressDplName();
    }

    public void setHopIngressDpl(final SffName sffName, final long pathId, DataPlaneLocator ingressHopDpl) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId, true);
        sffDpl.ingressHopDpl = ingressHopDpl;
    }

    public DataPlaneLocator getHopIngressDpl(final SffName sffName, final long pathId) {
        SffDataPlaneLocators sffDpl = getSffDpl(sffName, pathId);

        if (sffDpl == null) {
            LOG.debug("SffGraph getSffIngressDpl cant find sffDpl for sff [{}] path [{}]", sffName.getValue(), pathId);
            return null;
        }

        return sffDpl.getIngressHopDpl();
    }

    public void logDpls() {
        LOG.info("SffGraph.logDpls [{}] entries", pathIdToSffDataPlaneLocators.size());
        // Print the SFF DPLs
        Set<Long> dplKeys = this.getSffDplKeys();
        for (Long key : dplKeys) {
            Map<SffName, SffDataPlaneLocators> sffDpls = this.getSffDplsForPath(key);
            Set<SffName> sffDplKeys = sffDpls.keySet();
            for (SffName sffDplKey : sffDplKeys) {
                SffDataPlaneLocators sffDpl = sffDpls.get(sffDplKey);
                // TODO need to get the locator details for any/all transport
                LOG.info(
                        "SFF [{}] pathId [{}] IngressDpl [{}] EgressDpl [{}] IngressHopDpl Transport [{}] Vlan ID [{}]",
                        sffDpl.getSffName(), sffDpl.getPathId(), sffDpl.getIngressDplName(), sffDpl.getEgressDplName(),
                        ((sffDpl.getIngressHopDpl() == null) ? "null" : sffDpl.getIngressHopDpl()
                            .getTransport()
                            .getName()),
                        // ((Mpls)sffDpl.getIngressHopDpl().getLocatorType()).getMplsLabel());
                        ((sffDpl
                            .getIngressHopDpl() == null) ? "null" : ((Mac) sffDpl.getIngressHopDpl().getLocatorType())
                                .getVlanId()));
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
        LOG.info("SffGraph.logEgressDpls [{}] entries", pathIdToPathEgressLocators.size());
        // Print the Path Egress DPLs
        Set<Long> egressDplKeys = this.getEgressLocatorKeys();
        for (Long pathId : egressDplKeys) {
            DataPlaneLocator dpl = this.getPathEgressDpl(pathId);
            // TODO need to get the locator details for any/all transport
            LOG.info("Path Egress DPL pathId [{}] Dpl Transport [{}] Vlan ID [{}]", pathId,
                    dpl.getTransport().getName(), ((Mac) dpl.getLocatorType()).getVlanId());
        }

    }
}
