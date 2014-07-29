/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * This class has the APIs to operate on the ServiceFunctionType
 * datastore.
 *
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 *
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderServiceTypeAPI implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceTypeAPI.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private String methodName = null;
    private Class[] parameterTypes;
    Object[] parameters;

    SfcProviderServiceTypeAPI (Object[] params, String m) {
        int i = 0;
        this.methodName = m;
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        this.parameters = Arrays.copyOf(params, params.length);
        for (Object obj : parameters) {
            this.parameterTypes[i] = obj.getClass();
            i++;
        }

    }

    SfcProviderServiceTypeAPI (Object[] params, Class[] paramsTypes, String m) {
        this.methodName = m;
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        this.parameters = Arrays.copyOf(params, params.length);
        this.parameterTypes = Arrays.copyOf(paramsTypes, paramsTypes.length);
    }

    public static  SfcProviderServiceTypeAPI getCreateServiceFunctionToServiceType(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "createServiceFunctionTypeEntry");
    }

    public static  SfcProviderServiceTypeAPI getDeleteServiceFunctionFromServiceType (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "deleteServiceFunctionTypeEntry");
    }


    public void createServiceFunctionTypeEntry (ServiceFunction serviceFunction) {


        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        String sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child

        SftServiceFunctionNameKey sftServiceFunctionNameKey = new SftServiceFunctionNameKey(serviceFunction.getName());

        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
               .child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class,sftServiceFunctionNameKey).build();


        // Create a item in the list keyed by service function name
        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();
        sftServiceFunctionNameBuilder = sftServiceFunctionNameBuilder.setName(serviceFunction.getName());
        SftServiceFunctionName sftServiceFunctionName = sftServiceFunctionNameBuilder.build();

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                sftentryIID, sftServiceFunctionName, true);
        writeTx.commit();

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }

    public void deleteServiceFunctionTypeEntry (ServiceFunction serviceFunction) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        String sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey  serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child
        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        SftServiceFunctionNameKey sftServiceFunctionNameKey = new SftServiceFunctionNameKey(serviceFunction.getName());
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class).
                child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class,sftServiceFunctionNameKey).build();

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                sftentryIID);
        writeTx.commit();

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    public static ServiceFunctionType getServiceFunctionTypeList (String type) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionType> sftListIID;
        ServiceFunctionTypeKey serviceFunctionTypeKey;
        serviceFunctionTypeKey = new ServiceFunctionTypeKey(type);

            /*
             * We iterate thorough the list of service function types and for each one we get a suitable
             * Service Function
             */
        sftListIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey).build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionType> serviceFunctionTypeObject = null;
        try {
            serviceFunctionTypeObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sftListIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (serviceFunctionTypeObject instanceof ServiceFunctionType) {
            ServiceFunctionType serviceFunctionType = (ServiceFunctionType) serviceFunctionTypeObject;
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionType;
        } else {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }

    @Override
    public void run() {
        if (methodName != null) {
            Class<?> c = this.getClass();
            Method method;
            try {
                method = c.getDeclaredMethod(methodName, parameterTypes);
                method.invoke(this, parameters);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

    }
}
