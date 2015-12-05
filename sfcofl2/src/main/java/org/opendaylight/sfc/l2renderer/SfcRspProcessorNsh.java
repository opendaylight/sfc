package org.opendaylight.sfc.l2renderer;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;

public class SfcRspProcessorNsh extends SfcRspTransportProcessorBase {

    @Override
    public void setRspTransports() {
        // Nothing to be done for NSH
    }

    @Override
    public void configureSfTransportIngressFlow(SffGraph.SffGraphEntry entry) {
        // nothing needs to be done for NSH
        // same as for configureSffTransportIngressFlow
    }

    @Override
    public void configureSffTransportIngressFlow(SffGraph.SffGraphEntry entry) {
        this.sfcFlowProgrammer.configureVxlanGpeTransportIngressFlow(entry.getSrcSff().getValue());
    }

    @Override
    public void configureSffPathMapperFlow(SffGraph.SffGraphEntry entry) {
        // Path Mapping is not needed for NSH, since the path is in the NSH header
    }

    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry) {
        String dstIp = String.valueOf(((IpPortLocator) dstSffLocatorType).getIp().getValue());
        long nsp = entry.getPathId();
        short nsi = entry.getServiceIndex();
        this.sfcFlowProgrammer.configureVxlanGpeNextHopFlow(entry.getSrcSff().getValue(), dstIp, nsp, nsi);
    }

    @Override
    public void configureSfTransportEgressFlow(SffGraph.SffGraphEntry entry) {
        // TODO nothing special to do here, since the SF will be NSH too
    }

    @Override
    public void configureSffTransportEgressFlow(SffGraph.SffGraphEntry entry) {
        long nsp = entry.getPathId();
        short nsi = entry.getServiceIndex();
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());

        this.sfcFlowProgrammer.configureVxlanGpeTransportEgressFlow(
                sffNodeName, nsp, nsi, srcOfsPort, isLastServiceIndex);
        if (isLastServiceIndex) {
            this.sfcFlowProgrammer.configureNshNscTransportEgressFlow(
                    sffNodeName, nsp, nsi, OutputPortValues.INPORT.toString());
        }
    }
}
