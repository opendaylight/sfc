/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.processors;

import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.ClassifierInterface;
import org.opendaylight.sfc.scfofrenderer.utils.SfcNshHeader;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfMatch;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.LogicallyAttachedClassifier;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
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

public class ClassifierRspUpdateProcessor {

    private ClassifierInterface classifierInterface;

    private static final Logger LOG = LoggerFactory.getLogger(ClassifierRspUpdateProcessor.class);

    private final ClassifierHandler classifierHandler;

    // hide the default constructor
    private ClassifierRspUpdateProcessor() {classifierHandler = new ClassifierHandler();}

    public ClassifierRspUpdateProcessor(LogicallyAttachedClassifier theClassifierInterface) {
        this();
        classifierInterface = theClassifierInterface;
    }

    /**
     * Process adding classifier flows upon a RenderedServicePath update.
     * The old classifier flows were already deleted, so this method only cares about writing the new flows.
     * The RSP is passed as parameter, since within genius the interface state is currently cached, which makes its
     * RPC return the old data-plane ID for the first SF in the RSP.
     * Also, passing the RSP as a parameter enables us to skip that data-store read operation.
     *
     * @param theClassifier the classifier node to be added
     * @param theAcl        the ACL we want to install in the classifier
     * @param theRsp        the {@link RenderedServicePath} object for which we will re-install the classifier flows
     * @return              a List of {@link FlowDetails} having all the generated flows, which will be later installed
     */
    public List<FlowDetails> processClassifier(
            final SclServiceFunctionForwarder theClassifier,
            final Acl theAcl,
            final RenderedServicePath theRsp) {
        LOG.info("processClassifier - Processing the upadate of RSP {}", theRsp.getPathId());

        SffName sffName = new SffName(theClassifier.getName());

        Optional<String> itfName = classifierHandler.getInterfaceNameFromClassifier(theClassifier);

        ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);

        // RSP updates only supported when attached to a logical SFF
        if (!classifierHandler.usesLogicalInterfaces(sff)) {
            LOG.error("processClassifier - Not attached to a logical SFF; Bailing out");
            return Collections.emptyList();
        }

        if (!itfName.isPresent()) {
            LOG.error("processClassifier - Could not extract a LogicalInterface from the classifier's attachment point");
            return Collections.emptyList();
        }

        Optional<String> nodeName = classifierInterface.getNodeName(itfName.get());
        // No need to perform service binding again; we're only concerned w/ the movement of the first SF

        if(!nodeName.isPresent()) {
            LOG.error("processClassifier - Could not extract the node name from the OVS interface");
            return Collections.emptyList();
        }

        Optional<Long> inPort = classifierInterface.getInPort(itfName.get(), nodeName.get());
        if (!inPort.isPresent()) {
            LOG.error("processClassifier - port is null; returning empty list");
            return Collections.emptyList();
        }

        return theAcl
                .getAccessListEntries()
                .getAce()
                .stream()
                .map(theAce -> processAce(theRsp,
                        nodeName.get(),
                        theClassifier.getName(),
                        theAcl.getAclName(),
                        inPort.get(),
                        theAce))
                .reduce(new ArrayList<>(),
                        (dstList, theList) ->
                                Stream.concat(dstList.stream(), theList.stream()).collect(Collectors.toList()));
    }

    /**
     * Install an ACE entry, belonging to the given ACL, on the SFF having the supplied node name.
     * This method is called on RSP updates.
     *
     * @param theRsp        the {@link RenderedServicePath} for which this ACE applies, having the updated DpnIdType.
     * @param nodeName      the node name where the classifier flows will be installed
     * @param theScfName    the provisioned name of the classifier - used to generate the flow key name
     * @param aclName       the name of the provisioned ACL object
     * @param inPort        the in port of the classifier
     * @param theAce        the {@link Ace} object for which we want to install flows
     * @return              a list having all the {@link FlowDetails} that will be installed in the classifier
     */
    private List<FlowDetails> processAce(RenderedServicePath theRsp,
                                         String nodeName,
                                         String theScfName,
                                         String aclName,
                                         long inPort,
                                         Ace theAce) {
        List<FlowDetails> theFlows = new ArrayList<>();

        String ruleName = theAce.getRuleName();
        if (ruleName == null) {
            LOG.error("processAce - ruleName is null; returning empty list");
            return Collections.emptyList();
        }

        NodeConnectorId port = new NodeConnectorId(String.format("%s:%s", nodeName, inPort));

        LOG.debug("processAce: in port: {}", inPort);
        // Match
        Match match = new SfcScfMatch()
                .setPortMatch(port)
                .setAclMatch(theAce.getMatches())
                .build();

        SfcNshHeader nsh = SfcNshHeader.getSfcNshHeader(theRsp);

        if (nsh == null) {
            LOG.error("processAce - nsh is null; returning empty list");
            return Collections.emptyList();
        }

        String flowKey = classifierHandler.buildFlowKeyName(theScfName, aclName, ruleName, ".out");

        // write the flows into the classifier
        LOG.info("processAce - About to create flows");

        theFlows.add(classifierInterface.initClassifierTable(nodeName));
        theFlows.add(classifierInterface.createClassifierOutFlow(flowKey, match, nsh, nodeName));

        // DPDK is not supported for logical SFF, thus, its flows are not installed here
        return theFlows;
    }
}
