package org.opendaylight.sfc.l2renderer;

import java.util.concurrent.ExecutionException;

public class SfcL2FlowProgrammerTestMoc implements SfcL2FlowProgrammerInterface {

    //
    // Simple boolean flags to be able to tell which methods were called.
    //
    public boolean configureIpv4TransportIngressFlowCalled;
    public boolean configureVlanTransportIngressFlowCalled;
    public boolean configureVxlanGpeTransportIngressFlowCalled;
    public boolean configureMplsTransportIngressFlowCalled;

    public boolean configureMacIngressFlowCalled;
    public boolean configureMplsIngressFlowCalled;
    public boolean configureVlanIngressFlowCalled;
    public boolean configureVxlanGpeIngressFlowCalled;

    public boolean configureNextHopFlowCalled;
    public boolean configureVxlanGpeNextHopFlowCalled;

    public boolean configureMacTransportEgressFlowCalled;
    public boolean configureVlanTransportEgressFlowCalled;
    public boolean configureVxlanGpeTransportEgressFlowCalled;
    public boolean configureMplsTransportEgressFlowCalled;

    public boolean configureTransportIngressTableMatchAnyCalled;
    public boolean configureIngressTableMatchAnyCalled;
    public boolean configureNextHopTableMatchAnyCalled;
    public boolean configureTransportEgressTableMatchAnyCalled;

    // Reset the method called boolean flags.
    // Should be called after each test.
    public void resetCalledMethods() {
        configureIpv4TransportIngressFlowCalled      = false;
        configureVlanTransportIngressFlowCalled      = false;
        configureVxlanGpeTransportIngressFlowCalled  = false;
        configureMplsTransportIngressFlowCalled      = false;

        configureMacIngressFlowCalled       = false;
        configureMplsIngressFlowCalled      = false;
        configureVlanIngressFlowCalled      = false;
        configureVxlanGpeIngressFlowCalled  = false;

        configureNextHopFlowCalled          = false;
        configureVxlanGpeNextHopFlowCalled  = false;

        configureMacTransportEgressFlowCalled       = false;
        configureVlanTransportEgressFlowCalled      = false;
        configureVxlanGpeTransportEgressFlowCalled  = false;
        configureMplsTransportEgressFlowCalled      = false;

        configureTransportIngressTableMatchAnyCalled = false;
        configureIngressTableMatchAnyCalled          = false;
        configureNextHopTableMatchAnyCalled          = false;
        configureTransportEgressTableMatchAnyCalled  = false;
    }

    public SfcL2FlowProgrammerTestMoc() {
        resetCalledMethods();
    }

    @Override
    public void shutdown() throws ExecutionException, InterruptedException {
        // TODO Auto-generated method stub
    }

    @Override
    public short getTableBase() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setTableBase(short tableBase) {
        // TODO Auto-generated method stub
    }

    //---------------------------------------------
    //
    //            Transport Ingress methods
    //
    //---------------------------------------------

    @Override
    public void configureIpv4TransportIngressFlow(String sffNodeName, boolean isAddFlow) {
        configureIpv4TransportIngressFlowCalled = true;
    }

    @Override
    public void configureVlanTransportIngressFlow(String sffNodeName, boolean isAddFlow) {
        configureVlanTransportIngressFlowCalled = true;
    }

    @Override
    public void configureVxlanGpeTransportIngressFlow(String sffNodeName, boolean isAddFlow) {
        configureVxlanGpeTransportIngressFlowCalled = true;
    }

    @Override
    public void configureMplsTransportIngressFlow(String sffNodeName, boolean isAddFlow) {
        configureMplsTransportIngressFlowCalled = true;
    }

    //---------------------------------------------
    //
    //            Ingress methods
    //
    //---------------------------------------------

    @Override
    public void configureMacIngressFlow(
            String sffNodeName, String mac, long pathId, boolean isSf, boolean isAddFlow) {
        configureMacIngressFlowCalled = true;
    }

    @Override
    public void configureMplsIngressFlow(
            String sffNodeName, long label, long pathId, boolean isSf, boolean isAddFlow) {
        configureMplsIngressFlowCalled = true;
    }

    @Override
    public void configureVlanIngressFlow(
            String sffNodeName, int vlan, long pathId, boolean isSf, boolean isAddFlow) {
        configureVlanIngressFlowCalled = true;
    }

    @Override
    public void configureVxlanGpeIngressFlow(
            String sffNodeName, long nsp, short nsi, long pathId, boolean isAddFlow) {
        configureVxlanGpeIngressFlowCalled = true;
    }

    //---------------------------------------------
    //
    //            Next Hop methods
    //
    //---------------------------------------------

    @Override
    public void configureNextHopFlow(
            String sffNodeName, long sfpId, String srcMac, String dstMac, boolean isAddFlow) {
        configureNextHopFlowCalled = true;
    }

    @Override
    public void configureVxlanGpeNextHopFlow(
            String sffNodeName, String dstIp, long nsp, short nsi, boolean isAddFlow) {
        configureVxlanGpeNextHopFlowCalled = true;
    }

    //---------------------------------------------
    //
    //            Transport Egress methods
    //
    //---------------------------------------------

    @Override
    public void configureMacTransportEgressFlow(
            String sffNodeName,
            String srcMac,
            String dstMac,
            String port,
            long pathId,
            boolean setDscp,
            boolean isAddFlow) {
        configureMacTransportEgressFlowCalled = true;
    }

    @Override
    public void configureVlanTransportEgressFlow(
            String sffNodeName,
            String srcMac,
            String dstMac,
            int dstVlan,
            String port,
            long pathId,
            boolean setDscp,
            boolean isAddFlow) {
        configureVlanTransportEgressFlowCalled = true;
    }

    @Override
    public void configureVxlanGpeTransportEgressFlow(
            String sffNodeName,
            long nshNsp,
            short nshNsi,
            String port,
            boolean isAddFlow) {
        configureVxlanGpeTransportEgressFlowCalled = true;
    }

    @Override
    public void configureMplsTransportEgressFlow(
            String sffNodeName,
            String srcMac,
            String dstMac,
            long mplsLabel,
            String port,
            long pathId,
            boolean setDscp,
            boolean isAddFlow) {
        configureMplsTransportEgressFlowCalled = true;
    }

    //---------------------------------------------
    //
    //            Match Any methods
    //
    //---------------------------------------------

    @Override
    public void configureTransportIngressTableMatchAny(
            String sffNodeName,
            boolean doDrop,
            boolean isAddFlow) {
        configureTransportIngressTableMatchAnyCalled = true;
    }

    @Override
    public void configureIngressTableMatchAny(
            String sffNodeName,
            boolean doDrop,
            boolean isAddFlow) {
        configureIngressTableMatchAnyCalled = true;
    }

    @Override
    public void configureNextHopTableMatchAny(
            String sffNodeName,
            boolean doDrop,
            boolean isAddFlow) {
        configureNextHopTableMatchAnyCalled = true;
    }

    @Override
    public void configureTransportEgressTableMatchAny(
            String sffNodeName,
            boolean doDrop,
            boolean isAddFlow) {
        configureTransportEgressTableMatchAnyCalled = true;
    }

}
