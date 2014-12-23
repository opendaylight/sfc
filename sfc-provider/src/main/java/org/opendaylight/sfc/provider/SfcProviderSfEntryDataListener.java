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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


/**
 * This class gets called whenever there is a change to
 * a Service Function list entry, i.e.,
 * added/deleted/modified.
 *
 * <p
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderSfEntryDataListener implements DataChangeListener  {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfEntryDataListener.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();


    /**
     * This method is called whenever there is change in a SF. Before doing any changes
     * it takes a global lock in order to ensure it is the only writer.
     * @param change
     */
    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        printTraceStart(LOG);

        odlSfc.getLock();

        // SF ORIGINAL
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction originalServiceFunction = (ServiceFunction) entry.getValue();
                LOG.debug("\n########## getOriginalConfigurationData {}  {}",
                        originalServiceFunction.getType(), originalServiceFunction.getName());
            }
        }

        // SF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction createdServiceFunction = (ServiceFunction) entry.getValue();

                Object[] serviceTypeObj = {createdServiceFunction};
                Class[] serviceTypeClass = {ServiceFunction.class};
                Future future = odlSfc.getExecutor().submit(SfcProviderServiceTypeAPI
                        .getCreateServiceFunctionTypeEntry(serviceTypeObj, serviceTypeClass));
                try {
                    LOG.debug("getCreateServiceFunctionTypeEntry returns: {}", future.get());

                } catch (InterruptedException e) {
                    LOG.warn("failed to ...." , e);
                } catch (ExecutionException e) {
                    LOG.warn("failed to ...." , e);
                }

                LOG.debug("\n########## getCreatedConfigurationData {}  {}",
                        createdServiceFunction.getType(), createdServiceFunction.getName());
            }

        }

        // SF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if( dataObject instanceof  ServiceFunction) {
                ServiceFunction originalServiceFunction = (ServiceFunction) dataObject;

                Object[] serviceFunctionObj = {originalServiceFunction};
                Class[] serviceFunctionClass = {ServiceFunction.class};
                Future future = odlSfc.getExecutor().submit(SfcProviderServiceTypeAPI
                        .getDeleteServiceFunctionFromServiceType(serviceFunctionObj, serviceFunctionClass));
                try {
                    LOG.debug("getDeleteServiceFunctionFromServiceType returns: {}", future.get());
                } catch (InterruptedException e) {
                    LOG.warn("failed to ...." , e);
                } catch (ExecutionException e) {
                    LOG.warn("failed to ...." , e);
                }

                /* Before removing RSPs used by this Service Function, we need to remove all
                 * references in the SFF/SF operational trees
                 */
                String sfName = originalServiceFunction.getName();
                List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.
                        readServiceFunctionStateExecutor(sfName);
                List<String> rspList = new ArrayList<>();
                if ((sfServicePathList != null) && (!sfServicePathList.isEmpty())) {
                    if (SfcProviderServiceFunctionAPI.deleteServiceFunctionStateExecutor(sfName)) {
                    } else {
                        LOG.error("{}: Failed to delete SF {} operational state",
                                Thread.currentThread().getStackTrace()[1], sfName);
                    }
                    for (SfServicePath sfServicePath : sfServicePathList) {
                        String rspName = sfServicePath.getName();
                        SfcProviderServiceForwarderAPI
                                .deletePathFromServiceForwarderStateExecutor(rspName);
                        rspList.add(rspName);
                    }
                    SfcProviderRenderedPathAPI.deleteRenderedServicePathsExecutor(rspList);
                }
            }
        }


        // SF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject
                = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunction) && (!(dataCreatedObject.containsKey(entry.getKey())))) {


                DataObject dataObject = dataOriginalDataObject.get(entry.getKey());
                ServiceFunction originalServiceFunction = (ServiceFunction) dataObject;
                Object[] serviceFunctionObj = {originalServiceFunction};
                Class[] serviceFunctionClass = {ServiceFunction.class};


                ServiceFunction updatedServiceFunction = (ServiceFunction) entry.getValue();

                // We only update SF type entry if type has changed
                if (!updatedServiceFunction.getType().equals(originalServiceFunction.getType())) {

                    // We remove the original SF from SF type list
                    Future future = odlSfc.getExecutor().submit(SfcProviderServiceTypeAPI
                            .getDeleteServiceFunctionFromServiceType(serviceFunctionObj, serviceFunctionClass));
                    try {
                        LOG.debug("getDeleteServiceFunctionFromServiceType returns: {}", future.get());
                    } catch (InterruptedException e) {
                        LOG.warn("failed to ...." , e);
                    } catch (ExecutionException e) {
                        LOG.warn("failed to ...." , e);
                    }
                    // We create a independent entry
                    serviceFunctionObj[0] = updatedServiceFunction;
                    serviceFunctionClass[0] = ServiceFunction.class;
                    odlSfc.getExecutor().submit(SfcProviderServiceTypeAPI
                            .getCreateServiceFunctionTypeEntry(serviceFunctionObj, serviceFunctionClass));
                }

               /* Before removing RSPs used by this Service Function, we need to remove all
                 * references in the SFF/SF operational trees
                 */
                String sfName = originalServiceFunction.getName();
                List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.
                        readServiceFunctionStateExecutor(sfName);
                List<String> rspList = new ArrayList<>();
                if ((sfServicePathList != null) && (!sfServicePathList.isEmpty())) {
                    if (SfcProviderServiceFunctionAPI.deleteServiceFunctionStateExecutor(sfName)) {
                    } else {
                        LOG.error("{}: Failed to delete SF {} operational state",
                                Thread.currentThread().getStackTrace()[1], sfName);
                    }
                    for (SfServicePath sfServicePath : sfServicePathList) {
                        String rspName = sfServicePath.getName();
                        SfcProviderServiceForwarderAPI
                                .deletePathFromServiceForwarderStateExecutor(rspName);
                        rspList.add(rspName);
                    }
                    SfcProviderRenderedPathAPI.deleteRenderedServicePathsExecutor(rspList);
                }
                /* We do not update the SFF dictionary. Since the user configured it in the first place,
                 * (s)he is also responsible for updating it.
                 */
            }
        }
        odlSfc.releaseLock();
        printTraceStop(LOG);
    }

}
