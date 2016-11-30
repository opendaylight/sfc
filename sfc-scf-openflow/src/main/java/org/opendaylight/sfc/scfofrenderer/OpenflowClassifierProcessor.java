/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import com.google.common.eventbus.Subscribe;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.ClassifierGeniusIntegration;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicallyAttachedClassifier;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenflowClassifierProcessor implements ClassifierHandler{

    private WriteTransaction tx = null;

    // true if we're adding a classifier node, false if we're deleting it
    private boolean addClassifier = true;

    private ClassifierInterface classifierInterface = null;

    private BareClassifier bareClassifier;

    private LogicallyAttachedClassifier logicallyAttachedClassifier;

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowClassifierProcessor.class);

    // hide the default constructor
    private OpenflowClassifierProcessor() {}

    public OpenflowClassifierProcessor(WriteTransaction theTx,
                                       LogicallyAttachedClassifier theLogicClassifier,
                                       BareClassifier theBareClassifier) {
        tx = theTx;
        classifierInterface = theBareClassifier;
        logicallyAttachedClassifier = theLogicClassifier;
        bareClassifier = theBareClassifier;
    }

    /**
     * Process an Scf object, adding or removing the OF rules into the respective OVS
     * This method is called on result of classifier addition / removal.
     *
     * @param theClassifier the classifier node to be added
     * @param theAcl        the ACL we want to install in the classifier
     * @return              a List of {@link FlowDetails} having all the generated flows, which will be later installed
     */
    public List<FlowDetails> processClassifier(
            final SclServiceFunctionForwarder theClassifier,
            final Acl theAcl,
            final boolean addClassifierScenario) {
        addClassifier = addClassifierScenario;

        SffName sffName = new SffName(theClassifier.getName());

        Optional<String> itfName = getInterfaceNameFromClassifier(theClassifier);

        ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);

        if (sff == null || !itfName.isPresent()) {
            LOG.error("createdServiceFunctionClassifier: " +
                            "Cannot install ACL rules in classifier. SFF exists? {}; Interface exists? {}",
                    sff != null,
                    itfName.isPresent());
            return Collections.emptyList();
        }

        // choose which handler to use
        classifierInterface = usesLogicalInterfaces(sff) ?
                logicallyAttachedClassifier : bareClassifier.setSff(sff);

        Optional<String> nodeName = classifierInterface.getNodeName(itfName.get());

        if(!nodeName.isPresent()) {
            LOG.error("createdServiceFunctionClassifier: Could not extract the node name from the OVS interface");
            return Collections.emptyList();
        }

        // bind/unbind the interface in genius, if the classifier is attached to a logical interface
        // (according to the scenario)
        if (usesLogicalInterfaces(sff)) {
            if (addClassifierScenario) {
                ClassifierGeniusIntegration.performGeniusServiceBinding(tx, itfName.get());
                LOG.info("processClassifier - Bound interface {}", itfName.get());
            }
            else {
                ClassifierGeniusIntegration.performGeniusServiceUnbinding(tx, itfName.get());
                LOG.info("processClassifier - Unbound interface {}", itfName.get());
            }
        }

        return theAcl
                .getAccessListEntries()
                .getAce()
                .stream()
                .map(theAce -> processAce(nodeName.get(),
                        sff,
                        theClassifier.getName(),
                        theAcl.getAclName(),
                        itfName.get(),
                        theAce))
                .reduce(new ArrayList<>(),
                        (dstList, theList) ->
                                Stream.concat(dstList.stream(), theList.stream()).collect(Collectors.toList()));
    }

    /**
     * Install an ACE entry, belonging to the given ACL, on the SFF identified through the specified nodeName.
     * This method is called on result of classifier addition / removal.
     *
     * @param nodeName      the compute node data-plane ID where the ACL is about to be written
     * @param theSff        the SFF to which the classifier is connected
     * @param theScfName    the name of the classifier
     * @param aclName       the name of the ACL
     * @param theIfName     the interface we want to classify
     * @param theAce        the ACE
     * @return              a List of {@link FlowDetails} having all the generated flows, which will be later installed
     */
    private List<FlowDetails> processAce(String nodeName, ServiceFunctionForwarder theSff, String theScfName,
                                        String aclName, String theIfName, Ace theAce) {

        List<FlowDetails> theFlows = new ArrayList<>();

        String ruleName = theAce.getRuleName();
        if (ruleName == null) {
            LOG.error("processAce - ruleName is null; returning empty list");
            return Collections.emptyList();
        }

        LOG.info("processAce - NodeName: {}; IF name: {}", nodeName, theIfName);

        Optional<Long> inPort = classifierInterface.getInPort(theIfName, nodeName);
        if (!inPort.isPresent()) {
            LOG.error("processAce - port is null; returning empty list");
            return Collections.emptyList();
        }

        NodeConnectorId port = new NodeConnectorId(String.format("%s:%s", nodeName, inPort.get()));

        LOG.debug("processAce: in port: {}", inPort.get());
        // Match
        Match match = new SfcScfMatch()
                .setPortMatch(port)
                .setAclMatch(theAce.getMatches())
                .build();

        LOG.debug("processAce: Match object created: {}", match);
        // Action
        Optional<Actions> actions = Optional.ofNullable(theAce.getActions());
        if (!actions.isPresent()) {
            LOG.error("processAce: action is null; returning empty list");
            return Collections.emptyList();
        }

        Optional<RspName> rspName = actions
                .map(theActions -> theActions.getAugmentation(Actions1.class))
                .map(actions1 -> (AclRenderedServicePath) actions1.getSfcAction())
                .map(aclRsp -> new RspName(aclRsp.getRenderedServicePath()));

        if (!rspName.isPresent()) {
            LOG.error("processAce - Could not retrieve the RSP name from the given Actions object: {}", actions.get());
        }

        LOG.debug("processAce - The RSP name: {}", rspName.get());
        SfcNshHeader nsh = SfcNshHeader.getSfcNshHeader(rspName.get());

        if (nsh == null) {
            LOG.error("processAce: nsh is null; returning empty list");
            return Collections.emptyList();
        }

        String flowKey = buildFlowKeyName(theScfName, aclName, ruleName, ".out");

        // add a classifier
        if (addClassifier) {
            // write the flows into the classifier
            LOG.info("processAce - About to create flows");
            FlowBuilder initFlow, theOutFlow;
            initFlow = classifierInterface.initClassifierTable();
            theOutFlow  = classifierInterface.createClassifierOutFlow(flowKey, match, nsh, nodeName);

            theFlows.add(addRspRelatedFlowIntoNode(nodeName, initFlow, nsh.getNshNsp()));
            theFlows.add(addRspRelatedFlowIntoNode(nodeName, theOutFlow, nsh.getNshNsp()));

            // add DPDK flows
            Long dpdkPort = SfcOvsUtil.getDpdkOfPort(nodeName, null);
            LOG.debug("processAce - The DPDK port: {}", dpdkPort);
            if (dpdkPort != null) {
                theFlows.add(addRspRelatedFlowIntoNode(
                        nodeName,
                        SfcScfOfUtils.initClassifierDpdkOutputFlow(dpdkPort), nsh.getNshNsp()));
                theFlows.add(addRspRelatedFlowIntoNode(
                        nodeName,
                        SfcScfOfUtils.initClassifierDpdkInputFlow(nodeName, dpdkPort), nsh.getNshNsp()));
            }
        }
        else
        {
            LOG.info("processAce - About to delete the *out* flows");
            theFlows.add(deleteFlowFromTable(nodeName, flowKey, ClassifierGeniusIntegration.getClassifierTable()));
        }

        // when the classifier is attached to a logical SFF, there's no need to process the reverse RSP, so we bail
        if (usesLogicalInterfaces(theSff)) {
            return theFlows;
        }

        List<FlowDetails> theReverseRspFlows = processReverseRsp(rspName.get(),
                theScfName,
                aclName,
                nodeName,
                theAce.getRuleName(),
                inPort.get(),
                theSff);
        if (!theReverseRspFlows.isEmpty()) {
            theFlows.addAll(theReverseRspFlows);
        }

        LOG.debug("processAce - flow size: {}", theFlows.size());
        return theFlows;
    }

    /**
     * Add the classifier flows for reverse RSPs.
     *
     * @param theRspName    the RSP from which we want to derive the reverse RSP
     * @param theScfName    the name of the classifier who will process this reverse RSP traffic
     * @param theAclName    the name of the ACL
     * @param theNodeName   the compute node name where we will install the classifier flows for the reverse RSP
     * @param theRuleName   the name of the ACE
     * @param port          the output port of the classifier node
     * @param theSff        the SFF to which the classifier is connected
     * @return              a List of {@link FlowDetails} having all the generated flows, which will be later installed
     */
    protected List<FlowDetails> processReverseRsp(RspName theRspName,
                                                      String theScfName,
                                                      String theAclName,
                                                      String theNodeName,
                                                      String theRuleName,
                                                      long port,
                                                      ServiceFunctionForwarder theSff) {

        LOG.info("processReverseRsp - RSP name: {}", theRspName.getValue());
        List<FlowDetails> theFlows = new ArrayList<>();

        RspName reverseRspName = getReverseRspName(theRspName);
        SfcNshHeader reverseNsh = SfcNshHeader.getSfcNshHeader(reverseRspName);

        if (reverseNsh == null) {
            LOG.warn("processReverseRsp: reverseNsh is null");
            return Collections.emptyList();
        } else {
            String flowKey = buildFlowKeyName(theScfName, theAclName, theRuleName, ".in");

            if (addClassifier) {
                Optional<FlowDetails> theInFlow =
                        Optional.ofNullable(classifierInterface.createClassifierInFlow(flowKey, reverseNsh, port))
                        .map(flowBuilder -> addRspRelatedFlowIntoNode(theNodeName, flowBuilder, reverseNsh.getNshNsp()));
                if (theInFlow.isPresent()) {
                    LOG.info("processReverseRsp: Adding in flow to node {}", theNodeName);
                    theFlows.add(theInFlow.get());
                }
            }
            else {
                FlowDetails deleteRelayFlow =
                        deleteFlowFromTable(theNodeName, flowKey, ClassifierGeniusIntegration.getClassifierTable());
                theFlows.add(deleteRelayFlow);
            }

            SffName lastSffName = reverseNsh.getSffName();
            if (lastSffName != null &&
                    !reverseNsh.getSffName().equals(theSff.getName())) {
                ServiceFunctionForwarder lastSff =
                        SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(lastSffName);
                String lastNodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(lastSff);
                if (lastNodeName == null) {
                    LOG.error("processReverseRsp: lastNodeName is null");
                    // keep old behaviour, and return the flows up 'till now
                    return theFlows;
                } else {
                    flowKey = buildFlowKeyName(theScfName, theAclName, theRuleName, ".relay");
                    if (addClassifier) {
                        Ip ip = SfcOvsUtil.getSffVxlanDataLocator(theSff);
                        reverseNsh.setVxlanIpDst(ip.getIp().getIpv4Address());
                        reverseNsh.setVxlanUdpPort(ip.getPort());
                        Optional<FlowDetails> theRelayFlow =
                                Optional.ofNullable(classifierInterface.createClassifierRelayFlow(flowKey, reverseNsh))
                                        .map(flowBuilder -> addRspRelatedFlowIntoNode(lastNodeName,
                                                flowBuilder,
                                                reverseNsh.getNshNsp()));
                        if (theRelayFlow.isPresent()) {
                            LOG.info("processReverseRsp: Adding relay flow to node {}", lastNodeName);
                            theFlows.add(theRelayFlow.get());
                        }
                    }
                    else {
                        FlowDetails deleteRelayFlow = deleteFlowFromTable(lastNodeName,
                                flowKey,
                                ClassifierGeniusIntegration.getClassifierTable());
                        theFlows.add(deleteRelayFlow);
                    }
                }
            }
        }
        return theFlows;
    }

    /**
     * Handler method of the {@link com.google.common.eventbus.EventBus class}
     *
     * @param theTx
     */
    @Subscribe public void refreshTransaction(WriteTransaction theTx) {
        LOG.debug("refreshTransaction - refreshing the transaction.");
        tx = theTx;
    }
}
