/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestAclTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SbRestScfEntryDataListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionClassifier> {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestScfEntryDataListener.class);

    private final ExecutorService executorService;

    @Inject
    public SbRestScfEntryDataListener(DataBroker dataBroker, ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, SfcInstanceIdentifiers.SCF_ENTRY_IID);
        this.executorService = executorService;
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<ServiceFunctionClassifier> instanceIdentifier,
                    @Nonnull ServiceFunctionClassifier serviceFunctionClassifier) {
        update(instanceIdentifier, serviceFunctionClassifier, serviceFunctionClassifier);
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<ServiceFunctionClassifier> instanceIdentifier,
                       @Nonnull ServiceFunctionClassifier serviceFunctionClassifier) {
        LOG.debug("Deleted Service Classifier Name: {}", serviceFunctionClassifier.getName());

        if (serviceFunctionClassifier.getAcl() != null) {
            new SbRestAclTask(RestOperation.DELETE, serviceFunctionClassifier.getAcl().getName(),
                              serviceFunctionClassifier.getAcl().getType(),
                              serviceFunctionClassifier.getSclServiceFunctionForwarder(), executorService).run();
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<ServiceFunctionClassifier> instanceIdentifier,
                       @Nonnull ServiceFunctionClassifier originalDataObject,
                       @Nonnull ServiceFunctionClassifier updatedServiceFunctionClassifier) {
        LOG.debug("Updated Service Classifier Name: {}", updatedServiceFunctionClassifier.getName());

        if (updatedServiceFunctionClassifier.getAcl() != null) {
            Acl accessList = SfcProviderAclAPI.readAccessList(updatedServiceFunctionClassifier.getAcl().getName(),
                                                              updatedServiceFunctionClassifier.getAcl().getType());
            new SbRestAclTask(RestOperation.PUT, accessList,
                              updatedServiceFunctionClassifier.getSclServiceFunctionForwarder(), executorService).run();
        }
    }
}
