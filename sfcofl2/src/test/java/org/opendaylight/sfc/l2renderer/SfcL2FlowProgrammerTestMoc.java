package org.opendaylight.sfc.l2renderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.opendaylight.sfc.l2renderer.sfg.GroupBucketInfo;


public class SfcL2FlowProgrammerTestMoc implements SfcL2FlowProgrammerInterface {

    //
    // Simple boolean flags to be able to tell which methods were called.
    //
    public enum MethodIndeces {
        configureIpv4TransportIngressFlowMethodIndex,
        configureVlanTransportIngressFlowMethodIndex,
        configureVxlanGpeTransportIngressFlowMethodIndex,
        configureMplsTransportIngressFlowMethodIndex,

        configureMacPathMapperFlowMethodIndex,
        configureMplsPathMapperFlowMethodIndex,
        configureVlanPathMapperFlowMethodIndex,
        configureVxlanGpePathMapperFlowMethodIndex,

        configureNextHopFlowMethodIndex,
        configureVxlanGpeNextHopFlowMethodIndex,

        configureMacTransportEgressFlowMethodIndex,
        configureVlanTransportEgressFlowMethodIndex,
        configureVxlanGpeTransportEgressFlowMethodIndex,
        configureMplsTransportEgressFlowMethodIndex,

        configureTransportIngressTableMatchAnyMethodIndex,
        configurePathMapperTableMatchAnyMethodIndex,
        configureNextHopTableMatchAnyMethodIndex,
        configureTransportEgressTableMatchAnyMethodIndex,
        configureGroupMethodIndex,
        configureGroupNextHopFlow,

        MAX_INDEX;
    }

    public MethodIndeces methodIndeces;

    private Map<MethodIndeces, Integer> methodCallCounts;

    public SfcL2FlowProgrammerTestMoc() {
        methodCallCounts = new HashMap<MethodIndeces, Integer>();
        // Populate the Map with all zeros
        for(MethodIndeces methodIndex : MethodIndeces.values()) {
            methodCallCounts.put(methodIndex, 0);
        }
        resetCalledMethods();
    }

    // Reset the method called values.
    // Should be called after each test.
    public void resetCalledMethods() {
        for(MethodIndeces methodIndex : MethodIndeces.values()) {
            methodCallCounts.put(methodIndex, 0);
        }
    }

    public int getMethodCalledCount(MethodIndeces methodIndex) {
        return methodCallCounts.get(methodIndex).intValue();
    }

    private void incrementMethodCalled(MethodIndeces methodIndex) {
        int value = methodCallCounts.get(methodIndex).intValue();
        methodCallCounts.put(methodIndex, Integer.valueOf(value+1));
    }

    @Override
    public void shutdown() throws ExecutionException, InterruptedException {
    }

    @Override
    public short getTableBase() {
        return 0;
    }

    @Override
    public void setTableBase(short tableBase) {
    }

    //---------------------------------------------
    //
    //            Transport Ingress methods
    //
    //---------------------------------------------

    @Override
    public void configureIpv4TransportIngressFlow(String sffNodeName, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureIpv4TransportIngressFlowMethodIndex);
    }

    @Override
    public void configureVlanTransportIngressFlow(String sffNodeName, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureVlanTransportIngressFlowMethodIndex);
    }

    @Override
    public void configureVxlanGpeTransportIngressFlow(String sffNodeName, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureVxlanGpeTransportIngressFlowMethodIndex);
    }

    @Override
    public void configureMplsTransportIngressFlow(String sffNodeName, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureMplsTransportIngressFlowMethodIndex);
    }

    //---------------------------------------------
    //
    //            Ingress methods
    //
    //---------------------------------------------

    @Override
    public void configureMacPathMapperFlow(
            String sffNodeName, String mac, long pathId, boolean isSf, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureMacPathMapperFlowMethodIndex);
    }

    @Override
    public void configureMplsPathMapperFlow(
            String sffNodeName, long label, long pathId, boolean isSf, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureMplsPathMapperFlowMethodIndex);
    }

    @Override
    public void configureVlanPathMapperFlow(
            String sffNodeName, int vlan, long pathId, boolean isSf, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureVlanPathMapperFlowMethodIndex);
    }

    @Override
    public void configureVxlanGpePathMapperFlow(
            String sffNodeName, long nsp, short nsi, long pathId, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureVxlanGpePathMapperFlowMethodIndex);
    }

    //---------------------------------------------
    //
    //            Next Hop methods
    //
    //---------------------------------------------

    @Override
    public void configureNextHopFlow(
            String sffNodeName, long sfpId, String srcMac, String dstMac, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureNextHopFlowMethodIndex);
    }

    @Override
    public void configureVxlanGpeNextHopFlow(
            String sffNodeName, String dstIp, long nsp, short nsi, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureVxlanGpeNextHopFlowMethodIndex);
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
        incrementMethodCalled(MethodIndeces.configureMacTransportEgressFlowMethodIndex);
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
        incrementMethodCalled(MethodIndeces.configureVlanTransportEgressFlowMethodIndex);
    }

    @Override
    public void configureVxlanGpeTransportEgressFlow(
            String sffNodeName,
            long nshNsp,
            short nshNsi,
            String port,
            boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureVxlanGpeTransportEgressFlowMethodIndex);
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
        incrementMethodCalled(MethodIndeces.configureMplsTransportEgressFlowMethodIndex);
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
        incrementMethodCalled(MethodIndeces.configureTransportIngressTableMatchAnyMethodIndex);
    }

    @Override
    public void configurePathMapperTableMatchAny(
            String sffNodeName,
            boolean doDrop,
            boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configurePathMapperTableMatchAnyMethodIndex);
    }

    @Override
    public void configureNextHopTableMatchAny(
            String sffNodeName,
            boolean doDrop,
            boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureNextHopTableMatchAnyMethodIndex);
    }

    @Override
    public void configureTransportEgressTableMatchAny(
            String sffNodeName,
            boolean doDrop,
            boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureTransportEgressTableMatchAnyMethodIndex);
    }

    @Override
    public void configureGroup(
            final String sffNodeName,
            final String openflowNodeId,
            final String sfgName,
            final long sfgId,
            int groupType,
            List<GroupBucketInfo> bucketInfos,
            final boolean isAddGroup) {
        incrementMethodCalled(MethodIndeces.configureGroupMethodIndex);
    }

    @Override
    public void configureGroupNextHopFlow(String sffNodeName, long sfpId, String srcMac, long groupId,
            String groupName, boolean isAddFlow) {
        incrementMethodCalled(MethodIndeces.configureGroupNextHopFlow);
    }
}
