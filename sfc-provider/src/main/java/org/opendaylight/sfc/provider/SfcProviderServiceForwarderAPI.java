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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
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
public class SfcProviderServiceForwarderAPI implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceForwarderAPI.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private String methodName = null;
    private Object[] parameters;
    private Class[] parameterTypes;


    SfcProviderServiceForwarderAPI (Object[] params, String m) {
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

    SfcProviderServiceForwarderAPI (Object[] params, Class[] paramsTypes, String m) {
        this.methodName = m;
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        this.parameters = Arrays.copyOf(params, params.length);
        this.parameterTypes = Arrays.copyOf(paramsTypes, paramsTypes.length);
    }


    public static  SfcProviderServiceForwarderAPI getDeleteServiceFunctionFromForwarder(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI (params, paramsTypes, "deleteServiceFunctionFromForwarder");
    }

    public static  SfcProviderServiceForwarderAPI getCreateServiceForwarderAPI (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI (params, paramsTypes, "createServiceFunctionForwarder");
    }

    public static  SfcProviderServiceForwarderAPI getUpdateServiceForwarderAPI (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI (params, paramsTypes, "updateServiceFunctionForwarder");
    }




    public void createServiceFunctionForwarder (ServiceFunction serviceFunction) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionForwarder> sffIID;
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(serviceFunction.getServiceFunctionForwarder());
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .build();

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(serviceFunction.getServiceFunctionForwarder());

        ArrayList<ServiceFunctionDictionary> serviceFunctionDictionaryList = new ArrayList<>();
        ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder = new ServiceFunctionDictionaryBuilder();
        serviceFunctionDictionaryBuilder.setName(serviceFunction.getName()).setType(serviceFunction.getType())
                .setServiceFunctionForwarder(serviceFunction.getServiceFunctionForwarder())
                .setSfDataPlaneLocator(serviceFunction.getSfDataPlaneLocator());
        serviceFunctionDictionaryList.add(serviceFunctionDictionaryBuilder.build());

        serviceFunctionForwarderBuilder.setServiceFunctionDictionary(serviceFunctionDictionaryList);

        LOG.debug("\n########## Creating Forwarder: {}  Service Function: {} "
                ,serviceFunction.getServiceFunctionForwarder(), serviceFunction.getName());

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                sffIID, serviceFunctionForwarderBuilder.build(), true);
        writeTx.commit();
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    public void deleteServiceFunctionFromForwarder (ServiceFunction serviceFunction) {
        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionDictionary> sffIID;
        ServiceFunctionDictionaryKey serviceFunctionDictionaryKey =
                new ServiceFunctionDictionaryKey(serviceFunction.getName());
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(serviceFunction.getServiceFunctionForwarder());
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .child(ServiceFunctionDictionary.class, serviceFunctionDictionaryKey)
                .build();

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                sffIID);
        writeTx.commit();
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }
    @SuppressWarnings("unused")
    public void updateServiceFunctionForwarder (ServiceFunction serviceFunction) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        deleteServiceFunctionFromForwarder(serviceFunction);
        createServiceFunctionForwarder(serviceFunction);

    }
    @SuppressWarnings("unused")
    public void createServiceFunctionForwarders (ServiceFunctionChains serviceFunctionchains) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionForwarders> sffIID;

        // Prepare top container and list
        ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder = new ServiceFunctionForwardersBuilder();
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .build();
        ArrayList<ServiceFunctionForwarder> serviceFunctionForwarderList = new ArrayList<>();

        List<ServiceFunctionChain> serviceFunctionChainList = serviceFunctionchains.getServiceFunctionChain();
        // Iterate through all Service Function Chains
        for (ServiceFunctionChain serviceFunctionChain : serviceFunctionChainList) {

            // Iterate thorough all Service Functions in a single chain
            List<SfcServiceFunction> sfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
            for (SfcServiceFunction sfcServiceFunction :sfcServiceFunctionList) {

                // Read all data of a single Service Service
                ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfcServiceFunction.getName());

                // Build a single service function forwarder
                ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                        new ServiceFunctionForwarderKey(serviceFunction.getServiceFunctionForwarder());
                ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
                serviceFunctionForwarderBuilder.setName(serviceFunction.getServiceFunctionForwarder());
                serviceFunctionForwarderBuilder.setKey(serviceFunctionForwarderKey);

                ArrayList<ServiceFunctionDictionary> serviceFunctionDictionaryList = new ArrayList<>();
                ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder = new ServiceFunctionDictionaryBuilder();
                serviceFunctionDictionaryBuilder.setName(serviceFunction.getName()).setType(serviceFunction.getType())
                        .setServiceFunctionForwarder(serviceFunction.getServiceFunctionForwarder())
                        .setSfDataPlaneLocator(serviceFunction.getSfDataPlaneLocator());
                serviceFunctionDictionaryList.add(serviceFunctionDictionaryBuilder.build());

                serviceFunctionForwarderBuilder.setServiceFunctionDictionary(serviceFunctionDictionaryList);

                serviceFunctionForwarderList.add(serviceFunctionForwarderBuilder.build());

                LOG.debug("\n########## Creating Forwarder: {}  Service Function: {} "
                        , serviceFunction.getServiceFunctionForwarder(), serviceFunction.getName());


            }
            serviceFunctionForwardersBuilder.setServiceFunctionForwarder(serviceFunctionForwarderList);
            WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sffIID, serviceFunctionForwardersBuilder.build(), true);
            writeTx.commit();
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }
    @SuppressWarnings("unused")
    public void deleteServiceFunctionForwarder (ServiceFunction serviceFunction) {

        /*
         * TODO: We assume that if a ServiceFunction exists it belongs to a ServiceFunctionForwarder
         *
         * But this is not necessarily always true since the SFF could be deleted through
         * RESTconf. So, later more checks will be necessary.
         */


        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionDictionary> sffIID;
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(serviceFunction.getServiceFunctionForwarder());
        ServiceFunctionDictionaryKey serviceFunctionDictionaryKey =
                new ServiceFunctionDictionaryKey(serviceFunction.getName());
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .child(ServiceFunctionDictionary.class, serviceFunctionDictionaryKey )
                .build();
        LOG.debug("\n########## Deleting Forwarder: {}  Service Function: {} "
                ,serviceFunction.getServiceFunctionForwarder(), serviceFunction.getName());

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                sffIID);
        writeTx.commit();
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    @SuppressWarnings("unused")
    public static ServiceFunctionForwarder readServiceFunctionForwarder(String name) {
        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(name);
        InstanceIdentifier<ServiceFunctionForwarder> sffIID;
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionForwarder> serviceFunctionForwarderObject = null;
        try {
            serviceFunctionForwarderObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sffIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (serviceFunctionForwarderObject.get() instanceof ServiceFunctionForwarder) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionForwarderObject.get();
        } else {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }

    public static void addPathIdtoServiceFunctionForwarder(ServiceFunctionPath serviceFunctionPath) {
        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        InstanceIdentifier<ServiceFunctionForwarders> sffsIID;
        ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder = new ServiceFunctionForwardersBuilder();
        ArrayList<ServiceFunctionForwarder> serviceFunctionForwarderList =  new ArrayList<>();
        List<SfpServiceFunction> sfpServiceFunctionArrayList = serviceFunctionPath.getSfpServiceFunction();

        for (SfpServiceFunction sfpServiceFunction : sfpServiceFunctionArrayList) {

            ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                    new ServiceFunctionForwarderKey(sfpServiceFunction.getServiceFunctionForwarder());
            ServiceFunctionForwarder serviceFunctionForwarder =  readServiceFunctionForwarder (sfpServiceFunction.getServiceFunctionForwarder());
            ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
            if (serviceFunctionForwarder != null) {
                //serviceFunctionForwarderBuilder.setPathId(serviceFunctionPath.getPathId());
                serviceFunctionForwarderBuilder.setName(sfpServiceFunction.getServiceFunctionForwarder());
                serviceFunctionForwarderBuilder.setSffDataPlaneLocator(serviceFunctionForwarder.getSffDataPlaneLocator());
                serviceFunctionForwarderBuilder.setServiceFunctionDictionary(serviceFunctionForwarder.getServiceFunctionDictionary());
                serviceFunctionForwarderBuilder.setKey(serviceFunctionForwarderKey);

            } else {
                LOG.error("Failed to read Service Function Forwarder from data store");
                continue;
            }
            serviceFunctionForwarderList.add(serviceFunctionForwarderBuilder.build());
        }

        serviceFunctionForwardersBuilder.setServiceFunctionForwarder(serviceFunctionForwarderList);
        sffsIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                sffsIID, serviceFunctionForwardersBuilder.build(), true);
        writeTx.commit();

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

        //serviceFunctionForwardersBuilder.setServiceFunctionForwarder(serviceFunctionForwarderList);

    }

    // TODO: need to check for sff-data-plane-locator
    /*
     * This method checks if a SFF is complete and can be sent to southbound devices
     */
    public static boolean checkServiceFunctionForwarder (ServiceFunctionForwarder serviceFunctionForwarder) {
        if ((serviceFunctionForwarder.getName() != null)  &&
                (serviceFunctionForwarder.getServiceFunctionDictionary() != null)) {
            return true;
        } else {
            return false;
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
