/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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

import java.util.concurrent.ExecutionException;

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
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionType> sftEntryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                    .child(ServiceFunctionType.class, sft.getKey()).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sftEntryIID, sft, true);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected ServiceFunctionType readServiceFunctionType(String serviceFunctionTypeName) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionType sft = null;
        InstanceIdentifier<ServiceFunctionType> sftIID;
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(serviceFunctionTypeName);
        sftIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey).build();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunctionType> serviceFunctionChainDataObject = null;
            try {
                serviceFunctionChainDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sftIID).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (serviceFunctionChainDataObject != null
                    && serviceFunctionChainDataObject.isPresent()) {
                sft = serviceFunctionChainDataObject.get();
            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return sft;
    }

    protected boolean deleteServiceFunctionType(String serviceFunctionTypeName) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(serviceFunctionTypeName);
        InstanceIdentifier<ServiceFunctionType> sftEntryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey).toInstance();

        if (dataBroker != null) {
            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sftEntryIID);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected boolean putAllServiceFunctionTypes(ServiceFunctionTypes sfts) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionTypes> sftsIID = InstanceIdentifier.builder(ServiceFunctionTypes.class).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, sftsIID, sfts);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected ServiceFunctionTypes readAllServiceFunctionTypes() {
        ServiceFunctionTypes sfts = null;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionTypes> sftsIID = InstanceIdentifier.builder(ServiceFunctionTypes.class).toInstance();

        if (odlSfc.getDataProvider() != null) {
            ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceFunctionTypes> serviceFunctionTypesDataObject = null;
            try {
                serviceFunctionTypesDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sftsIID).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (serviceFunctionTypesDataObject != null
                    && serviceFunctionTypesDataObject.isPresent()) {
                sfts = serviceFunctionTypesDataObject.get();
            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return sfts;
    }

    protected boolean deleteAllServiceFunctionTypes() {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunctionTypes> sftsIID = InstanceIdentifier.builder(ServiceFunctionTypes.class).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sftsIID);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    /*
    public static ServiceFunctionType getServiceFunctionTypeList(String typeName) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionType> sftListIID;
        ServiceFunctionTypeKey serviceFunctionTypeKey;
        serviceFunctionTypeKey = new ServiceFunctionTypeKey(typeName);

        sftListIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey).build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionType> serviceFunctionTypeObject = null;
        try {
            serviceFunctionTypeObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sftListIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (serviceFunctionTypeObject != null &&
                (serviceFunctionTypeObject.get() instanceof ServiceFunctionType)) {
            ServiceFunctionType serviceFunctionType = serviceFunctionTypeObject.get();
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionType;
        } else {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }
    */

    public void createServiceFunctionTypeEntry(ServiceFunction serviceFunction) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        String sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child

        SftServiceFunctionNameKey sftServiceFunctionNameKey = new SftServiceFunctionNameKey(serviceFunction.getName());

        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class, sftServiceFunctionNameKey).build();


        // Create a item in the list keyed by service function name
        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();
        sftServiceFunctionNameBuilder = sftServiceFunctionNameBuilder.setName(serviceFunction.getName());
        SftServiceFunctionName sftServiceFunctionName = sftServiceFunctionNameBuilder.build();

        WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                sftentryIID, sftServiceFunctionName, true);
        writeTx.commit();

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }

    public void deleteServiceFunctionTypeEntry(ServiceFunction serviceFunction) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        String sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child
        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        SftServiceFunctionNameKey sftServiceFunctionNameKey = new SftServiceFunctionNameKey(serviceFunction.getName());
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class, sftServiceFunctionNameKey).build();

        WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                sftentryIID);
        writeTx.commit();

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

}
