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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerInterface;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfBaseProviderUtils;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.sfc.ofrenderer.utils.operdsupdate.OperDsUpdateHandlerInterface;
import org.opendaylight.sfc.ofrenderer.utils.operdsupdate.OperDsUpdateHandlerLSFFImpl;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacChaining;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Nsh;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Transport;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcOfRspProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRspProcessor.class);
    private final SfcOfFlowProgrammerInterface sfcOfFlowProgrammer;
    private final SfcOfBaseProviderUtils sfcOfProviderUtils;
    private final SfcSynchronizer sfcSynchronizer;
    private final Map<NodeId, Boolean> sffInitialized;
    private final OperDsUpdateHandlerInterface operDsHandler;
    private final Map<String, SfcRspTransportProcessorBase> rspTransportProcessors;
    private final SfcGeniusRpcClient theGeniusRpcClient;
    private static final String TRANSPORT_ENCAP_SEPARATOR_STRING = "//";

    /*
     * Logical SFF always assumes vxlan-gpe tunnels for inter-sff transport, and
     * eth-encapsulated NSH for sff-sf transport. The initial "LogicalInterface"
     * prefix is used as an identifier only (that is, it is not a true
     * transport)
     */
    private static final String LOGICAL_SFF_TRANSPORT_PROCESSOR_KEY = LogicalInterface.class.getName()
            + TRANSPORT_ENCAP_SEPARATOR_STRING + Nsh.class.getName();

    public SfcOfRspProcessor(SfcOfFlowProgrammerInterface sfcOfFlowProgrammer,
            SfcOfBaseProviderUtils sfcOfProviderUtils, SfcSynchronizer sfcSynchronizer,
            RpcProviderRegistry rpcProviderRegistry, DataBroker dataBroker) {
        this.sfcOfFlowProgrammer = sfcOfFlowProgrammer;
        this.sfcOfProviderUtils = sfcOfProviderUtils;
        this.sfcSynchronizer = sfcSynchronizer;
        this.sffInitialized = new HashMap<>();
        this.theGeniusRpcClient = new SfcGeniusRpcClient(rpcProviderRegistry);
        this.operDsHandler = new OperDsUpdateHandlerLSFFImpl(dataBroker);
        this.rspTransportProcessors = new HashMap<>();

        this.rspTransportProcessors.put(getTransportEncapName(VxlanGpe.class.getName(), Nsh.class.getName()),
                new SfcRspProcessorNshVxgpe());
        this.rspTransportProcessors.put(getTransportEncapName(Mac.class.getName(), Nsh.class.getName()),
                new SfcRspProcessorNshEth());
        this.rspTransportProcessors.put(getTransportEncapName(Mpls.class.getName(), Transport.class.getName()),
                new SfcRspProcessorMpls());
        this.rspTransportProcessors.put(getTransportEncapName(Mac.class.getName(), Transport.class.getName()),
                new SfcRspProcessorVlan());
        this.rspTransportProcessors.put(getTransportEncapName(Mac.class.getName(), MacChaining.class.getName()),
                new SfcRspProcessorMacChaining());
        this.rspTransportProcessors.put(LOGICAL_SFF_TRANSPORT_PROCESSOR_KEY,
                new SfcRspProcessorLogicalSff(getGeniusRpcClient(), getOperDsHandler()));
        rspTransportProcessors.forEach((key, value) -> value.setFlowProgrammer(sfcOfFlowProgrammer));
        rspTransportProcessors.forEach((key, value) -> value.setSfcProviderUtils(sfcOfProviderUtils));
    }

    /**
     * Main entry point for processing an RSP. Orchestrates logic to call
     * different FlowProgrammer flow creation methods.
     *
     * @param rsp
     *            - a newly created/updated Rendered Service Path
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
            // Now process the entries in the SFF Graph and populate the flow
            // tables
            //
            SffGraph.SffGraphEntry entry;
            Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
            sfcOfFlowProgrammer.setTableIndexMapper(transportProcessor.getTableIndexMapper().isPresent()
                    ? transportProcessor.getTableIndexMapper().get()
                    : null);
            while (sffGraphIter.hasNext()) {
                entry = sffGraphIter.next();
                LOG.debug("build flows of entry: {}", entry);
                // The flows created by initializeSff dont belong to any
                // particular RSP
                sfcOfFlowProgrammer.setFlowRspId(OpenflowConstants.SFC_FLOWS);
                initializeSff(entry, transportProcessor);
                sfcOfFlowProgrammer.setFlowRspId(rsp.getPathId());
                configureTransportIngressFlows(entry, sffGraph, transportProcessor);
                configurePathMapperFlows(entry, sffGraph, transportProcessor);
                configureNextHopFlows(entry, sffGraph, transportProcessor);
                configureTransportEgressFlows(entry, sffGraph, transportProcessor);
            }

            // Flush the flows to the data store
            this.sfcOfFlowProgrammer.flushFlows();

            // Update the operational datastore if necessary (without blocking)
            transportProcessor.updateOperationalDSInfo(sffGraph, rsp);

            LOG.info("Processing complete for RSP: name [{}] Id [{}]", rsp.getName(), rsp.getPathId());

        } catch (SfcRenderingException e) {
            LOG.error("SfcRenderingException in processRenderedServicePath: ", e.getMessage(), e);
        } finally {
            // If there were any errors, purge any remaining flows so they're
            // not written
            this.sfcOfFlowProgrammer.purgeFlows();
            sfcSynchronizer.unlock();
            sfcOfProviderUtils.removeRsp(rsp.getPathId());
        }
    }

    /**
     * Deletes the OpenFlow flows associated with this Rendered Service Path.
     *
     * @param rsp
     *            - the Rendered Service Path to delete
     */
    public void deleteRenderedServicePath(RenderedServicePath rsp) {
        Set<NodeId> clearedSffNodeIDs = sfcOfFlowProgrammer.deleteRspFlows(rsp.getPathId());
        for (NodeId sffNodeId : clearedSffNodeIDs) {
            setSffInitialized(sffNodeId, false);
        }

        // not necessary to build a transport processor; simply update SFF state
        // if the RSP
        // being deleted contains dpnid information (asynchronously)
        getOperDsHandler().onRspDeletion(rsp);
    }

    private OperDsUpdateHandlerInterface getOperDsHandler() {
        return operDsHandler;
    }

    /**
     * Given the RSP transport type + encapsulation (and the rsp graph, for
     * lsff), return a RSP Transport Processor that will call the appropriate
     * FlowProgrammer methods.
     *
     * @param sffGraph
     *            - sffGraph generated for the RSP
     * @param rsp
     *            - contains the RSP transport type & encapsulation
     *
     * @return an RSP Transport Processor for the RSP.
     */
    private SfcRspTransportProcessorBase getReusableTransportProcessor(SffGraph sffGraph, RenderedServicePath rsp) {
        String transportProcessorKey = sffGraph.isUsingLogicalSFF() ? LOGICAL_SFF_TRANSPORT_PROCESSOR_KEY
                : getTransportEncapName(rsp.getTransportType().getName(), rsp.getSfcEncapsulation().getName());
        SfcRspTransportProcessorBase transportProcessor = rspTransportProcessors.get(transportProcessorKey);
        if (transportProcessor == null) {
            throw new SfcRenderingException("getTransportProcessor no processor for transport ["
                    + rsp.getTransportType().getName() + "] encap [" + rsp.getSfcEncapsulation() + "] ");
        }
        LOG.debug("getTransportProcessor :: transport [{}] encap [{} selected transport processor [{}]]",
                rsp.getTransportType().getName(), rsp.getSfcEncapsulation(), transportProcessor.getClass());
        return transportProcessor;
    }

    /**
     * Given the RSP transport type + encapsulation, return an Rsp Transport
     * Processor that will call the appropriate FlowProgrammer methods.
     *
     * @param sffGraph
     *            - the graph used for rsp generation
     * @param rsp
     *            - contains the RSP transport type and encapsulation
     *
     * @return an RSP Transport Processor for the RSP.
     */
    public SfcRspTransportProcessorBase getTransportProcessor(SffGraph sffGraph, RenderedServicePath rsp) {
        SfcRspTransportProcessorBase transportProcessor = getReusableTransportProcessor(sffGraph, rsp);
        transportProcessor.setRsp(rsp);
        transportProcessor.setSffGraph(sffGraph);
        return transportProcessor;
    }

    /**
     * Given an RSP, create and populate an SffGraph.
     *
     * @param rsp
     *            - input to create the graph
     * @return a newly populates SffGraph
     */
    private SffGraph populateSffGraph(RenderedServicePath rsp) {
        SffGraph sffGraph = new SffGraph();

        // Setting to INGRESS for the first graph entry, which is the RSP
        // Ingress
        SffName prevSffName = new SffName(SffGraph.INGRESS);
        // Set to null in the first graph entry
        DpnIdType srcDpnId = null;

        Iterator<RenderedServicePathHop> servicePathHopIter = rsp.getRenderedServicePathHop().iterator();
        SfName sfName = null;
        SfName prevSfName = null;
        String sfgName = null;
        SffGraph.SffGraphEntry entry;
        short lastServiceIndex = rsp.getStartingIndex();
        boolean isForwardPath = !rsp.isReversePath();

        while (servicePathHopIter.hasNext()) {
            RenderedServicePathHop rspHop = servicePathHopIter.next();
            SffName curSffName = rspHop.getServiceFunctionForwarder();
            sfName = rspHop.getServiceFunctionName();
            sfgName = rspHop.getServiceFunctionGroupName();

            entry = sffGraph.addGraphEntry(prevSffName, curSffName, sfName, sfgName, rsp.getPathId(),
                    isForwardPath, rspHop.getServiceIndex());
            entry.setPrevSf(prevSfName);
            lastServiceIndex = rspHop.getServiceIndex();
            prevSfName = sfName;
            prevSffName = curSffName;
            ServiceFunction sf = sfcOfProviderUtils.getServiceFunction(sfName, rsp.getPathId());
            List<String> logicalInterfaces = SfcGeniusDataUtils.getSfLogicalInterfaces(sf);
            if (!logicalInterfaces.isEmpty()) {
                // All logical interfaces of the SF should be on the same DPN, use the first one
                String logicalInterfaceName = logicalInterfaces.get(0);
                LOG.debug("SF uses logical interfaces -> storing id for the dataplane node (interface:{})",
                        logicalInterfaceName);
                Optional<DpnIdType> dpnid = getGeniusRpcClient()
                        .getDpnIdFromInterfaceNameFromGeniusRPC(logicalInterfaceName);
                if (!dpnid.isPresent()) {
                    throw new SfcRenderingException("populateSffGraph:failed.dpnid for interface ["
                            + logicalInterfaceName + "] was not returned by genius. "
                            + "Rendered service path cannot be generated at this time");
                }
                LOG.debug("populateSffGraph: retrieved dpn id for SF {} :[{}] ", sf.getName(), dpnid.get());
                entry.setDstDpnId(dpnid.get());
            }
            entry.setSrcDpnId(srcDpnId);
            LOG.debug("populateSffGraph:added graph entry: [{}]", entry);
            srcDpnId = entry.getDstDpnId();

        }
        // Add the final connection, which will be the RSP Egress
        // Using the previous sfName as the SrcSf
        entry = sffGraph.addGraphEntry(prevSffName, SffGraph.EGRESS, sfName, sfgName, rsp.getPathId(), isForwardPath,
                (short) (lastServiceIndex - 1));
        entry.setPrevSf(prevSfName);
        entry.setSrcDpnId(srcDpnId);

        LOG.debug("populateSffGraph: added final graph entry: [{}]", entry);
        return sffGraph;
    }

    /**
     * Call the appropriate flow creation methods on the TransportProcessor for
     * the TransportIngress table.
     *
     * @param entry
     *            - data for the current flows to be created
     * @param sffGraph
     *            - contains data for the RSP
     * @param transportProcessor
     *            - specific TransportProcessor to call into
     */
    private void configureTransportIngressFlows(SffGraph.SffGraphEntry entry, SffGraph sffGraph,
            SfcRspTransportProcessorBase transportProcessor) {

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            return;
        }

        ServiceFunctionForwarder sffDst = sfcOfProviderUtils.getServiceFunctionForwarder(entry.getDstSff(),
                entry.getPathId());
        SffDataPlaneLocator sffDstIngressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(sffDst,
                sffGraph.getSffIngressDpl(entry.getDstSff(), entry.getPathId()));

        transportProcessor.configureSffTransportIngressFlow(entry, sffDstIngressDpl);

        // Configure the SF related flows
        if (entry.getSf() != null) {
            ServiceFunction sf = sfcOfProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
            boolean isForwardPath = entry.isForwardPath();
            SfDataPlaneLocator sfDpl = sfcOfProviderUtils.getIngressSfDataPlaneLocator(sf, sffDst, isForwardPath);
            transportProcessor.configureSfTransportIngressFlow(entry, sfDpl);
        }
    }

    /**
     * Call the appropriate flow creation methods on the TransportProcessor for
     * the PathMapper table.
     *
     * @param entry
     *            - data for the current flows to be created
     * @param sffGraph
     *            - contains data for the RSP
     * @param transportProcessor
     *            - specific TransportProcessor to call into
     */
    private void configurePathMapperFlows(SffGraph.SffGraphEntry entry, SffGraph sffGraph,
            SfcRspTransportProcessorBase transportProcessor) {

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            return;
        }

        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(entry.getDstSff(), entry.getPathId());

        // configure SFF-SFF-SF ingress -OR- Ingress-SFF-SF ingress flow using
        // dstHopIngressDpl
        transportProcessor.configureSffPathMapperFlow(entry, dstHopIngressDpl);

        // configure the SF Ingress Flow
        if (entry.getSf() != null) {
            ServiceFunctionForwarder sffDst = sfcOfProviderUtils.getServiceFunctionForwarder(entry.getDstSff(),
                    entry.getPathId());
            ServiceFunction sf = sfcOfProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
            boolean isForwardPath = entry.isForwardPath();
            SfDataPlaneLocator sfDpl = sfcOfProviderUtils.getIngressSfDataPlaneLocator(sf, sffDst, isForwardPath);
            transportProcessor.configureSfPathMapperFlow(entry, sfDpl);
        }
    }

    /**
     * Call the appropriate flow creation methods on the TransportProcessor for
     * the NextHop table.
     *
     * @param entry
     *            - data for the current flows to be created
     * @param sffGraph
     *            - contains data for the RSP
     * @param transportProcessor
     *            - specific TransportProcessor to call into
     */
    private void configureNextHopFlows(SffGraph.SffGraphEntry entry, SffGraph sffGraph,
            SfcRspTransportProcessorBase transportProcessor) {

        LOG.debug("configureNextHopFlows: entry:{}, sffGraph:{}", entry, sffGraph);

        long pathId = entry.getPathId();
        boolean isForwardPath = entry.isForwardPath();
        SfName dstSfName = entry.getSf();
        SfName srcSfName = entry.getPrevSf();
        SffName dstSffName = entry.getDstSff();
        SffName srcSffName = entry.getSrcSff();
        ServiceFunction dstSf = sfcOfProviderUtils.getServiceFunction(dstSfName, pathId);
        ServiceFunction srcSf = sfcOfProviderUtils.getServiceFunction(srcSfName, pathId);
        ServiceFunctionForwarder dstSff = sfcOfProviderUtils.getServiceFunctionForwarder(dstSffName, pathId);
        ServiceFunctionForwarder srcSff = sfcOfProviderUtils.getServiceFunctionForwarder(srcSffName, pathId);
        SfDataPlaneLocator dstSfDpl;
        SfDataPlaneLocator srcSfDpl;
        dstSfDpl = sfcOfProviderUtils.getEgressSfDataPlaneLocator(dstSf, dstSff, isForwardPath);
        srcSfDpl = sfcOfProviderUtils.getIngressSfDataPlaneLocator(srcSf, srcSff, isForwardPath);

        if (dstSfDpl != null) {
            if (srcSffName.equals(SffGraph.INGRESS)) {
                // Configure the GW-SFF-SF NextHop using sfDpl
                transportProcessor.configureNextHopFlow(entry, (SffDataPlaneLocator) null, dstSfDpl);

                // If its Ingress, nothing else to be done
                return;
            } else {
                SffDataPlaneLocatorName sffEgressDpl = sffGraph.getSffEgressDpl(srcSffName, pathId);
                SffDataPlaneLocator sffSrcEgressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(srcSff, sffEgressDpl);
                // Configure the SFF-SFF-SF NextHop using sfDpl
                transportProcessor.configureNextHopFlow(entry, sffSrcEgressDpl, dstSfDpl);
            }
        }

        if (dstSffName.equals(SffGraph.EGRESS)) {
            // If dstSff is EGRESS, the SF is actually on the srcSff
            // Configure the SF-SFF-GW NextHop, we dont have the DstDpl, leaving
            // it blank
            transportProcessor.configureNextHopFlow(entry, srcSfDpl, (SffDataPlaneLocator) null);
        }

        if (entry.isIntraLogicalSFFEntry()) {
            // SFF-SFF nexthop is not needed in logical SFF,
            // the underlying tunnels already have ips set in the tunnel mesh
            return;
        }

        // Configure the SFF-SFF NextHop using the sfDpl and sffDstIngressDpl
        if (srcSfDpl != null) {
            if (srcSffName.getValue().equals(dstSffName.getValue())) {
                // If the next hop is on this SFF then go straight to the next
                // SF
                // Configure SF-SFF-SF NextHop on the same SFF
                transportProcessor.configureNextHopFlow(entry, srcSfDpl, dstSfDpl);
            } else {
                // Configure the SFF-SFF NextHop using the sfDpl and
                // sffDstIngressDpl
                SffDataPlaneLocatorName sffIngressDpl = sffGraph.getSffIngressDpl(dstSffName, pathId);
                SffDataPlaneLocator sffDstIngressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(dstSff, sffIngressDpl);
                transportProcessor.configureNextHopFlow(entry, srcSfDpl, sffDstIngressDpl);
            }
        }
    }

    /**
     * Call the appropriate flow creation methods on the TransportProcessor for
     * the TransportEgress table.
     *
     * @param entry
     *            - data for the current flows to be created
     * @param sffGraph
     *            - contains data for the RSP
     * @param transportProcessor
     *            - specific TransportProcessor to call into
     */
    private void configureTransportEgressFlows(SffGraph.SffGraphEntry entry, SffGraph sffGraph,
            SfcRspTransportProcessorBase transportProcessor) {

        long pathId = entry.getPathId();
        boolean isForwardPath = entry.isForwardPath();
        SfName dstSfName = entry.getSf();
        SffName dstSffName = entry.getDstSff();
        SffName srcSffName = entry.getSrcSff();
        ServiceFunction dstSf = sfcOfProviderUtils.getServiceFunction(dstSfName, pathId);
        ServiceFunctionForwarder dstSff = sfcOfProviderUtils.getServiceFunctionForwarder(dstSffName, pathId);
        ServiceFunctionForwarder srcSff = sfcOfProviderUtils.getServiceFunctionForwarder(srcSffName, pathId);
        SfDataPlaneLocator dstSfDpl;
        SffDataPlaneLocator dstSffDpl;
        dstSfDpl = sfcOfProviderUtils.getEgressSfDataPlaneLocator(dstSf, dstSff, isForwardPath);
        dstSffDpl = sfcOfProviderUtils.getEgressSffDataPlaneLocator(dstSf, dstSff, isForwardPath);
        SffDataPlaneLocatorName sffEgressDpl = sffGraph.getSffEgressDpl(srcSffName, pathId);
        SffDataPlaneLocator sffSrcEgressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(srcSff, sffEgressDpl);

        // Configure the SFF-Egress Transport Egress

        if (dstSffName.equals(SffGraph.EGRESS)) {
            DataPlaneLocator pathEgressDpl = sffGraph.getPathEgressDpl(pathId);
            transportProcessor.configureSffTransportEgressFlow(entry, sffSrcEgressDpl, null, pathEgressDpl);
            return;
        }

        if (dstSfDpl != null) {
            transportProcessor.configureSfTransportEgressFlow(entry, dstSffDpl, dstSfDpl, dstSfDpl);
        }

        // Nothing else to be done for Ingress
        if (srcSffName.equals(SffGraph.INGRESS)) {
            return;
        }

        // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
        if (!srcSffName.getValue().equals(dstSffName.getValue()) || entry.isIntraLogicalSFFEntry()
                && !entry.getSrcDpnId().getValue().equals(entry.getDstDpnId().getValue())) {

            SffDataPlaneLocatorName sffIngressDpl = sffGraph.getSffIngressDpl(dstSffName, entry.getPathId());
            SffDataPlaneLocator sffDstIngressDpl = sfcOfProviderUtils.getSffDataPlaneLocator(dstSff, sffIngressDpl);
            // This is the HOP DPL details between srcSFF and dstSFF, for
            // example: VLAN ID 100
            DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(dstSffName, pathId);

            transportProcessor.configureSffTransportEgressFlow(entry, sffSrcEgressDpl, sffDstIngressDpl,
                    dstHopIngressDpl);
        }
    }

    /*
     * TODO what about SFG?? Currently there is nobody available to maintain the
     * Service Function Groups (SFGs) code, nor are there any tests available
     *
     * private void configureSffEgressForGroup(SffGraph.SffGraphEntry entry,
     * SffGraph sffGraph) { }
     *
     * private void configureGroupNextHopFlow(final SffName sffName,
     * SffDataPlaneLocator srcSffDpl, long groupId, String groupName, final long
     * pathId, final short serviceIndex) { }
     *
     */

    /**
     * Initialize the SFF by creating the match any flows, if not already
     * created.
     *
     * @param entry
     *            - contains the SFF and RSP id
     * @param transportProcessor
     *            the transport processor to use when initialization flows are
     *            transport-dependent
     */
    private void initializeSff(SffGraph.SffGraphEntry entry, SfcRspTransportProcessorBase transportProcessor) {
        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            return;
        }

        String sffNodeName = sfcOfProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId(),
                entry.getDstDpnId());
        if (sffNodeName == null) {
            throw new SfcRenderingException("initializeSff SFF [" + entry.getDstSff().getValue() + "] does not exist");
        }

        NodeId sffNodeId = new NodeId(sffNodeName);
        if (!getSffInitialized(sffNodeId)) {
            LOG.debug("Initializing SFF [{}] node [{}]", entry.getDstSff().getValue(), sffNodeName);

            /* For OVS DPDK, add default NORMAL action flows */
            Long outputPort = SfcOvsUtil.getDpdkOfPort(sffNodeName, null);
            if (outputPort != null) {
                this.sfcOfFlowProgrammer.configureClassifierTableDpdkOutput(sffNodeName, outputPort);
                this.sfcOfFlowProgrammer.configureClassifierTableDpdkInput(sffNodeName, outputPort);
            }

            transportProcessor.configureClassifierTableMatchAny(sffNodeName);
            if (entry.usesLogicalSFF()) {
                this.sfcOfFlowProgrammer.configureTransportIngressTableMatchAnyResubmit(sffNodeName,
                        NwConstants.LPORT_DISPATCHER_TABLE);
                this.sfcOfFlowProgrammer.configureTransportEgressTableMatchAnyResubmit(sffNodeName,
                        NwConstants.LPORT_DISPATCHER_TABLE);
            } else {
                this.sfcOfFlowProgrammer.configureTransportIngressTableMatchAny(sffNodeName);
                this.sfcOfFlowProgrammer.configureTransportEgressTableMatchAny(sffNodeName);
            }
            this.sfcOfFlowProgrammer.configurePathMapperTableMatchAny(sffNodeName);
            this.sfcOfFlowProgrammer.configurePathMapperAclTableMatchAny(sffNodeName);
            this.sfcOfFlowProgrammer.configureNextHopTableMatchAny(sffNodeName);

            setSffInitialized(sffNodeId, true);
        }
    }

    //
    // Internal util methods
    //

    /**
     * Given an SFF name, determine if its been initialized yet or not. Called
     * by initializeSff()
     *
     * @param sffNodeId
     *            The SFF node ID to check
     * @return true if its been initialized, false otherwise
     */
    private boolean getSffInitialized(final NodeId sffNodeId) {
        Boolean isInitialized = sffInitialized.get(sffNodeId);

        if (isInitialized == null) {
            return false;
        }

        return isInitialized;
    }

    /**
     * Set a given SFF as initialized. Called by initializeSff()
     *
     * @param sffNodeId
     *            - the SFF to set
     * @param initialized
     *            - boolean value to set the SFF as
     */
    private void setSffInitialized(final NodeId sffNodeId, boolean initialized) {
        // If the value is already in the map, its value will be replaced
        sffInitialized.put(new NodeId(sffNodeId), initialized);
    }

    private String getTransportEncapName(final String transportName, final String encapName) {
        return transportName + TRANSPORT_ENCAP_SEPARATOR_STRING + encapName;
    }

    /**
     * Private getter (eases mocking).
     *
     * @return the instance providing access to Genius RPCs
     */
    private SfcGeniusRpcClient getGeniusRpcClient() {
        return theGeniusRpcClient;
    }
}
