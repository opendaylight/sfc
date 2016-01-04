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
import java.util.Map;

import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
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

    /**
     * Main entry point for processing an RSP. Orchestrates logic to call
     * different FlowProgrammer flow creation methods.
     *
     * @param rsp - a newly created/updated Rendered Service Path
     */
    public void processRenderedServicePath(RenderedServicePath rsp) {
        // if this method takes too long, consider launching it in a thread
        try {
            // This call blocks until the lock is obtained
            sfcSynchronizer.lock();

            sfcL2ProviderUtils.addRsp(rsp.getPathId());

            //
            // Populate the SFF Connection Graph
            //
            SffGraph sffGraph = populateSffGraph(rsp);

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
                initializeSff(entry);
                sfcL2FlowProgrammer.setFlowRspId(rsp.getPathId());

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

    /**
     * Deletes the OpenFlow flows associated with this Rendered Service Path.
     *
     * @param rsp - the Rendered Service Path to delete
     */
    public void deleteRenderedServicePath(RenderedServicePath rsp) {
        sfcL2FlowProgrammer.deleteRspFlows(rsp.getPathId());
    }

    /**
     * Given the RSP transport type, return an Rsp Transport Processor that
     * will call the appropriate FlowProgrammer methods.
     *
     * @param sffGraph - used to inject dependencies into the newly created object.
     * @param rsp - contains the RSP transport type
     *
     * @return an RSP Transport Processor for the RSP.
     */
    public SfcRspTransportProcessorBase getTransportProcessor(SffGraph sffGraph, RenderedServicePath rsp) {
        try {
            Class<? extends SfcRspTransportProcessorBase> transportClass =
                    rspTransportProcessors.get(rsp.getTransportType().getName());
            SfcRspTransportProcessorBase transportProcessor = transportClass.newInstance();
            transportProcessor.setFlowProgrammer(sfcL2FlowProgrammer);
            transportProcessor.setRsp(rsp);
            transportProcessor.setSffGraph(sffGraph);
            transportProcessor.setSfcProviderUtils(sfcL2ProviderUtils);

            return transportProcessor;
        } catch(Exception e) {
            throw new RuntimeException("getTransportProcessor no processor for transport [" + rsp.getTransportType().getName() + "]" + e);
        }
    }

    /**
     * Given an RSP, create and populate an SffGraph.
     *
     * @param rsp - input to create the graph
     * @return a newly populates SffGraph
     */
    private SffGraph populateSffGraph(RenderedServicePath rsp) {
        SffGraph sffGraph = new SffGraph();

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

        return sffGraph;
    }

    /**
     * Call the appropriate flow creation methods on the TransportProcessor for
     * the TransportIngress table.
     *
     * @param entry - data for the current flows to be created
     * @param sffGraph - contains data for the RSP
     * @param transportProcessor - specific TransportProcessor to call into
     */
    private void configureTransportIngressFlows(SffGraph.SffGraphEntry entry,
            SffGraph sffGraph, SfcRspTransportProcessorBase transportProcessor) {

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            return;
        }

        ServiceFunctionForwarder sffDst =
                sfcL2ProviderUtils.getServiceFunctionForwarder(entry.getDstSff(), entry.getPathId());
        SffDataPlaneLocator sffDstIngressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffDst,
                sffGraph.getSffIngressDpl(entry.getDstSff(), entry.getPathId()));

        transportProcessor.configureSffTransportIngressFlow(entry, sffDstIngressDpl);

        // Configure the SF related flows
        if (entry.getSf() != null) {
            transportProcessor.configureSfTransportIngressFlow(entry);
        }
    }

    /**
     * Call the appropriate flow creation methods on the TransportProcessor for
     * the PathMapper table.
     *
     * @param entry - data for the current flows to be created
     * @param sffGraph - contains data for the RSP
     * @param transportProcessor - specific TransportProcessor to call into
     */
    private void configurePathMapperFlows(SffGraph.SffGraphEntry entry,
            SffGraph sffGraph, SfcRspTransportProcessorBase transportProcessor) {

        if(entry.getDstSff().equals(SffGraph.EGRESS)) {
            return;
        }

        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(entry.getDstSff(), entry.getPathId());
        LocatorType sffLocatorType = dstHopIngressDpl.getLocatorType();
        if (sffLocatorType == null) {
            throw new RuntimeException("configurePathMapperFlows hopDpl locatorType is null for sff: " + entry.getDstSff());
        }

        // configure SFF-SFF-SF ingress -OR- Ingress-SFF-SF ingress flow using dstHopIngressDpl
        transportProcessor.configureSffPathMapperFlow(entry, dstHopIngressDpl);

        // configure the SF Ingress Flow
        if (entry.getSf() != null) {
            ServiceFunction sf = sfcL2ProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
            SfDataPlaneLocator sfDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(sf, entry.getDstSff());
            if (sfDpl == null) {
                throw new RuntimeException("configurePathMapperFlows sf Dpl is null for sf: " + entry.getSf() + ", and sff: " + entry.getDstSff());
            }
            transportProcessor.configureSfPathMapperFlow(entry, sfDpl);
        }
    }

    /**
     * Call the appropriate flow creation methods on the TransportProcessor for
     * the NextHop table.
     *
     * @param entry - data for the current flows to be created
     * @param sffGraph - contains data for the RSP
     * @param transportProcessor - specific TransportProcessor to call into
     */
    private void configureNextHopFlows(SffGraph.SffGraphEntry entry,
            SffGraph sffGraph, SfcRspTransportProcessorBase transportProcessor) {

        ServiceFunction sfDst = sfcL2ProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
        SfDataPlaneLocator sfDstDpl = (sfDst == null) ? null : sfcL2ProviderUtils.getSfDataPlaneLocator(sfDst, entry.getDstSff());
        if (sfDstDpl != null) {
            if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
                // Configure the GW-SFF-SF NextHop using sfDpl
                transportProcessor.configureNextHopFlow(entry, (SffDataPlaneLocator) null, sfDstDpl);

                // If its Ingress, nothing else to be done
                return;
            } else {
                ServiceFunctionForwarder sffSrc =
                        sfcL2ProviderUtils.getServiceFunctionForwarder(entry.getSrcSff(), entry.getPathId());
                SffDataPlaneLocator sffSrcEgressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffSrc,
                        sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
                // Configure the SFF-SFF-SF NextHop using sfDpl
                transportProcessor.configureNextHopFlow(entry, sffSrcEgressDpl, sfDstDpl);
            }
        }

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            // If dstSff is EGRESS, the SF is actually on the srcSff
            SfDataPlaneLocator sfSrcDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(sfDst, entry.getSrcSff());

            // Configure the SF-SFF-GW NextHop, we dont have the DstDpl, leaving it blank
            transportProcessor.configureNextHopFlow(entry, sfSrcDpl, (SffDataPlaneLocator) null);
        }


        SfDataPlaneLocator sfSrcDpl = null;
        if (entry.getPrevSf() != null) {
            sfSrcDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(
                    sfcL2ProviderUtils.getServiceFunction(entry.getPrevSf(), entry.getPathId()), entry.getSrcSff());
        }

        // Configure the SFF-SFF NextHop using the sfDpl and sffDstIngressDpl
        if (sfSrcDpl != null) {
            if (entry.getSrcSff().getValue().equals(entry.getDstSff().getValue())) {
                // If the next hop is on this SFF then go straight to the next SF
                // Configure SF-SFF-SF NextHop on the same SFF
                transportProcessor.configureNextHopFlow(entry, sfSrcDpl, sfDstDpl);
            } else {
                // Configure the SFF-SFF NextHop using the sfDpl and sffDstIngressDpl
                ServiceFunctionForwarder sffDst =
                        sfcL2ProviderUtils.getServiceFunctionForwarder(entry.getDstSff(), entry.getPathId());
                SffDataPlaneLocator sffDstIngressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffDst,
                        sffGraph.getSffIngressDpl(entry.getDstSff(), entry.getPathId()));
                transportProcessor.configureNextHopFlow(entry, sfSrcDpl, sffDstIngressDpl);
            }
        }
    }

    /**
     * Call the appropriate flow creation methods on the TransportProcessor for
     * the TransportEgress table.
     *
     * @param entry - data for the current flows to be created
     * @param sffGraph - contains data for the RSP
     * @param transportProcessor - specific TransportProcessor to call into
     */
    private void configureTransportEgressFlows(SffGraph.SffGraphEntry entry,
            SffGraph sffGraph, SfcRspTransportProcessorBase transportProcessor) {

        // Configure the SFF-Egress Transport Egress
        ServiceFunctionForwarder sffSrc =
                sfcL2ProviderUtils.getServiceFunctionForwarder(entry.getSrcSff(), entry.getPathId());
        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            SffDataPlaneLocator sffSrcEgressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffSrc,
                    sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));

            transportProcessor.configureSffTransportEgressFlow(
                    entry, sffSrcEgressDpl, null,
                    sffGraph.getPathEgressDpl(entry.getPathId()));

            return;
        }

        // Configure the SFF-SF Transport Egress using sfDpl
        ServiceFunction sfDst = sfcL2ProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
        SfDataPlaneLocator sfDstDpl = sfcL2ProviderUtils.getSfDataPlaneLocator(sfDst, entry.getDstSff());
        ServiceFunctionForwarder sffDst =
                sfcL2ProviderUtils.getServiceFunctionForwarder(entry.getDstSff(), entry.getPathId());
        if (sfDstDpl != null) {
            SffSfDataPlaneLocator sffSfDpl = sfcL2ProviderUtils.getSffSfDataPlaneLocator(sffDst, entry.getSf());
            SffDataPlaneLocator sffDstDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffDst, sffSfDpl.getSffDplName());
            transportProcessor.configureSfTransportEgressFlow(entry, sffDstDpl, sfDstDpl, sfDstDpl);
        }

        // Nothing else to be done for Ingress
        if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
            return;
        }

        // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
        if (! entry.getSrcSff().getValue().equals(entry.getDstSff().getValue())) {
            SffDataPlaneLocator sffDstIngressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffDst,
                    sffGraph.getSffIngressDpl(entry.getDstSff(), entry.getPathId()));
            SffDataPlaneLocator sffSrcEgressDpl = sfcL2ProviderUtils.getSffDataPlaneLocator(sffSrc,
                    sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
            // This is the HOP DPL details between srcSFF and dstSFF, for example: VLAN ID 100
            DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(entry.getDstSff(), entry.getPathId());
            transportProcessor.configureSffTransportEgressFlow(
                    entry, sffSrcEgressDpl, sffDstIngressDpl, dstHopIngressDpl);
        }
    }

    /* TODO what about SFG??
     * Currently there is nobody available to maintain the Service
     * Function Groups (SFGs) code, nor are there any tests available

    private void configureSffEgressForGroup(SffGraph.SffGraphEntry entry, SffGraph sffGraph) {
    }

    private void configureGroupNextHopFlow(final SffName sffName, SffDataPlaneLocator srcSffDpl, long groupId,
            String groupName, final long pathId, final short serviceIndex) {
    }

    */


    /**
     * Initialize the SFF by creating the match any flows, if not already created.
     *
     * @param entry - contains the SFF and RSP id
     */
    private void initializeSff(SffGraph.SffGraphEntry entry) {
        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            return;
        }

        String sffNodeName = sfcL2ProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        if (sffNodeName == null) {
            throw new RuntimeException("initializeSff SFF [" + entry.getDstSff().getValue() + "] does not exist");
        }

        if (!getSffInitialized(entry.getDstSff())) {
            LOG.debug("Initializing SFF [{}] node [{}]", entry.getDstSff().getValue(), sffNodeName);
            this.sfcL2FlowProgrammer.configureClassifierTableMatchAny(sffNodeName);
            this.sfcL2FlowProgrammer.configureTransportIngressTableMatchAny(sffNodeName);
            this.sfcL2FlowProgrammer.configurePathMapperTableMatchAny(sffNodeName);
            this.sfcL2FlowProgrammer.configurePathMapperAclTableMatchAny(sffNodeName);
            this.sfcL2FlowProgrammer.configureNextHopTableMatchAny(sffNodeName);
            this.sfcL2FlowProgrammer.configureTransportEgressTableMatchAny(sffNodeName);

            setSffInitialized(entry.getDstSff(), true);
        }
    }


    //
    // Internal util methods
    //


    /**
     * Given an SFF name, determine if its been initialized yet or not.
     * Called by initializeSff()
     *
     * @param sffName - SFF to check
     * @return true if its been initialized, false otherwise
     */
    private boolean getSffInitialized(final SffName sffName) {
        Boolean isInitialized = sffInitialized.get(sffName);

        if (isInitialized == null) {
            return false;
        }

        return isInitialized.booleanValue();
    }

    /**
     * Set a given SFF as initialized.
     * Called by initializeSff()
     *
     * @param sffName - the SFF to set
     * @param initialized - boolean value to set the SFF as
     */
    private void setSffInitialized(final SffName sffName, boolean initialized) {
        // If the value is already in the map, its value will be replaced
        sffInitialized.put(sffName, initialized);
    }
}
