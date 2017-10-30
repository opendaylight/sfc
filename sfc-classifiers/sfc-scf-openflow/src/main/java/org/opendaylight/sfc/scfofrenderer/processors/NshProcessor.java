/*
 * Copyright (c) 2016 Hewlett Packard Enterprise Development LP. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.processors;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.ClassifierInterface;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.ClassifierGeniusIntegration;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcNshHeader;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfMatch;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfOfUtils;
import org.opendaylight.sfc.util.openflow.writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NshProcessor implements ClassifierProcessorInterface {

    // true if we're adding a classifier node, false if we're deleting it
    private boolean addClassifier = true;

    private ClassifierInterface classifierInterface;

    private ClassifierHandler classifierHandler;

    private static final Logger LOG = LoggerFactory.getLogger(NshProcessor.class);

    public NshProcessor(ClassifierInterface classifierInterface,
                        ClassifierHandler classifierHandler,
                        boolean addClassifier) {
        this.classifierInterface = classifierInterface;
        this.classifierHandler = classifierHandler;
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

        Optional<Long> inPort = classifierInterface.getInPort(theIfName, nodeName);
        // Build the match object if possible; throw a RuntimeException if the ACE is not correctly provisioned
        Match match = inPort.map(port -> String.format("%s:%s", nodeName, port))
                .map(NodeConnectorId::new)
                .map(connectorId -> new SfcScfMatch().setPortMatch(connectorId))
                .map(scfMatch -> scfMatch.setAclMatch(theAce.getMatches()))
                .orElseThrow(IllegalArgumentException::new)
                .build();


        Optional<SfcNshHeader> nsh = rspName.map(SfcNshHeader::getSfcNshHeader);

        if (!nsh.isPresent()) {
            LOG.error("processAce: nsh is null; returning empty list");
            return Collections.emptyList();
        }

        String flowKey = classifierHandler.buildFlowKeyName(theScfName, aclName, ruleName, ".out");

        // add a classifier
        if (addClassifier) {
            // write the flows into the classifier
            LOG.info("processAce - About to create flows");
            theFlows.add(classifierInterface.initClassifierTable(nodeName));
            theFlows.add(classifierInterface.createClassifierOutFlow(flowKey, match, nsh.get(), nodeName));
            theFlows.addAll(classifierInterface.createDpdkFlows(nodeName, nsh.get().getNshNsp()));
        } else {
            LOG.info("processAce - About to delete the *out* flows");
            theFlows.add(classifierHandler.deleteFlowFromTable(nodeName, flowKey,
                    classifierHandler.usesLogicalInterfaces(theSff)
                            ? ClassifierGeniusIntegration.getClassifierTable() :
                              SfcScfOfUtils.getClassifierTable()));
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
     * @param theRspName
     *            the RSP from which we want to derive the reverse RSP
     * @param theScfName
     *            the name of the classifier who will process this reverse RSP
     *            traffic
     * @param theAclName
     *            the name of the ACL
     * @param theNodeName
     *            the compute node name where we will install the classifier
     *            flows for the reverse RSP
     * @param theRuleName
     *            the name of the ACE
     * @param port
     *            the output port of the classifier node
     * @param theSff
     *            the SFF to which the classifier is connected
     * @return a List of {@link FlowDetails} having all the generated flows,
     *         which will be later installed
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
        SfcNshHeader reverseNsh = SfcNshHeader.getSfcNshHeader(reverseRspName);

        if (reverseNsh == null) {
            LOG.warn("processReverseRsp: reverseNsh is null");
            return Collections.emptyList();
        }

        String flowKey = classifierHandler.buildFlowKeyName(theScfName, theAclName, theRuleName, ".in");

        if (addClassifier) {
            Optional.ofNullable(classifierInterface.createClassifierInFlow(flowKey, reverseNsh, port, theNodeName))
                    .ifPresent(theFlows::add);
        } else {
            FlowDetails deleteRelayFlow =
                    classifierHandler.deleteFlowFromTable(theNodeName, flowKey,
                            classifierHandler.usesLogicalInterfaces(theSff)
                                    ? ClassifierGeniusIntegration.getClassifierTable() :
                                      SfcScfOfUtils.getClassifierTable());
            theFlows.add(deleteRelayFlow);
        }

        Optional<String> lastNodeName = Optional.ofNullable(reverseNsh.getSffName())
                .filter(sffName -> !sffName.equals(theSff.getName()))
                .map(SfcProviderServiceForwarderAPI::readServiceFunctionForwarder)
                .map(SfcOvsUtil::getOpenFlowNodeIdForSff);

        if (!lastNodeName.isPresent()) {
            return theFlows;
        }

        processReverseRspRelayFlow(lastNodeName.get(), theSff, reverseNsh, flowKey)
                .ifPresent(theFlows::add);

        return theFlows;
    }

     /**
     * Return a FlowDetails object that represent the relay flow - i.e. how to
     * exit the chain - if any.
     *
     * @param nodeName
     *            the nodeName where the flow will be installed. Should be on
     *            the first SFF of the chain - last of the reverse chain.
     * @param theSff
     *            the SFF name where the flow will be installed
     * @param reverseNsh
     *            the {@link SfcNshHeader} object having the related data for
     *            the reverse chain
     * @param theFlowKey
     *            the name of the analogous 'in' flow
     * @return a {@link FlowDetails} object if possible, and empty Optional
     *         otherwise
     */
    protected Optional<FlowDetails> processReverseRspRelayFlow(String nodeName, ServiceFunctionForwarder theSff,
                                                               SfcNshHeader reverseNsh, String theFlowKey) {
        Optional<FlowDetails> relayFlow;
        String flowKey = theFlowKey.replaceFirst(".in", ".relay");
        if (addClassifier) {
            Ip ip = SfcOvsUtil.getSffVxlanDataLocator(theSff);
            if (ip == null || ip.getIp() == null || ip.getPort() == null) {
                return Optional.empty();
            }

            relayFlow = Optional.of(reverseNsh)
                    .map(theReverseNsh -> theReverseNsh.setVxlanIpDst(ip.getIp().getIpv4Address())
                            .setVxlanUdpPort(ip.getPort()))
                    .map(theReverseNshHeader -> classifierInterface.createClassifierRelayFlow(flowKey,
                            theReverseNshHeader, nodeName));
        } else {
            relayFlow = Optional.of(classifierHandler.deleteFlowFromTable(nodeName, flowKey,
                    classifierHandler.usesLogicalInterfaces(theSff)
                            ? ClassifierGeniusIntegration.getClassifierTable() :
                              SfcScfOfUtils.getClassifierTable()));
        }
        return relayFlow;
    }


}


