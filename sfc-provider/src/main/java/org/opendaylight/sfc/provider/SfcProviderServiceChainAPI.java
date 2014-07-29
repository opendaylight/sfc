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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static  SfcProviderServiceChainAPI getAddChainToChainState (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceChainAPI(params, paramsTypes, "addChainToChainState");
    }

    @SuppressWarnings("unused")
    private void addChainToChainState (ServiceFunctionChain serviceFunctionChain) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionChainStateKey serviceFunctionChainStateKey = new ServiceFunctionChainStateKey(serviceFunctionChain.getName());
        InstanceIdentifier<ServiceFunctionChainState> sfcoIID = InstanceIdentifier.builder(ServiceFunctionChainsState.class)
                .child(ServiceFunctionChainState.class, serviceFunctionChainStateKey).build();

        ServiceFunctionChainStateBuilder serviceFunctionChainStateBuilder = new ServiceFunctionChainStateBuilder();
        serviceFunctionChainStateBuilder.setName(serviceFunctionChain.getName());

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.OPERATIONAL,
                sfcoIID, serviceFunctionChainStateBuilder.build(), true);
        writeTx.commit();
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    public static void addPathToServiceFunctionChainState (ServiceFunctionChain serviceFunctionChain,
                                                     ServiceFunctionPath serviceFunctionPath) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionChainStateKey serviceFunctionChainStateKey = new ServiceFunctionChainStateKey(serviceFunctionChain.getName());
        InstanceIdentifier<ServiceFunctionChainState> sfcoIID = InstanceIdentifier.builder(ServiceFunctionChainsState.class)
                .child(ServiceFunctionChainState.class, serviceFunctionChainStateKey).build();

        ServiceFunctionChainStateBuilder serviceFunctionChainStateBuilder = new ServiceFunctionChainStateBuilder();
        ArrayList<String> sfcServiceFunctionPathArrayList = new ArrayList<>();
        sfcServiceFunctionPathArrayList.add(serviceFunctionPath.getName());
        serviceFunctionChainStateBuilder.setSfcServiceFunctionPath(sfcServiceFunctionPathArrayList);
        serviceFunctionChainStateBuilder.setName(serviceFunctionChain.getName());

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.OPERATIONAL,
                sfcoIID, serviceFunctionChainStateBuilder.build(), true);
        writeTx.commit();
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);


    }


    public static ServiceFunctionChain readServiceFunctionChain(String serviceFunctionChainName) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionChain> sfcIID;
        ServiceFunctionChainKey serviceFunctionChainKey = new ServiceFunctionChainKey(serviceFunctionChainName);
        sfcIID = InstanceIdentifier.builder(ServiceFunctionChains.class)
                .child(ServiceFunctionChain.class, serviceFunctionChainKey).build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionChain> serviceFunctionChainObject = null;
        try {
            serviceFunctionChainObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfcIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (serviceFunctionChainObject.get() instanceof ServiceFunctionChain) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionChainObject.get();
        } else {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }

    private InstanceIdentifier<SfcServiceFunction> getServiceFunctionIIDFromChain (ServiceFunctionChain sfc, ServiceFunction sf) {
        SfcServiceFunctionKey serviceFunctionKey = new SfcServiceFunctionKey(sf.getName());
        InstanceIdentifier<SfcServiceFunction> sfIID = InstanceIdentifier.builder(ServiceFunctionChains.class)
                .child(ServiceFunctionChain.class, sfc.getKey())
                .child(SfcServiceFunction.class, serviceFunctionKey).build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<SfcServiceFunction> serviceFunctionObject = null;
        try {
            serviceFunctionObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (serviceFunctionObject.get() instanceof SfcServiceFunction) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return sfIID;
        } else {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }


    public static ServiceFunctionChains getServiceFunctionChainsRef () {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionChains> sfcsIID;
        sfcsIID = InstanceIdentifier.builder(ServiceFunctionChains.class).build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionChains> serviceFunctionChainsObject = null;
        try {
            serviceFunctionChainsObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfcsIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (serviceFunctionChainsObject.get() instanceof ServiceFunctionChains) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionChainsObject.get();
        } else {
            LOG.error("\n########## Failed to get Service Function Chains reference: {}",
                    Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }

    public static ServiceFunctionChainsState getServiceFunctionChainsStateRef () {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionChainsState> sfcsIID;
        sfcsIID = InstanceIdentifier.builder(ServiceFunctionChainsState.class).build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionChainsState> serviceFunctionChainStateObject = null;
        try {
            serviceFunctionChainStateObject = readTx.read(LogicalDatastoreType.OPERATIONAL, sfcsIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (serviceFunctionChainStateObject.get() instanceof ServiceFunctionChainsState) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionChainStateObject.get();
        } else {
            LOG.error("\n########## Failed to get Service Function Chains reference: {}",
                    Thread.currentThread().getStackTrace()[1]);
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
