/*
 * Copyright (c) 2016 Hewlett Packard Enterprise Development LP. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.openflow.processors;

import java.util.Iterator;
import org.opendaylight.sfc.util.macchaining.SfcModelUtil;
import org.opendaylight.sfc.util.macchaining.VirtualMacAddress;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MacBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcRspProcessorMacChaining extends SfcRspTransportProcessorBase {

    private static final Logger LOG = LoggerFactory.getLogger(SfcRspProcessorMacChaining.class);

    /*
    * Set the RSP path egress DPL and SFF Hop Ingress DPLs for
    * the MAC Chainig considering connected SFF dictionary
    */
    @Override
    public void setRspTransports() {
        LOG.debug("SfcRspProcessorMacChaining - setRspTransports");

        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();

        while (sffGraphIter.hasNext()) {

            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            LOG.info("RspTransport entry: {}", entry.toString());

            if (entry.getSrcSff().equals(entry.getDstSff())) {
                // It may be that multiple SFs are on the same SFF
                // If so, we dont need to set the transports again
                // Otherwise the SFF ingress DPL will be overwritten
                continue;
            }

            DataPlaneLocatorBuilder dpl = new DataPlaneLocatorBuilder();
            dpl.setTransport(rsp.getTransportType());
            MacBuilder macBuilder = new MacBuilder();
            if (!entry.getDstSff().equals(entry.getSrcSff())
                    && !entry.getSrcSff().equals(SffGraph.INGRESS)
                    && !entry.getDstSff().equals(SffGraph.EGRESS)) {

                // setting Egress DPL of source SFF
                SffDataPlaneLocator srcSffDplEgress = SfcModelUtil.searchSrcDplInConnectedSffs(
                        entry.getSrcSff(), entry.getDstSff());
                if (srcSffDplEgress == null) {
                    LOG.error(" cannot find SFF dictionary in classifier {} to  {} ",
                            entry.getSrcSff(), entry.getDstSff());

                } else {
                    sffGraph.setSffEgressDpl(entry.getSrcSff(),rsp.getPathId().toJava(), srcSffDplEgress.getName());
                }

                // setting Ingress DPL of destination SFF
                SffDataPlaneLocator dstSffDplIngress = SfcModelUtil.searchSrcDplInConnectedSffs(
                        entry.getDstSff(), entry.getSrcSff());
                if (dstSffDplIngress == null) {
                    LOG.error(" cannot find SFF dictionary in classifier {} to  {} ",
                            entry.getDstSff(), entry.getSrcSff());

                } else {
                    sffGraph.setSffEgressDpl(entry.getDstSff(),rsp.getPathId().toJava(), dstSffDplIngress.getName());
                }
            }

            dpl.setLocatorType(macBuilder.build());

            if (entry.getDstSff().equals(SffGraph.EGRESS)) {
                sffGraph.setPathEgressDpl(entry.getPathId(), dpl.build());
                LOG.info("sffGraph {}", sffGraph.getPathEgressDpl(rsp.getPathId().toJava()).toString());
            } else {
                sffGraph.setHopIngressDpl(entry.getDstSff(), entry.getPathId(), dpl.build());
                LOG.info("sffGraph {}", sffGraph.getHopIngressDpl(entry.getDstSff(),
                    rsp.getPathId().toJava()).toString());
            }
        }
    }

    @Override
    public void configureSfTransportIngressFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        LOG.debug("SfcRspProcessorMacChaining - "
                + "configure-Sf-TransportIngressFlow nothing to do here, just go to next table");
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        this.sfcFlowProgrammer.configureMacChainingTransportIngressFlow(sffNodeName);
    }

    @Override
    public void configureSffTransportIngressFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator dstSffDpl) {
        LOG.debug("SfcRspProcessorMacChaining - "
                + "configure-Sff-TransportIngressFlow nothing to do here, just go to next table");
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        this.sfcFlowProgrammer.configureMacChainingTransportIngressFlow(sffNodeName);
    }

    @Override
    public void configureSfPathMapperFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        LOG.debug("SfcRspProcessorMacChaining - "
                + "configure-Sf-PathMapperFlow nothing to do here, just got to next table");

    }

    @Override
    public void configureSffPathMapperFlow(SffGraph.SffGraphEntry entry, DataPlaneLocator hopDpl) {
        LOG.debug("SfcRspProcessorMacChaining - "
                + "configure-Sff-PathMapperFlow nothing to do here, just got to next table");

    }



    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl,
                                     SfDataPlaneLocator dstSfDpl) {
        LOG.debug("SfcRspProcessorMacChaining - configureNextHopFlow {} sf {} to sf {} ", entry.getServiceIndex(),
                srcSfDpl == null
                        ? "null" : srcSfDpl.getName().toString(),
                dstSfDpl == null
                        ? "null" : dstSfDpl.getName().toString());
        // nothing to do
    }

    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
                                     SffDataPlaneLocator dstSffDpl) {
        LOG.debug("SfcRspProcessorMacChaining - configureNextHopFlow {} sff {} to sff {}", entry.getServiceIndex(),
                srcSffDpl == null
                        ? "null" : srcSffDpl.getName().toString(),
                dstSffDpl == null
                        ? "null" : dstSffDpl.getName().toString());
        // nothing to do
    }

    /**
     * Configure the Next Hop flow from an SFF to an SF.
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
                                     SfDataPlaneLocator dstSfDpl) {
        LOG.debug("SfcRspProcessorMacChaining - configureNextHopFlow {} sff {} to sf {}", entry.getServiceIndex(),
                srcSffDpl == null
                        ? "null" : srcSffDpl.getName().toString(),
                dstSfDpl == null
                        ? "null" : dstSfDpl.getName().toString());

        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());

        VirtualMacAddress hopMac = VirtualMacAddress.getForwardAddress(entry.getPathId(), 0);

        String vmac = hopMac.getHop(entry.getServiceIndex()).getValue();
        String dstSfMac = dstSfDpl == null ? null : sfcProviderUtils.getSfDplMac(dstSfDpl);
        String nextVMac = hopMac.getHop((short)(entry.getServiceIndex() - 1)).getValue();

        boolean isL2Transparent = false;
        if (entry.getPrevSf() != null) {
            ServiceFunctionType serviceFunctionType = sfcProviderUtils.getServiceFunctionType(
                    entry.getPrevSf(), entry.getPathId());
            //if L2 Transparent boolean is not filled, assume it is not L2 transparent
            if (serviceFunctionType.isL2Transparent() != null) {
                isL2Transparent = serviceFunctionType.isL2Transparent();
            }
        }

        this.sfcFlowProgrammer.configureMacChainingNextHopFlow(sffNodeName, vmac, dstSfMac, nextVMac, isL2Transparent);

    }

    /**
     * Configure the Next Hop flow from an SF to an SFF.
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSfDpl - the particular SF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl,
                                     SffDataPlaneLocator dstSffDpl) {
        LOG.debug("SfcRspProcessorMacChaining - configureNextHopFlow {} sf {} to sff {}", entry.getServiceIndex(),
                srcSfDpl == null
                        ? "null" : srcSfDpl.getName().toString(),
                dstSffDpl == null
                        ? "null" : dstSffDpl.getName().toString());

        VirtualMacAddress hopMac = VirtualMacAddress.getForwardAddress(entry.getPathId(), 0);

        // as we are changing SFF need to write rule on srcSFF to forward packet to dstSFF
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());

        String vmac = hopMac.getHop(entry.getServiceIndex()).getValue();
        String nextSfMac = null;
        if (dstSffDpl != null) {
            nextSfMac = sfcProviderUtils.getSffDplMac(dstSffDpl);
        }
        boolean isL2Transparent = false;
        if (entry.getPrevSf() != null) {
            ServiceFunctionType serviceFunctionType = sfcProviderUtils.getServiceFunctionType(
                    entry.getPrevSf(), entry.getPathId());
            //if L2 Transparent boolean is not filled, asume it is not L2 transparent
            if (serviceFunctionType.isL2Transparent() != null) {
                isL2Transparent = serviceFunctionType.isL2Transparent();
            }
        }

        this.sfcFlowProgrammer.configureMacChainingNextHopFlow(sffNodeName, vmac, nextSfMac, null, isL2Transparent);
    }

    /**
     * Configure the Transport Egress flow from an SFF to an SF.
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     * @param hopDpl - Hop DPL used to create the flow
     */
    @Override
    public void configureSfTransportEgressFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
                                               SfDataPlaneLocator dstSfDpl, DataPlaneLocator hopDpl) {
        LOG.debug("SfcRspProcessorMacChaining - configureSfTransportEgressFlow {} srcSffDpl {} to dstSfDpl {}",
                entry.getServiceIndex(),
                srcSffDpl == null
                        ? "null" : srcSffDpl.getName().toString(),
                dstSfDpl == null
                        ? "null" : dstSfDpl.getName().toString());

        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        if (srcOfsPortStr == null) {
            throw new SfcRenderingException("configureSffTransportEgressFlow OFS port not avail for SFF ["
                    + entry.getDstSff() + "] sffDpl [" + srcSffDpl.getName().getValue() + "]");
        }

        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());

        String dstMac = sfcProviderUtils.getSfDplMac(dstSfDpl);

        LOG.info("SFFNodeName {}", sffNodeName);
        this.sfcFlowProgrammer.configureMacChainingSfTransportEgressFlow(sffNodeName, dstMac, srcOfsPortStr, null);

    }

    /**
     * Configure the Transport Egress flow from an SFF to an SFF.
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     * @param hopDpl - Hop DPL used to create the flow
     */
    @Override
    public void configureSffTransportEgressFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl,
                                                SffDataPlaneLocator dstSffDpl, DataPlaneLocator hopDpl) {
        LOG.debug("SfcRspProcessorMacChaining - configureSffTransportEgressFlow {} srcSffDpl {} to dstSffDpl {}",
                entry.getServiceIndex(),
                srcSffDpl == null
                        ? "null" : srcSffDpl.getName().toString(),
                dstSffDpl == null
                        ? "null" : dstSffDpl.getName().toString());

        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        if (srcOfsPortStr == null) {
            throw new SfcRenderingException("configureSffTransportEgressFlow OFS port not avail for SFF ["
                    + entry.getDstSff() + "] sffDpl [" + srcSffDpl.getName().getValue() + "]");
        }

        // For the SF transport Ingress flow, we'll write to the Dst SFF as opposed to typically writing to the Src SFF
        String sffNodeName;
        if (entry.getSrcSff().equals(SffGraph.INGRESS)) {
            sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        } else {
            sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());

        }

        String dstMac = null;
        if (dstSffDpl != null) {
            dstMac = sfcProviderUtils.getSffDplMac(dstSffDpl);
        }
        if (dstMac == null) {
            // if the dstMac is null this is not a valid flow
            return;
        }

        VirtualMacAddress hopMac = VirtualMacAddress.getForwardAddress(entry.getPathId(), 0);
        String vmac = hopMac.getHop(entry.getServiceIndex()).getValue();

        LOG.info("SFFNodeName {}", sffNodeName);

        this.sfcFlowProgrammer.configureMacChainingSfTransportEgressFlow(sffNodeName, dstMac, srcOfsPortStr, vmac);
    }

}
