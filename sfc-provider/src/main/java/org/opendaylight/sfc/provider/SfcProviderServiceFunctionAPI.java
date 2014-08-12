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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This class has the APIs to operate on the ServiceFunction
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
public class SfcProviderServiceFunctionAPI implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceFunctionAPI.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private String methodName = null;
    private Object[] parameters;
    private Class[] parameterTypes;

    SfcProviderServiceFunctionAPI (Object[] params, Class[] paramsTypes, String m) {
        this.methodName = m;
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        this.parameters = Arrays.copyOf(params, params.length);
        this.parameterTypes = Arrays.copyOf(paramsTypes, paramsTypes.length);
    }

    public static  SfcProviderServiceFunctionAPI getDeleteServicePathFromServiceFunctionState (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI (params, paramsTypes, "deleteServicePathFromServiceFunctionState");
    }

    public static ServiceFunction readServiceFunction(String serviceFunctionName) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunction> sfIID;
        ServiceFunctionKey serviceFunctionKey = new ServiceFunctionKey(serviceFunctionName);
        sfIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, serviceFunctionKey).build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunction> serviceFunctiondataObject = null;
        try {
            serviceFunctiondataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (serviceFunctiondataObject != null &&
                (serviceFunctiondataObject.get() instanceof ServiceFunction)) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctiondataObject.get();
        } else {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }

    /*
     * When a Service Path is deleted directly (not as a consequence of deleting a SF), we need
     * to remove its reference from all the ServiceFunction states.
     */
    @SuppressWarnings("unused")
    public void deleteServicePathFromServiceFunctionState (ServiceFunctionPath serviceFunctionPath) {

        List<SfpServiceFunction>  sfpServiceFunctionList = serviceFunctionPath.getSfpServiceFunction();
        for (SfpServiceFunction sfpServiceFunction : sfpServiceFunctionList) {
            String serviceFunctionName = sfpServiceFunction.getName();
            ServiceFunctionState serviceFunctionState = readServiceFunctionState(serviceFunctionName);
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(serviceFunctionName);
            InstanceIdentifier<ServiceFunctionState> sfStateIID =
                    InstanceIdentifier.builder(ServiceFunctionsState.class)
                            .child(ServiceFunctionState.class, serviceFunctionStateKey)
                            .build();

            List<String> sfServiceFunctionPathList = serviceFunctionState.getSfServiceFunctionPath();
            List<String> newPathList = new ArrayList<>();
            newPathList.addAll(sfServiceFunctionPathList);
            newPathList.remove(serviceFunctionPath.getName());
            ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
            serviceFunctionStateBuilder.setName(serviceFunctionName);
            serviceFunctionStateBuilder.setSfServiceFunctionPath(newPathList);


            ReadWriteTransaction writeTx = odlSfc.dataProvider.newReadWriteTransaction();
            writeTx.put(LogicalDatastoreType.CONFIGURATION, sfStateIID, serviceFunctionStateBuilder.build(), true);
            writeTx.commit();
        }
    }


    public static ServiceFunctionState readServiceFunctionState (String serviceFunctionName) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        ServiceFunctionState serviceFunctionState;
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionState> dataSfcStateObject = null;
        try {
            dataSfcStateObject = readTx.read(LogicalDatastoreType.OPERATIONAL, sfStateIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (dataSfcStateObject != null &&
                (dataSfcStateObject.get() instanceof ServiceFunctionState)) {
            serviceFunctionState = dataSfcStateObject.get();
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionState;
        } else {
            LOG.error("\n########## Could not find Service Function State for service function {}",
                    serviceFunctionName);
            return null;
        }
    }

    public static void deleteServiceFunctionState (String serviceFunctionName) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);


        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.OPERATIONAL, sfStateIID);
        writeTx.commit();
    }

    /*
     * We add the path name to the operational store of each SF in the path.
     */
    public static void addPathToServiceFunctionState (ServiceFunctionPath serviceFunctionPath) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        ArrayList<String> sfcServiceFunctionPathArrayList = new ArrayList<>();
        sfcServiceFunctionPathArrayList.add(serviceFunctionPath.getName());

        serviceFunctionStateBuilder.setSfServiceFunctionPath(sfcServiceFunctionPathArrayList);

        List<SfpServiceFunction> sfpServiceFunctionList = serviceFunctionPath.getSfpServiceFunction();
        for (SfpServiceFunction sfpServiceFunction : sfpServiceFunctionList) {
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfpServiceFunction.getName());
            InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier.builder(ServiceFunctionsState.class)
                    .child(ServiceFunctionState.class, serviceFunctionStateKey).build();
            serviceFunctionStateBuilder.setName(sfpServiceFunction.getName());

            WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.OPERATIONAL,
                    sfStateIID, serviceFunctionStateBuilder.build(), true);
            writeTx.commit();

            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
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
