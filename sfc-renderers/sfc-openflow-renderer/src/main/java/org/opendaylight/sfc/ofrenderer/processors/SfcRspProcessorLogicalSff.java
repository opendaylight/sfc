/*
*
 * Copyright (c) 2016 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.processors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opendaylight.sfc.genius.rpc.SfcGeniusRpcClient;
import org.opendaylight.sfc.ofrenderer.processors.SffGraph.SffGraphEntry;
import org.opendaylight.sfc.ofrenderer.utils.SfcLogicalInterfaceOfUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.LogicalInterfaceLocator;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

/**
 * @author ediegra
 * Eth+NSH will be used between the SFF and SF, and VXGPE+NSH will still be used
 * between the SFFs.
 * In the future when the RspManager is finished, we wont have to mix transports
 * in this class, as it will be called per hop.
 *
 */
public class SfcRspProcessorLogicalSff extends SfcRspTransportProcessorBase {

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
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry,
                                     SffDataPlaneLocator srcSffDpl,
                                     SfDataPlaneLocator dstSfDpl) {

        MacAddress theMacAddr = getMacAddress(dstSfDpl);
        this.sfcFlowProgrammer.configureNshEthNextHopFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(),
                        entry.getPathId(), entry.getDstDpnId()),
                theMacAddr.getValue(), entry.getPathId(),
                entry.getServiceIndex());

    }

    /*
     * Configure the Next Hop flow from an SF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSfDpl  - the particular SF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SffDataPlaneLocator dstSffDpl) {
        // SF-SFF nexthop is not needed in logical SFF
        // (the underlying tunnels already have ips setted in the tunnel mesh, only port selection
        //is required)

    }

    /*
     * Configure the Next Hop flow from an SFF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSfDpl - the particular SF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SfDataPlaneLocator dstSfDpl) {

        MacAddress dstSfMac = getMacAddress(dstSfDpl);
        this.sfcFlowProgrammer.configureNshEthNextHopFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId(), entry.getDstDpnId()),
                dstSfMac.getValue(),
                entry.getPathId(),
                entry.getServiceIndex());
    }

    /*
     * Configure the Next Hop flow from an SFF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl) {
        // SFF-SFF nexthop is not needed in logical SFF
        // (the underlying tunnels already have ips setted in the tunnel mesh, only port selection
        //is required)
    }


    //
    // TransportEgress methods
    //

    /*
     * Configure the Transport Egress flow from an SFF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     * @param hopDpl - Hop DPL used to create the flow
     */
    @Override
    public void configureSfTransportEgressFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
            SfDataPlaneLocator dstSfDpl, DataPlaneLocator hopDpl) {
        LOG.debug("configureTransportEgressFlows:sff->sf egress from a logical sff");
        ServiceFunction sfDst = sfcProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
        // When the SF is using a logical SFF, the transport egress flows are retrieved from genius
        String sfLogicalInterface = SfcLogicalInterfaceOfUtils.getSfLogicalInterface(sfDst);
        LOG.debug("configureTransportEgressFlows:sff->sf egress. Target interface:{} si:{}",
                sfLogicalInterface, entry.getServiceIndex());
        List<Action> actionList = SfcGeniusRpcClient
                    .getInstance().getEgressActionsFromGeniusRPC(
                            sfLogicalInterface, false);
            if (actionList == null || actionList.isEmpty()) {
                throw new RuntimeException("Failure during transport egress config. Genius did not return"
                        + " egress actions for logical interface [" + sfLogicalInterface
                        + "] (sf:" + sfDst + ")");
            }
            sfcFlowProgrammer.configureSfTransportEgressFlow(
                    sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId(), entry.getDstDpnId()),
                    entry.getPathId(), entry.getServiceIndex(), actionList);
    }

    /**
     * Configure the Transport Egress flow from an SFF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     * @param hopDpl - Hop DPL used to create the flow
     */
    @Override
    public void configureSffTransportEgressFlow(
            SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl, DataPlaneLocator hopDpl) {
        long nsp = entry.getPathId();
        short nsi = entry.getServiceIndex();
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId(), entry.getSrcDpnId());

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            this.sfcFlowProgrammer.configureNshEthLastHopTransportEgressFlow(sffNodeName,nsp,nsi);
        } else {
            LOG.debug("configureSffTransportEgressFlow: called for non-final graph entry");
            if (entry.isIntraLogicalSFFEntry()) {
                // in this case, use Genius to program egress flow
                // 1. Get dpid for both source sff, dst sff
                DpnIdType srcDpid = entry.getSrcDpnId();
                DpnIdType dstDpid = entry.getDstDpnId();
                // 2, use genius to retrieve dst interface name (ITM manager RPC)
                String targetInterfaceName = SfcGeniusRpcClient.getInstance()
                        .getTargetInterfaceFromGeniusRPC(srcDpid, dstDpid);

                LOG.debug("configureSffTransportEgressFlow: srcDpn [{}] destDpn [{}] interface to use: [{}]", srcDpid, dstDpid, targetInterfaceName);
                // 3, use genius for retrieving egress actions (Interface Manager RPC)
                List<Action> actionList = SfcGeniusRpcClient
                        .getInstance().getEgressActionsFromGeniusRPC(
                                targetInterfaceName, true);
                if (actionList == null || actionList.isEmpty()) {
                    throw new RuntimeException("Failure during transport egress config. Genius did not return"
                            + " egress actions for logical interface [" + targetInterfaceName
                            + "] (src dpnid:" + srcDpid + "; dst dpnid:" + dstDpid + ")");
                }
                // 4, write those actions
                this.sfcFlowProgrammer.configureSffTransportEgressFlow(
                        sffNodeName, nsp, nsi, actionList);
            }
        }
    }

    @Override
    public void setRspTransports() {
        // TODO Auto-generated method stub
    }

    /** Given a {@link}SfDataPlaneLocator, the method returns the mac address
     * @param dstSfDpl the data plane locator
     * @return  the {@link}MacAddress
     */
    private MacAddress getMacAddress(SfDataPlaneLocator dstSfDpl) {
        MacAddress theMacAddr = null;
        LOG.debug("getMacAddress:starting. dstSfDpl:{}", dstSfDpl);
        String ifName = ((LogicalInterfaceLocator) dstSfDpl.getLocatorType())
                .getInterfaceName();
        theMacAddr = SfcLogicalInterfaceOfUtils.getServiceFunctionMacAddress(ifName);
        LOG.debug("Read interface's [{}] MAC address [{}]", ifName,
                theMacAddr != null ? theMacAddr.getValue() : "NULL");
        return theMacAddr;
    }


}