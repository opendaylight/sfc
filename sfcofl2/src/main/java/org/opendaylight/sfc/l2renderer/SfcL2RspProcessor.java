/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MplsLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mpls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcL2RspProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2RspProcessor.class);
    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;
    private SfcL2BaseProviderUtils sfcL2ProviderUtils;
    private SfcSynchronizer sfcSynchronizer;
    private Map<SffName, Boolean> sffInitialized;
    private Map<String, Class<? extends SfcRspTransportProcessorBase>> rspTransportProcessors;
    private static final Long SFC_FLOWS = new Long(0xdeadbeef);

    public SfcL2RspProcessor(
            SfcL2FlowProgrammerInterface sfcL2FlowProgrammer,
            SfcL2BaseProviderUtils sfcL2ProviderUtils,
            SfcSynchronizer sfcSynchronizer) {
        this.sfcL2FlowProgrammer = sfcL2FlowProgrammer;
        this.sfcL2ProviderUtils = sfcL2ProviderUtils;
        this.sfcSynchronizer = sfcSynchronizer;
        this.sffInitialized = new HashMap<SffName, Boolean>();
        this.rspTransportProcessors = new HashMap<String, Class<? extends SfcRspTransportProcessorBase>>();
        this.rspTransportProcessors.put(VxlanGpe.class.getName(), SfcRspProcessorNsh.class);
        this.rspTransportProcessors.put(Mpls.class.getName(), SfcRspProcessorMpls.class);
        this.rspTransportProcessors.put(Mac.class.getName(), SfcRspProcessorVlan.class);
    }

    // if this method takes too long, consider launching it in a thread
    public void processRenderedServicePath(RenderedServicePath rsp) {
        try {
            // This call blocks until the lock is obtained
            sfcSynchronizer.lock();

            sfcL2ProviderUtils.addRsp(rsp.getPathId());

            //
            // Populate the SFF Connection Graph
            //
            SffGraph sffGraph = new SffGraph();
            populateSffGraph(sffGraph, rsp);

            SfcRspTransportProcessorBase transportProcessor = getTransportProcessor(sffGraph, rsp);

            //
            // Populate the SFF ingress and egress DPLs from the sffGraph
            //
            transportProcessor.processSffDpls();

            //
            // Internally calculate and set the RSP transport values
            //
            transportProcessor.setRspTransports();

            //
            // Now process the entries in the SFF Graph and populate the flow tables
            //
            SffGraph.SffGraphEntry entry = null;
            Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
            while (sffGraphIter.hasNext()) {
                entry = sffGraphIter.next();
                LOG.debug("build flows of entry: {}", entry);

                // The flows created by initializeSff dont belong to any particular RSP
                sfcL2FlowProgrammer.setFlowRspId(SFC_FLOWS);
                if (!entry.getDstSff().equals(SffGraph.EGRESS)) {
                    initializeSff(entry.getDstSff(), entry.getPathId());
                }

                sfcL2FlowProgrammer.setFlowRspId(rsp.getPathId());
                configureSffIngress(entry, sffGraph, transportProcessor);
                configureSffEgress(entry, sffGraph, transportProcessor);

                configureTransportIngressFlows(entry, sffGraph, transportProcessor);
                configurePathMapperFlows(entry, sffGraph, transportProcessor);
                configureNextHopFlows(entry, sffGraph, transportProcessor);
                configureTransportEgressFlows(entry, sffGraph, transportProcessor);
            }

            LOG.info("Processing complete for RSP: name [{}] Id [{}]", rsp.getName(), rsp.getPathId());

        } catch (RuntimeException e) {
            LOG.error("RuntimeException in processRenderedServicePath: ", e.getMessage(), e);
        } finally {
            sfcSynchronizer.unlock();
            sfcL2ProviderUtils.removeRsp(rsp.getPathId());
        }
    }

    public void deleteRenderedServicePath(RenderedServicePath rsp) {
        sfcL2FlowProgrammer.deleteRspFlows(rsp.getPathId());
    }

    public SfcRspTransportProcessorBase getTransportProcessor(SffGraph sffGraph, RenderedServicePath rsp) {
        try {
            Class<? extends SfcRspTransportProcessorBase> transportClass =
                    rspTransportProcessors.get(rsp.getTransportType().getName());
            SfcRspTransportProcessorBase transportProcessor = transportClass.newInstance();
            transportProcessor.setFlowProgrammer(sfcL2FlowProgrammer);
            transportProcessor.setRsp(rsp);
            transportProcessor.setSffGraph(sffGraph);

            return transportProcessor;
        } catch(Exception e) {
            throw new RuntimeException("getTransportProcessor no processor for transport [" + rsp.getTransportType().getName() + "]");
        }
    }

    private void populateSffGraph(SffGraph sffGraph, RenderedServicePath rsp) {
        // Setting to INGRESS for the first graph entry, which is the RSP Ingress
        SffName prevSffName = new SffName(SffGraph.INGRESS);

        Iterator<RenderedServicePathHop> servicePathHopIter = rsp.getRenderedServicePathHop().iterator();
        SfName sfName = null;
        SfName prevSfName = null;
        String sfgName = null;
        SffGraph.SffGraphEntry entry = null;
        short lastServiceIndex = rsp.getStartingIndex();
        while (servicePathHopIter.hasNext()) {
            RenderedServicePathHop rspHop = servicePathHopIter.next();
            SffName curSffName = rspHop.getServiceFunctionForwarder();
            sfName = rspHop.getServiceFunctionName();
            sfgName = rspHop.getServiceFunctionGroupName();

            LOG.info("processRenderedServicePath pathId [{}] renderedServicePathHop [{}]", rsp.getPathId(),
                    rspHop.getHopNumber());

            entry = sffGraph.addGraphEntry(prevSffName, curSffName, sfName, sfgName, rsp.getPathId(),
                    rspHop.getServiceIndex());
            entry.setPrevSf(prevSfName);
            lastServiceIndex = rspHop.getServiceIndex();
            prevSfName = sfName;
            prevSffName = curSffName;
        }
        // Add the final connection, which will be the RSP Egress
        // Using the previous sfName as the SrcSf
        entry = sffGraph.addGraphEntry(prevSffName, SffGraph.EGRESS, sfName, sfgName, rsp.getPathId(),
                (short) (lastServiceIndex - 1));
        entry.setPrevSf(prevSfName);

    }

    private void configureTransportIngressFlows(SffGraph.SffGraphEntry entry,
            SffGraph sffGraph, SfcRspTransportProcessorBase transportProcessor) {

        if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
            transportProcessor.configureSffTransportIngressFlow(entry);
            return;
        }

        // Configure the SF related flows
        if (entry.getSf() != null) {
            transportProcessor.configureSfTransportIngressFlow(entry);
        }
    }

    private void configurePathMapperFlows(SffGraph.SffGraphEntry entry,
            SffGraph sffGraph, SfcRspTransportProcessorBase transportProcessor) {

        final SffName sffDstName = entry.getDstSff();
        DataPlaneLocator hopDpl = sffGraph.getHopIngressDpl(sffDstName, entry.getPathId());
        LocatorType sffLocatorType = hopDpl.getLocatorType();
        if (sffLocatorType == null) {
            throw new RuntimeException("configureSffIngressFlow hopDpl locatorType is null for sff: " + sffDstName);
        }

        // TODO figure this out

        /* OLD code
        // configure SFF-SFF-SF ingress flow using dstHopIngressDpl
        configureSffPathMapperFlow(sffDstName, false, dstHopIngressDpl, entry.getPathId(), entry.getServiceIndex());

        // configure the Ingress-SFF-SF ingress Flow
        configureSffPathMapperFlow(sffDstName, false, dstHopIngressDpl, entry.getPathId(), entry.getServiceIndex());

        // configure the SF Ingress Flow, setting negative pathId so it wont
        // set metadata and will goto classification table instead of NextHop
        configureSffPathMapperFlow(sffDstName, true, sfDpl, entry.getPathId(), entry.getServiceIndex());
        */

        transportProcessor.configureSffPathMapperFlow(entry);
    }

    private void configureNextHopFlows(SffGraph.SffGraphEntry entry,
            SffGraph sffGraph, SfcRspTransportProcessorBase transportProcessor) {
        // This is the HOP DPL details between srcSFF and dstSFF, for example: VLAN ID 100
        final SffName sffDstName = entry.getDstSff();
        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(sffDstName, entry.getPathId());
        if (dstHopIngressDpl == null) {
            throw new RuntimeException("configureSffIngress SFF [" + sffDstName
                    + "] Hop Ingress DPL is null for pathId [" + entry.getPathId() + "]");
        }

        // This is the case of either SFF-SFF-SF or GW-SFF-SF, where the source
        // info is in the SFF Egress DPL and the dstDpl is the SF DPL

        // This is the case of SF-SFF-SFF flows

    }

    private void configureTransportEgressFlows(SffGraph.SffGraphEntry entry,
            SffGraph sffGraph, SfcRspTransportProcessorBase transportProcessor) {

        // TODO figure this out

        /* OLD code
        // Configure the SFF-Egress Transport Egress
        configureSffTransportEgressFlow(entry.getSrcSff(), sffSrcEgressDpl, null,
                    sffGraph.getPathEgressDpl(entry.getPathId()), entry.getPathId(), entry.getServiceIndex(), true);

        // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
        configureSffTransportEgressFlow(entry.getSrcSff(), sffSrcEgressDpl, sffDstIngressDpl, dstHopIngressDpl,
                entry.getPathId(), entry.getServiceIndex(), false);

        // Configure the SFF-Egress Transport Egress
        configureSffTransportEgressFlow(entry.getSrcSff(), sffSrcEgressDpl, null,
                    sffGraph.getPathEgressDpl(entry.getPathId()), entry.getPathId(), entry.getServiceIndex(), true);
         */

    }

    /******************************************
     *
     *
     *                    OLD CODE
     *
     *
     ******************************************
     */

    
    private void configureSffEgressForGroup(SffGraph.SffGraphEntry entry, SffGraph sffGraph) {
        LOG.debug("configureSffEgressForGroup srcSff [{}] dstSff [{}] sfg [{}] pathId [{}] serviceIndex [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSfg(), entry.getPathId(), entry.getServiceIndex());

        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(entry.getDstSff(), entry.getPathId());

        ServiceFunctionForwarder sffSrc =
                sfcL2ProviderUtils.getServiceFunctionForwarder(entry.getSrcSff(), entry.getPathId());
        SffDataPlaneLocator sffSrcEgressDpl = null;
        ServiceFunctionGroup sfg = sfcL2ProviderUtils.getServiceFunctionGroup(entry.getSfg(), entry.getPathId());
        List<SfcServiceFunction> sfs = sfg.getSfcServiceFunction();

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            sffSrcEgressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffSrc,
                    sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));

            for (SfcServiceFunction sfcServiceFunction : sfs) {
                SfName sfName = new SfName(sfcServiceFunction.getName());
                ServiceFunction sfDst = sfcL2ProviderUtils.getServiceFunction(sfName, entry.getPathId());
                SfDataPlaneLocator sfDstDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(sfDst, entry.getSrcSff());
                // Configure the SF-SFF-GW NextHop, we dont have the GW mac, leaving it blank
                configureSffNextHopFlow(entry.getSrcSff(), sfDstDpl, (SffDataPlaneLocator) null, entry.getPathId(),
                        entry.getServiceIndex());
            }

            // Configure the SFF-Egress Transport Egress
            configureSffTransportEgressFlow(entry.getSrcSff(), sffSrcEgressDpl, null,
                    sffGraph.getPathEgressDpl(entry.getPathId()), entry.getPathId(), entry.getServiceIndex(), true);

            // Nothing else to be done for the egress tables
            return;
        }

        ServiceFunctionForwarder sffDst =
                sfcL2ProviderUtils.getServiceFunctionForwarder(entry.getDstSff(), entry.getPathId());
        if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // Configure the GW-SFF-SF NextHop using sfDpl
            // configureSffNextHopFlow(entry.getDstSff(), (SffDataPlaneLocator) null, sfDstDpl,
            // entry.getPathId(), entry.getServiceIndex());
            configureGroupNextHopFlow(entry.getDstSff(), (SffDataPlaneLocator) null, sfg.getGroupId(), sfg.getName(),
                    entry.getPathId(), entry.getServiceIndex());
        } else {
            sffSrcEgressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffSrc,
                    sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
            // Configure the SFF-SFF-SF NextHop using sfDpl
            configureGroupNextHopFlow(entry.getDstSff(), sffSrcEgressDpl, sfg.getGroupId(), sfg.getName(),
                    entry.getPathId(), entry.getServiceIndex());
        }

        if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // Nothing else to be done for the egress tables
            return;
        }

        SffDataPlaneLocator sffDstIngressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffDst,
                sffGraph.getSffIngressDpl(entry.getDstSff(), entry.getPathId()));

        for (SfcServiceFunction sfcServiceFunction : sfs) {
            SfName sfName = new SfName(sfcServiceFunction.getName());
            ServiceFunction sfSrc = sfcL2ProviderUtils.getServiceFunction(sfName, entry.getPathId());
            SfDataPlaneLocator sfSrcDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(sfSrc, sffDst.getName());

            // Configure the SFF-SFF NextHop using the sfDpl and sffDstIngressDpl
            configureSffNextHopFlow(entry.getSrcSff(), sfSrcDpl, sffDstIngressDpl, entry.getPathId(),
                    entry.getServiceIndex());

        }

        // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
        configureSffTransportEgressFlow(entry.getSrcSff(), sffSrcEgressDpl, sffDstIngressDpl, dstHopIngressDpl,
                entry.getPathId(), entry.getServiceIndex(), false);

    }

    /****************************************************************
     * Flow Table Orchestration methods
     ****************************************************************/

    /**
     * Populate the TransportIngress and Ingress Flow Tables
     *
     * @param entry
     */
    private void configureSffIngress(SffGraph.SffGraphEntry entry, SffGraph sffGraph, SfcRspTransportProcessorBase transportProcessor) {
        LOG.debug("configureSffIngress srcSff [{}] dstSff [{}] sf [{}] sfg [{}] pathId [{}] serviceIndex [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSf(), entry.getSfg(), entry.getPathId(),
                entry.getServiceIndex());

        final SffName sffDstName = entry.getDstSff();

        if (sffDstName.equals(SffGraph.EGRESS)) {
            // Nothing to be done for the ingress tables, skip it
            return;
        }

        ServiceFunctionForwarder sffDst = sfcL2ProviderUtils.getServiceFunctionForwarder(sffDstName, entry.getPathId());
        SffDataPlaneLocator sffDstIngressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffDst,
                sffGraph.getSffIngressDpl(sffDstName, entry.getPathId()));
        if (sffDstIngressDpl == null) {
            throw new RuntimeException("configureSffIngress SFF [" + sffDstName
                    + "] does not have a DataPlaneLocator for pathId [" + entry.getPathId() + "]");
        }

        // This is the HOP DPL details between srcSFF and dstSFF, for example: VLAN ID 100
        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(sffDstName, entry.getPathId());
        if (dstHopIngressDpl == null) {
            throw new RuntimeException("configureSffIngress SFF [" + sffDstName
                    + "] Hop Ingress DPL is null for pathId [" + entry.getPathId() + "]");
        }

        // Configure the SF related flows
        if (entry.getSf() != null) {
            SffSfDataPlaneLocator sffSfDpl = sfcL2ProviderUtils.getSffSfDataPlaneLocator(sffDst, entry.getSf());
            ServiceFunction sf = sfcL2ProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
            SfDataPlaneLocator sfDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(sf, sffSfDpl);
            if (sfDpl == null) {
                throw new RuntimeException(
                        "Cant find SFF [" + sffDstName + "] to SF [" + entry.getSf() + "] DataPlaneLocator");
            }
            configureSingleSfIngressFlow(entry, sffDstName, dstHopIngressDpl, sfDpl);
        } else if (entry.getSfg() != null) {
            LOG.debug("configure ingress flow for SFG {}", entry.getSfg());
            ServiceFunctionGroup sfg = sfcL2ProviderUtils.getServiceFunctionGroup(entry.getSfg(), entry.getPathId());
            List<SfcServiceFunction> sfgSfs = sfg.getSfcServiceFunction();
            for (SfcServiceFunction sfgSf : sfgSfs) {
                LOG.debug("configure ingress flow for SF {}", sfgSf);
                SffSfDataPlaneLocator sffSfDpl =
                        sfcL2ProviderUtils.getSffSfDataPlaneLocator(sffDst, new SfName(sfgSf.getName()));
                ServiceFunction sf = sfcL2ProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
                SfDataPlaneLocator sfDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(sf, sffSfDpl);
                if (sfDpl == null) {
                    throw new RuntimeException(
                            "Cant find SFF [" + sffDstName + "] to SF [" + sfgSf + "] DataPlaneLocator");
                }
                configureSingleSfIngressFlow(entry, sffDstName, dstHopIngressDpl, sfDpl);

            }
        }

        // ARP flows for TcpProxy type of SFF
        ServiceFunction sf = sfcL2ProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
        // FIXME: I would caution against this approach. Instead you may want to see if
        // ServiceFunctionType has "bidirectional" = True in future.
        if (sf.getType().getValue().equals("tcp-proxy")) {
            ServiceFunctionDictionary sffSfDict = sfcL2ProviderUtils.getSffSfDictionary(sffDst, entry.getSf());
            String sffMac = sfcL2ProviderUtils.getDictPortInfoMac(sffDst, sffSfDict);
            // If the SF is a TCP Proxy, then we need to reply to the ARP Request messages
            if (sffMac != null) {
                this.sfcL2FlowProgrammer.configureArpTransportIngressFlow(
                        sfcL2ProviderUtils.getSffOpenFlowNodeName(sffDstName, entry.getPathId()), sffMac);
            }
        }
        // Configure the Service Chain Ingress flow(s)
        if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // configure the SFF Transport Ingress Flow using the dstHopIngressDpl
            configureSffTransportIngressFlow(sffDstName, dstHopIngressDpl, entry.getPathId());
            return;
        }

        // configure SFF-SFF-SF ingress flow using dstHopIngressDpl
        configureSffPathMapperFlow(sffDstName, false, dstHopIngressDpl, entry.getPathId(), entry.getServiceIndex());
    }

    private void configureSingleSfIngressFlow(SffGraph.SffGraphEntry entry, final SffName sffDstName,
            DataPlaneLocator dstHopIngressDpl, SfDataPlaneLocator sfDpl) {
        // configure the Ingress-SFF-SF ingress Flow
        configureSffPathMapperFlow(sffDstName, false, dstHopIngressDpl, entry.getPathId(), entry.getServiceIndex());
        // configure the SF Transport Ingress Flow
        configureSffTransportIngressFlow(sffDstName, sfDpl, entry.getPathId());
        // configure the SF Ingress Flow, setting negative pathId so it wont
        // set metadata and will goto classification table instead of NextHop
        configureSffPathMapperFlow(sffDstName, true, sfDpl, entry.getPathId(), entry.getServiceIndex());
    }

    /**
     * Populate the NextHop and TransportEgress Flow Tables
     *
     * @param entry
     */
    private void configureSffEgress(SffGraph.SffGraphEntry entry, SffGraph sffGraph, SfcRspTransportProcessorBase transportProcessor) {
        LOG.debug("configureSffEgress srcSff [{}] dstSff [{}] sf [{}] pathId [{}] serviceIndex [{}]", entry.getSrcSff(),
                entry.getDstSff(), entry.getSf(), entry.getPathId(), entry.getServiceIndex());

        // SFGs
        if (entry.getSfg() != null) {
            configureSffEgressForGroup(entry, sffGraph);
            return;
        }

        /*
         * These are the SFFGraph entries and how the NextHops are calculated:
         * (retrieved with: grep addEntry karaf.log | awk -F "|" '{print $6}')
         *
         * "openflow:2" => SFF1, "openflow:2" => SFF2
         * srcSff dstSff SF pathId => SFF, NHop, MACsrc
         * [ingress] [openflow:2] [sf1] [1] => 1, SF1, GW1/null (dont put macSrc and use lower
         * priority)
         * [openflow:2] [openflow:3] [sf2] [1] => 1, SFF2, SF1
         * => 2, SF2, SFF1ulEgr
         * [openflow:3] [egress] [null] [1] => 2, GW2, SFF2dlEgr (dont have GW macDst, leave out of
         * nextHop)
         * [ingress] [openflow:3] [sf2] [2] => 2, SF2, GW2/null (dont put macSrc and use lower
         * priority)
         * [openflow:3] [openflow:2] [sf1] [2] => 2, SFF1, SF2
         * => 1, SF1, SFF2dlEgr
         * [openflow:2] [egress] [null] [2] => 1, GW1, SFF1ulEgr (dont have GW macDst, leave out of
         * nextHop)
         */

        // This is the HOP DPL details between srcSFF and dstSFF, for example: VLAN ID 100
        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(entry.getDstSff(), entry.getPathId());

        ServiceFunctionForwarder sffSrc =
                sfcL2ProviderUtils.getServiceFunctionForwarder(entry.getSrcSff(), entry.getPathId());
        SffDataPlaneLocator sffSrcEgressDpl = null;
        ServiceFunction sfDst = sfcL2ProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
        SfDataPlaneLocator sfDstDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(sfDst, entry.getDstSff());

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            // If dstSff is EGRESS, the SF is actually on the srcSff
            sfDstDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(sfDst, entry.getSrcSff());
            sffSrcEgressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffSrc,
                    sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));

            // Configure the SF-SFF-GW NextHop, we dont have the GW mac, leaving it blank
            configureSffNextHopFlow(entry.getSrcSff(), sfDstDpl, (SffDataPlaneLocator) null, entry.getPathId(),
                    entry.getServiceIndex());

            // Configure the SFF-Egress Transport Egress
            configureSffTransportEgressFlow(entry.getSrcSff(), sffSrcEgressDpl, null,
                    sffGraph.getPathEgressDpl(entry.getPathId()), entry.getPathId(), entry.getServiceIndex(), true);

            // Nothing else to be done for the egress tables
            return;
        }

        ServiceFunctionForwarder sffDst =
                sfcL2ProviderUtils.getServiceFunctionForwarder(entry.getDstSff(), entry.getPathId());
        if (sfDstDpl != null) {
            if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
                // Configure the GW-SFF-SF NextHop using sfDpl
                configureSffNextHopFlow(entry.getDstSff(), (SffDataPlaneLocator) null, sfDstDpl, entry.getPathId(),
                        entry.getServiceIndex());
            } else {
                sffSrcEgressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffSrc,
                        sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
                // Configure the SFF-SFF-SF NextHop using sfDpl
                configureSffNextHopFlow(entry.getDstSff(), sffSrcEgressDpl, sfDstDpl, entry.getPathId(),
                        entry.getServiceIndex());
            }

            ServiceFunctionDictionary sffSfDict = sfcL2ProviderUtils.getSffSfDictionary(sffDst, entry.getSf());
            // Configure the SFF-SF Transport Egress using sfDpl
            configureSffTransportEgressFlow(entry.getDstSff(), sffSfDict, sfDstDpl, sfDstDpl, entry.getPathId(),
                    entry.getServiceIndex());
        }

        if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // Nothing else to be done for the egress tables
            return;
        }

        SfDataPlaneLocator sfSrcDpl = null;
        if (entry.getPrevSf() != null) {
            sfSrcDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(
                    sfcL2ProviderUtils.getServiceFunction(entry.getPrevSf(), entry.getPathId()), entry.getSrcSff());
        }

        SffDataPlaneLocator sffDstIngressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffDst,
                sffGraph.getSffIngressDpl(entry.getDstSff(), entry.getPathId()));

        // Configure the SFF-SFF NextHop using the sfDpl and sffDstIngressDpl
        if (entry.getSrcSff().equals(entry.getDstSff())) {
            // If the next hop is on this SFF then go straight to the next SF
            // Configure SF-SFF-SF NextHop on the same SFF
            if (sfSrcDpl != null) {
                configureSffNextHopFlow(entry.getSrcSff(), sfSrcDpl, sfDstDpl, entry.getPathId(),
                        entry.getServiceIndex());
            }
        } else {
            // Configure the SFF-SFF NextHop using the sfDpl and sffDstIngressDpl
            if (sfSrcDpl != null) {
                configureSffNextHopFlow(entry.getSrcSff(), sfSrcDpl, sffDstIngressDpl, entry.getPathId(),
                        entry.getServiceIndex());
            }

            // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
            configureSffTransportEgressFlow(entry.getSrcSff(), sffSrcEgressDpl, sffDstIngressDpl, dstHopIngressDpl,
                    entry.getPathId(), entry.getServiceIndex(), false);
        }
    }

    private void initializeSff(final SffName sffName, final long pathId) {
        String sffNodeName = sfcL2ProviderUtils.getSffOpenFlowNodeName(sffName, pathId);
        if (sffNodeName == null) {
            throw new RuntimeException("initializeSff SFF [" + sffName + "] does not exist");
        }

        if (!getSffInitialized(sffName)) {
            LOG.debug("Initializing SFF [{}] node [{}]", sffName, sffNodeName);
            this.sfcL2FlowProgrammer.configureClassifierTableMatchAny(sffNodeName);
            this.sfcL2FlowProgrammer.configureTransportIngressTableMatchAny(sffNodeName);
            this.sfcL2FlowProgrammer.configurePathMapperTableMatchAny(sffNodeName);
            this.sfcL2FlowProgrammer.configurePathMapperAclTableMatchAny(sffNodeName);
            this.sfcL2FlowProgrammer.configureNextHopTableMatchAny(sffNodeName);
            this.sfcL2FlowProgrammer.configureTransportEgressTableMatchAny(sffNodeName);

            setSffInitialized(sffName, true);
        }
    }

    /**
     * Call the sfcL2FlowProgrammer to write the Transport Ingress Flow
     *
     * @param sffName - which SFF to write the flow to
     * @param dpl - details about the transport to write
     */
    private void configureSffTransportIngressFlow(final SffName sffName, DataPlaneLocator dpl, long pathId) {
        String sffNodeName = sfcL2ProviderUtils.getSffOpenFlowNodeName(sffName, pathId);
        if (sffNodeName == null) {
            throw new RuntimeException("configureSffTransportIngressFlow SFF [" + sffName + "] does not exist");
        }

        LOG.debug("configureSffTransportIngressFlow sff [{}] node [{}]", sffName, sffNodeName);

        LocatorType sffLocatorType = dpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // The TransportIngress flows may be common to more than one RSP,
        // so they shouldnt be deleted when the RSP is deleted
        sfcL2FlowProgrammer.setFlowRspId(SFC_FLOWS);

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            if (((MacAddressLocator) sffLocatorType).getVlanId() == null) {
                this.sfcL2FlowProgrammer.configureIpv4TransportIngressFlow(sffNodeName);
            } else {
                this.sfcL2FlowProgrammer.configureVlanTransportIngressFlow(sffNodeName);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            this.sfcL2FlowProgrammer.configureMplsTransportIngressFlow(sffNodeName);
        } else if (implementedInterface.equals(Ip.class)) {
            // VxLAN-gpe, it is IP flow with VLAN tag
            if (dpl.getTransport().equals(VxlanGpe.class)) {
                // Only support VxLAN-gpe + NSH currently
                this.sfcL2FlowProgrammer.configureVxlanGpeTransportIngressFlow(sffNodeName);
            }
        }

        // The next flows will be stored with the RSP pathId
        sfcL2FlowProgrammer.setFlowRspId(pathId);
    }

    private void configureSffPathMapperFlow(final SffName sffDstName, boolean isSf, DataPlaneLocator hopDpl,
            final long pathId, final short serviceIndex) {
        String sffNodeName = sfcL2ProviderUtils.getSffOpenFlowNodeName(sffDstName, pathId);
        if (sffNodeName == null) {
            throw new RuntimeException("configureSffIngressFlow SFF [" + sffDstName + "] does not exist");
        }

        LOG.debug("configureSffIngressFlow sff [{}] node [{}] pathId [{}] serviceIndex [{}]", sffDstName, sffNodeName,
                pathId, serviceIndex);

        LocatorType sffLocatorType = hopDpl.getLocatorType();
        if (sffLocatorType == null) {
            throw new RuntimeException("configureSffIngressFlow hopDpl locatorType is null for sff: " + sffDstName);
        }
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            Integer vlanTag = ((MacAddressLocator) sffLocatorType).getVlanId();
            if (vlanTag != null) {
                this.sfcL2FlowProgrammer.configureVlanPathMapperFlow(sffNodeName, vlanTag, pathId, isSf);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            long mplsLabel = ((MplsLocator) sffLocatorType).getMplsLabel();
            this.sfcL2FlowProgrammer.configureMplsPathMapperFlow(sffNodeName, mplsLabel, pathId, isSf);
        }
        // Nothing to be done for IP/UDP VxLAN-gpe, as the RSP ID is the NSH.nsp
        // else if (implementedInterface.equals(Ip.class)) {
        // if (hopDpl.getTransport().equals(VxlanGpe.class))

    }

    // Simple pass-through method
    // This is the case of either SFF-SFF-SF or GW-SFF-SF, where the source
    // info is in the SFF Egress DPL and the dstDpl is the SF DPL
    private void configureSffNextHopFlow(final SffName sffName, SffDataPlaneLocator srcSffDpl,
            SfDataPlaneLocator dstSfDpl, final long pathId, final short serviceIndex) {
        DataPlaneLocator srcDpl = null;
        String srcMac = null;
        if (srcSffDpl != null) {
            srcDpl = srcSffDpl.getDataPlaneLocator();
            srcMac = sfcL2ProviderUtils.getDplPortInfoMac(srcSffDpl);
        }
        String dstMac = sfcL2ProviderUtils.getSfDplMac(dstSfDpl);
        configureSffNextHopFlow(sffName, srcDpl, dstSfDpl, srcMac, dstMac, pathId, serviceIndex);
    }

    // Simple pass-through method
    // This is the case of SF-SFF-SFF flows
    private void configureSffNextHopFlow(final SffName sffName, SfDataPlaneLocator srcSfDpl,
            SffDataPlaneLocator dstSffDpl, final long pathId, final short serviceIndex) {
        String srcMac = sfcL2ProviderUtils.getSfDplMac(srcSfDpl);
        String dstMac = sfcL2ProviderUtils.getDplPortInfoMac(dstSffDpl);
        DataPlaneLocator dstDpl = ((dstSffDpl == null) ? null : dstSffDpl.getDataPlaneLocator());
        configureSffNextHopFlow(sffName, srcSfDpl, dstDpl, srcMac, dstMac, pathId, serviceIndex);
    }

    private void configureSffNextHopFlow(final SffName sffName, SfDataPlaneLocator srcSfDpl,
            SfDataPlaneLocator dstSfDpl, final long pathId, final short serviceIndex) {
        String srcMac = sfcL2ProviderUtils.getSfDplMac(srcSfDpl);
        String dstMac = sfcL2ProviderUtils.getSfDplMac(dstSfDpl);
        configureSffNextHopFlow(sffName, srcSfDpl, dstSfDpl, srcMac, dstMac, pathId, serviceIndex);
    }

    // This version is only used by the previous 2 configureSffNextHopFlow() signatures
    private void configureSffNextHopFlow(final SffName sffName, DataPlaneLocator srcDpl, DataPlaneLocator dstDpl,
            String srcMac, String dstMac, final long pathId, final short serviceIndex) {

        String sffNodeName = sfcL2ProviderUtils.getSffOpenFlowNodeName(sffName, pathId);
        if (sffNodeName == null) {
            throw new RuntimeException("configureSffNextHopFlow SFF [" + sffName + "] does not exist");
        }

        LOG.debug("configureSffNextHopFlow sff [{}] pathId [{}] serviceIndex [{}] srcMac [{}] dstMac [{}]", sffName,
                pathId, serviceIndex, srcMac, dstMac);

        LocatorType srcSffLocatorType = (srcDpl != null) ? srcDpl.getLocatorType() : null;
        LocatorType dstSffLocatorType = (dstDpl != null) ? dstDpl.getLocatorType() : null;
        // Assuming srcDpl and dstDpl are of the same type
        Class<? extends DataContainer> implementedInterface = (srcDpl != null) ? srcSffLocatorType
            .getImplementedInterface() : dstSffLocatorType.getImplementedInterface();

        if (implementedInterface.equals(Ip.class)) {
            // VxLAN-gpe, it is IP/UDP flow with VLAN tag
            if (dstDpl != null) {
                if (dstDpl.getTransport().equals(VxlanGpe.class)) {
                    String dstIp = String.valueOf(((IpPortLocator) dstSffLocatorType).getIp().getValue());
                    long nsp = pathId;
                    short nsi = serviceIndex;
                    this.sfcL2FlowProgrammer.configureVxlanGpeNextHopFlow(sffNodeName, dstIp, nsp, nsi);
                }
            }
        } else {
            // Same thing for Mac/VLAN and MPLS
            this.sfcL2FlowProgrammer.configureMacNextHopFlow(sffNodeName, pathId, srcMac, dstMac);
        }
    }

    private void configureGroupNextHopFlow(final SffName sffName, SffDataPlaneLocator srcSffDpl, long groupId,
            String groupName, final long pathId, final short serviceIndex) {
        // DataPlaneLocator srcDpl = null;
        // currently support only mac
        String srcMac = null;
        if (srcSffDpl != null) {
            // srcDpl = srcSffDpl.getDataPlaneLocator();
            srcMac = sfcL2ProviderUtils.getDplPortInfoMac(srcSffDpl);
        }
        String sffNodeName = sfcL2ProviderUtils.getSffOpenFlowNodeName(sffName, pathId);
        if (sffNodeName == null) {
            throw new RuntimeException("configureSffNextHopFlow SFF [" + sffName + "] does not exist");
        }

        this.sfcL2FlowProgrammer.configureGroupNextHopFlow(sffName.getValue(), pathId, srcMac, groupId, groupName);
    }

    // Simple pass-through method that calculates the srcOfsPort
    // and srcMac from the srcSffSfDict, and the dstMac from the dstDpl
    private void configureSffTransportEgressFlow(final SffName sffName, ServiceFunctionDictionary srcSffSfDict,
            SfDataPlaneLocator dstSfDpl, DataPlaneLocator hopDpl, long pathId, short serviceIndex) {
        ServiceFunctionForwarder sff = sfcL2ProviderUtils.getServiceFunctionForwarder(sffName, pathId);
        SffSfDataPlaneLocator srcSffSfDpl = srcSffSfDict.getSffSfDataPlaneLocator();
        DataPlaneLocator srcSffDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sff, srcSffSfDpl.getSffDplName()).getDataPlaneLocator();
        String srcOfsPortStr = sfcL2ProviderUtils.getDictPortInfoPort(sff, srcSffSfDict);
        if (srcOfsPortStr == null) {
            throw new RuntimeException("configureSffTransportEgressFlow OFS port not avail for SFF ["
                    + sffName.getValue() + "] sffSfDict [" + srcSffSfDict.getName() + "]");
        }
        String srcMac = sfcL2ProviderUtils.getDictPortInfoMac(sff, srcSffSfDict);
        String dstMac = sfcL2ProviderUtils.getSfDplMac(dstSfDpl);

        ServiceFunction sf = sfcL2ProviderUtils.getServiceFunction(srcSffSfDict.getName(), pathId);
        // FIXME: I would caution against this approach. Instead you may want to see if
        // ServiceFunctionType has "bidirectional" = True in future.
        if (sf.getType().getValue().equals("tcp-proxy")) {
            // If the SF is a TCP Proxy, we need this additional flow for the SF:
            // - a flow that will also check for TCP Syn and do a PktIn
            configureSffTransportEgressFlow(sffName, srcSffDpl, dstSfDpl, hopDpl, srcOfsPortStr, srcMac, dstMac, pathId,
                    serviceIndex, true, false, true);
        } else {
            // TODO since TCP SYN is not supported until OpenFlow 1.5,
            // for now write one or the other of these flows
            configureSffTransportEgressFlow(sffName, srcSffDpl, dstSfDpl, hopDpl, srcOfsPortStr, srcMac, dstMac, pathId,
                    serviceIndex, true, false, false);
        }
    }

    // Simple pass-through method that calculates the srcOfsPort
    // and srcMac from the srcDpl, and the dstMac from the dstDpl
    private void configureSffTransportEgressFlow(final SffName sffName, SffDataPlaneLocator srcDpl,
            SffDataPlaneLocator dstDpl, DataPlaneLocator hopDpl, long pathId, short serviceIndex,
            boolean isLastServiceIndex) {
        String srcOfsPortStr = sfcL2ProviderUtils.getDplPortInfoPort(srcDpl);
        if (srcOfsPortStr == null) {
            throw new RuntimeException("configureSffTransportEgressFlow OFS port not avail for SFF [" + sffName
                    + "] srcDpl [" + srcDpl.getName() + "]");
        }
        String srcMac = sfcL2ProviderUtils.getDplPortInfoMac(srcDpl);
        String dstMac = sfcL2ProviderUtils.getDplPortInfoMac(dstDpl);
        configureSffTransportEgressFlow(sffName, srcDpl.getDataPlaneLocator(),
                ((dstDpl == null) ? null : dstDpl.getDataPlaneLocator()), hopDpl, srcOfsPortStr, srcMac, dstMac, pathId,
                serviceIndex, false, isLastServiceIndex, false);
    }

    // This version is only used by the previous 2 configureSffTransportEgressFlow() signatures
    private void configureSffTransportEgressFlow(final SffName sffName, DataPlaneLocator srcDpl,
            DataPlaneLocator dstDpl, DataPlaneLocator hopDpl, String srcOfsPort, String srcMac, String dstMac,
            long pathId, short serviceIndex, boolean isSf, boolean isLastServiceIndex, boolean doPktIn) {
        if (hopDpl == null) {
            throw new RuntimeException("configureSffTransportEgressFlow SFF [" + sffName + "] hopDpl is null");
        }

        ServiceFunctionForwarder sff = sfcL2ProviderUtils.getServiceFunctionForwarder(sffName, pathId);
        if (sff == null) {
            throw new RuntimeException("configureSffTransportEgressFlow SFF [" + sffName + "] does not exist");
        }

        String sffNodeName = sfcL2ProviderUtils.getSffOpenFlowNodeName(sffName, pathId);
        if (sffNodeName == null) {
            throw new RuntimeException(
                    "configureSffTransportEgressFlow Sff Node name for SFF [" + sffName + "] does not exist");
        }

        LOG.debug("configureSffTransportEgressFlow sff [{}] node [{}]", sffName, sffNodeName);

        LocatorType hopLocatorType = hopDpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = hopLocatorType.getImplementedInterface();

        if (implementedInterface.equals(Mac.class)) {
            // Mac and possibly VLAN
            Integer vlanTag = ((MacAddressLocator) hopLocatorType).getVlanId();
            if (vlanTag != null) {
                this.sfcL2FlowProgrammer.configureVlanTransportEgressFlow(sffNodeName, srcMac, dstMac, vlanTag,
                        srcOfsPort, pathId, isSf, doPktIn);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            long mplsLabel = ((MplsLocator) hopLocatorType).getMplsLabel();
            this.sfcL2FlowProgrammer.configureMplsTransportEgressFlow(sffNodeName, srcMac, dstMac, mplsLabel,
                    srcOfsPort, pathId, isSf, doPktIn);
        } else if (implementedInterface.equals(Ip.class)) {
            // VxLAN-gpe, it is IP/UDP flow with VLAN tag
            if (hopDpl.getTransport().equals(VxlanGpe.class)) {
                long nsp = pathId;
                short nsi = serviceIndex;
                this.sfcL2FlowProgrammer.configureVxlanGpeTransportEgressFlow(sffNodeName, nsp, nsi, srcOfsPort,
                        isLastServiceIndex);
                if (isLastServiceIndex) {
                    this.sfcL2FlowProgrammer.configureNshNscTransportEgressFlow(sffNodeName, nsp, nsi,
                            OutputPortValues.INPORT.toString());
                }
            }
        }
    }


    /****************************************************************
     * Internal util methods
     ****************************************************************/

    private boolean getSffInitialized(final SffName sffName) {
        Boolean isInitialized = sffInitialized.get(sffName);

        if (isInitialized == null) {
            return false;
        }

        return isInitialized.booleanValue();
    }

    private void setSffInitialized(final SffName sffName, boolean initialized) {
        // If the value is already in the map, its value will be replaced
        sffInitialized.put(sffName, initialized);
    }

}
