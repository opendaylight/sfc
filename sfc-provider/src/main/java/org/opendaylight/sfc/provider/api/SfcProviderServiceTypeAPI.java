/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class has the APIs to operate on the ServiceFunctionType
 * datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 * <p/>
 * @since 2014-06-30
 */
public class SfcProviderServiceTypeAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceTypeAPI.class);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();
    private static final String FAILED_TO_STR = "failed to ...";

    SfcProviderServiceTypeAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceTypeAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderServiceTypeAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "putServiceFunctionType");
    }

    public static SfcProviderServiceTypeAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "readServiceFunctionType");
    }

    public static SfcProviderServiceTypeAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "deleteServiceFunctionType");
    }

    public static SfcProviderServiceTypeAPI getPutAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "putAllServiceFunctionTypes");
    }

    public static SfcProviderServiceTypeAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "readAllServiceFunctionTypes");
    }

    public static SfcProviderServiceTypeAPI getDeleteAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "deleteAllServiceFunctionTypes");
    }

    public static SfcProviderServiceTypeAPI getCreateServiceFunctionTypeEntry(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "createServiceFunctionTypeEntry");
    }

    public static SfcProviderServiceTypeAPI getDeleteServiceFunctionTypeEntry(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "deleteServiceFunctionTypeEntry");
    }

    public static SfcProviderServiceTypeAPI getDeleteServiceFunctionFromServiceType(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "deleteServiceFunctionTypeEntry");
    }

    public static boolean putServiceFunctionTypeExecutor(ServiceFunctionType sft) {

        printTraceStart(LOG);
        boolean ret = false;
        Object[] serviceTypeObj = {sft};
        Class[] serviceTypeClass = {ServiceFunctionType.class};
        SfcProviderServiceTypeAPI sfcProviderServiceTypeAPI = SfcProviderServiceTypeAPI
                .getPut(serviceTypeObj, serviceTypeClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderServiceTypeAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getRead: {}", future.get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("failed to ....", e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads the operational state for a service function.
     * <p/>
     *
     * @param serviceFunctionTypeIdentity SF name
     * @return A ServiceFunctionType object
     */
    public static ServiceFunctionType readServiceFunctionTypeExecutor(Class<? extends ServiceFunctionTypeIdentity> serviceFunctionTypeIdentity) {

        printTraceStart(LOG);
        ServiceFunctionType ret = null;
        Object[] serviceTypeObj = {serviceFunctionTypeIdentity};
        Class[] serviceTypeClass = {Class.class};
        SfcProviderServiceTypeAPI sfcProviderServiceTypeAPI = SfcProviderServiceTypeAPI
                .getRead(serviceTypeObj, serviceTypeClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderServiceTypeAPI);
        try {
            ret = (ServiceFunctionType) future.get();
            LOG.debug("getRead: {}", future.get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("failed to ....", e);
        }
        printTraceStop(LOG);
        return ret;
    }

    public static boolean deleteServiceFunctionTypeEntryExecutor(ServiceFunction serviceFunction) {
        boolean ret = false;
        Object[] serviceTypeObj = {serviceFunction};
        Class[] serviceTypeClass = {ServiceFunction.class};
        SfcProviderServiceTypeAPI sfcProviderServiceTypeAPI = SfcProviderServiceTypeAPI
                .getDeleteServiceFunctionTypeEntry(serviceTypeObj, serviceTypeClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderServiceTypeAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServiceFunctionTypeEntry: {}", future.get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn(FAILED_TO_STR, e);
        }
        return ret;
    }

    public static boolean createServiceFunctionTypeEntryExecutor(ServiceFunction serviceFunction) {
        boolean ret = false;
        Object[] serviceTypeObj = {serviceFunction};
        Class[] serviceTypeClass = {ServiceFunction.class};
        SfcProviderServiceTypeAPI sfcProviderServiceTypeAPI = SfcProviderServiceTypeAPI
                .getCreateServiceFunctionTypeEntry(serviceTypeObj, serviceTypeClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderServiceTypeAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getCreateRenderedServicePathEntryAPI: {}", future.get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn(FAILED_TO_STR, e);
        }
        return ret;
    }

    protected boolean putServiceFunctionType(ServiceFunctionType sft) {
        boolean ret;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionType> sftEntryIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class)
                        .child(ServiceFunctionType.class, sft.getKey()).build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sftEntryIID, sft, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method is used to retrieve a Service Function Type from the DataStore
     * <p/>
     *
     * @param serviceFunctionType Service Function Type abstract class
     * @return Service Function Type Object which contains a list of SF of this type
     */
    public static ServiceFunctionType readServiceFunctionType(Class<? extends ServiceFunctionTypeIdentity> serviceFunctionType) {
        printTraceStart(LOG);
        ServiceFunctionType sft;
        InstanceIdentifier<ServiceFunctionType> sftIID;
        ServiceFunctionTypeKey serviceFunctionTypeKey = new
                ServiceFunctionTypeKey(serviceFunctionType);
        sftIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey).build();
        sft = SfcDataStoreAPI.readTransactionAPI(sftIID, LogicalDatastoreType.CONFIGURATION);
        if (sft == null) {
            LOG.error("Could not read Service Function list for Type {} " +
                    "", serviceFunctionType);
        }
        printTraceStop(LOG);
        return sft;
    }

    /**
     * This method is used to delete a Service Function entry from the
     * Service Function Type list
     * <p/>
     *
     * @param serviceFunction Service Function object
     * @return Service Function Type Object
     */
    public boolean deleteServiceFunctionTypeEntry(ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        boolean ret = false;
        Class<? extends ServiceFunctionTypeIdentity> sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child
        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        SftServiceFunctionNameKey sftServiceFunctionNameKey =
                new SftServiceFunctionNameKey(serviceFunction.getName());
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class, sftServiceFunctionNameKey).build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sftentryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to delete Service Function Type: {}, for Service Function: {}",
                    serviceFunction.getType(), serviceFunction.getName());
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
     * This method is used to delete all Service Function names under a
     * Service Function Type list. It basically removes the list of all service
     * functions of a given type. The Service Functions themselves are not touched
     * by this function.
     * <p/>
     *
     * @param serviceFunctionType Service Function Type abstract class
     * @return Service Function Type Object
     */
    public boolean deleteServiceFunctionType(Class<? extends ServiceFunctionTypeIdentity> serviceFunctionType) {
        printTraceStart(LOG);
        boolean ret = false;


        ServiceFunctionTypeKey serviceFunctionTypeKey = new
                ServiceFunctionTypeKey(serviceFunctionType);
        InstanceIdentifier<ServiceFunctionType> sftEntryIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class)
                        .child(ServiceFunctionType.class, serviceFunctionTypeKey).build();
        if (SfcDataStoreAPI.deleteTransactionAPI(sftEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to delete Service Type: {}", serviceFunctionType);
        }
        printTraceStop(LOG);
        return ret;
    }

    protected boolean putAllServiceFunctionTypes(ServiceFunctionTypes sfts) {
        boolean ret;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionTypes> sftsIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class).build();
        ret = SfcDataStoreAPI.writePutTransactionAPI(sftsIID, sfts, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads and returns an object with all Service Function Types
     * present in the Data Store
     * <p/>
     *
     * @return Nothing.
     */
    protected ServiceFunctionTypes readAllServiceFunctionTypes() {
        ServiceFunctionTypes sfts;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionTypes> sftsIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

        sfts = SfcDataStoreAPI.readTransactionAPI(sftsIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return sfts;
    }

    /**
     * Delete all Service Function Types from data store
     * <p/>
     *
     * @return Nothing.
     */
    protected boolean deleteAllServiceFunctionTypes() {
        boolean ret;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionTypes> sftsIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

        ret = SfcDataStoreAPI.deleteTransactionAPI(sftsIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method creates a Service function Type entry from a Service
     * Function.
     * <p/>
     *
     * @param serviceFunction Service Function Object
     * @return Nothing.
     */
    public boolean createServiceFunctionTypeEntry(ServiceFunction serviceFunction) {

        printTraceStart(LOG);

        boolean ret = false;
        Class<? extends ServiceFunctionTypeIdentity> sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child

        SftServiceFunctionNameKey sftServiceFunctionNameKey =
                new SftServiceFunctionNameKey(serviceFunction.getName());

        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class, sftServiceFunctionNameKey).build();

        // Create a item in the list keyed by service function name
        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder =
                new SftServiceFunctionNameBuilder();
        sftServiceFunctionNameBuilder = sftServiceFunctionNameBuilder
                .setName(serviceFunction.getName());
        SftServiceFunctionName sftServiceFunctionName =
                sftServiceFunctionNameBuilder.build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(sftentryIID, sftServiceFunctionName, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to create Service Function Type for Service Function: {}", serviceFunction.getName());
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads a Service function Type entry from a Service
     * Function.
     * <p/>
     *
     * @param serviceFunction Service Function Object
     * @return Nothing.
     */
    public static SftServiceFunctionName readServiceFunctionTypeEntry(ServiceFunction serviceFunction) {

        printTraceStart(LOG);

        boolean ret = false;
        Class<? extends ServiceFunctionTypeIdentity> sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child

        SftServiceFunctionNameKey sftServiceFunctionNameKey =
                new SftServiceFunctionNameKey(serviceFunction.getName());

        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class, sftServiceFunctionNameKey).build();

        // Create a item in the list keyed by service function name
//        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder =
//                new SftServiceFunctionNameBuilder();
//        sftServiceFunctionNameBuilder = sftServiceFunctionNameBuilder
//                .setName(serviceFunction.getName());
//        SftServiceFunctionName sftServiceFunctionName =
//                sftServiceFunctionNameBuilder.build();

        printTraceStop(LOG);
        return (SfcDataStoreAPI.readTransactionAPI(sftentryIID, LogicalDatastoreType.CONFIGURATION));
    }


}
