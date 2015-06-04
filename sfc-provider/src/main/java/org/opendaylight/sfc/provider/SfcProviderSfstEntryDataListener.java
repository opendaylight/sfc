/*
 * Copyright (c) 2015 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;


import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.api.*;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


public class SfcProviderSfstEntryDataListener implements DataChangeListener  {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfstEntryDataListener.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static boolean isCreateTrue = false;
    /**
     * This method is called whenever there is change in a SF Schedule Type. Before doing any changes
     * it takes a global lock in order to ensure it is the only writer.
     * @param change
     */
    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {
        printTraceStart(LOG);
        odlSfc.getLock();

        // SF Schedule Type ORIGINAL
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunctionSchedulerType) {
                ServiceFunctionSchedulerType origServiceFunctionSchedulerType =
                        (ServiceFunctionSchedulerType) entry.getValue();
                LOG.debug("\n########## getOriginalConfigurationData {} {}",
                        origServiceFunctionSchedulerType.getType(), origServiceFunctionSchedulerType.getName());
            }
        }

        // SF Schedule Type CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunctionSchedulerType) {
                ServiceFunctionSchedulerType createdServiceFunctionSchedulerType =
                        (ServiceFunctionSchedulerType) entry.getValue();
                LOG.debug("\n########## createdServiceFunctionSchedulerType {} {}",
                        createdServiceFunctionSchedulerType.getType(), createdServiceFunctionSchedulerType.getName());
                try {
                    if (createdServiceFunctionSchedulerType.isEnabled() == true) {
                        isCreateTrue = true;

                        Object[] sfstObj = {};
                        Class[] sfstClass = {};
                        SfcProviderScheduleTypeAPI sfcProviderScheduleTypeAPI = SfcProviderScheduleTypeAPI
                                .getReadAll(sfstObj, sfstClass);
                        Future future = odlSfc.getExecutor().submit(sfcProviderScheduleTypeAPI);
                        if(future.get()!=null) {
                            ServiceFunctionSchedulerTypes serviceFunctionSchedulerTypes =
                                    (ServiceFunctionSchedulerTypes)future.get();
                            List<ServiceFunctionSchedulerType> sfScheduleTypeList =
                                    serviceFunctionSchedulerTypes.getServiceFunctionSchedulerType();
                            for (ServiceFunctionSchedulerType sfst : sfScheduleTypeList) {
                                if (sfst.isEnabled() == true) {
                                    if (!(sfst.getType().equals(createdServiceFunctionSchedulerType.getType()))) {
                                        ServiceFunctionSchedulerType sfstUpdate = new ServiceFunctionSchedulerTypeBuilder()
                                                                                    .setName(sfst.getName())
                                                                                    .setType(sfst.getType())
                                                                                    .setEnabled(false).build();

                                        Object[] sfstObjUpdate = {sfstUpdate};
                                        Class[] sfstClassUpdate = {ServiceFunctionSchedulerType.class};
                                        future = odlSfc.getExecutor().submit(SfcProviderScheduleTypeAPI
                                            .getPut(sfstObjUpdate, sfstClassUpdate));
                                        break;
                                    }
                                }
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    LOG.warn("failed to ...." , e);
                } catch (ExecutionException e) {
                    LOG.warn("failed to ...." , e);
                }
            }
        }

        // SF Schedule Type DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if( dataObject instanceof  ServiceFunctionSchedulerType) {
                ServiceFunctionSchedulerType origServiceFunctionSchedulerType =
                        (ServiceFunctionSchedulerType) dataObject;
                LOG.debug("\n########## deletedServiceFunctionSchedulerType {} {}",
                        origServiceFunctionSchedulerType.getType(), origServiceFunctionSchedulerType.getName());
            }
        }

        // SF Schedule Type UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject
                = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionSchedulerType) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionSchedulerType updatedServiceFunctionSchedulerType = (ServiceFunctionSchedulerType) entry.getValue();
                LOG.debug("\n########## updatedServiceFunctionSchedulerType {} {}",
                        updatedServiceFunctionSchedulerType.getType(), updatedServiceFunctionSchedulerType.getName());

                try {
                    if (isCreateTrue==false) {
                        if (updatedServiceFunctionSchedulerType.isEnabled() == true) {
                            Object[] sfstObj = {};
                            Class[] sfstClass = {};
                            SfcProviderScheduleTypeAPI sfcProviderScheduleTypeAPI = SfcProviderScheduleTypeAPI
                                    .getReadAll(sfstObj, sfstClass);
                            Future future = odlSfc.getExecutor().submit(sfcProviderScheduleTypeAPI);
                            if(future.get()!=null) {
                                ServiceFunctionSchedulerTypes serviceFunctionSchedulerTypes =
                                        (ServiceFunctionSchedulerTypes)future.get();
                                List<ServiceFunctionSchedulerType> sfScheduleTypeList =
                                        serviceFunctionSchedulerTypes.getServiceFunctionSchedulerType();
                                for (ServiceFunctionSchedulerType sfst : sfScheduleTypeList) {
                                    if (sfst.isEnabled() == true) {
                                        if (!(sfst.getType().equals(updatedServiceFunctionSchedulerType.getType()))) {
                                            ServiceFunctionSchedulerType sfstUpdate = new ServiceFunctionSchedulerTypeBuilder()
                                                                                        .setName(sfst.getName())
                                                                                        .setType(sfst.getType())
                                                                                        .setEnabled(false).build();

                                            Object[] serviceFunctionSchedulerTypeObj = {sfstUpdate};
                                            Class[] serviceFunctionSchedulerTypeClass = {ServiceFunctionSchedulerType.class};
                                            future = odlSfc.getExecutor().submit(SfcProviderScheduleTypeAPI
                                                .getPut(serviceFunctionSchedulerTypeObj, serviceFunctionSchedulerTypeClass));
                                            break;

                                        }
                                    }
                                }
                            }
                        }
                    } else if (isCreateTrue==true) {
                        isCreateTrue=false;
                    }
                } catch (InterruptedException e) {
                    LOG.warn("failed to ...." , e);
                } catch (ExecutionException e) {
                    LOG.warn("failed to ...." , e);
                }
            }
        }

        odlSfc.releaseLock();
        printTraceStop(LOG);
    }
}
