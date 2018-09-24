/*
 * Copyright (c) 2014, 2017 Ericsson Inc. and others. All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sf.ovs.rev160107.SfDplOvsAugmentation;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sf.proxy.rev160125.SfLocatorProxyAugmentation;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sf.proxy.rev160125.proxy.ProxyDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SfcRspProcessorNshVxgpe class
 *
 * <p>
 * This implementation is used when the SFP:transport-type is VxGpe and the
 * SFP:sfc-encapsulation is NSH. In this case, it is assumed that Vxgpe+NSH
 * will be used between the SFF and SF and SFF to SFF.
 * There are times, however, when the SFF-SFF encap/transport is Vxgpe+NSH,
 * but the SFF-SF encap/transport will be Eth/NSH, in which case, this class
 * will make the appropriate calls into the SfcRspProcessorNshEth class
 *
 * @author ebrjohn
 *
 */
public class SfcRspProcessorNshVxgpe extends SfcRspTransportProcessorBase {

    private static final Logger LOG = LoggerFactory.getLogger(SfcRspProcessorNshVxgpe.class);
    SfcRspProcessorNshEth sfcRspProcessorNshEth;

    /*
     * Set the RSP path egress DPL and SFF Hop Ingress DPLs for the NSH
     * encapsulation and Eth transport type
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

    //
    // TransportIngress methods
    //

    /**
     * Configure the Transport Ingress flow for SFs.
     *
     * @param entry
     *            - RSP hop info used to create the flow
     */
    @Override
    public void configureSfTransportIngressFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        // If its Eth+NSH call into
        // SfcRspProcessorNshEth.configureSfTransportIngressFlow()
        if (sfDpl.getLocatorType() instanceof Mac) {
            // In this case, the SfLoopbackEncapsulatedEgress and
            // SfReturnLoopbackIngress flows are not needed
            sfcRspProcessorNshEth = getSfcRspProcessorNshEth();
            sfcRspProcessorNshEth.configureSfTransportIngressFlow(entry, sfDpl);

            return;
        }

