/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSffTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestSffEntryDataListener extends SbRestAbstractDataListener<ServiceFunctionForwarder> {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSffEntryDataListener.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public SbRestSffEntryDataListener(DataBroker dataBroker) {
        super(dataBroker, SfcInstanceIdentifiers.SFF_ENTRY_IID, LogicalDatastoreType.CONFIGURATION);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ServiceFunctionForwarder>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<ServiceFunctionForwarder> change: changes) {
            DataObjectModification<ServiceFunctionForwarder> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    ServiceFunctionForwarder updatedServiceFunctionForwarder = rootNode.getDataAfter();
                    LOG.debug("\nUpdated Service Function Forwarder Name: {}",
                            updatedServiceFunctionForwarder.getName());

                    RestOperation restOp = rootNode.getDataBefore() == null ? RestOperation.POST
                            : RestOperation.PUT;
                    executor.execute(new SbRestSffTask(restOp, updatedServiceFunctionForwarder, executor));
                    break;
                case DELETE:
                    ServiceFunctionForwarder originalServiceFunctionForwarder = rootNode.getDataBefore();
                    LOG.debug("\nDeleted Service Function Forwarder Name: {}",
                            originalServiceFunctionForwarder.getName());

                    executor.execute(new SbRestSffTask(RestOperation.DELETE, originalServiceFunctionForwarder,
                            executor));
                    break;
                default:
                    break;
            }
        }

        printTraceStop(LOG);
    }
}
