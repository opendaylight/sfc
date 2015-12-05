/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import java.util.Iterator;

import org.opendaylight.sfc.l2renderer.SffGraph.SffGraphEntry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MacBuilder;

public class SfcRspProcessorVlan extends SfcRspTransportProcessorBase {
    private static final int VLAN_ID_INCR_HOP = 1;
    private static final int VLAN_ID_INCR_RSP = 100;
    private static int lastVlanId = 0;

    public SfcRspProcessorVlan() {
    }

    @Override
    public void setRspTransports() {
        int hopIncrement = VLAN_ID_INCR_HOP;
        int transportData = lastVlanId + VLAN_ID_INCR_RSP;
        lastVlanId = transportData;

        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            LOG.debug("RspTransport entry: {}", entry);

            if (entry.getSrcSff().equals(entry.getDstSff())) {
                // It may be that multiple SFs are on the same SFF
                // If so, we dont need to set the transports again
                // Otherwise the SFF ingress DPL will be overwritten
                continue;
            }

            DataPlaneLocatorBuilder dpl = new DataPlaneLocatorBuilder();
            dpl.setTransport(rsp.getTransportType());
            MacBuilder macBuilder = new MacBuilder();
            macBuilder.setVlanId(transportData);
            dpl.setLocatorType(macBuilder.build());

            if (entry.getDstSff().equals(SffGraph.EGRESS)) {
                sffGraph.setPathEgressDpl(entry.getPathId(), dpl.build());
            } else {
                sffGraph.setHopIngressDpl(entry.getDstSff(), entry.getPathId(), dpl.build());
            }
            transportData += hopIncrement;
        }
    }

    //
    // TransportIngress methods
    //

    @Override
    public void configureSfTransportIngressFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        this.sfcFlowProgrammer.configureVlanTransportIngressFlow(sffNodeName);
    }

    @Override
    public void configureSffTransportIngressFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        this.sfcFlowProgrammer.configureVlanTransportIngressFlow(sffNodeName);
    }

    //
    // PathMapper methods
    //

    @Override
    public void configureSfPathMapperFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        Integer vlanTag = ((MacAddressLocator) sfDpl.getLocatorType()).getVlanId();
        if (vlanTag != null) {
            this.sfcFlowProgrammer.configureVlanPathMapperFlow(sffNodeName, vlanTag, entry.getPathId(), true);
        }
    }

    public void configureSffPathMapperFlow(SffGraph.SffGraphEntry entry, DataPlaneLocator hopDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        Integer vlanTag = ((MacAddressLocator) hopDpl.getLocatorType()).getVlanId();
        if (vlanTag != null) {
            this.sfcFlowProgrammer.configureVlanPathMapperFlow(sffNodeName, vlanTag, entry.getPathId(), false);
        }
    }

    //
    // NextHop methods
    //

    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getDplPortInfoMac(srcSffDpl);
        String dstMac = sfcProviderUtils.getSfDplMac(dstSfDpl);
        this.sfcFlowProgrammer.configureMacNextHopFlow(sffNodeName, entry.getPathId(), srcMac, dstMac);
    }

    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SffDataPlaneLocator dstSffDpl) {
        // in this case, we use the SrcSff instead of the typical DstSff
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getSfDplMac(srcSfDpl);
        String dstMac = sfcProviderUtils.getDplPortInfoMac(dstSffDpl);
        this.sfcFlowProgrammer.configureMacNextHopFlow(sffNodeName, entry.getPathId(), srcMac, dstMac);
    }

    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SfDataPlaneLocator dstSfDpl) {
        // in this case, we use the SrcSff instead of the typical DstSff
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getSfDplMac(srcSfDpl);
        String dstMac = sfcProviderUtils.getSfDplMac(dstSfDpl);
        this.sfcFlowProgrammer.configureMacNextHopFlow(sffNodeName, entry.getPathId(), srcMac, dstMac);
    }

    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getDplPortInfoMac(srcSffDpl);
        String dstMac = sfcProviderUtils.getDplPortInfoMac(dstSffDpl);
        this.sfcFlowProgrammer.configureMacNextHopFlow(sffNodeName, entry.getPathId(), srcMac, dstMac);
    }

    //
    // TransportEgress methods
    //

    @Override
    public void configureSfTransportEgressFlow(
            SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl, DataPlaneLocator hopDpl) {
        Integer vlanTag = ((MacAddressLocator) hopDpl.getLocatorType()).getVlanId();
        if (vlanTag == null) {
            return;
        }

        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        if (srcOfsPortStr == null) {
            throw new RuntimeException("configureSffTransportEgressFlow OFS port not avail for SFF ["
                    + entry.getDstSff() + "] sffDpl [" + srcSffDpl.getName().getValue() + "]");
        }

        // For the SF transport Egress flow, we'll write to the Dst SFF as opposed to typically writing to the Src SFF
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getDplPortInfoMac(srcSffDpl);
        String dstMac = sfcProviderUtils.getSfDplMac(dstSfDpl);

        ServiceFunction sf = sfcProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
        // FIXME: I would caution against this approach. Instead you may want to see if
        // ServiceFunctionType has "bidirectional" = True in future.
        if (sf.getType().getValue().equals("tcp-proxy")) {
            // If the SF is a TCP Proxy, we need this additional flow for the SF:
            // - a flow that will also check for TCP Syn and do a PktIn
            this.sfcFlowProgrammer.configureArpTransportIngressFlow(sffNodeName, srcMac);

            // TODO need to figure out when to call the following
            //this.sfcFlowProgrammer.configureVlanSfTransportEgressFlow(
            //        sffNodeName, srcMac, dstMac, vlanTag,
            //        srcOfsPortStr, entry.getPathId(), true);
        } else {
            this.sfcFlowProgrammer.configureVlanSfTransportEgressFlow(
                    sffNodeName, srcMac, dstMac, vlanTag,
                    srcOfsPortStr, entry.getPathId(), false);
        }
    }

    @Override
    public void configureSffTransportEgressFlow(
            SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl, DataPlaneLocator hopDpl) {
        Integer vlanTag = ((MacAddressLocator) hopDpl.getLocatorType()).getVlanId();
        if (vlanTag == null) {
            return;
        }

        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        if (srcOfsPortStr == null) {
            throw new RuntimeException("configureSffTransportEgressFlow OFS port not avail for SFF ["
                    + entry.getDstSff() + "] sffDpl [" + srcSffDpl.getName().getValue() + "]");
        }

        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getDplPortInfoMac(srcSffDpl);
        String dstMac = sfcProviderUtils.getDplPortInfoMac(dstSffDpl);
        this.sfcFlowProgrammer.configureVlanTransportEgressFlow(
                sffNodeName, srcMac, dstMac, vlanTag,
                srcOfsPortStr, entry.getPathId());
    }
}
