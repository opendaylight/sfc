/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

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

/**
 * This class gets called whenever there is a change to
 * a Service Function Classifier list entry, i.e.,
 * added/deleted/modified.
 * <p/>
 * <p/>
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


        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier originalServiceFunctionClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\n########## Original ServiceFunctionClassifier name: {}", originalServiceFunctionClassifier.getName());
                LOG.debug("\n########## Original ServiceFunctionClassifier ACL: {}", originalServiceFunctionClassifier.getAccessList());
                LOG.debug("\n########## Original ServiceFunctionClassifier SFP: {}", originalServiceFunctionClassifier.getServiceFunctionPath());
            }
        }

        // Classifier CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier createdServiceFunctionClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\n########## Created ServiceFunctionClassifier name: {}", createdServiceFunctionClassifier.getName());

                String serviceFunctionPathName = null;
                serviceFunctionPathName = createdServiceFunctionClassifier.getServiceFunctionPath();
                if (serviceFunctionPathName != null) {
                    Object[] params = {createdServiceFunctionClassifier.getAccessList(),
                            createdServiceFunctionClassifier.getServiceFunctionPath()};
                    Class[] paramsTypes = {String.class, String.class};

                    odlSfc.executor.submit(SfcProviderAclAPI
                            .getSetAclEntriesSfcAction(params, paramsTypes));

                }
            }
        }

        // Classifier UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionClassifier) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionClassifier updatedServiceFunctionClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.info("\n########## Modified ServiceFunctionClassifier Name {}",
                        updatedServiceFunctionClassifier.getName());

                //if ACL was changed, unset AclEntries in old ACL
                ServiceFunctionClassifier originalServiceFunctionClassifier =
                        (ServiceFunctionClassifier) dataOriginalConfigurationObject.get(entry.getKey());
                if (originalServiceFunctionClassifier != null &&
                        (!originalServiceFunctionClassifier.getAccessList()
                                .equalsIgnoreCase(updatedServiceFunctionClassifier.getAccessList()))) {

                    Object[] params = {originalServiceFunctionClassifier.getAccessList()};
                    Class[] paramsTypes = {String.class};
                    odlSfc.executor.submit(SfcProviderAclAPI
                            .getUnSetAclEntriesSfcAction(params, paramsTypes));
                }

                //set AclEntries in new ACL
                Object[] params = {updatedServiceFunctionClassifier.getAccessList(),
                        updatedServiceFunctionClassifier.getServiceFunctionPath()};
                Class[] paramsTypes = {String.class, String.class};
                odlSfc.executor.submit(SfcProviderAclAPI
                        .getSetAclEntriesSfcAction(params, paramsTypes));
            }
        }

        // Classifier DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalConfigurationObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier originalServiceFunctionClassifier = (ServiceFunctionClassifier) dataObject;

                Object[] params = {originalServiceFunctionClassifier.getAccessList()};
                Class[] paramsTypes = {String.class};
                odlSfc.executor.submit(SfcProviderAclAPI
                        .getUnSetAclEntriesSfcAction(params, paramsTypes));
            }
        }

        printTraceStop(LOG);
    }


}
