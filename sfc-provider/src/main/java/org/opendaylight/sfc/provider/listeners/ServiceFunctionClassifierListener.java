/*
 * Copyright (c) 2016, 2017 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service Function
 * Classifiers taking the appropriate actions.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
@Singleton
public class ServiceFunctionClassifierListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionClassifier> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionClassifierListener.class);

    @Inject
    public ServiceFunctionClassifierListener(DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ServiceFunctionClassifiers.class).child(ServiceFunctionClassifier.class));
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<ServiceFunctionClassifier> instanceIdentifier,
                    @Nonnull ServiceFunctionClassifier serviceFunctionClassifier) {
        LOG.debug("Adding Service Function Classifier: {}", serviceFunctionClassifier.getName());

        if (serviceFunctionClassifier.getName() != null && serviceFunctionClassifier.getAcl() != null) {
            // call executor to write <ACL, Classifier> entry into ACL
            // operational store
            SfcProviderAclAPI.addClassifierToAccessListState(serviceFunctionClassifier.getAcl().getName(),
                                                             serviceFunctionClassifier.getAcl().getType(),
                                                             serviceFunctionClassifier.getName());
        }
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<ServiceFunctionClassifier> instanceIdentifier,
                       @Nonnull ServiceFunctionClassifier serviceFunctionClassifier) {
        if (serviceFunctionClassifier.getAcl() != null) {
            LOG.debug("Removing Service Function Classifier: {}", serviceFunctionClassifier.getName());
            if (serviceFunctionClassifier.getName() != null) {
                // call executor to delete <ACL, Classifier> entry from ACL
                // operational store
                SfcProviderAclAPI.deleteClassifierFromAccessListState(serviceFunctionClassifier.getAcl().getName(),
                                                                      serviceFunctionClassifier.getAcl().getType(),
                                                                      serviceFunctionClassifier.getName());
            }
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<ServiceFunctionClassifier> instanceIdentifier,
                       @Nonnull ServiceFunctionClassifier originalServiceFunctionClassifier,
                       @Nonnull ServiceFunctionClassifier updatedServiceFunctionClassifier) {
        if (originalServiceFunctionClassifier.getAcl() != null
                && updatedServiceFunctionClassifier.getAcl() != null && !originalServiceFunctionClassifier.getAcl()
                .equals(updatedServiceFunctionClassifier.getAcl())) {

            if (updatedServiceFunctionClassifier.getAcl() != null) {
                // call executor to write <ACL, Classifier> entry into ACL
                // operational store
                SfcProviderAclAPI.addClassifierToAccessListState(updatedServiceFunctionClassifier.getAcl().getName(),
                                                                 updatedServiceFunctionClassifier.getAcl().getType(),
                                                                 updatedServiceFunctionClassifier.getName());
            }
            // Remove old <ACL, Classifier> entry from ACL
            SfcProviderAclAPI.deleteClassifierFromAccessListState(originalServiceFunctionClassifier.getAcl().getName(),
                                                                  originalServiceFunctionClassifier.getAcl().getType(),
                                                                  originalServiceFunctionClassifier.getName());
        }
    }
}
