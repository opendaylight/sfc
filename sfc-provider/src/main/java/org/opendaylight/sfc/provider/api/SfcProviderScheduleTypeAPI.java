/*
 * Copyright (c) 2015 Intel .Ltd, and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class SfcProviderScheduleTypeAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderScheduleTypeAPI.class);

    public static ServiceFunctionSchedulerType readEnabledServiceFunctionScheduleTypeEntry() {
        ServiceFunctionSchedulerType ret = null;
        printTraceStart(LOG);
        ServiceFunctionSchedulerTypes serviceFunctionSchedulerTypes =
                SfcProviderScheduleTypeAPI.readAllServiceFunctionScheduleTypes();
        List<ServiceFunctionSchedulerType> sfScheduleTypeList =
                serviceFunctionSchedulerTypes.getServiceFunctionSchedulerType();
        for (ServiceFunctionSchedulerType serviceFunctionSchedulerType : sfScheduleTypeList) {
            if (serviceFunctionSchedulerType.isEnabled()) {
                ret = serviceFunctionSchedulerType;
                break;
            }
        }
        printTraceStop(LOG);
        return ret;
    }

    public static boolean putServiceFunctionScheduleType(ServiceFunctionSchedulerType serviceFunctionSchedulerType) {
        boolean ret;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionSchedulerType> sfstEntryIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                    .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerType.getKey())
                    .build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sfstEntryIID, serviceFunctionSchedulerType,
                LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    public static boolean deleteServiceFunctionScheduleType(
            Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerType) {
        printTraceStart(LOG);
        boolean ret = false;

        ServiceFunctionSchedulerTypeKey serviceFunctionSchedulerTypeKey =
                new ServiceFunctionSchedulerTypeKey(serviceFunctionSchedulerType);
        InstanceIdentifier<ServiceFunctionSchedulerType> sfstIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                    .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerTypeKey)
                    .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfstIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to delete Service Function Schedule Type: {}", serviceFunctionSchedulerType);
        }

        printTraceStop(LOG);
        return ret;
    }

    public static ServiceFunctionSchedulerType readServiceFunctionScheduleType(
            Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerTypeIdentity) {
        printTraceStart(LOG);
        ServiceFunctionSchedulerType serviceFunctionSchedulerType;

        InstanceIdentifier<ServiceFunctionSchedulerType> sfstIID;
        ServiceFunctionSchedulerTypeKey serviceFunctionSchedulerTypeKey =
                new ServiceFunctionSchedulerTypeKey(serviceFunctionSchedulerTypeIdentity);

        sfstIID = InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
            .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerTypeKey)
            .build();

        serviceFunctionSchedulerType = SfcDataStoreAPI.readTransactionAPI(sfstIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return serviceFunctionSchedulerType;
    }

    public static ServiceFunctionSchedulerTypes readAllServiceFunctionScheduleTypes() {
        ServiceFunctionSchedulerTypes serviceFunctionSchedulerTypes;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionSchedulerTypes> schedulerTypesIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class).build();

        serviceFunctionSchedulerTypes =
                SfcDataStoreAPI.readTransactionAPI(schedulerTypesIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return serviceFunctionSchedulerTypes;
    }
}
