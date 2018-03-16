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
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SbRestSfEntryDataListener extends AbstractSyncDataTreeChangeListener<ServiceFunction> {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfEntryDataListener.class);

    private final ExecutorService executorService;

    @Inject
    public SbRestSfEntryDataListener(DataBroker dataBroker, ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, SfcInstanceIdentifiers.SF_ENTRY_IID);
        this.executorService = executorService;
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<ServiceFunction> instanceIdentifier,
                    @Nonnull ServiceFunction serviceFunction) {
        update(instanceIdentifier, serviceFunction, serviceFunction);
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<ServiceFunction> instanceIdentifier,
                       @Nonnull ServiceFunction serviceFunction) {
        LOG.debug("Deleted Service Function Name: {}", serviceFunction.getName());
        new SbRestSfTask(RestOperation.DELETE, serviceFunction, executorService).run();
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<ServiceFunction> instanceIdentifier,
                       @Nonnull ServiceFunction originalDataObject, @Nonnull ServiceFunction updatedServiceFunction) {
        LOG.debug("Updated Service Function Name: {}", updatedServiceFunction.getName());
        new SbRestSfTask(RestOperation.PUT, updatedServiceFunction, executorService).run();
    }
}
