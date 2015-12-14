/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mpls;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerInterface;

public abstract class SfcRspTransportProcessorBase {
    protected static final Logger LOG = LoggerFactory.getLogger(SfcRspTransportProcessorBase.class);
    protected SfcL2FlowProgrammerInterface sfcFlowProgrammer;
    protected SfcL2BaseProviderUtils sfcProviderUtils;
    protected RenderedServicePath rsp;
    protected SffGraph sffGraph;

    private static final String FUNCTION = "function";
    private static final String IP = "ip";
    private static final String LISP = "lisp";
    private static final String MAC = "mac";
    private static final String MPLS = "mpls";

    public SfcRspTransportProcessorBase() {
        this.rsp = null;
        this.sfcProviderUtils = null;
        this.sfcFlowProgrammer = null;
        this.sffGraph = null;
    }

    public SfcRspTransportProcessorBase(
            RenderedServicePath rsp,
            SfcL2BaseProviderUtils sfcProviderUtils,
            SfcL2FlowProgrammerInterface sfcFlowProgrammer,
            SffGraph sffGraph) {
        this.rsp = rsp;
        this.sfcProviderUtils = sfcProviderUtils;
        this.sfcFlowProgrammer = sfcFlowProgrammer;
        this.sffGraph = sffGraph;
    }

    // Dependency injectors
    public void setRsp(RenderedServicePath rsp) {
        this.rsp = rsp;
    }

    public void setFlowProgrammer(SfcL2FlowProgrammerInterface sfcFlowProgrammer) {
        this.sfcFlowProgrammer = sfcFlowProgrammer;
    }

    public void setSfcProviderUtils(SfcL2BaseProviderUtils sfcProviderUtils) {
        this.sfcProviderUtils = sfcProviderUtils;
    }

    public void setSffGraph(SffGraph sffGraph) {
        this.sffGraph = sffGraph;
    }

    public abstract void setRspTransports();

    //
    // Abstract Flow programming methods
    //
    public abstract void configureSfTransportIngressFlow(SffGraph.SffGraphEntry entry);
    public abstract void configureSffTransportIngressFlow(SffGraph.SffGraphEntry entry);

