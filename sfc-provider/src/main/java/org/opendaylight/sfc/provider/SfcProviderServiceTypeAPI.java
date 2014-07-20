/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
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

/*
    public static  SfcProviderServiceTypeAPI getSfcProviderDeleteServiceFunctionFromServiceType (Object[] params) {
        return new SfcProviderServiceTypeAPI(params, "deleteServiceFunctionTypeEntry");
    }

    public static  SfcProviderServiceTypeAPI getSfcProviderCreateServiceFunctionToServiceType(Object[] params) {
        return new SfcProviderServiceTypeAPI(params, "createServiceFunctionTypeEntry");
    }

    */
    public static  SfcProviderServiceTypeAPI getCreateServiceFunctionToServiceType(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "createServiceFunctionTypeEntry");
    }

    public static  SfcProviderServiceTypeAPI getDeleteServiceFunctionFromServiceType (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceTypeAPI(params, paramsTypes, "deleteServiceFunctionTypeEntry");
    }


    @Override
    public void run() {
        if (methodName != null) {
            //Class[] parameterTypes = {ServiceFunctionChain.class};
            Class c = this.getClass();
            Method method = null;
            try {
                method = c.getDeclaredMethod(methodName, parameterTypes);
                method.invoke (this, parameters);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
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

        final DataModificationTransaction t = odlSfc.dataProvider
                .beginTransaction();
        t.putConfigurationData(sftentryIID, sftServiceFunctionName);

        try {
            t.commit().get();
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Failed to create Service Function entry in Service Type List", e);
        }

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

        final DataModificationTransaction t = odlSfc.dataProvider
                .beginTransaction();
        t.removeConfigurationData(sftentryIID);
        try {
            t.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to delete Service Function entry in Service Type List failed", e);
        }
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
        DataObject dataObject = odlSfc.dataProvider.readConfigurationData(sftListIID);
        if (dataObject instanceof ServiceFunctionType) {
            ServiceFunctionType serviceFunctionType = (ServiceFunctionType) dataObject;
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionType;
        } else {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }


    }
}
