/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class gets called whenever there is a change to
 * a Service Function Classifier list entry, i.e.,
 * added/deleted/modified.
 * <p>
 *
 * @author Andrej Kincel (akincel@cisco.com)
 * @version 0.1
 * @since 2014-11-11
 */
public class SfcProviderScfEntryDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderScfEntryDataListener.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        ServiceFunctionClassifier originalServiceFunctionClassifier = null;

        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionClassifier) {
                originalServiceFunctionClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\n########## Original ServiceFunctionClassifier name: {}", originalServiceFunctionClassifier.getName());
                LOG.debug("\n########## Original ServiceFunctionClassifier ACL: {}", originalServiceFunctionClassifier.getAcl());
            }
        }

        // Classifier CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier createdServiceFunctionClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\n########## Created ServiceFunctionClassifier name: {}", createdServiceFunctionClassifier.getName());

                if ((createdServiceFunctionClassifier.getName() != null) && (createdServiceFunctionClassifier.getAcl() != null)) {
                    //call executor to write <ACL, Classifier> entry into ACL operational store
                    SfcProviderAclAPI.addClassifierToAccessListState(createdServiceFunctionClassifier.getAcl().getName(),
                            createdServiceFunctionClassifier.getAcl().getType(),
                            createdServiceFunctionClassifier.getName());
                }
            }
        }

        // Classifier UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionClassifier) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionClassifier updatedServiceFunctionClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\n########## Modified ServiceFunctionClassifier Name {}",
                        updatedServiceFunctionClassifier.getName());

                if ((originalServiceFunctionClassifier != null && originalServiceFunctionClassifier.getAcl() != null &&
                        updatedServiceFunctionClassifier.getAcl() != null)
                        && !originalServiceFunctionClassifier.getAcl().equals(updatedServiceFunctionClassifier.getAcl())) {

                    if (updatedServiceFunctionClassifier.getAcl() !=null) {
                        //call executor to write <ACL, Classifier> entry into ACL operational store
                        SfcProviderAclAPI.addClassifierToAccessListState(updatedServiceFunctionClassifier.getAcl().getName(),
                                updatedServiceFunctionClassifier.getAcl().getType(),
                                updatedServiceFunctionClassifier.getName());
                    }
                    // if Access List is empty string, Classifier should be not more linked to the origin Access List
                    else {
                        //call executor to delete <ACL, Classifier> entry from ACL operational store
                        SfcProviderAclAPI.deleteClassifierFromAccessListState(originalServiceFunctionClassifier.getAcl().getName(),
                                originalServiceFunctionClassifier.getAcl().getType(),
                                originalServiceFunctionClassifier.getName());
                    }
                }
            }
        }

        // Classifier DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalConfigurationObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier deletedServiceFunctionClassifier = (ServiceFunctionClassifier) dataObject;

                if ((deletedServiceFunctionClassifier.getName() != null) && (deletedServiceFunctionClassifier.getAcl() != null)){
                    //call executor to delete <ACL, Classifier> entry from ACL operational store
                    SfcProviderAclAPI.deleteClassifierFromAccessListState(deletedServiceFunctionClassifier.getAcl().getName(),
                            deletedServiceFunctionClassifier.getAcl().getType(),
                            deletedServiceFunctionClassifier.getName());
                }
            }
        }

        printTraceStop(LOG);
    }


}
