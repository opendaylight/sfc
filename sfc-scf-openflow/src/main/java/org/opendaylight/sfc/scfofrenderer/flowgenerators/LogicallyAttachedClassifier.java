/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.flowgenerators;

import com.google.common.base.Strings;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcNshHeader;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfOfUtils;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.ClassifierGeniusIntegration;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicallyAttachedClassifier implements ClassifierInterface {
    private final LogicalClassifierDataGetter logicalSffDataGetter;

    private final ClassifierHandler classifierHandler;

    private static final Logger LOG = LoggerFactory.getLogger(LogicallyAttachedClassifier.class);

    private static final short GENIUS_DISPATCHER_TABLE = 0x11;

    private static final short TUN_GPE_NP_NSH = 0x4;

    public LogicallyAttachedClassifier(LogicalClassifierDataGetter theDataGetter) {
        logicalSffDataGetter = theDataGetter;
        classifierHandler = new ClassifierHandler();
    }

    @Override
    public FlowDetails initClassifierTable(String nodeName) {
        MatchBuilder match = new MatchBuilder();

        Action geniusDispatcher = SfcOpenflowUtils.createActionResubmitTable(GENIUS_DISPATCHER_TABLE, 0);
        List<Action> theActionList = new ArrayList<Action>() {{ add(geniusDispatcher); }};
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActionList);

        FlowBuilder fb = SfcOpenflowUtils.createFlowBuilder(
                ClassifierGeniusIntegration.getClassifierTable(),
                SfcScfOfUtils.FLOW_PRIORITY_MATCH_ANY, "MatchAny", match, isb);

        LOG.info("initClassifierTable - jump to table: {}", ClassifierGeniusIntegration.getClassifierTable());
        return classifierHandler.addRspRelatedFlowIntoNode(nodeName, fb, OpenflowConstants.SFC_FLOWS);
    }

    /**
     * Create the flows for the genius integrated classifier
     * @param flowKey               the key for the flow objects
     * @param match                 the Match object
     * @param sfcNshHeader          all related NSH info is encapsulated within this object
     * @param classifierNodeName    the node name of the classifier (ex: "openflow:dpnID")
     * @return                      a FlowBuilder object containing the desired flow
     */
    @Override
    public FlowDetails createClassifierOutFlow(String flowKey,
                                               Match match,
                                               SfcNshHeader sfcNshHeader,
                                               String classifierNodeName) {

        if ( Strings.isNullOrEmpty(flowKey) || sfcNshHeader == null || Strings.isNullOrEmpty(classifierNodeName) ) {
            LOG.error("createClassifierOutFlow - Wrong inputs; either the flow key of the NSH header are not correct");
            return null;
        }

        LOG.info("createClassifierOutFlow - Validated inputs");
        List<Action> theActions = buildNshActions(sfcNshHeader);

        InstructionsBuilder isb;
        // if the classifier is co-located w/ the first SFF, we simply jump to the SFC ingress table
        // otherwise, we forward the packet through the respective tunnel to the intended SFF
        DpnIdType classifierNodeDpnId = LogicalClassifierDataGetter.getDpnIdFromNodeName(classifierNodeName);
        DpnIdType firstHopDataplaneId = logicalSffDataGetter.getFirstHopDataplaneId(sfcNshHeader.getRsp())
                .orElseThrow(IllegalArgumentException::new);
        if(classifierNodeDpnId.equals(firstHopDataplaneId)) {
            LOG.info("createClassifierOutFlow - Classifier co-located w/ first SFF; jump to transport ingress: {}",
                    ClassifierGeniusIntegration.getTransportIngressTable());
            // generate the flows
            isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActions);
            // create GotoTable instruction - dispatcher table
            SfcOpenflowUtils.appendGotoTableInstruction(isb, ClassifierGeniusIntegration.getTransportIngressTable());
        }
        else
        {
            String theTunnelIf = logicalSffDataGetter.getInterfaceBetweenDpnIds(classifierNodeDpnId, firstHopDataplaneId)
                    .orElseThrow(RuntimeException::new);

            LOG.info("createClassifierOutFlow - Must go through tunnel: {}. src: {}; dst: {}",
                    theTunnelIf,
                    classifierNodeDpnId.getValue().toString(),
                    firstHopDataplaneId.getValue().toString());

            // since these actions from genius *have to* be appended to other actions, we must pass
            // the size of the current actions as an offset, so that genius generates the actions in
            // the correct 'order'
            List<Action> actionList =
                    logicalSffDataGetter.getEgressActionsForTunnelInterface(theTunnelIf, theActions.size());

            if (!actionList.isEmpty()) {
                theActions.addAll(actionList);
            }

            isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActions);
        }

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey))
                .setTableId(ClassifierGeniusIntegration.getClassifierTable())
                .setKey(new FlowKey(new FlowId(flowKey)))
                .setPriority(SfcScfOfUtils.FLOW_PRIORITY_CLASSIFIER)
                .setMatch(match)
                .setInstructions(isb.build());
        return classifierHandler.addRspRelatedFlowIntoNode(classifierNodeName, flowb, sfcNshHeader.getNshNsp());
    }

    // this type of classifier does not require 'in' flows
    @Override
    public FlowDetails createClassifierInFlow(String flowKey, SfcNshHeader sfcNshHeader, Long outPort, String nodeName) {
        return null;
    }

    // this type of classifier does not require 'relay' flows
    @Override
    public FlowDetails createClassifierRelayFlow(String flowKey, SfcNshHeader sfcNshHeader, String nodeName) {
        return null;
    }

    @Override
    public List<FlowDetails> createDpdkFlows(String nodeName, long rspPathId) {
        // DPDK flows are not supported in logical SFF
        return Collections.emptyList();
    }

    /**
     * Get the name of the compute node connected to the supplied interfaceName
     * @param theInterfaceName  the interface name.
     * @return                  the name of the compute node hosting the supplied SF. ex: "openflow:xxx"
     */
    @Override
    public Optional<String> getNodeName(String theInterfaceName) {
        return logicalSffDataGetter.getNodeName(theInterfaceName);
    }

    /**
     * Get the input openflow port, given an interface name, and a nodeName, if any
     * @param ifName        the name of the neutron port
     * @param nodeName      the name of the node (ex: "openflow:xxx")
     * @return              the openflow port, if any
     */
    @Override
    public Optional<Long> getInPort(String ifName, String nodeName) {
        return getInPort(ifName);
    }

    /**
     * Get the name of the input openflow port, given an interface name
     * @param ifName    the name of the neutron port
     * @return          the input openflow port, if any
     */
    private Optional<Long> getInPort(String ifName) {
        return LogicalClassifierDataGetter.getOpenflowPort(ifName);
    }

    /**
     * Build a list of actions which will be installed into the classifier
     * @param theHeader the {@link SfcNshHeader} object encapsulating all NSH related data
     * @return          the List of {@link Action} related to NSH which will be pushed into the classifier
     */
    private List<Action> buildNshActions(SfcNshHeader theHeader) {
        return new ArrayList<Action>(){{
            int order = 0;
            add(SfcOpenflowUtils.createActionNxPushNsh(order++));
            add(SfcOpenflowUtils.createActionNxLoadNshMdtype(SfcScfOfUtils.NSH_MDTYPE_ONE, order++));
            add(SfcOpenflowUtils.createActionNxLoadNshNp(SfcScfOfUtils.NSH_NP_ETH, order++));
            add(SfcOpenflowUtils.createActionNxSetNsp(theHeader.getNshNsp(), order++));
            add(SfcOpenflowUtils.createActionNxSetNsi(theHeader.getNshStartNsi(), order++));
            add(SfcOpenflowUtils.createActionNxSetNshc1(theHeader.getNshMetaC1(), order++));
            add(SfcOpenflowUtils.createActionNxSetNshc2(theHeader.getNshMetaC2(), order++));
            add(SfcOpenflowUtils.createActionNxSetNshc3(theHeader.getNshMetaC3(), order++));
            add(SfcOpenflowUtils.createActionNxSetNshc4(theHeader.getNshMetaC4(), order++));
            add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(TUN_GPE_NP_NSH, order++));
        }};
    }
}