    public abstract void configureSfPathMapperFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator sfDpl);
    public abstract void configureSffPathMapperFlow(SffGraph.SffGraphEntry entry, DataPlaneLocator hopDpl);

    public abstract void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl);
    public abstract void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SffDataPlaneLocator dstSffDpl);
    public abstract void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SfDataPlaneLocator dstSfDpl);
    public abstract void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl);

    public abstract void configureSfTransportEgressFlow(
            SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl, DataPlaneLocator hopDpl);
    public abstract void configureSffTransportEgressFlow(
            SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl, DataPlaneLocator hopDpl);

    public void processSffDpls() {
        // Iterate the entries in the SFF Graph
        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
                continue;
            }
            LOG.debug("processSffDpl - handling entry {}", entry);
            ServiceFunctionForwarder srcSff =
                    sfcProviderUtils.getServiceFunctionForwarder(entry.getSrcSff(), entry.getPathId());
            if (srcSff == null) {
                throw new RuntimeException("processSffDpls srcSff is null [" + entry.getSrcSff() + "]");
            }

            // may be null if its EGRESS
            ServiceFunctionForwarder dstSff =
                    sfcProviderUtils.getServiceFunctionForwarder(entry.getDstSff(), entry.getPathId());
            if (dstSff != null) {
                // Set the SFF-SFF Hop DPL
                if (!setSffHopDataPlaneLocators(srcSff, dstSff, rsp.getTransportType().getName(), entry.getPathId(), sffGraph)) {
                    throw new RuntimeException(
                            "Unable to get SFF HOP DPLs srcSff [" + entry.getSrcSff() + "] dstSff [" + entry.getDstSff()
                                    + "] transport [" + rsp.getTransportType() + "] pathId [" + entry.getPathId() + "]");
                }
            }

            if (entry.getDstSff().equals(SffGraph.EGRESS)) {
                // The srcSff ingress DPL was set in the previous loop
                // iteration, now we need to set its egress DPL
                SffDataPlaneLocator srcSffIngressDpl = sfcProviderUtils.getSffDataPlaneLocator(srcSff,
                        sffGraph.getSffIngressDpl(entry.getSrcSff(), entry.getPathId()));
                if (!setSffRemainingHopDataPlaneLocator(srcSff, rsp.getTransportType().getName(), srcSffIngressDpl, true, entry.getPathId(),
                        sffGraph)) {
                    throw new RuntimeException("Unable to get SFF egress DPL srcSff [" + entry.getSrcSff()
                            + "] transport [" + rsp.getTransportType() + "] pathId [" + entry.getPathId() + "]");
                }
            } else {
                // The srcSff egress DPL was just set above, now set its ingress DPL
                SffDataPlaneLocator srcSffEgressDpl = sfcProviderUtils.getSffDataPlaneLocator(srcSff,
                        sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
                if (!setSffRemainingHopDataPlaneLocator(srcSff, rsp.getTransportType().getName(), srcSffEgressDpl, false, entry.getPathId(),
                        sffGraph)) {
                    throw new RuntimeException("Unable to get SFF HOP ingress DPL srcSff [" + entry.getSrcSff()
                            + "] transport [" + rsp.getTransportType() + "] pathId [" + entry.getPathId() + "]");
                }
            }
        }
    }

    /*
     * For the given sff that has either the ingress or egress DPL set, as indicated
     * by ingressDplSet, iterate the SFF DPLs looking for the one that has the same
     * transportType as the already set DPL. Once found set it as the SFF ingress/egress DPL.
     * For example, if ingressDplSet is true, then the SFF ingress DPL has already been
     * set, so set the SFF egress DPL that has the same transportType as the ingress DPL.
     */
    private boolean setSffRemainingHopDataPlaneLocator(final ServiceFunctionForwarder sff, final String rspTransport,
            SffDataPlaneLocator alreadySetSffDpl, boolean ingressDplSet, final long pathId, SffGraph sffGraph) {
        List<SffDataPlaneLocator> sffDplList = sff.getSffDataPlaneLocator();
        if (sffDplList.size() == 1) {
            // Nothing to be done here
            sffGraph.setSffIngressDpl(sff.getName(), pathId, sffDplList.get(0).getName());
            sffGraph.setSffEgressDpl(sff.getName(), pathId, sffDplList.get(0).getName());
            return true;
        }

        for (SffDataPlaneLocator sffDpl : sffDplList) {
            LOG.debug("try to match sffDpl name: {}, type: {}", sffDpl.getName(),
                    sffDpl.getDataPlaneLocator().getTransport().getName());
            if (sffDpl.getName().equals(alreadySetSffDpl.getName())) {
                continue;
            }
            if (sffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                if (ingressDplSet) {
                    // the SFF ingressDpl was already set, so we need to set the egress
                    sffGraph.setSffEgressDpl(sff.getName(), pathId, sffDpl.getName());
                } else {
                    sffGraph.setSffIngressDpl(sff.getName(), pathId, sffDpl.getName());
                }

                return true;
            }
        }

        return false;
    }

    // Returns true if it was possible to set the SFF DPLs
    private boolean setSffHopDataPlaneLocators(final ServiceFunctionForwarder prevSff,
            final ServiceFunctionForwarder curSff, final String rspTransport, final long pathId, SffGraph sffGraph) {
        List<SffDataPlaneLocator> prevSffDplList = prevSff.getSffDataPlaneLocator();
        List<SffDataPlaneLocator> curSffDplList = curSff.getSffDataPlaneLocator();
        boolean hasSingleDpl = false;

        // If the prevSffDplList has just one DPL, nothing special needs to be done
        // Just check that its DPL transport matches the RSP transport
        if (prevSffDplList.size() == 1) {
            SffDataPlaneLocator prevSffDpl = prevSffDplList.get(0);
            if (!prevSffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                LOG.warn("SFF [{}] transport type [{}] does not match the RSP DPL transport type [{}]", prevSff,
                        prevSffDpl.getDataPlaneLocator().getTransport().getName(), rspTransport);
                return false;
            }
            sffGraph.setSffEgressDpl(prevSff.getName(), pathId, prevSffDpl.getName());
            sffGraph.setSffIngressDpl(prevSff.getName(), pathId, prevSffDpl.getName());

            // Nothing else needs to be done
            hasSingleDpl = true;
        }

        if (curSffDplList.size() == 1) {
            SffDataPlaneLocator curSffDpl = curSffDplList.get(0);
            if (!curSffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                LOG.warn("SFF [{}] transport type [{}] does not match the RSP DPL transport type [{}]", curSff,
                        curSffDpl.getDataPlaneLocator().getTransport().getName(), rspTransport);
                return false;
            }
            sffGraph.setSffEgressDpl(curSff.getName(), pathId, curSffDpl.getName());
            sffGraph.setSffIngressDpl(curSff.getName(), pathId, curSffDpl.getName());

            // Nothing else needs to be done
            hasSingleDpl = true;
        }

        if (hasSingleDpl) {
            return true;
        }

        // This is an O(n squared) search, can be improved using a hash table.
        // Considering there should only be 3-4 DPLs, its not worth the extra
        // code to improve it.
        for (SffDataPlaneLocator prevSffDpl : prevSffDplList) {
            if (!prevSffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                // Only check the transport types that the RSP uses
                LOG.debug("Discarding prevSff [{}] dpl [{}] rspTransport [{}]", prevSff,
                        prevSffDpl.getDataPlaneLocator().getTransport().getName(), rspTransport);
                continue;
            }

            for (SffDataPlaneLocator curSffDpl : curSffDplList) {
                if (!curSffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                    // Only check the transport types that the RSP uses
                    LOG.debug("Discarding curSff [{}] dpl [{}] rspTransport [{}]", curSff,
                            curSffDpl.getDataPlaneLocator().getTransport().getName(), rspTransport);
                    continue;
                }

                LocatorType prevLocatorType = prevSffDpl.getDataPlaneLocator().getLocatorType();
                LocatorType curLocatorType = curSffDpl.getDataPlaneLocator().getLocatorType();
                LOG.debug("comparing prev locator [{}] : [{}] to [{}] : [{}]", prevSffDpl.getName(), prevLocatorType,
                        curSffDpl.getName(), curLocatorType);
                if (compareLocatorTypes(prevLocatorType, curLocatorType)) {
                    sffGraph.setSffEgressDpl(prevSff.getName(), pathId, prevSffDpl.getName());
                    sffGraph.setSffIngressDpl(curSff.getName(), pathId, curSffDpl.getName());
                    return true;
                }
            }
        }

        return false;
    }

    private boolean compareLocatorTypes(LocatorType lhs, LocatorType rhs) {
        if (lhs.getImplementedInterface() != rhs.getImplementedInterface()) {
            return false;
        }
        String type = lhs.getImplementedInterface().getSimpleName().toLowerCase();

        switch (type) {
            case IP:
                // TODO what makes 2 NSH IP DPLs equal? Assuming its the Port, as each IP will be
                // different
                // should we instead check the tunnel Vnid?
                if (((Ip) lhs).getPort().getValue().intValue() == ((Ip) rhs).getPort().getValue().intValue()) {
                    return true;
                }
                break;
            case MAC:
                if (((Mac) lhs).getVlanId() != null && ((Mac) rhs).getVlanId() != null) {
                    if (((Mac) lhs).getVlanId().intValue() == ((Mac) rhs).getVlanId().intValue()) {
                        return true;
                    }
                }
                break;
            case MPLS:
                if (((Mpls) lhs).getMplsLabel().longValue() == ((Mpls) rhs).getMplsLabel().longValue()) {
                    return true;
                }
                break;

            case FUNCTION:
            case LISP:
            default:
                break;
        }

        return false;
    }
}
