/*
 * Copyright (c) 2016 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.processors;

import java.util.List;
import java.util.Optional;

import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.genius.util.appcoexistence.SfcTableIndexMapper;
import org.opendaylight.sfc.genius.util.appcoexistence.SfcTableIndexMapperBuilder;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.sfc.ofrenderer.processors.SffGraph.SffGraphEntry;
import org.opendaylight.sfc.ofrenderer.utils.operDsUpdate.OperDsUpdateHandlerInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.LogicalInterfaceLocator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;

/**
 * RSP processor class for RSPs which use Logical SFFs
 * Transport protocols are the same than in the NshEth processor (that is,
 * Eth+NSH between the SFF and SF, and VXGPE+NSH between the SFFs)
 * Differences with NSH-ETH processor:
 *
 *  - Even though method signatures use both Sff and Sf data plane locators (for keeping the
 *    interfaces defined in the base transport processor), they are not used in
 *    general (Logical SFFs don't have DPLs; SFs connected to logical SFFs use
 *    Logical Interfaces as DPLs). Instead, the data plane node ids for the switches
 *    participating in the hop (stored in the SffGraphEntry container) are extensively used.
 * - Transport egress actions are provided by Genius
 *
 * In the future when the RspManager is finished, we
 * wont have to mix transports in this class, as it will be called per hop.
 *
 * @author ediegra
 *
 */
public class SfcRspProcessorLogicalSff extends SfcRspTransportProcessorBase {

    private final SfcTableIndexMapper tableIndexMapper;
    private SfcGeniusRpcClient sfcGeniusRpcClient;
    private OperDsUpdateHandlerInterface operDsHandler;

