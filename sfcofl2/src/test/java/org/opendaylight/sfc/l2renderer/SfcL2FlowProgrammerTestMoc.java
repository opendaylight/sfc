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
        configureArpTransportIngressFlowMethodIndex,

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
        configureNshNscTransportEgressFlowMethodIndex,

        configureTransportIngressTableMatchAnyMethodIndex,
        configurePathMapperTableMatchAnyMethodIndex,
        configurePathMapperAclTableMatchAnyMethodIndex,
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

    @Override
    public void setFlowRspId(final Long rspId) {
    }

    @Override
    public void deleteRspFlows(final Long rspId) {
    }

    //---------------------------------------------
    //
    //            Transport Ingress methods
    //
    //---------------------------------------------

    @Override
    public void configureIpv4TransportIngressFlow(String sffNodeName) {
        incrementMethodCalled(MethodIndeces.configureIpv4TransportIngressFlowMethodIndex);
    }

    @Override
    public void configureVlanTransportIngressFlow(String sffNodeName) {
        incrementMethodCalled(MethodIndeces.configureVlanTransportIngressFlowMethodIndex);
    }

    @Override
    public void configureVxlanGpeTransportIngressFlow(String sffNodeName) {
        incrementMethodCalled(MethodIndeces.configureVxlanGpeTransportIngressFlowMethodIndex);
    }

    @Override
    public void configureMplsTransportIngressFlow(String sffNodeName) {
        incrementMethodCalled(MethodIndeces.configureMplsTransportIngressFlowMethodIndex);
    }

    //---------------------------------------------
    //
    //            PathMapper methods
    //
    //---------------------------------------------

    @Override
    public void configureMacPathMapperFlow(
            String sffNodeName, String mac, long pathId, boolean isSf) {
        incrementMethodCalled(MethodIndeces.configureMacPathMapperFlowMethodIndex);
    }

    @Override
    public void configureMplsPathMapperFlow(
            String sffNodeName, long label, long pathId, boolean isSf) {
        incrementMethodCalled(MethodIndeces.configureMplsPathMapperFlowMethodIndex);
    }

    @Override
    public void configureVlanPathMapperFlow(
            String sffNodeName, int vlan, long pathId, boolean isSf) {
        incrementMethodCalled(MethodIndeces.configureVlanPathMapperFlowMethodIndex);
    }

    @Override
    public void configureVxlanGpePathMapperFlow(
            String sffNodeName, long nsp, short nsi, long pathId) {
        incrementMethodCalled(MethodIndeces.configureVxlanGpePathMapperFlowMethodIndex);
    }

    //---------------------------------------------
    //
    //            Next Hop methods
    //
    //---------------------------------------------

    @Override
    public void configureNextHopFlow(
            String sffNodeName, long sfpId, String srcMac, String dstMac) {
        incrementMethodCalled(MethodIndeces.configureNextHopFlowMethodIndex);
    }

    @Override
    public void configureVxlanGpeNextHopFlow(
            String sffNodeName, String dstIp, long nsp, short nsi) {
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
            final boolean isLastHop,
            boolean doPktIn) {
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
            final boolean isLastHop,
            boolean doPktIn) {
        incrementMethodCalled(MethodIndeces.configureVlanTransportEgressFlowMethodIndex);
    }

    @Override
    public void configureVxlanGpeTransportEgressFlow(
            String sffNodeName,
            long nshNsp,
            short nshNsi,
            String port,
            final boolean isLastHop,
            final boolean doPktIn) {
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
            final boolean isLastHop,
            boolean doPktIn) {
        incrementMethodCalled(MethodIndeces.configureMplsTransportEgressFlowMethodIndex);
    }

    @Override
    public void configureNshNscTransportEgressFlow(
            String sffNodeName,
            final long nshNsp,
            final short nshNsi,
            String switchPort) {
        incrementMethodCalled(MethodIndeces.configureNshNscTransportEgressFlowMethodIndex);
    }

    //---------------------------------------------
    //
    //            Match Any methods
    //
    //---------------------------------------------

    @Override
    public void configureTransportIngressTableMatchAny(
            String sffNodeName,
            boolean doDrop) {
        incrementMethodCalled(MethodIndeces.configureTransportIngressTableMatchAnyMethodIndex);
    }

    @Override
    public void configurePathMapperTableMatchAny(
            String sffNodeName,
            boolean doDrop) {
        incrementMethodCalled(MethodIndeces.configurePathMapperTableMatchAnyMethodIndex);
    }

    @Override
    public void configurePathMapperAclTableMatchAny(
            String sffNodeName,
            boolean doDrop) {
        incrementMethodCalled(MethodIndeces.configurePathMapperAclTableMatchAnyMethodIndex);
    }

    @Override
    public void configureNextHopTableMatchAny(
            String sffNodeName,
            boolean doDrop) {
        incrementMethodCalled(MethodIndeces.configureNextHopTableMatchAnyMethodIndex);
    }

    @Override
    public void configureTransportEgressTableMatchAny(
            String sffNodeName,
            boolean doDrop) {
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
    public void configureGroupNextHopFlow(String sffNodeName, long sfpId, String srcMac, long groupId, String groupName) {
        incrementMethodCalled(MethodIndeces.configureGroupNextHopFlow);
    }

    @Override
    public void configureArpTransportIngressFlow(String sffNodeName, String mac) {
        incrementMethodCalled(MethodIndeces.configureArpTransportIngressFlowMethodIndex);
    }
}
