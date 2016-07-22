/*
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
 * @author ebrjohn
 * Eth+NSH will be used between the SFF and SF, and VXGPE+NSH will still be used
 * between the SFFs.
 * In the future when the RspManager is finished, we wont have to mix transports
 * in this class, as it will be called per hop.
 *
 */
public class SfcRspProcessorNshEth extends SfcRspTransportProcessorBase {

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
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId()),
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
        // The dstSffDpl is intentionally set to null for certain cases that
        // can be used by other transports. For Nsh, just skip it.
        if(dstSffDpl != null) {
            IpPortLocator dstSffLocator = (IpPortLocator) dstSffDpl.getDataPlaneLocator().getLocatorType();
            this.sfcFlowProgrammer.configureNshVxgpeNextHopFlow(
                    sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId()),
                    new String(dstSffLocator.getIp().getValue()),
                    entry.getPathId(),
                    entry.getServiceIndex());
        }
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
        IpPortLocator dstSffLocator = (IpPortLocator) dstSffDpl.getDataPlaneLocator().getLocatorType();
        this.sfcFlowProgrammer.configureNshVxgpeNextHopFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId()),
                new String(dstSffLocator.getIp().getValue()),
                entry.getPathId(),
                entry.getServiceIndex());
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
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        this.sfcFlowProgrammer.configureNshEthTransportEgressFlow(
                sffNodeName, entry.getPathId(), entry.getServiceIndex(), srcOfsPortStr);
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
        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            this.sfcFlowProgrammer.configureNshNscTransportEgressFlow(
                    sffNodeName, nsp, nsi, OutputPortValues.INPORT.toString());
            this.sfcFlowProgrammer.configureNshVxgpeLastHopTransportEgressFlow(
                    sffNodeName, nsp, nsi, srcOfsPortStr);
        } else {
            LOG.debug("configureSffTransportEgressFlow:writing transport egress for sff {}", sffNodeName);
            this.sfcFlowProgrammer.configureNshVxgpeTransportEgressFlow(
                sffNodeName, nsp, nsi, srcOfsPortStr);

        }
    }

    private List<String> getIpsForLastSFFInChain(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl) {
        List<String> ips = new ArrayList<>();
        if (entry.getSrcDpnId() == null) {
            ips.add(new String(((IpPortLocator) srcSffDpl.getDataPlaneLocator().getLocatorType()).getIp().getValue()));
        } else {
            List<IpAddress> geniusIps = SfcGeniusRpcClient.getInstance().getEndpointIpForDPN(entry.getSrcDpnId());
            for (IpAddress anIp: geniusIps) {
                ips.add(new String(anIp.getValue()));
            }
        }
        return ips;
    }

    @Override
    public void setRspTransports() {
        // TODO Auto-generated method stub

    }

//    private List<String> getIpsForSff(DpnIdType dpnid) {
//        List<String> ips = new ArrayList<>();
//
//            List<IpAddress> geniusIps = SfcGeniusRpcClient.getInstance().getEndpointIpForDPN(dpnid);
//            for (IpAddress anIp: geniusIps) {
//                ips.add(new String(anIp.getValue()));
//            }
//
//        return ips;
//    }
    /** Given a {@link}SfDataPlaneLocator, the method returns the mac address
     * @param dstSfDpl the data plane locator
     * @return  the {@link}MacAddress
     */
    private MacAddress getMacAddress(SfDataPlaneLocator dstSfDpl) {
        MacAddress theMacAddr = null;
        LOG.debug("getMacAddress:starting. dstSfDpl:{}", dstSfDpl);
        theMacAddr = ((MacAddressLocator) dstSfDpl.getLocatorType()).getMac();
        return theMacAddr;
    }
}
