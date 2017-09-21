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
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestAclTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestScfEntryDataListener extends SbRestAbstractDataListener<ServiceFunctionClassifier> {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestScfEntryDataListener.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public SbRestScfEntryDataListener() {
        setInstanceIdentifier(SfcInstanceIdentifiers.SCF_ENTRY_IID);
    }

    public void setDataProvider(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        registerAsDataChangeListener();
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ServiceFunctionClassifier>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<ServiceFunctionClassifier> change: changes) {
            DataObjectModification<ServiceFunctionClassifier> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    ServiceFunctionClassifier updatedClassifier = rootNode.getDataAfter();
                    LOG.debug("\nUpdated Service Classifier Name: {}", updatedClassifier.getName());

                    if (updatedClassifier.getAcl() != null) {
                        Acl accessList = SfcProviderAclAPI.readAccessList(updatedClassifier.getAcl().getName(),
                                updatedClassifier.getAcl().getType());

                        RestOperation restOp = rootNode.getDataBefore() == null ? RestOperation.POST
                                : RestOperation.PUT;
                        Runnable task = new SbRestAclTask(restOp, accessList,
                                updatedClassifier.getSclServiceFunctionForwarder(), executor);
                        executor.execute(task);
                    }
                    break;
                case DELETE:
                    ServiceFunctionClassifier originalClassifier = rootNode.getDataBefore();
                    LOG.debug("\nDeleted Service Classifier Name: {}", originalClassifier.getName());

                    if (originalClassifier.getAcl() != null) {
                        Runnable task = new SbRestAclTask(RestOperation.DELETE, originalClassifier.getAcl().getName(),
                                originalClassifier.getAcl().getType(),
                                originalClassifier.getSclServiceFunctionForwarder(), executor);
                        executor.execute(task);
                    }
                    break;
                default:
                    break;
            }
        }

        printTraceStop(LOG);
    }
}
