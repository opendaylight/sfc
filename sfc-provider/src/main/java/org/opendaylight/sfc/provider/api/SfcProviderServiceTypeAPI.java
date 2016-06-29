/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class has the APIs to operate on the ServiceFunctionType
 * datastore.
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 * @since 2014-06-30
 */
public class SfcProviderServiceTypeAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceTypeAPI.class);

    /**
     * This method creates a Service function Type entry from a Service
     * Function.
     *
     * @param serviceFunction Service Function Object
     * @return true if service type was created, false otherwise
     */
    public static boolean createServiceFunctionTypeEntry(ServiceFunction serviceFunction) {

        printTraceStart(LOG);

        boolean ret = false;
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(serviceFunction.getType());

        // Build the instance identifier all the way down to the bottom child

        // TODO As part of typedef refactoring, not messing with SFT's

        SfName sfName = new SfName(serviceFunction.getName().getValue());

        SftServiceFunctionNameKey sftServiceFunctionNameKey =
                new SftServiceFunctionNameKey(sfName);

        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
            .child(ServiceFunctionType.class, serviceFunctionTypeKey)
            .child(SftServiceFunctionName.class, sftServiceFunctionNameKey)
            .build();

        // Create a item in the list keyed by service function name
        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();
        sftServiceFunctionNameBuilder = sftServiceFunctionNameBuilder.setName(sfName);
        SftServiceFunctionName sftServiceFunctionName = sftServiceFunctionNameBuilder.build();


        if (SfcDataStoreAPI.writeMergeTransactionAPI(sftentryIID, sftServiceFunctionName,
                LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to create Service Function Type for Service Function: {}", serviceFunction.getName());
        }
        printTraceStop(LOG);
        return ret;
    }

    public static boolean putServiceFunctionType(ServiceFunctionType sft) {
        boolean ret;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionType> sftEntryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
            .child(ServiceFunctionType.class, sft.getKey())
            .build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sftEntryIID, sft, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method is used to retrieve a Service Function Type from the DataStore
     *
     * @param serviceFunctionType Service Function Type abstract class
     * @return Service Function Type Object which contains a list of SF of this type
     */
    public static ServiceFunctionType readServiceFunctionType(SftTypeName serviceFunctionType) {
        printTraceStart(LOG);
        ServiceFunctionType sft;
        InstanceIdentifier<ServiceFunctionType> sftIID;
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(serviceFunctionType);
        sftIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
            .child(ServiceFunctionType.class, serviceFunctionTypeKey)
            .build();
        sft = SfcDataStoreAPI.readTransactionAPI(sftIID, LogicalDatastoreType.CONFIGURATION);
        if (sft == null) {
            LOG.error("Could not read Service Function list for Type {} " + "", serviceFunctionType);
        }
        printTraceStop(LOG);
        return sft;
    }

    /**
     * This method is used to delete a Service Function entry from the
     * Service Function Type list
     *
     * @param serviceFunction Service Function object
     * @return Service Function Type Object
     */
    public static boolean deleteServiceFunctionTypeEntry(ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        boolean ret = false;
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(serviceFunction.getType());

        // Build the instance identifier all the way down to the bottom child
        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        // TODO as part of typedef refactoring not messing with SFTs
        SfName sfName = new SfName(serviceFunction.getName().getValue());
        SftServiceFunctionNameKey sftServiceFunctionNameKey =
                new SftServiceFunctionNameKey(sfName);
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
            .child(ServiceFunctionType.class, serviceFunctionTypeKey)
            .child(SftServiceFunctionName.class, sftServiceFunctionNameKey)
            .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sftentryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to delete Service Function Type: {}, for Service Function: {}", serviceFunction.getType(),
                    serviceFunction.getName());
        }
        List<SftServiceFunctionName> sftServiceFunctionNameList =
                readServiceFunctionType(serviceFunction.getType()).getSftServiceFunctionName();
        if (sftServiceFunctionNameList != null) {
            LOG.debug("List size for service type {} is: {}", serviceFunction.getType(),
                    sftServiceFunctionNameList.size());
            if (sftServiceFunctionNameList.size() == 0) {
                deleteServiceFunctionType(serviceFunction.getType());
            }
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Delete a ServiceFunctionType based on SftType (key)
     */

    public static boolean deleteServiceFunctionType(SftTypeName sftType) {
        ServiceFunctionType serviceFunctionType = readServiceFunctionType(sftType);
        if (serviceFunctionType != null) {
            return deleteServiceFunctionType(serviceFunctionType);
        }
        LOG.warn("deleteServiceFunctionType: Could not find ServiceFunctionType for {}", sftType);
        return false;
    }

    /**
     * This method is used to delete all Service Function names under a
     * Service Function Type list. It basically removes the list of all service
     * functions of a given type. The Service Functions themselves are not touched
     * by this function.
     *
     * @param serviceFunctionType Service Function Type abstract class
     * @return Service Function Type Object
     */
    public static boolean deleteServiceFunctionType(ServiceFunctionType serviceFunctionType) {
        printTraceStart(LOG);
        boolean ret = false;

        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(serviceFunctionType.getType());
        InstanceIdentifier<ServiceFunctionType> sftEntryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
            .child(ServiceFunctionType.class, serviceFunctionTypeKey)
            .build();
        if (SfcDataStoreAPI.deleteTransactionAPI(sftEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to delete Service Type: {}", serviceFunctionType);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads a Service function Type entry from a Service
     * Function.
     *
     * @param serviceFunction Service Function Object
     * @return Nothing.
     */
    public static SftServiceFunctionName readServiceFunctionTypeEntry(ServiceFunction serviceFunction) {

        printTraceStart(LOG);

        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(serviceFunction.getType());

        // Build the instance identifier all the way down to the bottom child
        // TODO as part of typedef refactoring not messing with SFTs
        SfName sfName = new SfName(serviceFunction.getName().getValue());
        SftServiceFunctionNameKey sftServiceFunctionNameKey =
                new SftServiceFunctionNameKey(sfName);

        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
            .child(ServiceFunctionType.class, serviceFunctionTypeKey)
            .child(SftServiceFunctionName.class, sftServiceFunctionNameKey)
            .build();

        printTraceStop(LOG);
        return (SfcDataStoreAPI.readTransactionAPI(sftentryIID, LogicalDatastoreType.CONFIGURATION));
    }
}
