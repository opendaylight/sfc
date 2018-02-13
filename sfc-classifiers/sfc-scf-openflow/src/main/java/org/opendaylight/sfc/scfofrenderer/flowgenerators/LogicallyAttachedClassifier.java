/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.flowgenerators;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.ClassifierGeniusIntegration;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcRspInfo;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfOfUtils;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicallyAttachedClassifier implements ClassifierInterface {
    private final LogicalClassifierDataGetter logicalSffDataGetter;

    private final ClassifierHandler classifierHandler;

    private static final Logger LOG = LoggerFactory.getLogger(LogicallyAttachedClassifier.class);

    public LogicallyAttachedClassifier(LogicalClassifierDataGetter theDataGetter) {
        logicalSffDataGetter = theDataGetter;
        classifierHandler = new ClassifierHandler();
    }

    @Override
    public FlowDetails initClassifierTable(String nodeId) {
        MatchBuilder match = new MatchBuilder();

        Action geniusDispatcher = SfcOpenflowUtils.createActionResubmitTable(NwConstants.LPORT_DISPATCHER_TABLE, 0);
        List<Action> theActionList = new ArrayList<>();
        theActionList.add(geniusDispatcher);
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActionList);

        FlowBuilder fb = SfcOpenflowUtils.createFlowBuilder(this.getClassifierTable(),
                SfcScfOfUtils.FLOW_PRIORITY_MATCH_ANY, "MatchAny", match, isb);

        LOG.info("initClassifierTable - jump to table: {}", NwConstants.LPORT_DISPATCHER_TABLE);
        return classifierHandler.addRspRelatedFlowIntoNode(nodeId, fb, OpenflowConstants.SFC_FLOWS);
    }

    @Override
    public FlowDetails createClassifierOutFlow(String nodeId, String flowKey, Match match, SfcRspInfo sfcRspInfo) {

        if (Strings.isNullOrEmpty(flowKey) || sfcRspInfo == null || Strings.isNullOrEmpty(nodeId)) {
            LOG.error("createClassifierOutFlow - Wrong inputs; either the flow key of the NSH header are not correct");
            return null;
        }

        LOG.info("createClassifierOutFlow - Validated inputs");
        List<Action> theActions = SfcScfOfUtils.buildNshActions(sfcRspInfo);

        InstructionsBuilder isb;
        // if the classifier is co-located w/ the first SFF, we simply jump to
        // the SFC ingress table
        // otherwise, we forward the packet through the respective tunnel to the
        // intended SFF
        DpnIdType classifierNodeDpnId = LogicalClassifierDataGetter.getDpnIdFromNodeName(nodeId);
        DpnIdType firstHopDataplaneId = logicalSffDataGetter.getFirstHopDataplaneId(sfcRspInfo.getRsp())
                .orElseThrow(IllegalArgumentException::new);
        if (classifierNodeDpnId.equals(firstHopDataplaneId)) {
            LOG.info("createClassifierOutFlow - Classifier co-located w/ first SFF; jump to transport ingress: {}",
                    this.getTransportIngressTable());
            // generate the flows
            isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActions);
            // create GotoTable instruction - dispatcher table
            SfcOpenflowUtils.appendGotoTableInstruction(isb, this.getTransportIngressTable());
        } else {
            String theTunnelIf = logicalSffDataGetter
                    .getInterfaceBetweenDpnIds(classifierNodeDpnId, firstHopDataplaneId)
                    .orElseThrow(RuntimeException::new);

            LOG.info("createClassifierOutFlow - Must go through tunnel {}. src: {}; dst: {}", theTunnelIf,
                    classifierNodeDpnId.getValue(), firstHopDataplaneId.getValue());

            // since these actions from genius *have to* be appended to other
            // actions, we must pass
            // the size of the current actions as an offset, so that genius
            // generates the actions in
            // the correct 'order'
            List<Action> actionList = logicalSffDataGetter.getEgressActionsForTunnelInterface(theTunnelIf,
                    theActions.size());

            if (!actionList.isEmpty()) {
                theActions.addAll(actionList);
            }

            isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActions);
        }

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey)).setTableId(this.getClassifierTable())
                .setKey(new FlowKey(new FlowId(flowKey))).setPriority(SfcScfOfUtils.FLOW_PRIORITY_CLASSIFIER)
                .setMatch(match).setInstructions(isb.build());
        return classifierHandler.addRspRelatedFlowIntoNode(nodeId, flowb, sfcRspInfo.getNshNsp());
    }

    // this type of classifier does not require 'in' flows
    @Override
    public FlowDetails createClassifierInFlow(String nodeId, String flowKey, SfcRspInfo sfcRspInfo, Long outPort) {
        return null;
    }

    // this type of classifier does not require 'relay' flows
    @Override
    public FlowDetails createClassifierRelayFlow(String nodeId, String flowKey, SfcRspInfo sfcRspInfo,
                                                 String classifierName) {
        return null;
    }

    @Override
    public List<FlowDetails> createDpdkFlows(String nodeId, SfcRspInfo sfcRspInfo) {
        // DPDK flows are not supported in logical SFF
        return Collections.emptyList();
    }

    @Override
    public Optional<String> getNodeName(String interfaceName) {
        return logicalSffDataGetter.getNodeName(interfaceName);
    }

    @Override
    public Optional<Long> getInPort(String nodeId, String interfaceName) {
        return getInPort(interfaceName);
    }

    /**
     * Get the name of the input openflow port, given an interface name.
     *
     * @param interfaceName
     *            the name of the neutron port
     * @return the input openflow port, if any
     */
    private Optional<Long> getInPort(String interfaceName) {
        return LogicalClassifierDataGetter.getOpenflowPort(interfaceName);
    }

    @Override
    public short getClassifierTable() {
        return ClassifierGeniusIntegration.getClassifierTable();
    }

    @Override
    public short getTransportIngressTable() {
        return ClassifierGeniusIntegration.getTransportIngressTable();
    }
}
