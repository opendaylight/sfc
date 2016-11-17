/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;


import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterImpl;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SfcScfOfProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfProcessor.class);
    private static SfcOfFlowWriterInterface openflowWriter;
    private OpenflowClassifierProcessor classifierProcessor;
    private WriteTransaction tx;
    private DataBroker dataBroker;
    private RpcProviderRegistry rpcProvider;

    public SfcScfOfProcessor(DataBroker theDataBroker, RpcProviderRegistry theRpcProvider) {
        tx = theDataBroker.newReadWriteTransaction();
        initProcessor(tx, theDataBroker, theRpcProvider);
    }

    public SfcScfOfProcessor(DataBroker theDataBroker,
                             RpcProviderRegistry theRpcProvider,
                             SfcOfFlowWriterInterface theOpenflowWriter) {
        dataBroker = theDataBroker;
        rpcProvider = theRpcProvider;
        tx = dataBroker.newReadWriteTransaction();
        openflowWriter = theOpenflowWriter;
        classifierProcessor = new OpenflowClassifierProcessor(tx, theRpcProvider);
    }

    public SfcScfOfProcessor(DataBroker theDataBroker,
                             RpcProviderRegistry theRpcProvider,
                             SfcOfFlowWriterInterface theOpenflowWriter,
                             OpenflowClassifierProcessor theClassifierProcessor) {
        dataBroker = theDataBroker;
        rpcProvider = theRpcProvider;
        tx = dataBroker.newReadWriteTransaction();
        openflowWriter = theOpenflowWriter;
        classifierProcessor = theClassifierProcessor;
    }

    private void initProcessor(WriteTransaction theTx, DataBroker theDataBroker, RpcProviderRegistry theRpcProvider) {
        openflowWriter = new SfcOfFlowWriterImpl(theTx);
        classifierProcessor = new OpenflowClassifierProcessor(theTx, theRpcProvider);
        dataBroker = theDataBroker;
        rpcProvider = theRpcProvider;
    }

   /**
    * create flows for service function classifier
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    *
    * @param  scf service function classifier
    * @return          create result
    */
    protected boolean createdServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        Optional<Acl> theAcl = Optional.ofNullable(scf)
                .map(ServiceFunctionClassifier::getAcl)
                .map(acl -> SfcProviderAclAPI.readAccessList(acl.getName(), acl.getType()));

        if ( !theAcl.isPresent() || !validateInputs(theAcl.get()) ) {
            LOG.error("createdServiceFunctionClassifier: Could not retrieve the ACL from the classifier: {}", scf);
            return false;
        }

        LOG.debug("createdServiceFunctionClassifier: ServiceFunctionClassifier name: {} ACL: {} SFF: {}",
                scf.getName(),
                scf.getAcl(),
                scf.getSclServiceFunctionForwarder());

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("createdServiceFunctionClassifier: sfflist is null");
            return false;
        }

        for (SclServiceFunctionForwarder classifier : sfflist) {
            List<FlowDetails> allFlows = classifierProcessor.processClassifier(classifier, theAcl.get(), true);
            LOG.debug("createdServiceFunctionClassifier - flow size: {}", allFlows.size());
            // if no flows were built to be written, we refresh the transaction,
            // thus discarding the genius interface binding
            if(allFlows.isEmpty()) {
                LOG.info("createdServiceFunctionClassifier - Could not successfully process classifier.");
                refreshTransaction();
                continue;
            }
            openflowWriter.writeFlows(allFlows);
        }

        openflowWriter.flushFlows();
        refreshTransaction();
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
    protected boolean deletedServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        Optional<Acl> theAcl = Optional.ofNullable(scf)
                .map(ServiceFunctionClassifier::getAcl)
                .map(acl -> SfcProviderAclAPI.readAccessList(acl.getName(), acl.getType()));

        if ( !theAcl.isPresent() || !validateInputs(theAcl.get()) ) {
            LOG.error("deletedServiceFunctionClassifier: Could not retrieve the ACL from the classifier: {}", scf);
            return false;
        }

        LOG.debug("deletedServiceFunctionClassifier: delete ServiceFunctionClassifier name: {} ACL: {} SFF: {}",
                scf.getName(),
                scf.getAcl(),
                scf.getSclServiceFunctionForwarder());

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("deletedServiceFunctionClassifier: sfflist is null");
            return false;
        }

        for (SclServiceFunctionForwarder classifier : sfflist) {
            List<FlowDetails> allFlows = classifierProcessor.processClassifier(classifier, theAcl.get(), false);
            // if no flows were built to be written, we refresh the transaction,
            // thus discarding the genius interface binding
            if(allFlows.isEmpty()) {
                LOG.info("deletedServiceFunctionClassifier - Could not successfully delete classifier.");
                refreshTransaction();
                continue;
            }
            openflowWriter.removeFlows(allFlows);
        }
        openflowWriter.deleteFlowSet();
        refreshTransaction();
        return true;
    }

    /**
     * Refresh the current datastore transaction, and use the same transaction object within the openflow writer.
     */
    private void refreshTransaction() {
        if (tx != null) {
            tx = dataBroker.newReadWriteTransaction();
            initProcessor(tx, dataBroker, rpcProvider);
        }
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
