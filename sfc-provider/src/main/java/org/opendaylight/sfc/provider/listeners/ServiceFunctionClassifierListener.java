/*
 * Copyright (c) 2016 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service Function
 * Classifiers taking the appropriate actions.
 *
 * @author David Suárez (david.suarez.fuentes@ericsson.com)
 *
 */
public class ServiceFunctionClassifierListener
        extends AbstractDataTreeChangeListener<ServiceFunctionClassifier> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionClassifierListener.class);

    private final DataBroker dataBroker;
    private ListenerRegistration<ServiceFunctionClassifierListener> listenerRegistration;

    public ServiceFunctionClassifierListener(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        LOG.debug("Initializing...");
        registerListeners();
    }

    private void registerListeners() {
        final DataTreeIdentifier<ServiceFunctionClassifier> treeId = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(ServiceFunctionClassifiers.class).child(ServiceFunctionClassifier.class));
        listenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    protected void add(ServiceFunctionClassifier serviceFunctionClassifier) {
        if (serviceFunctionClassifier != null) {
            LOG.debug("Adding Service Function Classifier: {}", serviceFunctionClassifier.getName());

            // TODO: literally copied from the existing code. It should be
            // optimized.
            if (serviceFunctionClassifier.getName() != null && serviceFunctionClassifier.getAcl() != null) {
                // call executor to write <ACL, Classifier> entry into ACL
                // operational store
                SfcProviderAclAPI.addClassifierToAccessListState(serviceFunctionClassifier.getAcl().getName(),
                        serviceFunctionClassifier.getAcl().getType(), serviceFunctionClassifier.getName());
            }
        }
    }

    @Override
    protected void remove(ServiceFunctionClassifier serviceFunctionClassifier) {
        if (serviceFunctionClassifier != null && serviceFunctionClassifier.getAcl() != null) {
            LOG.debug("Removing Service Function Classifier: {}", serviceFunctionClassifier.getName());
            // TODO: literally copied from the existing code. It should be
            // optimized
            if (serviceFunctionClassifier.getName() != null) {
                // call executor to delete <ACL, Classifier> entry from ACL
                // operational store
                SfcProviderAclAPI.deleteClassifierFromAccessListState(serviceFunctionClassifier.getAcl().getName(),
                        serviceFunctionClassifier.getAcl().getType(), serviceFunctionClassifier.getName());
            }
        }
    }

    @Override
    protected void update(ServiceFunctionClassifier originalServiceFunctionClassifier,
            ServiceFunctionClassifier updatedServiceFunctionClassifier) {
        // TODO: literally copied from the existing code. It should be
        // optimized
        if ( originalServiceFunctionClassifier != null && originalServiceFunctionClassifier.getAcl() != null
                && updatedServiceFunctionClassifier.getAcl() != null
                && !originalServiceFunctionClassifier.getAcl().equals(updatedServiceFunctionClassifier.getAcl())) {

            if (updatedServiceFunctionClassifier.getAcl() != null) {
                // call executor to write <ACL, Classifier> entry into ACL
                // operational store
                SfcProviderAclAPI.addClassifierToAccessListState(updatedServiceFunctionClassifier.getAcl().getName(),
                        updatedServiceFunctionClassifier.getAcl().getType(),
                        updatedServiceFunctionClassifier.getName());
            }
            // Remove old  <ACL, Classifier> entry from ACL
            SfcProviderAclAPI.deleteClassifierFromAccessListState(
                    originalServiceFunctionClassifier.getAcl().getName(),
                    originalServiceFunctionClassifier.getAcl().getType(),
                    originalServiceFunctionClassifier.getName());
        }
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }
}
