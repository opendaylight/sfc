/*
 * Copyright (c) 2016 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.openflow.processors;

import java.util.Iterator;

import org.opendaylight.sfc.renderers.openflow.processors.SffGraph.SffGraphEntry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sf.ovs.rev160107.SfDplOvsAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

/**
 * SfcRspProcessorNshEth class
 *
 * <p>
 * This implementation is used when the SFP:transport-type is MAC and the
 * SFP:sfc-encapsulation is NSH. In this case, it is assumed that Eth+NSH will
 * be used between the SFF and SF and SFF to SFF.
 *
 * <p>
 * Eth+NSH will be used between the SFF and SF, and VXGPE+NSH will still be used
 * between the SFFs. In the future when the RspManager is finished, we wont have
 * to mix transports in this class, as it will be called per hop.
 *
 * @author ebrjohn
 *
 */
public class SfcRspProcessorNshEth extends SfcRspTransportProcessorBase {

    //
    // TransportIngress methods
    //

    /*
     * Configure the Transport Ingress flow for SFs Not needed since the same
     * flow will be created in configureSffTransportIngressFlow()
     *
     * This method maybe called by SfcRspProcessorNshVxgpe.
     *
     * @param entry - RSP hop info used to create the flow
     */
    @Override
    public void configureSfTransportIngressFlow(SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        // Nothing needed here, covered by configureSffTransportIngressFlow()
    }

