/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.processors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerInterface;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfBaseProviderUtils;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcOfRspProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRspProcessor.class);
    private SfcOfFlowProgrammerInterface sfcOfFlowProgrammer;
    private SfcOfBaseProviderUtils sfcOfProviderUtils;
    private SfcSynchronizer sfcSynchronizer;
    private Map<NodeId, Boolean> sffInitialized;
    private Map<String, Class<? extends SfcRspTransportProcessorBase>> rspTransportProcessors;
    private static final Long SFC_FLOWS = new Long(0xdeadbeef);

    public SfcOfRspProcessor(
            SfcOfFlowProgrammerInterface sfcOfFlowProgrammer,
            SfcOfBaseProviderUtils sfcOfProviderUtils,
            SfcSynchronizer sfcSynchronizer) {
        this.sfcOfFlowProgrammer = sfcOfFlowProgrammer;
        this.sfcOfProviderUtils = sfcOfProviderUtils;
        this.sfcSynchronizer = sfcSynchronizer;
        this.sffInitialized = new HashMap<NodeId, Boolean>();
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

            sfcOfProviderUtils.addRsp(rsp.getPathId());

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
                sfcOfFlowProgrammer.setFlowRspId(SFC_FLOWS);
                initializeSff(entry);
                sfcOfFlowProgrammer.setFlowRspId(rsp.getPathId());

                configureTransportIngressFlows(entry, sffGraph, transportProcessor);
                configurePathMapperFlows(entry, sffGraph, transportProcessor);
                configureNextHopFlows(entry, sffGraph, transportProcessor);
                configureTransportEgressFlows(entry, sffGraph, transportProcessor);
            }

            // Flush the flows to the data store
            this.sfcOfFlowProgrammer.flushFlows();

            LOG.info("Processing complete for RSP: name [{}] Id [{}]", rsp.getName(), rsp.getPathId());

        } catch (RuntimeException e) {
            LOG.error("RuntimeException in processRenderedServicePath: ", e.getMessage(), e);
        } finally {
            // If there were any errors, purge any remaining flows so they're not written
            this.sfcOfFlowProgrammer.purgeFlows();
            sfcSynchronizer.unlock();
            sfcOfProviderUtils.removeRsp(rsp.getPathId());
        }
    }

    /**
     * Deletes the OpenFlow flows associated with this Rendered Service Path.
     *
     * @param rsp - the Rendered Service Path to delete
     */
    public void deleteRenderedServicePath(RenderedServicePath rsp) {
        Set<NodeId> clearedSffNodeIDs = sfcOfFlowProgrammer.deleteRspFlows(rsp.getPathId());
        for(NodeId sffNodeId : clearedSffNodeIDs){
            setSffInitialized(sffNodeId, false);
        }
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
            transportProcessor.setFlowProgrammer(sfcOfFlowProgrammer);
            transportProcessor.setRsp(rsp);
            transportProcessor.setSffGraph(sffGraph);
            transportProcessor.setSfcProviderUtils(sfcOfProviderUtils);

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
                sfcOfProviderUtils.getServiceFunctionForwarder(entry.getDstSff(), entry.getPathId());
        SffDataPlaneLocator sffDstIngressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(sffDst,
                sffGraph.getSffIngressDpl(entry.getDstSff(), entry.getPathId()));

        transportProcessor.configureSffTransportIngressFlow(entry, sffDstIngressDpl);

        // Configure the SF related flows
        if (entry.getSf() != null) {
            ServiceFunction sf = sfcOfProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
            SfDataPlaneLocator sfDpl = sfcOfProviderUtils.getSfDataPlaneLocator(sf, entry.getDstSff());
            transportProcessor.configureSfTransportIngressFlow(entry, sfDpl);
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
            ServiceFunction sf = sfcOfProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
            SfDataPlaneLocator sfDpl = sfcOfProviderUtils.getSfDataPlaneLocator(sf, entry.getDstSff());
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

        ServiceFunction sfDst = sfcOfProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
        SfDataPlaneLocator sfDstDpl = (sfDst == null) ? null : sfcOfProviderUtils.getSfDataPlaneLocator(sfDst, entry.getDstSff());
        if (sfDstDpl != null) {
            if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
                // Configure the GW-SFF-SF NextHop using sfDpl
                transportProcessor.configureNextHopFlow(entry, (SffDataPlaneLocator) null, sfDstDpl);

                // If its Ingress, nothing else to be done
                return;
            } else {
                ServiceFunctionForwarder sffSrc =
                        sfcOfProviderUtils.getServiceFunctionForwarder(entry.getSrcSff(), entry.getPathId());
                SffDataPlaneLocator sffSrcEgressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(sffSrc,
                        sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
                // Configure the SFF-SFF-SF NextHop using sfDpl
                transportProcessor.configureNextHopFlow(entry, sffSrcEgressDpl, sfDstDpl);
            }
        }

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            // If dstSff is EGRESS, the SF is actually on the srcSff
            SfDataPlaneLocator sfSrcDpl = sfcOfProviderUtils.getSfDataPlaneLocator(sfDst, entry.getSrcSff());

            // Configure the SF-SFF-GW NextHop, we dont have the DstDpl, leaving it blank
            transportProcessor.configureNextHopFlow(entry, sfSrcDpl, (SffDataPlaneLocator) null);
        }


        SfDataPlaneLocator sfSrcDpl = null;
        if (entry.getPrevSf() != null) {
            sfSrcDpl = sfcOfProviderUtils.getSfDataPlaneLocator(
                    sfcOfProviderUtils.getServiceFunction(entry.getPrevSf(), entry.getPathId()), entry.getSrcSff());
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
                        sfcOfProviderUtils.getServiceFunctionForwarder(entry.getDstSff(), entry.getPathId());
                SffDataPlaneLocator sffDstIngressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(sffDst,
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
                sfcOfProviderUtils.getServiceFunctionForwarder(entry.getSrcSff(), entry.getPathId());
        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            SffDataPlaneLocator sffSrcEgressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(sffSrc,
                    sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));

            transportProcessor.configureSffTransportEgressFlow(
                    entry, sffSrcEgressDpl, null,
                    sffGraph.getPathEgressDpl(entry.getPathId()));

            return;
        }

        // Configure the SFF-SF Transport Egress using sfDpl
        ServiceFunction sfDst = sfcOfProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
        SfDataPlaneLocator sfDstDpl = sfcOfProviderUtils.getSfDataPlaneLocator(sfDst, entry.getDstSff());
        ServiceFunctionForwarder sffDst =
                sfcOfProviderUtils.getServiceFunctionForwarder(entry.getDstSff(), entry.getPathId());
        if (sfDstDpl != null) {
            SffSfDataPlaneLocator sffSfDpl = sfcOfProviderUtils.getSffSfDataPlaneLocator(sffDst, entry.getSf());
            SffDataPlaneLocator sffDstDpl = sfcOfProviderUtils.getSffDataPlaneLocator(sffDst, sffSfDpl.getSffDplName());
            transportProcessor.configureSfTransportEgressFlow(entry, sffDstDpl, sfDstDpl, sfDstDpl);
        }

        // Nothing else to be done for Ingress
        if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
            return;
        }

        // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
        if (! entry.getSrcSff().getValue().equals(entry.getDstSff().getValue())) {
            SffDataPlaneLocator sffDstIngressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(sffDst,
                    sffGraph.getSffIngressDpl(entry.getDstSff(), entry.getPathId()));
            SffDataPlaneLocator sffSrcEgressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(sffSrc,
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

        String sffNodeName = sfcOfProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        if (sffNodeName == null) {
            throw new RuntimeException("initializeSff SFF [" + entry.getDstSff().getValue() + "] does not exist");
        }

        NodeId sffNodeId = new NodeId(sffNodeName);
        if (!getSffInitialized(sffNodeId)) {
            LOG.debug("Initializing SFF [{}] node [{}]", entry.getDstSff().getValue(), sffNodeName);
            this.sfcOfFlowProgrammer.configureClassifierTableMatchAny(sffNodeName);
            this.sfcOfFlowProgrammer.configureTransportIngressTableMatchAny(sffNodeName);
            this.sfcOfFlowProgrammer.configurePathMapperTableMatchAny(sffNodeName);
            this.sfcOfFlowProgrammer.configurePathMapperAclTableMatchAny(sffNodeName);
            this.sfcOfFlowProgrammer.configureNextHopTableMatchAny(sffNodeName);
            this.sfcOfFlowProgrammer.configureTransportEgressTableMatchAny(sffNodeName);

            setSffInitialized(sffNodeId, true);
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
    private boolean getSffInitialized(final NodeId sffNodeId) {
        Boolean isInitialized = sffInitialized.get(sffNodeId);

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
    private void setSffInitialized(final NodeId sffNodeId, boolean initialized) {
        // If the value is already in the map, its value will be replaced
        sffInitialized.put(new NodeId(sffNodeId), initialized);
    }
}
