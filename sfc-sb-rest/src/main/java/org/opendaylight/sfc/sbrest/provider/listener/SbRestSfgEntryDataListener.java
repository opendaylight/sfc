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
import org.opendaylight.genius.datastoreutils.listeners.AbstractAsyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfgTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SbRestSfgEntryDataListener extends AbstractAsyncDataTreeChangeListener<ServiceFunctionGroup> {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfgEntryDataListener.class);

    @Inject
    public SbRestSfgEntryDataListener(DataBroker dataBroker, ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, SfcInstanceIdentifiers.SFG_ENTRY_IID, executorService);
    }

    @Override
    public void add(@Nonnull ServiceFunctionGroup serviceFunctionGroup) {
        update(serviceFunctionGroup, serviceFunctionGroup);
    }

    @Override
    public void remove(@Nonnull ServiceFunctionGroup serviceFunctionGroup) {
        LOG.debug("Deleted Service Function Name: {}", serviceFunctionGroup.getName());
        new SbRestSfgTask(RestOperation.DELETE, serviceFunctionGroup, getExecutorService()).run();
    }

    @Override
    public void update(@Nonnull ServiceFunctionGroup originalServiceFunctionGroup,
                       ServiceFunctionGroup updatedServiceFunctionGroup) {
        LOG.debug("Modified Service Function Name: {}", updatedServiceFunctionGroup.getName());
        new SbRestSfgTask(RestOperation.PUT, updatedServiceFunctionGroup, getExecutorService()).run();
    }
}
