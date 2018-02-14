/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.flowgenerators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcRspInfo;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfOfUtils;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;

public class BareClassifier implements ClassifierInterface {
    private ServiceFunctionForwarder sff;

    private final ClassifierHandler classifierHandler;

    public BareClassifier() {
        classifierHandler = new ClassifierHandler();
    }

    public BareClassifier(ServiceFunctionForwarder theSff) {
        this();
        sff = theSff;
    }

    public BareClassifier setSff(ServiceFunctionForwarder theSff) {
        sff = theSff;
        return this;
    }

    @Override
    public FlowDetails initClassifierTable(String nodeId) {
        return classifierHandler.addRspRelatedFlowIntoNode(nodeId, SfcScfOfUtils.initClassifierTable(),
                OpenflowConstants.SFC_FLOWS);
    }

    @Override
    public FlowDetails createClassifierOutFlow(String nodeId, String flowKey, Match match, SfcRspInfo sfcRspInfo) {
        Long outPort = SfcOvsUtil.getVxlanGpeOfPort(nodeId);
        return classifierHandler.addRspRelatedFlowIntoNode(nodeId,
                SfcScfOfUtils.createClassifierOutFlow(flowKey, match, sfcRspInfo, outPort), sfcRspInfo.getNshNsp());
    }

    @Override
    public FlowDetails createClassifierInFlow(String nodeId, String flowKey, SfcRspInfo sfcRspInfo, Long port) {
        return classifierHandler.addRspRelatedFlowIntoNode(nodeId,
                SfcScfOfUtils.createClassifierInFlow(flowKey, sfcRspInfo, port), sfcRspInfo.getNshNsp());
    }

    @Override
    public FlowDetails createClassifierRelayFlow(String nodeId, String flowKey, SfcRspInfo sfcRspInfo) {
        return classifierHandler.addRspRelatedFlowIntoNode(nodeId,
                SfcScfOfUtils.createClassifierRelayFlow(flowKey, sfcRspInfo), sfcRspInfo.getNshNsp());
    }

    @Override
    public List<FlowDetails> createDpdkFlows(String nodeId, SfcRspInfo sfcRspInfo) {
        // add DPDK flows
        Long dpdkPort = SfcOvsUtil.getDpdkOfPort(nodeId, null);
        if (dpdkPort == null) {
            return Collections.emptyList();
        }
        List<FlowDetails> theFlows = new ArrayList<>();
        theFlows.add(classifierHandler.addRspRelatedFlowIntoNode(nodeId,
                SfcScfOfUtils.initClassifierDpdkOutputFlow(dpdkPort), sfcRspInfo.getNshNsp()));
        theFlows.add(classifierHandler.addRspRelatedFlowIntoNode(nodeId,
                SfcScfOfUtils.initClassifierDpdkInputFlow(nodeId, dpdkPort), sfcRspInfo.getNshNsp()));
        return theFlows;
    }

    @Override
    public Optional<String> getNodeName(String interfaceName) {
        return Optional.ofNullable(sff).map(SfcOvsUtil::getOpenFlowNodeIdForSff);
    }

    @Override
    public Optional<Long> getInPort(String nodeId, String interfaceName) {
        return Optional.ofNullable(SfcOvsUtil.getOfPortByName(nodeId, interfaceName));
    }

    @Override
    public short getClassifierTable() {
        return SfcScfOfUtils.TABLE_INDEX_CLASSIFIER;
    }

    @Override
    public short getTransportIngressTable() {
        return SfcScfOfUtils.TABLE_INDEX_INGRESS_TRANSPORT;
    }
}
