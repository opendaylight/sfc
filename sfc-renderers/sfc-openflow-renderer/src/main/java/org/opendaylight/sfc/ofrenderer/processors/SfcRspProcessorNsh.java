/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.processors;

import java.util.Iterator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sf.ovs.rev160107.SfDplOvsAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;


public class SfcRspProcessorNsh extends SfcRspTransportProcessorBase {

    /**
     * Set the RSP path egress DPL and SFF Hop Ingress DPLs for the NSH transport type.
     */
    @Override
    public void setRspTransports() {
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

            ServiceFunctionForwarder sff;
            if (entry.getDstSff().equals(SffGraph.EGRESS)) {
                sff = sfcProviderUtils.getServiceFunctionForwarder(entry.getSrcSff(), entry.getPathId());
            } else {
                sff = sfcProviderUtils.getServiceFunctionForwarder(entry.getDstSff(), entry.getPathId());
            }
            SffDataPlaneLocatorName sffEgressDplName = sffGraph.getSffEgressDpl(sff.getName(), entry.getPathId());
            LocatorType loc = sfcProviderUtils.getSffDataPlaneLocator(sff, sffEgressDplName)
                .getDataPlaneLocator()
                .getLocatorType();
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(((Ip) loc).getIp());
            ipBuilder.setPort(((Ip) loc).getPort());
            dpl.setLocatorType(ipBuilder.build());

            if (entry.getDstSff().equals(SffGraph.EGRESS)) {
                sffGraph.setPathEgressDpl(entry.getPathId(), dpl.build());
            } else {
                sffGraph.setHopIngressDpl(entry.getDstSff(), entry.getPathId(), dpl.build());
            }
        }
    }

    //
    // TransportIngress methods
    //

    /**
     * Configure the Transport Ingress flow for SFs
     *
     * @param entry - RSP hop info used to create the flow
     */
    @Override
    public void configureSfTransportIngressFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        /*
         * Currently the switch port the SF is connected to is stored by Tacker.
         * Here we take this name and convert it to the port number.
         */

        SfDplOvsAugmentation sfDplOvs = sfDpl.getAugmentation(SfDplOvsAugmentation.class);
        if(sfDplOvs == null) {
            LOG.info("SfcRspProcessorNsh::configureSfTransportIngressFlow NO sfDplOvs augmentation present");
            return;
        }
        LOG.info("SfcRspProcessorNsh::configureSfTransportIngressFlow sf [{}] sfDplOvs augmentation port [{}]",
                entry.getSf().getValue(), sfDplOvs.getOvsPort().getPortId());

        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        IpPortLocator dstSfLocator = (IpPortLocator) sfDpl.getLocatorType();
        String sfIp = new String(dstSfLocator.getIp().getValue());
        short vxlanUdpPort = dstSfLocator.getPort().getValue().shortValue();
        long sffPort = sfcProviderUtils.getPortNumberFromName(
                entry.getDstSff().getValue(), sfDplOvs.getOvsPort().getPortId(), entry.getPathId());

        /* SfLoopbackEncapsulatedEgress flow
           udp,nw_dst=11.0.0.5,tp_dst=6633 actions=output:4
           - match incoming vxlan packets destined for an sf, but the nw_dst is the sf ip and not the sff so it bypasses tunnel decap.
           - Key here is the nw_dst is the ip of the sf.
           - these packets would have been forwarded by the normal SFC egress rules, match NSH, set tun_dst, send out vtep.
             They would loop back to this bridge, hit this flow - bypassing the vtep and be forwarded to the sf
        */
        this.sfcFlowProgrammer.configureVxlanGpeSfLoopbackEncapsulatedEgressFlow(sffNodeName, sfIp, vxlanUdpPort, sffPort);

        /* SfReturnLoopbackIngress flow
           udp,in_port=4,tp_dst=6633 actions=LOCAL
           - match packets coming from the sf and that are vxlan packets - which means NSH and no decap
           - these packets are from the sf and encapped, so they simply get forwarded out the bridge LOCAL
             and end up coming back to the bridge into the vtep so that the hit the normal SFF flows.
         */
        this.sfcFlowProgrammer.configureVxlanGpeSfReturnLoopbackIngressFlow(sffNodeName, vxlanUdpPort, sffPort);

    }

    /**
     * Configure the Transport Ingress flow for SFFs
     *
     * @param entry - RSP hop info used to create the flow
     */
    @Override
    public void configureSffTransportIngressFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator dstSffDpl) {
        // TODO later use the dstSffDpl to get the tap port number
        this.sfcFlowProgrammer.configureVxlanGpeTransportIngressFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId()),
                entry.getPathId(),
                entry.getServiceIndex());
    }

    //
    // PathMapper methods
    //

    /**
     * Configure the Path Mapper flow for SFs
     *
     * @param entry - RSP hop info used to create the flow
     * @param sfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureSfPathMapperFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        // Path Mapping is not needed for NSH, since the path is in the NSH header
    }

    /**
     * Configure the Path Mapper flow for SFFs
     *
     * @param entry - RSP hop info used to create the flow
     * @param hopDpl - the particular SFF Hop DPL used to create the flow
     */
    @Override
    public void configureSffPathMapperFlow(SffGraph.SffGraphEntry entry, DataPlaneLocator hopDpl) {
        // Path Mapping is not needed for NSH, since the path is in the NSH header
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
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl) {
        IpPortLocator dstSfLocator = (IpPortLocator) dstSfDpl.getLocatorType();
        this.configureNextHopFlow(entry, entry.getDstSff(), new String(dstSfLocator.getIp().getValue()));
    }

    /**
     * Configure the Next Hop flow from an SF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSfDpl - the particular SF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SffDataPlaneLocator dstSffDpl) {
        // The dstSffDpl is intentionally set to null for certain cases that
        // can be used by other transports. For Nsh, just skip it.
        if(dstSffDpl != null) {
            IpPortLocator dstSffLocator = (IpPortLocator) dstSffDpl.getDataPlaneLocator().getLocatorType();
            this.configureNextHopFlow(entry, entry.getSrcSff(), new String(dstSffLocator.getIp().getValue()));
        }
    }

    /**
     * Configure the Next Hop flow from an SF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSfDpl - the particular SF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SfDataPlaneLocator dstSfDpl) {
        IpPortLocator dstSfLocator = (IpPortLocator) dstSfDpl.getLocatorType();
        this.configureNextHopFlow(entry, entry.getSrcSff(), new String(dstSfLocator.getIp().getValue()));
    }

    /**
     * Configure the Next Hop flow from an SFF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl) {
        IpPortLocator dstSffLocator = (IpPortLocator) dstSffDpl.getDataPlaneLocator().getLocatorType();
        this.configureNextHopFlow(entry, entry.getDstSff(), new String(dstSffLocator.getIp().getValue()));
    }

    /**
     * Internal util method for the above 4 configureNextHopFlow() methods
     *
     * @param entry - RSP hop info used to create the flow
     * @param dstIp - VxLan Next Hop Dest IP
     */
    private void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffName sffName, final String dstIp) {
        this.sfcFlowProgrammer.configureVxlanGpeNextHopFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(sffName, entry.getPathId()),
                dstIp, entry.getPathId(), entry.getServiceIndex());
    }

    //
    // TransportEgress methods
    //

    /**
     * Configure the Transport Egress flow from an SFF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     * @param hopDpl - Hop DPL used to create the flow
     */
    @Override
    public void configureSfTransportEgressFlow(
            SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl, DataPlaneLocator hopDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        this.sfcFlowProgrammer.configureVxlanGpeTransportEgressFlow(
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
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            this.sfcFlowProgrammer.configureNshNscTransportEgressFlow(
                    sffNodeName, nsp, nsi, OutputPortValues.INPORT.toString());
            this.sfcFlowProgrammer.configureVxlanGpeLastHopTransportEgressFlow(
                    sffNodeName, nsp, nsi, srcOfsPortStr);
            IpPortLocator srcSffLocator = (IpPortLocator) srcSffDpl.getDataPlaneLocator().getLocatorType();
            this.sfcFlowProgrammer.configureVxlanGpeAppCoexistTransportEgressFlow(
                    sffNodeName, nsp, nsi, new String(srcSffLocator.getIp().getValue()));
        } else {
            this.sfcFlowProgrammer.configureVxlanGpeTransportEgressFlow(
                    sffNodeName, nsp, nsi, srcOfsPortStr);
        }
    }
}
