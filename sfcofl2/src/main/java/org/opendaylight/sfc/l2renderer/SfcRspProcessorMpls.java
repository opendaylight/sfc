package org.opendaylight.sfc.l2renderer;

import java.util.Iterator;

import org.opendaylight.sfc.l2renderer.SffGraph.SffGraphEntry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MplsLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MplsBuilder;

public class SfcRspProcessorMpls extends SfcRspTransportProcessorBase {
    private static final int MPLS_LABEL_INCR_HOP = 1;
    private static final int MPLS_LABEL_INCR_RSP = 100;
    private static int lastMplsLabel;

    public SfcRspProcessorMpls() {
    }

    @Override
    public void setRspTransports() {
        int hopIncrement = MPLS_LABEL_INCR_HOP;
        int transportData = lastMplsLabel + MPLS_LABEL_INCR_RSP;
        lastMplsLabel = transportData;

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
            MplsBuilder mplsBuilder = new MplsBuilder();
            mplsBuilder.setMplsLabel((long) transportData);
            dpl.setLocatorType(mplsBuilder.build());

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
        // Even though this is MPLS, the SFs will be VLAN
        this.sfcFlowProgrammer.configureVlanTransportIngressFlow(sffNodeName);
    }

    @Override
    public void configureSffTransportIngressFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        this.sfcFlowProgrammer.configureMplsTransportIngressFlow(sffNodeName);
    }

    @Override
    public void configureSffPathMapperFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        long mplsLabel = ((MplsLocator) sffLocatorType).getMplsLabel();
        this.sfcFlowProgrammer.configureMplsPathMapperFlow(sffNodeName, mplsLabel, entry.getPathId(), isSf);
    }

    @Override
    public void configureNextHopFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());

        // Same thing for Mac/VLAN and MPLS
        this.sfcFlowProgrammer.configureMacNextHopFlow(sffNodeName, entry.getPathId(), srcMac, dstMac);
    }

    @Override
    public void configureSfTransportEgressFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        long mplsLabel = ((MplsLocator) hopLocatorType).getMplsLabel();
        this.sfcFlowProgrammer.configureMplsTransportEgressFlow(sffNodeName, srcMac, dstMac, mplsLabel,
                srcOfsPort, entry.getPathId(), isSf, doPktIn);
    }

    @Override
    public void configureSffTransportEgressFlow(SffGraphEntry entry) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        long mplsLabel = ((MplsLocator) hopLocatorType).getMplsLabel();
        this.sfcFlowProgrammer.configureMplsTransportEgressFlow(sffNodeName, srcMac, dstMac, mplsLabel,
                srcOfsPort, entry.getPathId(), isSf, doPktIn);
    }
}
