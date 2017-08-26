/*
 * Copyright (c) 2015, 2017 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfstateTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestSfstateEntryDataListener extends SbRestAbstractDataListener<ServiceFunctionState> {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfstateEntryDataListener.class);
    protected static ExecutorService executor = Executors.newFixedThreadPool(5);

    public SbRestSfstateEntryDataListener() {
        setInstanceIdentifier(SfcInstanceIdentifiers.SFSTATE_ENTRY_IID);
    }

    public void setDataProvider(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        registerAsDataChangeListener();
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ServiceFunctionState>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<ServiceFunctionState> change: changes) {
            DataObjectModification<ServiceFunctionState> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    ServiceFunctionState updatedServiceFunctionState = rootNode.getDataAfter();
                    LOG.debug("\nUpdated Service Function State Name: {}", updatedServiceFunctionState.getName());

                    executor.submit(new SbRestSfstateTask(RestOperation.PUT, updatedServiceFunctionState, executor));
                    break;
                case DELETE:
                    ServiceFunctionState originalServiceFunctionState = rootNode.getDataBefore();
                    LOG.debug("\nDeleted Service Function State Name: {}", originalServiceFunctionState.getName());

                    executor.submit(new SbRestSfstateTask(RestOperation.DELETE, originalServiceFunctionState,
                            executor));
                    break;
                default:
                    break;
            }
        }

        printTraceStop(LOG);
    }
}
