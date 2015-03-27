/*
 * Copyright (c) 2015 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeKey;


/**
 * This class defines the APIs to operate on the ServiceFunctionScheduleTypes
 * datastore.
 *
 * @author Johnson Li (johnson.li@intel.com)
 * @version 0.1
 * @since 2015-03-20
 */
public class SfcProviderScheduleTypeAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderScheduleTypeAPI.class);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();
    private static final String FAILED_TO_STR = "failed to ...";

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

    protected boolean putServiceFunctionScheduleType(ServiceFunctionSchedulerType sfst) {
        boolean ret = false;
        printTraceStart(LOG);

        if (dataBroker != null) {
            InstanceIdentifier<ServiceFunctionSchedulerType> sfstEntryIID =
                    InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                    .child(ServiceFunctionSchedulerType.class, sfst.getKey()).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sfstEntryIID, sfst, true);
            writeTx.commit();

            ret = true;
        }

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
                .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerTypeKey).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfstIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to delete Service Function Schedule Type: {}", serviceFunctionSchedulerType);
        }

        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionSchedulerType readServiceFunctionScheduleType(
            Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerType) {
        printTraceStart(LOG);
        ServiceFunctionSchedulerType sfst = null;

        InstanceIdentifier<ServiceFunctionSchedulerType> sfstIID;
        ServiceFunctionSchedulerTypeKey serviceFunctionSchedulerTypeKey = new
                ServiceFunctionSchedulerTypeKey(serviceFunctionSchedulerType);
        sfstIID = InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerTypeKey).build();

        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<ServiceFunctionSchedulerType> serviceFunctionSchedulerTypeOptional = null;
        try {
            serviceFunctionSchedulerTypeOptional = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, sfstIID).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read Service Function Schedule Type {} " + "", serviceFunctionSchedulerType);
        }
        if (serviceFunctionSchedulerTypeOptional != null
                && serviceFunctionSchedulerTypeOptional.isPresent()) {
            sfst = serviceFunctionSchedulerTypeOptional.get();
        }

        printTraceStop(LOG);
        return sfst;
    }

    protected ServiceFunctionSchedulerTypes readAllServiceFunctionScheduleTypes() {
        ServiceFunctionSchedulerTypes sfsts = null;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionSchedulerTypes> sfstsIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class).toInstance();

        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<ServiceFunctionSchedulerTypes> serviceFunctionSchedulerTypesOptional;
        try {
            serviceFunctionSchedulerTypesOptional = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, sfstsIID).get();
            if (serviceFunctionSchedulerTypesOptional != null
                    && serviceFunctionSchedulerTypesOptional.isPresent()) {
                sfsts = serviceFunctionSchedulerTypesOptional.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read All Service Function Schedule Types");
        }

        printTraceStop(LOG);
        return sfsts;
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
                    (ServiceFunctionSchedulerTypes)future.get();
            List<ServiceFunctionSchedulerType> sfScheduleTypeList =
                    serviceFunctionSchedulerTypes.getServiceFunctionSchedulerType();
            for (ServiceFunctionSchedulerType sfst : sfScheduleTypeList) {
                if (sfst.isEnabled() == true) {
                    ret = sfst;
                    break;
                }
            }
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }

        printTraceStop(LOG);
        return ret;
    }
}
