/*
 * Copyright (c) 2015 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class defines the APIs to operate on the ServiceFunctionScheduleTypes
 * datastore.
 *
 * @author Johnson Li (johnson.li@intel.com)
 * @author Vladimir Lavor (vladimir.lavor@pantheon.sk)
 * @version 0.1
 * @since 2015-03-20
 */
public class SfcProviderScheduleTypeAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderScheduleTypeAPI.class);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();

    SfcProviderScheduleTypeAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderScheduleTypeAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderScheduleTypeAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderScheduleTypeAPI(params, paramsTypes, "putServiceFunctionScheduleType");
    }

    public static SfcProviderScheduleTypeAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderScheduleTypeAPI(params, paramsTypes, "readServiceFunctionScheduleType");
    }

    public static SfcProviderScheduleTypeAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderScheduleTypeAPI(params, paramsTypes, "deleteServiceFunctionScheduleType");
    }

    public static SfcProviderScheduleTypeAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderScheduleTypeAPI(params, paramsTypes, "readAllServiceFunctionScheduleTypes");
    }

    /**
     * This method puts service function schedule type.
     * <p/>
     *
     * @return True if ST was put, false otherwise
     */
    public static boolean putServiceFunctionScheduleTypeExecutor(ServiceFunctionSchedulerType serviceFunctionSchedulerType) {
        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {serviceFunctionSchedulerType};
        Class[] servicePathClass = {ServiceFunctionSchedulerType.class};
        SfcProviderScheduleTypeAPI sfcProviderScheduleTypeAPI = SfcProviderScheduleTypeAPI
                .getPut(servicePathObj, servicePathClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderScheduleTypeAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServiceFunctionState: {}", future.get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("failed to ....", e);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method deletes service function schedule type.
     * <p/>
     *
     * @return True if ST was deleted, false otherwise
     */
    public static boolean deleteServiceFunctionScheduleTypeExecutor(Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerType) {
        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {serviceFunctionSchedulerType};
        Class[] servicePathClass = {serviceFunctionSchedulerType.getClass()};
        SfcProviderScheduleTypeAPI sfcProviderScheduleTypeAPI = SfcProviderScheduleTypeAPI
                .getDelete(servicePathObj, servicePathClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderScheduleTypeAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServiceFunctionState: {}", future.get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("failed to ....", e);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads service function schedule type.
     * <p/>
     *
     * @return If ST was read successfully, returns that ST
     */
    public static ServiceFunctionSchedulerType readServiceFunctionScheduleTypeExecutor(Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerType) {
        printTraceStart(LOG);
        ServiceFunctionSchedulerType ret = null;
        Object[] servicePathObj = {serviceFunctionSchedulerType};
        Class[] servicePathClass = {serviceFunctionSchedulerType.getClass()};
        SfcProviderScheduleTypeAPI sfcProviderScheduleTypeAPI = SfcProviderScheduleTypeAPI
                .getRead(servicePathObj, servicePathClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderScheduleTypeAPI);
        try {
            ret = (ServiceFunctionSchedulerType) future.get();
            LOG.debug("getDeleteServiceFunctionState: {}", future.get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("failed to ....", e);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
        }
        printTraceStop(LOG);
        return ret;
    }

    public static ServiceFunctionSchedulerType readEnabledServiceFunctionScheduleTypeEntryExecutor() {
        ServiceFunctionSchedulerType ret = null;
        Object[] sfstObj = {};
        Class[] sfstClass = {};

        printTraceStart(LOG);
        SfcProviderScheduleTypeAPI sfcProviderScheduleTypeAPI = SfcProviderScheduleTypeAPI
                .getReadAll(sfstObj, sfstClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderScheduleTypeAPI);
        try {
            LOG.debug("getReadAll returns: {}", future.get());
            ServiceFunctionSchedulerTypes serviceFunctionSchedulerTypes =
                    (ServiceFunctionSchedulerTypes) future.get();
            List<ServiceFunctionSchedulerType> sfScheduleTypeList =
                    serviceFunctionSchedulerTypes.getServiceFunctionSchedulerType();
            for (ServiceFunctionSchedulerType serviceFunctionSchedulerType : sfScheduleTypeList) {
                if (serviceFunctionSchedulerType.isEnabled()) {
                    ret = serviceFunctionSchedulerType;
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("failed to ....", e);
        }

        printTraceStop(LOG);
        return ret;
    }

    protected boolean putServiceFunctionScheduleType(ServiceFunctionSchedulerType serviceFunctionSchedulerType) {
        boolean ret;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionSchedulerType> sfstEntryIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                        .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerType.getKey()).build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sfstEntryIID, serviceFunctionSchedulerType,
                LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    protected boolean deleteServiceFunctionScheduleType(
            Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerType) {
        printTraceStart(LOG);
        boolean ret = false;

        ServiceFunctionSchedulerTypeKey serviceFunctionSchedulerTypeKey = new
                ServiceFunctionSchedulerTypeKey(serviceFunctionSchedulerType);
        InstanceIdentifier<ServiceFunctionSchedulerType> sfstIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                        .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerTypeKey).build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfstIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to delete Service Function Schedule Type: {}", serviceFunctionSchedulerType);
        }

        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionSchedulerType readServiceFunctionScheduleType(
            Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerTypeIdentity) {
        printTraceStart(LOG);
        ServiceFunctionSchedulerType serviceFunctionSchedulerType;

        InstanceIdentifier<ServiceFunctionSchedulerType> sfstIID;
        ServiceFunctionSchedulerTypeKey serviceFunctionSchedulerTypeKey = new
                ServiceFunctionSchedulerTypeKey(serviceFunctionSchedulerTypeIdentity);

        sfstIID = InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerTypeKey).build();

        serviceFunctionSchedulerType = SfcDataStoreAPI.readTransactionAPI(sfstIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return serviceFunctionSchedulerType;
    }

    protected ServiceFunctionSchedulerTypes readAllServiceFunctionScheduleTypes() {
        ServiceFunctionSchedulerTypes serviceFunctionSchedulerTypes;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionSchedulerTypes> schedulerTypesIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class).build();

        serviceFunctionSchedulerTypes = SfcDataStoreAPI.readTransactionAPI(schedulerTypesIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return serviceFunctionSchedulerTypes;
    }
}