    public SfcRspProcessorLogicalSff(SfcGeniusRpcClient sfcGeniusRpcClient, OperDsUpdateHandlerInterface operDsHandler) {
        // This transport processor relies on Genius for retrieving correct table indexes. In order
        // not to create a circular dependency between this class and Genius, this processor
        // provides Genius mapping class with the tables it uses for each function
        SfcTableIndexMapperBuilder builder = new SfcTableIndexMapperBuilder();
        builder.setTransportIngressTable(SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_INGRESS);
        builder.setPathMapperTable(SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER);
        builder.setPathMapperAclTable(SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER_ACL);
        builder.setNextHopTable(SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        builder.setTransportEgressTable(SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);

        tableIndexMapper = builder.build();
        this.sfcGeniusRpcClient = sfcGeniusRpcClient;
        this.operDsHandler = operDsHandler;
    }
    //
    // TransportIngress methods
    //

    /*
     * Configure the Transport Ingress flow for SFs
     * Not needed since the same flow will be created in configureSffTransportIngressFlow()
     *
     * @param entry - RSP hop info used to create the flow
     */
    @Override
    public void configureSfTransportIngressFlow(SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
    }

    /*
     * Configure the Transport Ingress flow for SFFs
     *
     * @param entry - RSP hop info used to create the flow
     * @param dstSffDpl - Not used in this processor
     */
    @Override
    public void configureSffTransportIngressFlow(SffGraphEntry entry, SffDataPlaneLocator dstSffDpl) {
        this.sfcFlowProgrammer.configureNshVxgpeTransportIngressFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId(), entry.getDstDpnId()),
                entry.getPathId(),
                entry.getServiceIndex());
    }

    //
    // PathMapper methods - not needed for NSH
    //

    /*
     */
    @Override
    public void configureSfPathMapperFlow(SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
    }

    /*
     */
    @Override
    public void configureSffPathMapperFlow(SffGraphEntry entry, DataPlaneLocator hopDpl) {
    }

    //
    // NextHop methods
    //
    /**
     * Configure the Next Hop flow from an SFF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - not used in this processor
     * @param dstSfDpl - not used in this processor
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry,
                                     SffDataPlaneLocator srcSffDpl,
                                     SfDataPlaneLocator dstSfDpl) {

        Optional<MacAddress> srcSfMac = getMacAddress(dstSfDpl, true);
        Optional<MacAddress> dstSfMac = getMacAddress(dstSfDpl, false);
        if (!srcSfMac.isPresent() || !dstSfMac.isPresent()) {
            throw new SfcRenderingException("Failed on mac address retrieval for dst SF dpl [" + dstSfDpl + "]");
        }
        LOG.debug("configureNextHopFlow from SFF to SF, SrcDpnId: {}, srcSfMac:{}, DstDpnId: {}, dstSfMac:{}, nsi:{}",
                   entry.getSrcDpnId(), srcSfMac, entry.getDstDpnId(), dstSfMac, entry.getServiceIndex());
        this.sfcFlowProgrammer.configureNshEthNextHopFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(),
                        entry.getPathId(), entry.getDstDpnId()),
                srcSfMac.get().getValue(), dstSfMac.get().getValue(),
                entry.getPathId(),
                entry.getServiceIndex());
    }

    /*
     * Configure the Next Hop flow from an SF to an SFF
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SffDataPlaneLocator dstSffDpl) {
        // SF-SFF nexthop is not needed in logical SFF
        // (the underlying tunnels already have ips set in the tunnel mesh, only transport
        // egress port selection is required)
    }

    /*
     * Configure the Next Hop flow from an SF to an SF
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SfDataPlaneLocator dstSfDpl) {
        // SF-SF nexthop is not needed in logical SFF
        // (the underlying tunnels already have ips set in the tunnel mesh, only transport
        // egress port selection is required)
    }

    /*
     * Configure the Next Hop flow from an SFF to an SFFq
     *
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl) {
        // SFF-SFF nexthop is not needed in logical SFF
        // (the underlying tunnels already have ips set in the tunnel mesh, only port selection
        //is required)
    }


    //
    // TransportEgress methods
    //

    /*
     * Configure the Transport Egress flow from an SFF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - not used in this processor
     * @param dstSfDpl - not used in this processor
     * @param hopDpl - not used in this processor
     */
    @Override
    public void configureSfTransportEgressFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
            SfDataPlaneLocator dstSfDpl, DataPlaneLocator hopDpl) {

        ServiceFunction sfDst = sfcProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
        String sfLogicalInterface = SfcGeniusDataUtils.getSfLogicalInterface(sfDst);
        LOG.debug("configureTransportEgressFlows:sff->sf egress from a logical sff. "
                + "Target interface:{} si:{}",
                sfLogicalInterface, entry.getServiceIndex());

        // When the SF is using a logical SFF, the transport egress flows are provided by Genius
        Optional<List<Action>> actionList =
                sfcGeniusRpcClient.getEgressActionsFromGeniusRPC(sfLogicalInterface, false, 0);

            if (!actionList.isPresent() || actionList.get().isEmpty()) {
                throw new SfcRenderingException("Failure during transport egress config. Genius did not return"
                        + " egress actions for logical interface [" + sfLogicalInterface
                        + "] (sf:" + sfDst + ")");
            }
            sfcFlowProgrammer.configureNshEthTransportEgressFlow(
                    sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId(), entry.getDstDpnId()),
                    entry.getPathId(), entry.getServiceIndex(), actionList.get());
    }

    /**
     * Configure the Transport Egress flow from an SFF to an SFF / chain
     * egress, depending on the graph entry
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - not used in this processor
     * @param dstSffDpl - not used in this processor
     * @param hopDpl - not used in this processor
     */
    @Override
    public void configureSffTransportEgressFlow(
            SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl, DataPlaneLocator hopDpl) {
        long nsp = entry.getPathId();
        short nsi = entry.getServiceIndex();
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId(), entry.getSrcDpnId());

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            LOG.debug("configureSffTransportEgressFlow: called for chain egress");
            SfDataPlaneLocator srcSfDpl = sfcProviderUtils
                    .getSfDataPlaneLocator(
                            sfcProviderUtils.getServiceFunction(
                                    entry.getPrevSf(), entry.getPathId()),
                            entry.getSrcSff());
            Optional<MacAddress> macAddress = getMacAddress(srcSfDpl, false);
            if (!macAddress.isPresent()) {
                throw new RuntimeException("Failed on mac address retrieval for dst SF dpl [" + srcSfDpl + "]");
            }

            this.sfcFlowProgrammer.configureNshEthLastHopTransportEgressFlow(
                    sffNodeName, nsp, nsi, macAddress.get());
        } else {
            LOG.debug("configureSffTransportEgressFlow: called for non-final graph entry");
            if (entry.isIntraLogicalSFFEntry()) {
                // in this case, use Genius to program egress flow
                // 1. Get dpid for both source sff, dst sff
                DpnIdType srcDpid = entry.getSrcDpnId();
                DpnIdType dstDpid = entry.getDstDpnId();
                // 2, use genius to retrieve dst interface name (ITM manager RPC)
                Optional<String> targetInterfaceName = sfcGeniusRpcClient
                        .getTargetInterfaceFromGeniusRPC(srcDpid, dstDpid);
                if (!targetInterfaceName.isPresent()) {
                    throw new SfcRenderingException("Failure during transport egress config. Genius did not return"
                            + " the interface to use between src dpnid:"
                            + srcDpid + "and dst dpnid:" + dstDpid + ")");
                }

                LOG.debug("configureSffTransportEgressFlow: srcDpn [{}] destDpn [{}] interface to use: [{}]",
                        srcDpid, dstDpid, targetInterfaceName.get());
                // 3, use genius for retrieving egress actions (Interface Manager RPC)
                Optional<List<Action>> actionList =
                        sfcGeniusRpcClient.getEgressActionsFromGeniusRPC(targetInterfaceName.get(), true, 0);

                if (!actionList.isPresent() || actionList.get().isEmpty()) {
                    throw new SfcRenderingException("Failure during transport egress config. Genius did not return"
                            + " egress actions for logical interface [" + targetInterfaceName.get()
                            + "] (src dpnid:" + srcDpid + "; dst dpnid:" + dstDpid + ")");
                }
                // 4, write those actions
                this.sfcFlowProgrammer.configureNshEthTransportEgressFlow(
                        sffNodeName, nsp, nsi, actionList.get());
            }
        }
    }

    @Override
    public void setRspTransports() {
    }

    /** Given a {@link}SfDataPlaneLocator for a SF which uses a logical
     * interface locator, the method returns the SF mac address (local end)
     * or the mac address for the OVS port to which the SF is connected
     * (remote end)
     * @param dstSfDpl the data plane locator
     * @param returnRemoteEnd true when the MAC for the OVS side is requested,
     *          false when the MAC for the SF side is requested
     *
     * @return  the optional {@link}MacAddress
     */
    private Optional<MacAddress> getMacAddress(SfDataPlaneLocator dstSfDpl,
            boolean returnRemoteEnd) {
        LOG.debug("getMacAddress:starting. dstSfDpl:{}, requested side is SFF? {}", dstSfDpl, returnRemoteEnd);
        String ifName = ((LogicalInterfaceLocator) dstSfDpl.getLocatorType())
                .getInterfaceName();
        Optional<MacAddress> theMacAddr = returnRemoteEnd
                ? SfcGeniusDataUtils.getServiceFunctionForwarderPortMacAddress(ifName)
                : SfcGeniusDataUtils.getServiceFunctionMacAddress(ifName);
        LOG.debug(
                "Read interface's [{}] (remoteEndMAC requested {}) MAC address [{}]",
                ifName, returnRemoteEnd, theMacAddr.isPresent()
                        ? theMacAddr.get().getValue() : "(empty)");
        return theMacAddr;
    }

    @Override
    public Optional<SfcTableIndexMapper> getTableIndexMapper() {
        return Optional.of(tableIndexMapper);
    }


    @Override
    public void configureClassifierTableMatchAny(final String sffNodeName) {
        // classifier table is not used in chains rendered by the LogicalSFF processor
    }

    @Override
    public void updateOperationalDSInfo(SffGraph theGraph, RenderedServicePath rsp) {
        operDsHandler.onRspCreation(theGraph, rsp);
    }
}
