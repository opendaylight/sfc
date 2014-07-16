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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This class has the APIs to operate on the ServiceFunctionPath
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
public class SfcProviderServicePathAPI implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServicePathAPI.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private String methodName = null;
    private Object[] parameters;
    private Class[] parameterTypes;
    public static int numCreatedServicePath = 0;


    SfcProviderServicePathAPI (Object[] params, String m) {
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

    SfcProviderServicePathAPI (Object[] params, Class[] paramsTypes, String m) {
        this.methodName = m;
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        this.parameters = Arrays.copyOf(params, params.length);
        this.parameterTypes = Arrays.copyOf(paramsTypes, paramsTypes.length);
    }

    public static  SfcProviderServicePathAPI getSfcProviderDeleteServicePathInstantiatedFromChain (Object[] params) {
        return new SfcProviderServicePathAPI(params, "deleteServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getSfcProviderDeleteServicePathInstantiatedFromChain (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getSfcProviderCreateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "createServiceFunctionPathEntry");
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
    private void deleteServicePathInstantiatedFromChain (ServiceFunctionChain serviceFunctionChain) {

        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        List<String> sfcServiceFunctionPathList = serviceFunctionChain.getSfcServiceFunctionPath();
        for (String pathName : sfcServiceFunctionPathList) {
            InstanceIdentifier<ServiceFunctionPath> sfpIID;
            ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(pathName);
            sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                    .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                    .build();

            final DataModificationTransaction t = odlSfc.dataProvider
                    .beginTransaction();
            t.removeConfigurationData(sfpIID);
            try {
                t.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("deleteServicePathInstantiatedFromChain failed", e);
            }
            LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

        }
    }
    public void createServiceFunctionPathEntry (ServiceFunctionChain serviceFunctionChain) {

        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        ArrayList<SfpServiceFunction> sfpServiceFunctionArrayList= new ArrayList<>();
        String serviceFunctionChainName = serviceFunctionChain.getName();
        SfpServiceFunctionBuilder sfpServiceFunctionBuilder = new SfpServiceFunctionBuilder();
        int pathId = numCreatedServicePath + 1;

        /*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         */
        List<SfcServiceFunction> SfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
        for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
            LOG.info("\n########## ServiceFunction name: {}", sfcServiceFunction.getName());

            /*
             * We iterate thorough the list of service function types and for each one we get a suitable
             * Service Function
             */

            ServiceFunctionType serviceFunctionType = SfcProviderServiceTypeAPI.getServiceFunctionTypeList(sfcServiceFunction.getType());
            if (serviceFunctionType != null) {
                for (SftServiceFunctionName sftServiceFunctionName : serviceFunctionType.getSftServiceFunctionName()) {
                    String serviceFunctionName  = sftServiceFunctionName.getName();
                    if (serviceFunctionName != null) {
                        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI
                                .readServiceFunction(serviceFunctionName);
                        if (serviceFunction != null) {
                            sfpServiceFunctionBuilder.setName(serviceFunctionName)
                                    .setServiceFunctionForwarder(serviceFunction.getServiceFunctionForwarder());

                        }
                        sfpServiceFunctionArrayList.add(sfpServiceFunctionBuilder.build());
                        break;
                    } else {
                        LOG.error("\n####### Could not find ServiceFunctionName in data store: {}",
                                Thread.currentThread().getStackTrace()[1]);
                        return;
                    }
                }
            } else {
                LOG.error("\n########## Could not find SFs of type: {}", Thread.currentThread().getStackTrace()[1]);
                return;
            }

        }

        //Build the service function path so it can be committed to datastore

        serviceFunctionPathBuilder.setSfpServiceFunction(sfpServiceFunctionArrayList);
        serviceFunctionPathBuilder.setName(serviceFunctionChainName + "-Path");
        // TODO: For now just monotonically incremented

        serviceFunctionPathBuilder.setPathId((long) pathId);
        // TODO: Find out the exact rules for service index generation
        serviceFunctionPathBuilder.setServiceIndex((short) (sfpServiceFunctionArrayList.size() + 1));

        ServiceFunctionPathKey serviceFuntionPathKey = new ServiceFunctionPathKey(serviceFunctionChainName + "-Path");
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFuntionPathKey)
                .build();


        final DataModificationTransaction t = odlSfc.dataProvider
                .beginTransaction();
        t.putConfigurationData(sfpIID, serviceFunctionPathBuilder.build());

        try {
            t.commit().get();
            numCreatedServicePath++;
            // Add the created path to the list of paths instantiated from this Service Chain
            SfcProviderServiceChainAPI
                    .addPathtoServiceFunctionChain(serviceFunctionChain, serviceFunctionPathBuilder.build());
            SfcProviderServiceForwarderAPI.addPathIdtoServiceFunctionForwarder(serviceFunctionPathBuilder.build());

        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Failed to create Service Path", e);
        }



        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }

    public void deleteServiceFunctionPathEntry (ServiceFunctionChain serviceFunctionChain) {

        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        String serviceFunctionChainName = serviceFunctionChain.getName();
        ServiceFunctionPathKey serviceFuntionPathKey = new ServiceFunctionPathKey(serviceFunctionChainName + "-Path");
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFuntionPathKey)
                .build();

        final DataModificationTransaction t = odlSfc.dataProvider
                .beginTransaction();
        t.removeConfigurationData(sfpIID);
        try {
            t.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("deleteServiceFunctionPathEntry failed", e);
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }

    public static ServiceFunctionPath readServiceFunctionPath (String path) {
        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionPathKey serviceFuntionPathKey = new ServiceFunctionPathKey(path);
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFuntionPathKey)
                .build();
        DataObject dataObject = odlSfc.dataProvider.readConfigurationData(sfpIID);
        if (dataObject instanceof ServiceFunctionPath) {
            ServiceFunctionPath serviceFunctionPath = (ServiceFunctionPath) dataObject;
            LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionPath;
        } else {
            return null;
        }
    }
}
