/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcScfOfDataListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfDataListener.class);
    private SfcScfOfProcessor sfcScfProcessor;
    private ListenerRegistration<DataChangeListener> registerListener;

    public SfcScfOfDataListener(
            DataBroker dataBroker,
            SfcScfOfProcessor sfcScfProcessor) {
        registerListener = dataBroker.registerDataChangeListener(
            LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.SCF_ENTRY_IID,
            this, DataBroker.DataChangeScope.BASE);
        this.sfcScfProcessor = sfcScfProcessor;
    }

    @Override
    public void close() throws Exception {
        if (registerListener != null)
            registerListener.close();
    }

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        ServiceFunctionClassifier originScf = null;

        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionClassifier) {
                originScf = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("Original ServiceFunctionClassifier name: {}\n", originScf.getName());
            }
        }

        // Classifier CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier scf = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("Created ServiceFunctionClassifier name: {}\n", scf.getName());
                this.sfcScfProcessor.createdServiceFunctionClassifier(scf);
            }
        }

        // Classifier UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionClassifier) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionClassifier scf = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("Updated ServiceFunctionClassifier name: {}\n", scf.getName());
                this.sfcScfProcessor.deletedServiceFunctionClassifier(originScf);
                this.sfcScfProcessor.createdServiceFunctionClassifier(scf);
            }
        }

        // Classifier DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalConfigurationObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier scf = (ServiceFunctionClassifier) dataObject;
                LOG.debug("Deleted ServiceFunctionClassifier name: {}\n", scf.getName());
                this.sfcScfProcessor.deletedServiceFunctionClassifier(scf);
            }
        }
    }
}