    /*
     * Configure the Transport Ingress flow for SFFs
     *
     * @param entry - RSP hop info used to create the flow
     */
    @Override
    public void configureSffTransportIngressFlow(SffGraphEntry entry, SffDataPlaneLocator dstSffDpl) {
        // Even though this calls configureNshVxgpeTransportIngressFlow,
        // it only matches on NSH NSP, nothing Vxgpe.
        this.sfcFlowProgrammer.configureNshVxgpeTransportIngressFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId()), entry.getPathId(),
                entry.getServiceIndex());
    }

    //
    // PathMapper methods - not needed for NSH
    //

    /*
     */
    @Override
    public void configureSfPathMapperFlow(SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        // Path Mapping is not needed for NSH, since the path is in the NSH
        // header
    }

    /*
     */
    @Override
    public void configureSffPathMapperFlow(SffGraphEntry entry, DataPlaneLocator hopDpl) {
        // Path Mapping is not needed for NSH, since the path is in the NSH
        // header
    }

    //
    // NextHop methods
    //

    /*
     * Configure the Next Hop flow from an SFF to an SF
     *
     * This method maybe called by SfcRspProcessorNshVxgpe.
     *
     * @param entry - RSP hop info used to create the flow
     *
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     *
     * @param dstSfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl) {
        MacAddress srcMacAddress;
        if (srcSffDpl == null) {
            // srcSffDpl will be null for the first hop in the chain, just use
            // the SFF DPL mac
            SffDataPlaneLocator dpl = sfcProviderUtils.getSffSfDictSffDpl(entry.getSf(), entry.getDstSff(),
                    entry.getPathId());
            srcMacAddress = ((MacAddressLocator) dpl.getDataPlaneLocator().getLocatorType()).getMac();
        } else {
            srcMacAddress = ((MacAddressLocator) srcSffDpl.getDataPlaneLocator().getLocatorType()).getMac();
        }
        if (srcMacAddress == null) {
            LOG.error("configureNextHopFlow SFF-SF: Source MAC address is null, cant continue");
            return;
        }
        String srcMac = srcMacAddress.getValue();

        if (((MacAddressLocator) dstSfDpl.getLocatorType()).getMac() == null) {
            LOG.error("configureNextHopFlow SFF-SF: Dest MAC address is null, cant continue");
            return;
        }
        String dstMac = ((MacAddressLocator) dstSfDpl.getLocatorType()).getMac().getValue();

        this.sfcFlowProgrammer.configureNshEthNextHopFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId()), srcMac, dstMac,
                entry.getPathId(), entry.getServiceIndex());
    }

    /*
     * Configure the Next Hop flow from an SF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     *
     * @param srcSfDpl - the particular SF DPL used to create the flow
     *
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SffDataPlaneLocator dstSffDpl) {
        // TODO need to do this for Eth+NSH
    }

    /*
     * Configure the Next Hop flow from an SF to an SF
     *
     * This method maybe called by SfcRspProcessorNshVxgpe.
     *
     * @param entry - RSP hop info used to create the flow
     *
     * @param srcSfDpl - the particular SF DPL used to create the flow
     *
     * @param dstSfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SfDataPlaneLocator dstSfDpl) {
        MacAddress srcSfMac = ((MacAddressLocator) srcSfDpl.getLocatorType()).getMac();
        if (srcSfMac == null) {
            LOG.error("configureNextHopFlow SF-SF, Source SF MAC address is null, cant continue");
            return;
        }

        MacAddress dstSfMac = ((MacAddressLocator) dstSfDpl.getLocatorType()).getMac();
        if (dstSfMac == null) {
            LOG.error("configureNextHopFlow SF-SF, Dest SF MAC address is null, cant continue");
            return;
        }

        this.sfcFlowProgrammer.configureNshEthNextHopFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId()), srcSfMac.getValue(),
                dstSfMac.getValue(), entry.getPathId(), entry.getServiceIndex());
    }

    /*
     * Configure the Next Hop flow from an SFF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     *
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     *
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
            SffDataPlaneLocator dstSffDpl) {
        // TODO need to do this for Eth+NSH
    }

    //
    // TransportEgress methods
    //

    /*
     * Configure the Transport Egress flow from an SFF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     *
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     *
     * @param dstSfDpl - the particular SF DPL used to create the flow
     *
     * @param hopDpl - Hop DPL used to create the flow
     */
    @Override
    public void configureSfTransportEgressFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
            SfDataPlaneLocator dstSfDpl, DataPlaneLocator hopDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        String portStr;

        SfDplOvsAugmentation sfDplOvs = dstSfDpl.augmentation(SfDplOvsAugmentation.class);
        if (sfDplOvs == null) {
            LOG.info("SfcRspProcessorEthNsh::configureSfTransportEgressFlow NO sfDplOvs augmentation present");
            portStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        } else {
            long sffPort = sfcProviderUtils.getPortNumberFromName(entry.getDstSff().getValue(),
                    sfDplOvs.getOvsPort().getPortId(), entry.getPathId());
            portStr = String.valueOf(sffPort);
        }

        this.sfcFlowProgrammer.configureNshEthTransportEgressFlow(sffNodeName, entry.getPathId(),
                entry.getServiceIndex(), portStr);
    }

    /*
     * Configure the Transport Egress flow from an SFF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     *
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     *
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     *
     * @param hopDpl - Hop DPL used to create the flow
     */
    @Override
    public void configureSffTransportEgressFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
            SffDataPlaneLocator dstSffDpl, DataPlaneLocator hopDpl) {
        // TODO need to do this for Eth+NSH
    }

    /*
     * Set the RSP path egress DPL and SFF Hop Ingress DPLs for the NSH encap
     * and Eth transport type
     */
    @Override
    public void setRspTransports() {
        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            LOG.debug("RspTransport entry: {}", entry);

            if (entry.getSrcSff().equals(entry.getDstSff())) {
                // It may be that multiple SFs are on the same SFF
                // If so, we don't need to set the transports again
                // Otherwise the SFF ingress DPL will be overwritten
                continue;
            }

            DataPlaneLocatorBuilder dpl = new DataPlaneLocatorBuilder();
            dpl.setTransport(rsp.getTransportType());

            SffName targetSffName = entry.getDstSff().equals(SffGraph.EGRESS) ? entry.getSrcSff() : entry.getDstSff();
            ServiceFunctionForwarder sff = sfcProviderUtils.getServiceFunctionForwarder(targetSffName,
                    entry.getPathId());
            SffDataPlaneLocatorName sffEgressDplName = sffGraph.getSffEgressDpl(sff.getName(), entry.getPathId());
            LocatorType loc = sfcProviderUtils.getSffDataPlaneLocator(sff, sffEgressDplName).getDataPlaneLocator()
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
