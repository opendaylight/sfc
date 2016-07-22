/*
 * Copyright (c) 2016 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.processors;

import java.util.Iterator;

import org.opendaylight.sfc.ofrenderer.processors.SffGraph.SffGraphEntry;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;

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


    /*
     * Configure the Next Hop flow from an SFF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSfDpl  - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl) {
        MacAddressLocator srcSffLocator = (MacAddressLocator) srcSffDpl.getDataPlaneLocator().getLocatorType();
        MacAddressLocator dstSfLocator = (MacAddressLocator) dstSfDpl.getLocatorType();
        this.sfcFlowProgrammer.configureNshEthNextHopFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId()),
                srcSffLocator.getMac().getValue(),
                dstSfLocator.getMac().getValue(),
                entry.getPathId(),
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
     * Configure the Next Hop flow from an SF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSfDpl - the particular SF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SfDataPlaneLocator dstSfDpl) {
        MacAddressLocator srcSfLocator = (MacAddressLocator) srcSfDpl.getLocatorType();
        MacAddressLocator dstSfLocator = (MacAddressLocator) dstSfDpl.getLocatorType();
        this.sfcFlowProgrammer.configureNshEthNextHopFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId()),
                srcSfLocator.getMac().getValue(),
                dstSfLocator.getMac().getValue(),
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

    /*
     * Configure the Transport Egress flow from an SFF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     * @param hopDpl - Hop DPL used to create the flow
     */
    @Override
    public void configureSffTransportEgressFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
            SffDataPlaneLocator dstSffDpl, DataPlaneLocator hopDpl) {
        long nsp = entry.getPathId();
        short nsi = entry.getServiceIndex();
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            this.sfcFlowProgrammer.configureNshNscTransportEgressFlow(
                    sffNodeName, nsp, nsi, OutputPortValues.INPORT.toString());
            this.sfcFlowProgrammer.configureNshVxgpeLastHopTransportEgressFlow(
                    sffNodeName, nsp, nsi, srcOfsPortStr);
            IpPortLocator srcSffLocator = (IpPortLocator) srcSffDpl.getDataPlaneLocator().getLocatorType();
            this.sfcFlowProgrammer.configureNshVxgpeAppCoexistTransportEgressFlow(
                    sffNodeName, nsp, nsi, new String(srcSffLocator.getIp().getValue()));
        } else {
            this.sfcFlowProgrammer.configureNshVxgpeTransportEgressFlow(
                    sffNodeName, nsp, nsi, srcOfsPortStr);
        }
    }


    /*
     * Set the RSP path egress DPL and SFF Hop Ingress DPLs for the NSH encap and Eth transport type
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

}
