/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.sfc.scfvpprenderer.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.scfvpprenderer.processors.VppClassifierProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SfcScfVppDataListener class for listening ServiceFunctionClassifier.
 *
 * @author Yi Yang (yi.y.yang@intel.com)
 */
@Singleton
public class SfcScfVppDataListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionClassifier> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfVppDataListener.class);

    private final VppClassifierProcessor classifierProcessor;

    @Inject
    public SfcScfVppDataListener(final DataBroker dataBroker, VppClassifierProcessor classifierProcessor) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ServiceFunctionClassifiers.class).child(ServiceFunctionClassifier.class));
        this.classifierProcessor = classifierProcessor;
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<ServiceFunctionClassifier> instanceIdentifier,
                    @Nonnull ServiceFunctionClassifier serviceFunctionClassifier) {
        LOG.debug("Add ServiceFunctionClassifier name: {}\n", serviceFunctionClassifier.getName());
        this.classifierProcessor.addScf(serviceFunctionClassifier);
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<ServiceFunctionClassifier> instanceIdentifier,
                       @Nonnull ServiceFunctionClassifier originalServiceFunctionClassifier,
                       @Nonnull ServiceFunctionClassifier updatedServiceFunctionClassifier) {
        if (originalServiceFunctionClassifier.getName() != null && updatedServiceFunctionClassifier.getName() != null
                && !originalServiceFunctionClassifier.equals(updatedServiceFunctionClassifier)) {
            LOG.debug("Updated ServiceFunctionClassifier name: {}", updatedServiceFunctionClassifier.getName());
            this.classifierProcessor.removeScf(originalServiceFunctionClassifier);
            this.classifierProcessor.addScf(updatedServiceFunctionClassifier);
        }
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<ServiceFunctionClassifier> instanceIdentifier,
                       @Nonnull ServiceFunctionClassifier serviceFunctionClassifier) {
        LOG.debug("Deleted ServiceFunctionClassifier name: {}", serviceFunctionClassifier.getName());
        this.classifierProcessor.removeScf(serviceFunctionClassifier);
    }
}
