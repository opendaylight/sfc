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
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths
        .ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public synchronized void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {



        printTraceStart(LOG);
        odlSfc.getLock();

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

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionPath) {
                ServiceFunctionPath createdServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                LOG.debug("\n########## Created ServiceFunctionChain name: {}", createdServiceFunctionPath.getName());
                Object[] servicePathObj = {createdServiceFunctionPath};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                        .getCreateServicePathAPI(servicePathObj, servicePathClass);
                Future future = odlSfc.executor.submit(sfcProviderServicePathAPI);
                try {
                    LOG.info("getCreateServicePathAPI: {}", future.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        // SFP UPDATE
        // TODO
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject =
                change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionPath) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionPath updatedServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                LOG.info("\n########## Modified Service Function Path Name {}",
                        updatedServiceFunctionPath.getName());
                Object[] servicePathObj = {updatedServiceFunctionPath};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                        .getUpdateServicePathAPI(servicePathObj, servicePathClass);
                odlSfc.executor.submit(sfcProviderServicePathAPI);

                /* Add SFP name to the operational store of each SFF found in the path.
                 * When a SFF is deleted or modified we delete all SFP associated with it.
                 */
                servicePathObj[0] = updatedServiceFunctionPath;
                servicePathClass[0] = ServiceFunctionPath.class;
                SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                        .getAddPathToServiceForwarderState(servicePathObj, servicePathClass);
                odlSfc.executor.submit(sfcProviderServiceForwarderAPI);

                /* Add SFP name to the operational store of each SFF found in the path.
                 * When a SFF is deleted or modified we delete all SFP associated with it.
                 */

                servicePathObj[0] = updatedServiceFunctionPath;
                servicePathClass[0] = ServiceFunctionPath.class;
                SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                        .getAddPathToServiceFunctionState(servicePathObj, servicePathClass);
                odlSfc.executor.submit(sfcProviderServiceFunctionAPI);

            }
        }


        // SFP DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if( dataObject instanceof ServiceFunctionPath) {

                // If a SFP is deleted we remove it form SF operational state
                ServiceFunctionPath originalServiceFunctionPath = (ServiceFunctionPath) dataObject;
                Object[] servicePathObj = {originalServiceFunctionPath};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                Future future = odlSfc.executor.submit(SfcProviderServiceFunctionAPI
                        .getDeleteServicePathFromServiceFunctionState(servicePathObj, servicePathClass));

                try {
                    LOG.info("getDeleteServicePathFromServiceFunctionState: {}", future.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }


                // If a SFP is deleted we remove it form SFF operational state
                servicePathObj[0] = originalServiceFunctionPath;
                servicePathClass[0] = ServiceFunctionPath.class;
                SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                        .getDeletePathFromServiceForwarderState(servicePathObj, servicePathClass);
                future = odlSfc.executor.submit(sfcProviderServiceForwarderAPI);

                try {
                    LOG.info("getDeletePathFromServiceForwarderState: {}", future.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                //Send to SB REST
                Object[] servicePathRestObj= {originalServiceFunctionPath, HttpMethod.DELETE};
                Class[] servicePathRestClass = {ServiceFunctionPath.class, String.class};
                odlSfc.executor.submit(SfcProviderServicePathAPI.getCheckServicePathAPI(
                        servicePathObj, servicePathClass));


            }
        }
        odlSfc.releaseLock();
        printTraceStop(LOG);
    }
}
