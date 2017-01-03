/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.flowgenerators;

import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcNshHeader;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfOfUtils;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BareClassifier implements ClassifierInterface {
    private ServiceFunctionForwarder sff;

    private final ClassifierHandler classifierHandler;

    public BareClassifier() {classifierHandler = new ClassifierHandler();}

    public BareClassifier(ServiceFunctionForwarder theSff) {
        this();
        sff = theSff;
    }

    public BareClassifier setSff(ServiceFunctionForwarder theSff) {
        sff = theSff;
        return this;
    }

    @Override
    public FlowDetails initClassifierTable(String nodeName) {
        return classifierHandler.addRspRelatedFlowIntoNode(nodeName,
                SfcScfOfUtils.initClassifierTable(),
                OpenflowConstants.SFC_FLOWS);
    }

    @Override
    public FlowDetails createClassifierOutFlow(String flowKey,
                                               Match match,
                                               SfcNshHeader sfcNshHeader,
                                               String classifierNodeName) {
        Long outPort = SfcOvsUtil.getVxlanGpeOfPort(classifierNodeName);
        return classifierHandler.addRspRelatedFlowIntoNode(classifierNodeName,
                SfcScfOfUtils.createClassifierOutFlow(flowKey, match, sfcNshHeader, outPort),
                sfcNshHeader.getNshNsp());
    }

    @Override
    public FlowDetails createClassifierInFlow(String flowKey, SfcNshHeader sfcNshHeader, Long port, String nodeName) {
        return classifierHandler.addRspRelatedFlowIntoNode(nodeName,
                SfcScfOfUtils.createClassifierInFlow(flowKey, sfcNshHeader, port),
                sfcNshHeader.getNshNsp());
    }

    @Override
    public FlowDetails createClassifierRelayFlow(String flowKey, SfcNshHeader sfcNshHeader, String nodeName) {
        return classifierHandler.addRspRelatedFlowIntoNode(nodeName,
                SfcScfOfUtils.createClassifierRelayFlow(flowKey, sfcNshHeader),
                sfcNshHeader.getNshNsp());
    }

    @Override
    public List<FlowDetails> createDpdkFlows(String nodeName, long rspPathId) {
        // add DPDK flows
        Long dpdkPort = SfcOvsUtil.getDpdkOfPort(nodeName, null);
        if (dpdkPort == null) {
            return Collections.emptyList();
        }
        List<FlowDetails> theFlows = new ArrayList<>();
        theFlows.add(classifierHandler.addRspRelatedFlowIntoNode(
                nodeName,
                SfcScfOfUtils.initClassifierDpdkOutputFlow(dpdkPort), rspPathId));
        theFlows.add(classifierHandler.addRspRelatedFlowIntoNode(
                nodeName,
                SfcScfOfUtils.initClassifierDpdkInputFlow(nodeName, dpdkPort), rspPathId));
        return theFlows;
    }

    @Override
    public Optional<String> getNodeName(String theInterfaceName) {
        return Optional.ofNullable(sff)
                .filter(theSff -> theSff.getAugmentation(SffOvsBridgeAugmentation.class) != null)
                .map(SfcOvsUtil::getOpenFlowNodeIdForSff);
    }

    @Override
    public Optional<Long> getInPort(String ifName, String nodeName) {
        return Optional.ofNullable(SfcOvsUtil.getOfPortByName(nodeName, ifName));
    }
}
