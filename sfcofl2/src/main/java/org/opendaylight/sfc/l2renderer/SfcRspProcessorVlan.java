package org.opendaylight.sfc.l2renderer;

import java.util.Iterator;

import org.opendaylight.sfc.l2renderer.SffGraph.SffGraphEntry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MacBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

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

    @Override
    public void configureSfTransportIngressFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        this.sfcFlowProgrammer.configureVlanTransportIngressFlow(sffNodeName);
    }

    @Override
    public void configureSffTransportIngressFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        this.sfcFlowProgrammer.configureVlanTransportIngressFlow(sffNodeName);
    }

    @Override
    public void configureSffPathMapperFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(entry.getDstSff(), entry.getPathId());
        Integer vlanTag = ((MacAddressLocator) dstHopIngressDpl.getLocatorType()).getVlanId();
        if (vlanTag != null) {
            this.sfcFlowProgrammer.configureVlanPathMapperFlow(sffNodeName, vlanTag, entry.getPathId(), isSf);
        }
    }

    @Override
    public void configureNextHopFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        this.sfcFlowProgrammer.configureMacNextHopFlow(sffNodeName, entry.getPathId(), srcMac, dstMac);
    }

    @Override
    public void configureSfTransportEgressFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        Integer vlanTag = ((MacAddressLocator) hopLocatorType).getVlanId();
        if (vlanTag != null) {
            this.sfcFlowProgrammer.configureVlanTransportEgressFlow(
                    sffNodeName, srcMac, dstMac, vlanTag,
                    srcOfsPort, entry.getPathId(), isSf, doPktIn);
        }
    }

    @Override
    public void configureSffTransportEgressFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        Integer vlanTag = ((MacAddressLocator) hopLocatorType).getVlanId();
        if (vlanTag != null) {
            this.sfcFlowProgrammer.configureVlanTransportEgressFlow(
                    sffNodeName, srcMac, dstMac, vlanTag,
                    srcOfsPort, entry.getPathId(), isSf, doPktIn);
        }
    }
}
