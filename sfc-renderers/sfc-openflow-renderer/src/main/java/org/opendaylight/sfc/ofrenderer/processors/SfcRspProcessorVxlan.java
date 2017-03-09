/*
 * Copyright (c) 2017 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.processors;

import java.util.Iterator;

import org.opendaylight.sfc.ofrenderer.processors.SffGraph.SffGraphEntry;
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
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sf.proxy.rev160125.SfLocatorProxyAugmentation;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sf.proxy.rev160125.proxy.ProxyDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;

/**
 * SfcRspProcessorVxlan class
 *
 * <p>
 * This implementation is used when the SFP:transport-type is Vxlan and the
 * SFP:sfc-encapsulation is transport. This implementation is for Non-NSH SFC.
 * In this case, it is assumed that Vxlan will be used between the classifier
 * to SFF and SFF to SFF. The SFF to SF traffic will not use encapsulation,
 * and will thus be the original packets.
 *
 * <p>
 * In this first implementation, it is assumed that all SFs will have the OneChainOnly
 * flag set true, which means reclassification will not be needed upon SF ingress.
 *
 * @author ebrjohn
 *
 */
public class SfcRspProcessorVxlan extends SfcRspTransportProcessorBase {

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

        // TODO what needs to be done here?? No encap is used towards the SFs with VXLAN
    }

    /**
     * Configure the Transport Ingress flow for SFFs.
     *
     * @param entry
     *            - RSP hop info used to create the flow
     */
    @Override
    public void configureSffTransportIngressFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator dstSffDpl) {
        // TODO later use the dstSffDpl to get the tap port number
        this.sfcFlowProgrammer.configureVxlanTransportIngressFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId()));
    }

    //
    // PathMapper methods
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
        // Using SingleChain SFs - map from the inport to the NSP
        // TODO get the port the SF is connected on
        this.sfcFlowProgrammer.configureVlanPathMapperFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId()), entry.getPathId());
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
        // Map the "encoded" VNI to [NSP, NSI]
        this.sfcFlowProgrammer.configureVlanPathMapperFlow(
                sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId()), entry.getPathId(),
                entry.getServiceIndex());
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

        IpPortLocator locator = (IpPortLocator) dstSfDpl.getLocatorType();
        String dstIp = new String(locator.getIp().getValue());

        this.configureNextHopFlow(entry, entry.getDstSff(), dstIp);
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
            this.configureNextHopFlow(entry, entry.getSrcSff(), new String(dstSffLocator.getIp().getValue()), null);
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

        IpPortLocator locator = (IpPortLocator) dstSfDpl.getLocatorType();
        String dstIp = new String(locator.getIp().getValue());
        String nshProxyIp = null;
        SfLocatorProxyAugmentation augment = dstSfDpl.getAugmentation(SfLocatorProxyAugmentation.class);
        if (augment != null) {
            ProxyDataPlaneLocator proxyDpl = augment.getProxyDataPlaneLocator();
            if (proxyDpl == null) {
                LOG.error("SfcRspProcessorNshVxgpe::configureNextHopFlow: proxyDpl is null");
                return;
            }
            locator = (IpPortLocator) proxyDpl.getLocatorType();
            nshProxyIp = new String(locator.getIp().getValue());
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
        this.configureNextHopFlow(entry, entry.getDstSff(), new String(dstSffLocator.getIp().getValue()), null);
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
        this.sfcFlowProgrammer.configureVxlanNextHopFlow(
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

        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId(),
                entry.getDstDpnId());
        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        this.sfcFlowProgrammer.configureVxlanTransportEgressFlow(sffNodeName, entry.getPathId(),
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
            this.sfcFlowProgrammer.configureVxlanLastHopTransportEgressFlow(sffNodeName, nsp, nsi, srcOfsPortStr);
            IpPortLocator srcSffLocator = (IpPortLocator) srcSffDpl.getDataPlaneLocator().getLocatorType();
            this.sfcFlowProgrammer.configureVxlanAppCoexistTransportEgressFlow(sffNodeName, nsp, nsi,
                    new String(srcSffLocator.getIp().getValue()));
        } else {
            this.sfcFlowProgrammer.configureVxlanTransportEgressFlow(sffNodeName, nsp, nsi, srcOfsPortStr);
        }
    }

}
