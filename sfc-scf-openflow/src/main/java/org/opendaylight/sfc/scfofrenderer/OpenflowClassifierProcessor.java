/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.ClassifierGeniusIntegration;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicallyAttachedClassifier;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.Interface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
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

public class OpenflowClassifierProcessor {

    private WriteTransaction tx = null;

    // true if we're adding a classifier node, false if we're deleting it
    private boolean addClassifier = true;

    private ClassifierInterface classifierInterface = null;

    private LogicalClassifierDataGetter dataGetter;

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowClassifierProcessor.class);

    // hide the default constructor
    private OpenflowClassifierProcessor() {}

    public OpenflowClassifierProcessor(WriteTransaction theTx, RpcProviderRegistry theRpcProvider) {
        tx = theTx;
        dataGetter = new LogicalClassifierDataGetter(new SfcGeniusRpcClient(theRpcProvider));
    }

    public OpenflowClassifierProcessor(WriteTransaction theTx,
                                       RpcProviderRegistry theRpcProvider,
                                       ClassifierInterface theLogicClassifier) {
        this(theTx, theRpcProvider);
        classifierInterface = theLogicClassifier;
    }

    /**
     * Process an Scf object, adding or removing the OF rules into the respective OVS
     *
     * @param theClassifier the classifier node to be added
     * @param theAcl        the ACL we want to install in the classifier
     * @return              a List of {@link FlowDetails} having all the generated flows, which will be later installed
     */
    protected List<FlowDetails> processClassifier(
            final SclServiceFunctionForwarder theClassifier,
            final Acl theAcl,
            final boolean addClassifierScenario) {

        LOG.debug("Processing classifier ...");
        addClassifier = addClassifierScenario;

        SffName sffName = new SffName(theClassifier.getName());

        Optional<String> itfName = getInterfaceNameFromClassifier(theClassifier);

        ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);

        LOG.debug("# of DpnIds: {}", sff == null || sff.getSffDataPlaneLocator() == null ?
                0 : sff.getSffDataPlaneLocator().size());

        if (sff == null || !itfName.isPresent()) {
            LOG.error("createdServiceFunctionClassifier: " +
                            "Cannot install ACL rules in classifier. SFF exists? {}; Interface exists? {}",
                    sff != null,
                    itfName.isPresent());
            return Collections.emptyList();
        }

        // lazy initialization of the classifierInterface
        if(classifierInterface == null) {
            classifierInterface = usesLogicalInterfaces(sff) ?
                    new LogicallyAttachedClassifier(sff, dataGetter) : new BareClassifier(sff);
        }

        // TODO - genius interface binding here

        Optional<String> nodeName = classifierInterface.getNodeName(itfName.get());
        if(!nodeName.isPresent()) {
            LOG.error("createdServiceFunctionClassifier: Could not extract the node name from the OVS interface");
            return Collections.emptyList();
        }
        else {
            LOG.debug("Node name: {}", nodeName.get());
        }

        return theAcl
                .getAccessListEntries()
                .getAce()
                .stream()
                .map(theAce ->
                        processAce(
                                nodeName.get(),
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
     * Install an ACE entry, belonging to the given ACL, on the SFF
     *
     * @param nodeName      the compute node data-plane ID where the ACL is about to be written
     * @param theSff        the SFF to which the classifier is connected
     * @param theScfName    the name of the classifier
     * @param aclName       the name of the ACL
     * @param theIfName     the interface we want to classify
     * @param theAce        the ACE
     * @return              a List of {@link FlowDetails} having all the generated flows, which will be later installed
     */
    protected List<FlowDetails> processAce(String nodeName, ServiceFunctionForwarder theSff, String theScfName,
                                        String aclName, String theIfName, Ace theAce) {

        List<FlowDetails> theFlows = new ArrayList<>();

        String ruleName = theAce.getRuleName();
        if (ruleName == null) {
            LOG.error("processAce - ruleName is null; returning empty list");
            return Collections.emptyList();
        }

        LOG.info("processAce - gonna get the port; NodeName: {}; IF name: {}", nodeName, theIfName);

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

            theFlows.add(buildFlowEncapsulation(nodeName, initFlow));
            theFlows.add(buildFlowEncapsulation(nodeName, theOutFlow));

            // add DPDK flows
            Long dpdkPort = SfcOvsUtil.getDpdkOfPort(nodeName, null);
            LOG.debug("processAce - The DPDK port: {}", dpdkPort);
            if (dpdkPort != null) {
                theFlows.add(buildFlowEncapsulation(
                        nodeName,
                        SfcScfOfUtils.initClassifierDpdkOutputFlow(dpdkPort)));
                theFlows.add(buildFlowEncapsulation(
                        nodeName,
                        SfcScfOfUtils.initClassifierDpdkInputFlow(nodeName, dpdkPort)));
            }
        }
        else
        {
            LOG.info("processAce - About to delete the *out* flows");
            theFlows.add(buildFlowEncapsulation(nodeName, flowKey, ClassifierGeniusIntegration.getClassifierTable()));
        }

        List<FlowDetails> theReverseRspFlows =
                processReverseRsp(rspName.get(), theScfName, aclName, nodeName, theAce.getRuleName(), inPort.get(), theSff);
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
                        .map(flowBuilder -> buildFlowEncapsulation(theNodeName, flowBuilder));
                if (theInFlow.isPresent()) {
                    LOG.info("processReverseRsp: Adding in flow to node {}", theNodeName);
                    theFlows.add(theInFlow.get());
                }
            }
            else {
                FlowDetails deleteRelayFlow =
                        buildFlowEncapsulation(theNodeName, flowKey, ClassifierGeniusIntegration.getClassifierTable());
                theFlows.add(deleteRelayFlow);
            }

            SffName lastSffName = reverseNsh.getSffName();
            if (lastSffName != null &&
                    !reverseNsh.getSffName().equals(theSff.getName())) {
                ServiceFunctionForwarder lastSff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(lastSffName);
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
                                        .map(flowBuilder -> buildFlowEncapsulation(lastNodeName, flowBuilder));
                        if (theRelayFlow.isPresent()) {
                            LOG.info("processReverseRsp: Adding relay flow to node {}", lastNodeName);
                            theFlows.add(theRelayFlow.get());
                        }
                    }
                    else {
                        FlowDetails deleteRelayFlow =
                                buildFlowEncapsulation(lastNodeName, flowKey, ClassifierGeniusIntegration.getClassifierTable());
                        theFlows.add(deleteRelayFlow);
                    }
                }
            }
        }
        return theFlows;
    }

    /**
     * Build the name of the FlowKey given the names of the classifier, ACL, ACE, and the type of flow.
     *
     * @param scfName   the name of the classifier
     * @param aclName   the name of the ACL
     * @param aceName   the name of the ACE
     * @param type      the type of flow. The possible types are: 'in', 'out', and 'relay'
     * @return          the name which will be given to the flow object
     */
    protected static String buildFlowKeyName(String scfName, String aclName, String aceName, String type) {
        return new StringBuffer()
                .append(scfName)
                .append(aclName)
                .append(aceName)
                .append(type)
                .toString();
    }

    /**
     * Given the name of an RSP, return its reverse RSP name.
     *
     * @param rspName       the RSP name
     * @return              the reverse RSP name
     */
    protected static RspName getReverseRspName(RspName rspName) {
        return rspName.getValue().endsWith("-Reverse") ?
                new RspName(rspName.getValue().replaceFirst("-Reverse", "")) :
                new RspName(rspName.getValue() + "-Reverse");
    }

    /**
     * Get the name of the interface we want to classify.
     *
     * @param theClassifier the classifier from which we want the InterfaceName
     * @return              the InterfaceName as a String, if present
     */
    private static Optional<String> getInterfaceNameFromClassifier(SclServiceFunctionForwarder theClassifier) {
        return Optional.ofNullable(theClassifier)
                .filter(classifier -> classifier.getAttachmentPointType() instanceof Interface)
                .map(classifier -> (Interface) classifier.getAttachmentPointType())
                .map(Interface::getInterface);
    }

    protected static FlowDetails buildFlowEncapsulation(String nodeName, String flowKey, short tableID) {
        return new FlowDetails(nodeName, new FlowKey(new FlowId(flowKey)), new TableKey(tableID));
    }

    protected static FlowDetails buildFlowEncapsulation(String nodeName, FlowBuilder flow) {
        return new FlowDetails(nodeName, flow.getKey(), new TableKey(flow.getTableId()), flow.build());
    }

    private boolean usesLogicalInterfaces(ServiceFunctionForwarder theSff) {
        return theSff.getSffDataPlaneLocator() == null;
    }
}
