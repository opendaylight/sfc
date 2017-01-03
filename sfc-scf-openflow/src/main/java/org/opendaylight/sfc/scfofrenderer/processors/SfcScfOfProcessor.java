/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.processors;


import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcScfOfProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfProcessor.class);
    private SfcOfFlowWriterInterface openflowWriter;
    private OpenflowClassifierProcessor classifierProcessor;
    private ClassifierHandler classifierHandler;

    public SfcScfOfProcessor(SfcOfFlowWriterInterface theOpenflowWriter,
                             OpenflowClassifierProcessor theClassifierProcessor) {
        openflowWriter = theOpenflowWriter;
        classifierProcessor = theClassifierProcessor;
        classifierHandler = new ClassifierHandler();
    }

    /**
    * create flows for service function classifier
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    *
    * @param  scf service function classifier
    * @return          create result
    */
    public boolean createdServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        Optional<Acl> theAcl = classifierHandler.extractAcl(scf);
        if ( !theAcl.isPresent() || !validateInputs(theAcl.get()) ) {
            LOG.error("createdServiceFunctionClassifier: Could not retrieve the ACL from the classifier: {}", scf);
            return false;
        }

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("deletedServiceFunctionClassifier: sfflist is null");
            return false;
        }

        openflowWriter.writeFlows(classifierProcessor.processClassifierList(theAcl.get(), true, sfflist));
        openflowWriter.flushFlows();
        return true;
    }

   /**
    * delete flows for service function classifier
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    *
    * @param  scf service function classifier
    * @return     delete result
    */
    public boolean deletedServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        Optional<Acl> theAcl = classifierHandler.extractAcl(scf);

        if ( !theAcl.isPresent() || !validateInputs(theAcl.get()) ) {
            LOG.error("createdServiceFunctionClassifier: Could not retrieve the ACL from the classifier: {}", scf);
            return false;
        }

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("deletedServiceFunctionClassifier: sfflist is null");
            return false;
        }

        openflowWriter.removeFlows(classifierProcessor.processClassifierList(theAcl.get(), false, sfflist));
        // so that we delete the initialization flows from SFFs that do not belong to any RSPs
        openflowWriter.clearSffsIfNoRspExists();
        openflowWriter.deleteFlowSet();
        return true;
    }

    /**
     * Check if the supplied ACL is valid.
     * @param theAcl    the provisioned ACL
     * @return          true if the ACL is valid, false otherwise
     */
    private boolean validateInputs(Acl theAcl) {
        String aclName = theAcl.getAclName();
        if (aclName == null) {
            LOG.error("deletedServiceFunctionClassifier: aclName is null");
            return false;
        }

        List<Ace> theAces = Optional.ofNullable(theAcl.getAccessListEntries())
                .map(AccessListEntries::getAce)
                .orElse(Collections.emptyList());

        if (theAces.isEmpty()) {
            LOG.error("deletedServiceFunctionClassifier: acesList is null");
            return false;
        }

        return true;
    }
}
