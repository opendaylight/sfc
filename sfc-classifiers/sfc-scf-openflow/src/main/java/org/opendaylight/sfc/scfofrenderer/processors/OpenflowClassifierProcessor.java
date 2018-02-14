/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.processors;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.BareClassifier;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.ClassifierInterface;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.LogicallyAttachedClassifier;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.MacChainingClassifier;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.ClassifierGeniusIntegration;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.util.openflow.writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacChaining;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenflowClassifierProcessor {

    private WriteTransaction tx;

    // true if we're adding a classifier node, false if we're deleting it
    private boolean addClassifier = true;

    private ClassifierInterface classifierInterface;

    private BareClassifier bareClassifier;

    private MacChainingClassifier macChainingClassifier;

    private LogicallyAttachedClassifier logicallyAttachedClassifier;

    private ClassifierHandler classifierHandler;

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowClassifierProcessor.class);

    // hide the default constructor
    private OpenflowClassifierProcessor() {
    }

    public OpenflowClassifierProcessor(WriteTransaction theTx, LogicallyAttachedClassifier theLogicClassifier,
            BareClassifier theBareClassifier) {
        tx = theTx;
        classifierInterface = theBareClassifier;
        logicallyAttachedClassifier = theLogicClassifier;
        bareClassifier = theBareClassifier;
        classifierHandler = new ClassifierHandler();
        macChainingClassifier = new MacChainingClassifier();
    }

    /**
     * Process a list of classifier switches objects, adding or removing flows
     * for the entire impacted RSP.
     *
     * @param theAcl
     *            the ACL object to install
     * @param onAddClassifier
     *            true when adding the classifier flows, false when deleting
     *            them
     * @param classifierList
     *            the list of {@link SclServiceFunctionForwarder} in which the
     *            classifier flows will be installed
     * @return the list of all the relevant flows to be installed
     */
    public List<FlowDetails> processClassifierList(Acl theAcl, boolean onAddClassifier,
            List<SclServiceFunctionForwarder> classifierList) {
        return classifierList.stream().map(classifier -> processClassifier(classifier, theAcl, onAddClassifier))
                .peek(theFlows -> LOG.info("createdServiceFunctionClassifier - flow size: {}", theFlows.size()))
                .reduce(new ArrayList<>(), (dstList, theList) -> Stream.concat(dstList.stream(), theList.stream())
                        .collect(Collectors.toList()));
    }

    /**
     * Process an Scf object, adding or removing the OF rules into the
     * respective OVS This method is called on result of classifier addition /
     * removal.
     *
     * @param theClassifier
     *            the classifier node to be added
     * @param theAcl
     *            the ACL we want to install in the classifier
     * @param addClassifierScenario
     *            true when adding the classifier flows, false when deleting
     *            them
     * @return a List of {@link FlowDetails} having all the generated flows,
     *         which will be later installed
     */
    public List<FlowDetails> processClassifier(final SclServiceFunctionForwarder theClassifier, final Acl theAcl,
            final boolean addClassifierScenario) {
        addClassifier = addClassifierScenario;

        Optional<ServiceFunctionForwarder> sff = Optional.of(new SffName(theClassifier.getName()))
                .map(SfcProviderServiceForwarderAPI::readServiceFunctionForwarder);

        Optional<String> itfName = classifierHandler.getInterfaceNameFromClassifier(theClassifier);

        if (!sff.isPresent() || !itfName.isPresent()) {
            LOG.error(
                    "createdServiceFunctionClassifier: "
                            + "Cannot install ACL rules in classifier. SFF exists? {}; Interface exists? {}",
                    sff.isPresent(), itfName.isPresent());
            return Collections.emptyList();
        }

        // bind/unbind the interface in genius, if the classifier is attached to a logical interface
        if (classifierHandler.usesLogicalInterfaces(sff.get())) {
            if (addClassifierScenario) {
                ClassifierGeniusIntegration.performGeniusServiceBinding(tx, itfName.get());
                LOG.info("processClassifier - Bound interface {}", itfName.get());
            } else {
                ClassifierGeniusIntegration.performGeniusServiceUnbinding(tx, itfName.get());
                LOG.info("processClassifier - Unbound interface {}", itfName.get());
            }
        }

        return theAcl.getAccessListEntries().getAce().stream()
                .map(theAce -> processAce(itfName, sff.get(), theClassifier.getName(), theAcl.getAclName(), theAce))
                .reduce(new ArrayList<>(), (dstList, theList) -> Stream.concat(dstList.stream(), theList.stream())
                        .collect(Collectors.toList()));
    }

    /**
     * Install an ACE entry, belonging to the given ACL, on the SFF identified
     * through the specified nodeName. This method is called on result of
     * classifier addition / removal.
     *
     * @param theSff
     *            the SFF to which the classifier is connected
     * @param theScfName
     *            the name of the classifier
     * @param aclName
     *            the name of the ACL
     * @param itfName
     *            the interface we want to classify
     * @param theAce
     *            the ACE
     * @return a List of {@link FlowDetails} having all the generated flows,
     *         which will be later installed
     */
    public List<FlowDetails> processAce(Optional<String> itfName, ServiceFunctionForwarder theSff, String theScfName,
                                        String aclName, Ace theAce) {

        String ruleName = theAce.getRuleName();
        if (Strings.isNullOrEmpty(ruleName)) {
            LOG.error("processAce - ruleName is null; returning empty list");
            return Collections.emptyList();
        }

        Optional<RspName> rspName = Optional.ofNullable(theAce.getActions())
                .map(theActions -> theActions.getAugmentation(Actions1.class))
                .map(actions1 -> (AclRenderedServicePath) actions1.getSfcAction())
                .map(aclRsp -> new RspName(aclRsp.getRenderedServicePath()));

        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName.get());

        final ClassifierProcessorInterface classifierProcessor;

        // choose which handler to use
        if (rsp.getSfcEncapsulation() == MacChaining.class) {
            classifierInterface = macChainingClassifier.setSff(theSff);
            classifierProcessor = new MacChainingProcessor(
                    this.classifierHandler, macChainingClassifier, addClassifier);
        } else {
            classifierInterface = classifierHandler.usesLogicalInterfaces(theSff)
                    ? logicallyAttachedClassifier : bareClassifier.setSff(theSff);
            classifierProcessor = new NshProcessor(this.classifierInterface, this.classifierHandler, addClassifier);
        }

        final Optional<String> nodeName = itfName.flatMap(classifierInterface::getNodeName);
        if (!nodeName.isPresent()) {
            LOG.error("Could not extract the node name from classifier on SFF {}", theSff.getName());
            return Collections.emptyList();
        }

        LOG.info("processAce - NodeName: {}; IF name: {}", nodeName, itfName.get());

        return classifierProcessor.processAceByProcessor(
                nodeName.get(), theSff, theScfName, aclName, itfName.get(), theAce, rspName);
    }

    /**
     * Handler method of the {@link com.google.common.eventbus.EventBus class}.
     *
     * @param theTx
     *            the new transaction being used by the OpenflowWriter to which
     *            this class is subscribed
     */
    @Subscribe
    public void refreshTransaction(WriteTransaction theTx) {
        LOG.debug("refreshTransaction - refreshing the transaction.");
        tx = theTx;
    }
}
