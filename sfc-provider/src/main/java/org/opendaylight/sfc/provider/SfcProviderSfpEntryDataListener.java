/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.sfc.provider;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.api.*;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class is the DataListener for SFP Entry changes.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */

public class SfcProviderSfpEntryDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfpEntryDataListener.class);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        printTraceStart(LOG);
        ODL_SFC.getLock();

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunctionPath) {
                ServiceFunctionPath originalServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                LOG.debug("\n########## Original Service path: {}",
                        originalServiceFunctionPath.getName());
            }
        }

        // SFP CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        /* For each SFP we perform the following transactions:
         *   0 - Check if classifier exists
         *   1 - Create RSP
         *   2 - Add Path to SFF State
         *   3 - Add path to SF state
         *   4 - Check if Chain is symmetric
         *   5 - if chain is symmetric repeat 1-3 for reverse path
         *
         * If any of these fail we delete the previous ones that succeeded.
         */

/*        RenderedServicePath renderedServicePath;
        RenderedServicePath revRenderedServicePath;
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet())
        {
            if (entry.getValue() instanceof ServiceFunctionPath) {

                ServiceFunctionPath createdServiceFunctionPath = (ServiceFunctionPath) entry.getValue();

                renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(createdServiceFunctionPath);
                if (renderedServicePath != null) {

                    if ((createdServiceFunctionPath.getClassifier() != null) &&
                            SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(createdServiceFunctionPath.getClassifier()) != null) {
                        SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor
                                (createdServiceFunctionPath.getClassifier(), renderedServicePath.getName());
                    }

                    if (createdServiceFunctionPath.isSymmetric() != null && createdServiceFunctionPath.isSymmetric()) {

                        revRenderedServicePath = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePath);
                        if (revRenderedServicePath == null) {
                            LOG.error("Failed to create symmetric service path: {}");
                        } else if ((createdServiceFunctionPath.getSymmetricClassifier() != null) &&
                                SfcProviderServiceClassifierAPI
                                        .readServiceClassifierExecutor(createdServiceFunctionPath.getSymmetricClassifier()) != null) {
                            SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor
                                    (createdServiceFunctionPath.getSymmetricClassifier(), revRenderedServicePath.getName());

                        } else {
                            LOG.warn("Symmetric Classifier not provided or does not exist");
                        }
                    }
                } else {
                    LOG.error("Failed to create RSP");
                }

            }
        }*/

/*        // SFP UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject =
                change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionPath) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionPath updatedServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                LOG.debug("\n########## Modified Service Function Path Name {}",
                        updatedServiceFunctionPath.getName());

                if (SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(updatedServiceFunctionPath.getName()) != null) {
                    renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(updatedServiceFunctionPath);
                    if (renderedServicePath != null) {
                        SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor
                                (updatedServiceFunctionPath.getClassifier(), renderedServicePath.getName());

                        if (updatedServiceFunctionPath.isSymmetric() != null && updatedServiceFunctionPath.isSymmetric() &&
                                (SfcProviderServiceClassifierAPI
                                        .readServiceClassifierExecutor(updatedServiceFunctionPath.getSymmetricClassifier()) != null)) {

                            revRenderedServicePath = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePath);
                            if (revRenderedServicePath != null) {
                                SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor
                                        (updatedServiceFunctionPath.getSymmetricClassifier(), revRenderedServicePath.getName());

                            }
                        }
                    }
                }
            }
        }*/


        // SFP DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if( dataObject instanceof ServiceFunctionPath) {

                // If a SFP is deleted we remove RSP and both SF and SFF operational states.
                ServiceFunctionPath originalServiceFunctionPath = (ServiceFunctionPath) dataObject;
                SfcProviderServiceForwarderAPI
                        .deletePathFromServiceForwarderStateExecutor(originalServiceFunctionPath);
                SfcProviderServiceFunctionAPI
                        .deleteServicePathFromServiceFunctionStateExecutor(originalServiceFunctionPath);

                List<SfpRenderedServicePath> sfpRenderedServicePathList = SfcProviderServicePathAPI
                        .readServicePathStateExecutor(originalServiceFunctionPath.getName());
                if ((sfpRenderedServicePathList != null) && (!sfpRenderedServicePathList.isEmpty())) {
                    for (SfpRenderedServicePath sfpRenderedServicePath : sfpRenderedServicePathList) {
                        String rspName = sfpRenderedServicePath.getName();
                        SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(rspName);
                        //Send to SB REST
                        SfcProviderServicePathAPI.checkServiceFunctionPathExecutor
                               (originalServiceFunctionPath, HttpMethod.DELETE);
                    }
                }
                SfcProviderServicePathAPI.deleteServicePathStateExecutor(originalServiceFunctionPath.getName());
            }
        }
        ODL_SFC.releaseLock();
        printTraceStop(LOG);
    }

}