        /*
         * Currently the switch port the SF is connected to is stored by Tacker.
         * Here we take this name and convert it to the port number.
         */
        SfDplOvsAugmentation sfDplOvs = sfDpl.augmentation(SfDplOvsAugmentation.class);
        if (sfDplOvs == null) {
            LOG.info("SfcRspProcessorNsh::configureSfTransportIngressFlow NO sfDplOvs augmentation present");
            return;
        }
        LOG.info("SfcRspProcessorNsh::configureSfTransportIngressFlow sf [{}] sfDplOvs augmentation port [{}]",
                entry.getSf().getValue(), sfDplOvs.getOvsPort().getPortId());

        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId(),
                entry.getDstDpnId());
        IpPortLocator dstSfLocator = (IpPortLocator) sfDpl.getLocatorType();
        String sfIp = dstSfLocator.getIp().stringValue();
        short vxlanUdpPort = dstSfLocator.getPort().getValue().shortValue();
        long sffPort = sfcProviderUtils.getPortNumberFromName(entry.getDstSff().getValue(),
                sfDplOvs.getOvsPort().getPortId(), entry.getPathId());

        /*
         * SfLoopbackEncapsulatedEgress flow udp,nw_dst=11.0.0.5,tp_dst=6633
         * actions=output:4 - match incoming vxlan packets destined for an sf,
         * but the nw_dst is the sf ip and not the sff so it bypasses tunnel
         * decap. - Key here is the nw_dst is the ip of the sf. - these packets
         * would have been forwarded by the normal SFC egress rules, match NSH,
         * set tun_dst, send out vtep. They would loop back to this bridge, hit
         * this flow - bypassing the vtep and be forwarded to the sf
         */
        this.sfcFlowProgrammer.configureNshVxgpeSfLoopbackEncapsulatedEgressFlow(sffNodeName, sfIp, vxlanUdpPort,
                sffPort);

        /*
         * SfReturnLoopbackIngress flow udp,in_port=4,tp_dst=6633 actions=LOCAL
         * - match packets coming from the sf and that are vxlan packets - which
         * means NSH and no decap - these packets are from the sf and encapped,
         * so they simply get forwarded out the bridge LOCAL and end up coming
         * back to the bridge into the vtep so that the hit the normal SFF
         * flows.
         */
        this.sfcFlowProgrammer.configureNshVxgpeSfReturnLoopbackIngressFlow(sffNodeName, vxlanUdpPort, sffPort);

    }

    /**
     * Configure the Transport Ingress flow for SFFs.
     *
     * @param entry
     *            - RSP hop info used to create the flow
     */
    @Override
    public void configureSffTransportIngressFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator dstSffDpl) {
        final String nodeName = sfcProviderUtils.getSffOpenFlowNodeName(
                entry.getDstSff(),
                entry.getPathId(),
                entry.getDstDpnId());
        this.sfcFlowProgrammer.configureEthNshTransportIngressFlow(nodeName);
        this.sfcFlowProgrammer.configureNshTransportIngressFlow(nodeName);
    }

    //
    // PathMapper methods - not needed for NSH
    //

    /**
     * Configure the Path Mapper flow for SFs.
     *
     * @param entry
     *            - RSP hop info used to create the flow
     * @param sfDpl
     *            - the particular SF DPL used to create the flow
     */
    @Override
    public void configureSfPathMapperFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        // Path Mapping is not needed for NSH, since the path is in the NSH
        // header
    }

    /**
     * Configure the Path Mapper flow for SFFs.
     *
     * @param entry
     *            - RSP hop info used to create the flow
     * @param hopDpl
     *            - the particular SFF Hop DPL used to create the flow
     */
    @Override
    public void configureSffPathMapperFlow(SffGraph.SffGraphEntry entry, DataPlaneLocator hopDpl) {
        // Path Mapping is not needed for NSH, since the path is in the NSH
        // header
    }

    //
    // NextHop methods
    //

    /*
     * Configure the Next Hop flow from an SFF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     *
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     *
     * @param dstSfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl) {

        // If its Eth+NSH call into SfcRspProcessorNshEth.configureNextHopFlow()
        if (dstSfDpl.getLocatorType() instanceof Mac) {
            sfcRspProcessorNshEth = getSfcRspProcessorNshEth();
            sfcRspProcessorNshEth.configureNextHopFlow(entry, srcSffDpl, dstSfDpl);

            return;
        }

        IpPortLocator locator = (IpPortLocator) dstSfDpl.getLocatorType();
        String dstIp = locator.getIp().stringValue();
        String nshProxyIp = null;
        SfLocatorProxyAugmentation augment = dstSfDpl.augmentation(SfLocatorProxyAugmentation.class);
        if (augment != null) {
            ProxyDataPlaneLocator proxyDpl = augment.getProxyDataPlaneLocator();
            if (proxyDpl == null) {
                LOG.error("SfcRspProcessorNshVxgpe::configureNextHopFlow: proxyDpl is null");
                return;
            }
            locator = (IpPortLocator) proxyDpl.getLocatorType();
            nshProxyIp = locator.getIp().stringValue();
        }
        this.configureNextHopFlow(entry, entry.getDstSff(), dstIp, nshProxyIp);
    }

    /**
     * Configure the Next Hop flow from an SF to an SFF.
     *
     * @param entry
     *            - RSP hop info used to create the flow
     * @param srcSfDpl
     *            - the particular SF DPL used to create the flow
     * @param dstSffDpl
     *            - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl,
            SffDataPlaneLocator dstSffDpl) {
        // The dstSffDpl is intentionally set to null for certain cases that
        // can be used by other transports. For Nsh, just skip it.
        if (dstSffDpl != null) {
            IpPortLocator dstSffLocator = (IpPortLocator) dstSffDpl.getDataPlaneLocator().getLocatorType();
            this.configureNextHopFlow(entry, entry.getSrcSff(), dstSffLocator.getIp().stringValue(), null);
        }
    }

    /**
     * Configure the Next Hop flow from an SF to an SF.
     *
     * @param entry
     *            - RSP hop info used to create the flow
     * @param srcSfDpl
     *            - the particular SF DPL used to create the flow
     * @param dstSfDpl
     *            - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl,
            SfDataPlaneLocator dstSfDpl) {

        // If its Eth+NSH call into SfcRspProcessorNshEth.configureNextHopFlow()
        if (dstSfDpl.getLocatorType() instanceof Mac) {
            // In this case, the SfLoopbackEncapsulatedEgress and
            // SfReturnLoopbackIngress flows are not needed
            sfcRspProcessorNshEth = getSfcRspProcessorNshEth();
            sfcRspProcessorNshEth.configureNextHopFlow(entry, srcSfDpl, dstSfDpl);

            return;
        }

        IpPortLocator locator = (IpPortLocator) dstSfDpl.getLocatorType();
        String dstIp = locator.getIp().stringValue();
        String nshProxyIp = null;
        SfLocatorProxyAugmentation augment = dstSfDpl.augmentation(SfLocatorProxyAugmentation.class);
        if (augment != null) {
            ProxyDataPlaneLocator proxyDpl = augment.getProxyDataPlaneLocator();
            if (proxyDpl == null) {
                LOG.error("SfcRspProcessorNshVxgpe::configureNextHopFlow: proxyDpl is null");
                return;
            }
            locator = (IpPortLocator) proxyDpl.getLocatorType();
            nshProxyIp = locator.getIp().stringValue();
        }
        this.configureNextHopFlow(entry, entry.getSrcSff(), dstIp, nshProxyIp);
    }

    /**
     * Configure the Next Hop flow from an SFF to an SFF.
     *
     * @param entry
     *            - RSP hop info used to create the flow
     * @param srcSffDpl
     *            - the particular SFF DPL used to create the flow
     * @param dstSffDpl
     *            - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
            SffDataPlaneLocator dstSffDpl) {
        IpPortLocator dstSffLocator = (IpPortLocator) dstSffDpl.getDataPlaneLocator().getLocatorType();
        this.configureNextHopFlow(entry, entry.getDstSff(), dstSffLocator.getIp().stringValue(), null);
    }

    /**
     * Internal util method for the above 4 configureNextHopFlow() methods.
     *
     * @param entry
     *            - RSP hop info used to create the flow
     * @param dstIp
     *            - VxLan Next Hop Dest IP
     */
    private void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffName sffName, final String dstIp,
        final String nshProxyIp) {
        this.sfcFlowProgrammer.configureNshVxgpeNextHopFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(sffName, entry.getPathId(), entry.getDstDpnId()), dstIp,
                nshProxyIp, entry.getPathId(), entry.getServiceIndex());
    }

    //
    // TransportEgress methods
    //

    /**
     * Configure the Transport Egress flow from an SFF to an SF.
     *
     * @param entry
     *            - RSP hop info used to create the flow
     * @param srcSffDpl
     *            - the particular SFF DPL used to create the flow
     * @param dstSfDpl
     *            - the particular SF DPL used to create the flow
     * @param hopDpl
     *            - Hop DPL used to create the flow
     */
    @Override
    public void configureSfTransportEgressFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
            SfDataPlaneLocator dstSfDpl, DataPlaneLocator hopDpl) {

        // If its Eth+NSH call into
        // SfcRspProcessorNshEth.configureSfTransportEgressFlow()
        if (dstSfDpl.getLocatorType() instanceof Mac) {
            sfcRspProcessorNshEth = getSfcRspProcessorNshEth();
            sfcRspProcessorNshEth.configureSfTransportEgressFlow(entry, srcSffDpl, dstSfDpl, hopDpl);

            return;
        }

        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId(),
                entry.getDstDpnId());
        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        this.sfcFlowProgrammer.configureNshVxgpeTransportEgressFlow(sffNodeName, entry.getPathId(),
                entry.getServiceIndex(), srcOfsPortStr);
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
        long nsp = entry.getPathId();
        short nsi = entry.getServiceIndex();
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);

        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            this.sfcFlowProgrammer.configureNshNscTransportEgressFlow(sffNodeName, nsp, nsi,
                    OutputPortValues.INPORT.toString());
            this.sfcFlowProgrammer.configureNshVxgpeLastHopTransportEgressFlow(sffNodeName, nsp, nsi, srcOfsPortStr);
            IpPortLocator srcSffLocator = (IpPortLocator) srcSffDpl.getDataPlaneLocator().getLocatorType();
            this.sfcFlowProgrammer.configureNshVxgpeAppCoexistTransportEgressFlow(sffNodeName, nsp, nsi,
                    srcSffLocator.getIp().stringValue());
        } else {
            this.sfcFlowProgrammer.configureNshVxgpeTransportEgressFlow(sffNodeName, nsp, nsi, srcOfsPortStr);
        }
    }

    private SfcRspProcessorNshEth getSfcRspProcessorNshEth() {
        if (sfcRspProcessorNshEth == null) {
            sfcRspProcessorNshEth = new SfcRspProcessorNshEth();
            sfcRspProcessorNshEth.setFlowProgrammer(sfcFlowProgrammer);
            sfcRspProcessorNshEth.setSfcProviderUtils(sfcProviderUtils);
        }

        return sfcRspProcessorNshEth;
    }
}
