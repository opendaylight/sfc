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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This class has the APIs to operate on the ServiceFunctionChain
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
public class SfcProviderServiceChainAPI implements Runnable {

    private ServiceFunctionChain serviceFunctionChain;
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPI.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private String methodName = null;
    private Object[] parameters;
    private Class[] parameterTypes;

    SfcProviderServiceChainAPI (Object[] params, String m) {
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

    SfcProviderServiceChainAPI (Object[] params, Class[] paramsTypes, String m) {
        this.methodName = m;
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        this.parameters = Arrays.copyOf(params, params.length);
        this.parameterTypes = Arrays.copyOf(paramsTypes, paramsTypes.length);
    }

    public static  SfcProviderServiceChainAPI getRemoveServiceFunctionFromChain (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceChainAPI(params, paramsTypes, "removeServiceFunctionFromChain");
    }

    public static void addPathtoServiceFunctionChain(ServiceFunctionChain serviceFunctionChain,
                                                     ServiceFunctionPath serviceFunctionPath) {

        ServiceFunctionChainKey serviceFunctionChainKey = new ServiceFunctionChainKey(serviceFunctionChain.getName());
        InstanceIdentifier sfcIID = InstanceIdentifier.builder(ServiceFunctionChains.class)
                .child(ServiceFunctionChain.class, serviceFunctionChainKey)
                .build();
        ServiceFunctionChainBuilder serviceFunctionChainBuilder = new ServiceFunctionChainBuilder();
        serviceFunctionChainBuilder.setName(serviceFunctionChain.getName());
        List<String> sfcServiceFunctionPathArrayList = serviceFunctionChain.getSfcServiceFunctionPath();
        sfcServiceFunctionPathArrayList.add(serviceFunctionPath.getName());
        serviceFunctionChainBuilder.setSfcServiceFunctionPath(sfcServiceFunctionPathArrayList);
        final DataModificationTransaction dataModificationTransaction = odlSfc.dataProvider
                .beginTransaction();
        dataModificationTransaction.putConfigurationData(sfcIID, serviceFunctionChainBuilder.build());

        try {
            dataModificationTransaction.commit().get();
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Failed to add Path to Service Function Chain", e);
        }

    }

    public static ServiceFunctionChain readServiceFunctionChain(String serviceFunctionChainName) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionChain> sfcIID;
        ServiceFunctionChainKey serviceFunctionChainKey = new ServiceFunctionChainKey(serviceFunctionChainName);
        sfcIID = InstanceIdentifier.builder(ServiceFunctionChains.class)
                .child(ServiceFunctionChain.class, serviceFunctionChainKey).build();

        DataObject dataObject = odlSfc.dataProvider.readConfigurationData(sfcIID);
        if (dataObject instanceof ServiceFunctionChain) {
            LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return (ServiceFunctionChain) dataObject;
        } else {
            LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }
    /*
     * We will remove the ServiceFunction from all existing Chains
     */
    public static void removeServiceFunctionFromChain (ServiceFunction serviceFunction) {

        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionChains serviceFunctionChains = getServiceFunctionChainsRef();
        if (serviceFunctionChains != null) {
            List<ServiceFunctionChain> serviceFunctionChainList = serviceFunctionChains.getServiceFunctionChain();
            InstanceIdentifier<SfcServiceFunction> sfIID;
            SfcServiceFunctionKey serviceFunctionKey;
            for (ServiceFunctionChain serviceFunctionChain : serviceFunctionChainList) {
                serviceFunctionKey = new SfcServiceFunctionKey(serviceFunction.getName());
                sfIID = InstanceIdentifier.builder(ServiceFunctionChains.class)
                        .child(ServiceFunctionChain.class, serviceFunctionChain.getKey())
                        .child(SfcServiceFunction.class, serviceFunctionKey).build();
                final DataModificationTransaction t = odlSfc.dataProvider
                        .beginTransaction();
                t.removeConfigurationData(sfIID);
                try {
                    t.commit().get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("removeServiceFunctionFromChain failed", e);
                }
            }
        } else {
            LOG.warn("No Service Function Chains configured");
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    public static ServiceFunctionChains getServiceFunctionChainsRef () {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionChains> sfcsIID;
        sfcsIID = InstanceIdentifier.builder(ServiceFunctionChains.class).build();

        DataObject dataObject = odlSfc.dataProvider.readConfigurationData(sfcsIID);
        if (dataObject instanceof ServiceFunctionChains) {
            ServiceFunctionChains serviceFunctionChains = (ServiceFunctionChains) dataObject;
            LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionChains;
        } else {
            LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }

    @Override
    public void run() {
        if (methodName != null) {
            //Class[] parameterTypes = {ServiceFunctionChain.class};
            Class c = this.getClass();
            Method method = null;
            try {
                method = c.getDeclaredMethod(methodName, parameterTypes);
                method.invoke(this, parameters);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

    }
}
