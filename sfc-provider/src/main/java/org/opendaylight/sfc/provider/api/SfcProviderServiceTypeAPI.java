/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

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
 * <p/>
 * <p/>
 * @since 2014-06-30
 */
public class SfcProviderServiceTypeAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceTypeAPI.class);

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

    public static SfcProviderServiceTypeAPI getCreateServiceFunctionToServiceType(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "createServiceFunctionTypeEntry");
    }

    public static SfcProviderServiceTypeAPI getDeleteServiceFunctionFromServiceType(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "deleteServiceFunctionTypeEntry");
    }

    protected boolean putServiceFunctionType(ServiceFunctionType sft) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionType> sftEntryIID =
                    InstanceIdentifier.builder(ServiceFunctionTypes.class)
                    .child(ServiceFunctionType.class, sft.getKey()).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sftEntryIID, sft, true);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method is used to retrieve a Service Function Type from the DataStore
     * <p>
     * @param serviceFunctionType Service Function Type abstract class
     * @return Service Function Type Object
     */
    protected ServiceFunctionType readServiceFunctionType(Class<? extends ServiceFunctionTypeIdentity> serviceFunctionType) {
        printTraceStart(LOG);
        ServiceFunctionType sft = null;
        InstanceIdentifier<ServiceFunctionType> sftIID;
        ServiceFunctionTypeKey serviceFunctionTypeKey = new
                ServiceFunctionTypeKey(serviceFunctionType);
        sftIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey).build();

        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<ServiceFunctionType> serviceFunctionChainDataObject = null;
        try {
            serviceFunctionChainDataObject = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, sftIID).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read Service Function list for Type {} " +
                    "", serviceFunctionType);
        }
        if (serviceFunctionChainDataObject != null
                && serviceFunctionChainDataObject.isPresent()) {
            sft = serviceFunctionChainDataObject.get();
        }

        printTraceStop(LOG);
        return sft;
    }

    /**
     * This method is used to delete a Service Function Type from the DataStore
     * <p>
     * @param serviceFunctionType Service Function Type abstract class
     * @return Service Function Type Object
     */
    protected boolean deleteServiceFunctionType(Class<? extends ServiceFunctionTypeIdentity> serviceFunctionType) {
        boolean ret;
        printTraceStart(LOG);
        ServiceFunctionTypeKey serviceFunctionTypeKey = new
                ServiceFunctionTypeKey(serviceFunctionType);
        InstanceIdentifier<ServiceFunctionType> sftEntryIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey).toInstance();

        SfcDataStoreCallback sfcDataStoreCallback = new SfcDataStoreCallback();
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, sftEntryIID);

        CheckedFuture<Void,TransactionCommitFailedException> submitFuture = writeTx.submit();
        Futures.addCallback(submitFuture, sfcDataStoreCallback);
        if (sfcDataStoreCallback.getTransactioSuccessful()) {
            ret = true;
        } else {
            ret = false;
            LOG.error("Failed to create Service Function Type entry for name: {}",
                    serviceFunctionType);
        }

        printTraceStop(LOG);
        return ret;
    }

    protected boolean putAllServiceFunctionTypes(ServiceFunctionTypes sfts) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionTypes> sftsIID =
                    InstanceIdentifier.builder(ServiceFunctionTypes.class).toInstance();
            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, sftsIID, sfts);
            writeTx.commit();
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads and returns an object with all Service Function Types
     * present in the Data Store
     * <p>
     * @param
     * @return Nothing.
     */
    protected ServiceFunctionTypes readAllServiceFunctionTypes() {
        ServiceFunctionTypes sfts = null;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionTypes> sftsIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class).toInstance();


        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<ServiceFunctionTypes> serviceFunctionTypesDataObject;
        try {
            serviceFunctionTypesDataObject = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, sftsIID).get();
            if (serviceFunctionTypesDataObject != null
                    && serviceFunctionTypesDataObject.isPresent()) {
                sfts = serviceFunctionTypesDataObject.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read All Service Function Types");
        }

        printTraceStop(LOG);
        return sfts;
    }

    /**
     * Delete all Service Function Types from data store
     * <p>
     * @param
     * @return Nothing.
     */
    protected boolean deleteAllServiceFunctionTypes() {
        boolean ret = false;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionTypes> sftsIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class).toInstance();
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                sftsIID);
        SfcDataStoreCallback sfcDataStoreCallback = new SfcDataStoreCallback();
        CheckedFuture<Void,TransactionCommitFailedException> submitFuture = writeTx.submit();
        Futures.addCallback(submitFuture, sfcDataStoreCallback);
        if (sfcDataStoreCallback.getTransactioSuccessful()) {
            ret = true;
        } else {
            LOG.error("Could not delete all Service Function Types");
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * This method creates a Service function Type entry from a Service
     * Function.
     * <p>
     * @param serviceFunction Service Function Object
     * @return Nothing.
     */
    public void createServiceFunctionTypeEntry(ServiceFunction serviceFunction) {

        printTraceStart(LOG);

        Class<? extends ServiceFunctionTypeIdentity> sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        if (readAllServiceFunctionTypes() == null) {
            InstanceIdentifier<ServiceFunctionTypes> sftIID;
            sftIID = InstanceIdentifier.builder(ServiceFunctionTypes.class).build();
            ServiceFunctionTypesBuilder serviceFunctionTypesBuilder = new ServiceFunctionTypesBuilder();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.CONFIGURATION,
                    sftIID, serviceFunctionTypesBuilder.build());
            //writeTx.commit();
            SfcDataStoreCallback sfcDataStoreCallback = new SfcDataStoreCallback();
            CheckedFuture<Void,TransactionCommitFailedException> submitFuture = writeTx.submit();
            Futures.addCallback(submitFuture, sfcDataStoreCallback);
            if (!sfcDataStoreCallback.getTransactioSuccessful()) {
                LOG.error("Failed to create top level Service Function Type object");

            }

        }

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

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                sftentryIID, sftServiceFunctionName, true);
        //writeTx.commit();
        SfcDataStoreCallback sfcDataStoreCallback = new SfcDataStoreCallback();
        CheckedFuture<Void,TransactionCommitFailedException> submitFuture = writeTx.submit();
        Futures.addCallback(submitFuture, sfcDataStoreCallback);
        if (!sfcDataStoreCallback.getTransactioSuccessful()) {
            LOG.warn("Failed to create Service Function Type entry for SF: {}",
                    serviceFunction.getName());

        }

        printTraceStop(LOG);

    }

    /**
     * This method is used to delete a Service Function Type from the Data Store
     * that was instantiated from a Service Function passed as a parameter
     * <p>
     * @param serviceFunction Service Function object
     * @return Service Function Type Object
     */
    public void deleteServiceFunctionTypeEntry(ServiceFunction serviceFunction) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        Class<? extends ServiceFunctionTypeIdentity> sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child
        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        SftServiceFunctionNameKey sftServiceFunctionNameKey =
                new SftServiceFunctionNameKey(serviceFunction.getName());
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class, sftServiceFunctionNameKey).build();

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                sftentryIID);
        SfcDataStoreCallback sfcDataStoreCallback = new SfcDataStoreCallback();
        CheckedFuture<Void,TransactionCommitFailedException> submitFuture = writeTx.submit();
        Futures.addCallback(submitFuture, sfcDataStoreCallback);

        printTraceStop(LOG);
    }
}
