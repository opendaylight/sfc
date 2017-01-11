/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.sfc.scfofrenderer.listeners;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
import org.opendaylight.sfc.scfofrenderer.processors.SfcScfOfProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test Suite to test the SfcScfOfDataListener class.
 *
 * @author Ursicio Martin (ursicio.javier.martin@ericsson.com)
 */

public class SfcScfOfDataListener extends AbstractDataTreeChangeListener<ServiceFunctionClassifier> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfDataListener.class);

    private SfcScfOfProcessor sfcScfProcessor;

    private final DataBroker dataBroker;
    private ListenerRegistration<SfcScfOfDataListener> listenerRegistration;

    public SfcScfOfDataListener(final DataBroker dataBroker, SfcScfOfProcessor sfcScfProcessor) {
        this.dataBroker = dataBroker;
        this.sfcScfProcessor = sfcScfProcessor;
        init();
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


    // Classifier CREATION
    @Override
    protected void add(ServiceFunctionClassifier serviceFunctionClassifier) {
        if (serviceFunctionClassifier != null) {
            LOG.debug("Created ServiceFunctionClassifier name: {}\n",
                    serviceFunctionClassifier.getName());
            this.sfcScfProcessor.createdServiceFunctionClassifier(serviceFunctionClassifier);

        }
    }

    // Classifier UPDATE
    @Override
    protected void update(ServiceFunctionClassifier originalServiceFunctionClassifier,
                          ServiceFunctionClassifier updatedServiceFunctionClassifier) {

        if (originalServiceFunctionClassifier.getName() != null && updatedServiceFunctionClassifier.getName() != null
                && !(originalServiceFunctionClassifier.equals(updatedServiceFunctionClassifier))) {
            LOG.debug("Updated ServiceFunctionClassifier name: {}\n", updatedServiceFunctionClassifier.getName());
            this.sfcScfProcessor.deletedServiceFunctionClassifier(originalServiceFunctionClassifier);
            this.sfcScfProcessor.createdServiceFunctionClassifier(updatedServiceFunctionClassifier);
        }
    }

    // Classifier DELETION
    @Override
    protected void remove(ServiceFunctionClassifier serviceFunctionClassifier){
        if (serviceFunctionClassifier != null) {
            LOG.debug("Deleted ServiceFunctionClassifier name: {}\n", serviceFunctionClassifier.getName());
            this.sfcScfProcessor.deletedServiceFunctionClassifier(serviceFunctionClassifier);
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
