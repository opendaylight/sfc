/*
 * Copyright (c) 2016 Hewlett Packard Enterprise Development LP. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.scfofrenderer.processors;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.MacChainingClassifier;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.ClassifierGeniusIntegration;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfMatch;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MacChainingProcessor implements ClassifierProcessorInterface {

    // true if we're adding a classifier node, false if we're deleting it
    private boolean addClassifier = true;

    private MacChainingClassifier macChainingClassifier;

    private ClassifierHandler classifierHandler;

    private static final Logger LOG = LoggerFactory.getLogger(MacChainingProcessor.class);

    public MacChainingProcessor(ClassifierHandler classifierHandler,
                                MacChainingClassifier macChainingClassifier,
                                boolean addClassifier) {
        this.classifierHandler = classifierHandler;
        this.macChainingClassifier = macChainingClassifier;
        this.addClassifier = addClassifier;
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
    public List<FlowDetails> processAceByProcessor(String nodeName, ServiceFunctionForwarder theSff, String theScfName,
                                                   String aclName, String theIfName, Ace theAce, Optional<RspName> rspName) {

        List<FlowDetails> theFlows = new ArrayList<>();

        String ruleName = theAce.getRuleName();
        if (Strings.isNullOrEmpty(ruleName)) {
            LOG.error("processAce - ruleName is null; returning empty list");
            return Collections.emptyList();
        }

        LOG.info("processAce - NodeName: {}; IF name: {}", nodeName, theIfName);


        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName.get());


        Optional<Long> inPort = macChainingClassifier.getInPort(theIfName, nodeName);
        // Build the match object if possible; throw a RuntimeException if the ACE is not correctly provisioned
        Match match = inPort.map(port -> String.format("%s:%s", nodeName, port))
                .map(NodeConnectorId::new)
                .map(connectorId -> new SfcScfMatch().setPortMatch(connectorId))
                .map(scfMatch -> scfMatch.setAclMatch(theAce.getMatches()))
                .orElseThrow(IllegalArgumentException::new)
                .build();

        String flowKey = classifierHandler.buildFlowKeyName(theScfName, aclName, ruleName, ".out");

        // add a classifier
        if (addClassifier) {
            // write the flows into the classifier
            LOG.info("processAce - About to create flows");
            theFlows.add(macChainingClassifier.initClassifierTable(nodeName));
            theFlows.add(macChainingClassifier.createClassifierOutFlow(flowKey, match, rspName.get(), nodeName));
        }
        else
        {
            LOG.info("processAce - About to delete the *out* flows");
            theFlows.add(classifierHandler.deleteFlowFromTable(nodeName,
                    flowKey,
                    ClassifierGeniusIntegration.getClassifierTable()));
        }

        // when the classifier is attached to a logical SFF, there's no need to process the reverse RSP, so we bail
        if (classifierHandler.usesLogicalInterfaces(theSff)) {
            return theFlows;
        }

        List<FlowDetails> theReverseRspFlows = processReverseRsp(rspName.get(),
                theScfName,
                aclName,
                nodeName,
                theAce.getRuleName(),
                inPort.get(),
                theSff);

        theFlows.addAll(theReverseRspFlows);

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

        RspName reverseRspName = SfcProviderRenderedPathAPI.generateReversedPathName(theRspName);

        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(theRspName);
        RenderedServicePathHop lastRspHop = Iterables.getLast(rsp.getRenderedServicePathHop());
        SffName reverseSff = lastRspHop.getServiceFunctionForwarder();


        String flowKey = classifierHandler.buildFlowKeyName(theScfName, theAclName, theRuleName, ".in");

        if (addClassifier) {
            Optional.ofNullable(macChainingClassifier.createClassifierInFlow(flowKey, reverseRspName, port, theNodeName))
                    .ifPresent(theFlows::add);
        }
        else {
            FlowDetails deleteRelayFlow =
                    classifierHandler.deleteFlowFromTable(theNodeName,
                            flowKey,
                            ClassifierGeniusIntegration.getClassifierTable());
            theFlows.add(deleteRelayFlow);
        }

        Optional<String> lastNodeName = Optional.ofNullable(reverseSff)
                .filter(sffName -> !sffName.equals(theSff.getName()))
                .map(SfcProviderServiceForwarderAPI::readServiceFunctionForwarder)
                .map(SfcOvsUtil::getOpenFlowNodeIdForSff);

        if (!lastNodeName.isPresent()) {
            return theFlows;
        }

        processReverseRspRelayFlow(lastNodeName.get(), theScfName, reverseRspName, flowKey)
                .ifPresent(theFlows::add);

        return theFlows;
    }

    /**

     * Return a FlowDetails object that represent the relay flow - i.e. how to exit the chain - if any.
     * @param nodeName          the nodeName where the flow will be installed. Should be on the first SFF of the
     *                          chain - last of the reverse chain.
     * @param classifierName    the classifier name
     * @param reverseRspName    the reverse RSP name
     * @return              a {@link FlowDetails} object if possible, and empty Optional otherwise
     */
    protected Optional<FlowDetails> processReverseRspRelayFlow(String nodeName,
                                                               String classifierName,
                                                               RspName reverseRspName,
                                                               String theFlowKey) {

        Optional<FlowDetails> relayFlow;
        String flowKey = theFlowKey.replaceFirst(".in", ".relay");
        if (addClassifier) {

            relayFlow = Optional.of(macChainingClassifier.createClassifierRelayFlow(flowKey, reverseRspName, nodeName, classifierName));
        }
        else {
            relayFlow = Optional.of(classifierHandler.deleteFlowFromTable(nodeName,
                    flowKey,
                    ClassifierGeniusIntegration.getClassifierTable()));
        }
        return relayFlow;
    }

}
