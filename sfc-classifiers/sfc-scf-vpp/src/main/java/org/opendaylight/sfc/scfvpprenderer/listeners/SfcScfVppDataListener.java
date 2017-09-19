/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.sfc.scfvpprenderer.listeners;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
import org.opendaylight.sfc.scfvpprenderer.processors.VppClassifierProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SfcScfVppDataListener class for listening ServiceFunctionClassifier.
 *
 * @author Yi Yang (yi.y.yang@intel.com)
 */

public class SfcScfVppDataListener extends AbstractDataTreeChangeListener<ServiceFunctionClassifier> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfVppDataListener.class);

    private final VppClassifierProcessor classifierProcessor;

    private final DataBroker dataBroker;
    private ListenerRegistration<SfcScfVppDataListener> listenerRegistration;

    public SfcScfVppDataListener(final DataBroker dataBroker, VppClassifierProcessor classifierProcessor) {
        this.dataBroker = dataBroker;
        this.classifierProcessor = classifierProcessor;
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
            LOG.debug("Add ServiceFunctionClassifier name: {}\n",
                    serviceFunctionClassifier.getName());
            this.classifierProcessor.addScf(serviceFunctionClassifier);

        }
    }

    // Classifier UPDATE
    @Override
    protected void update(ServiceFunctionClassifier originalServiceFunctionClassifier,
                          ServiceFunctionClassifier updatedServiceFunctionClassifier) {

        if (originalServiceFunctionClassifier.getName() != null && updatedServiceFunctionClassifier.getName() != null
                && !originalServiceFunctionClassifier.equals(updatedServiceFunctionClassifier)) {
            LOG.debug("Updated ServiceFunctionClassifier name: {}\n", updatedServiceFunctionClassifier.getName());
            this.classifierProcessor.removeScf(originalServiceFunctionClassifier);
            this.classifierProcessor.addScf(updatedServiceFunctionClassifier);
        }
    }

    // Classifier DELETION
    @Override
    protected void remove(ServiceFunctionClassifier serviceFunctionClassifier) {
        if (serviceFunctionClassifier != null) {
            LOG.debug("Deleted ServiceFunctionClassifier name: {}\n", serviceFunctionClassifier.getName());
            this.classifierProcessor.removeScf(serviceFunctionClassifier);
        }
    }

    @Override
    public void close() {
        LOG.debug("Closing listener...");
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }
}
