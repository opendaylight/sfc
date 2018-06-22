/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.listeners;

import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.scfofrenderer.processors.SfcScfOfProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes in Service Function Classifier.
 *
 * @author Ursicio Martin (ursicio.javier.martin@ericsson.com)
 */
public class SfcScfOfDataListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionClassifier> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfDataListener.class);

    private final SfcScfOfProcessor sfcScfProcessor;

    public SfcScfOfDataListener(DataBroker dataBroker, SfcScfOfProcessor sfcScfProcessor) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ServiceFunctionClassifiers.class).child(ServiceFunctionClassifier.class));
        this.sfcScfProcessor = sfcScfProcessor;
    }

    @Override
    public void add(@Nonnull ServiceFunctionClassifier serviceFunctionClassifier) {
        LOG.debug("Created ServiceFunctionClassifier name: {}\n", serviceFunctionClassifier.getName());
        this.sfcScfProcessor.createdServiceFunctionClassifier(serviceFunctionClassifier);
    }

    @Override
    public void update(@Nonnull ServiceFunctionClassifier originalServiceFunctionClassifier,
                       ServiceFunctionClassifier updatedServiceFunctionClassifier) {

        if (originalServiceFunctionClassifier.getName() != null && updatedServiceFunctionClassifier.getName() != null
                && !originalServiceFunctionClassifier.equals(updatedServiceFunctionClassifier)) {
            LOG.debug("Updated ServiceFunctionClassifier name: {}\n", updatedServiceFunctionClassifier.getName());
            this.sfcScfProcessor.deletedServiceFunctionClassifier(originalServiceFunctionClassifier);
            this.sfcScfProcessor.createdServiceFunctionClassifier(updatedServiceFunctionClassifier);
        }
    }

    @Override
    public void remove(@Nonnull ServiceFunctionClassifier serviceFunctionClassifier) {
        LOG.debug("Deleted ServiceFunctionClassifier name: {}\n", serviceFunctionClassifier.getName());
        this.sfcScfProcessor.deletedServiceFunctionClassifier(serviceFunctionClassifier);
    }
}
