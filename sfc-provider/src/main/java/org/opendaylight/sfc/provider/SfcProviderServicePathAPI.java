/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateKey;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class has the APIs to operate on the ServiceFunctionPath
 * datastore.
 *
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 * @see org.opendaylight.sfc.provider.SfcProviderSfpEntryDataListener
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
    private static AtomicInteger numCreatedPath = new AtomicInteger(0);


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

    public static  SfcProviderServicePathAPI getDeleteServicePathContainingFunction (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathContainingFunction");
    }

    public static  SfcProviderServicePathAPI getDeleteServicePathInstantiatedFromChain (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getCreateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "createServiceFunctionPathEntry");
    }

    public static  SfcProviderServicePathAPI getUpdateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServiceFunctionPathEntry");
    }

    public static  SfcProviderServicePathAPI getUpdateServicePathInstantiatedFromChain(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getUpdateServicePathContainingFunction(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathContainingFunction");
    }


    public int numCreatedPathIncrementGet() {
        return numCreatedPath.incrementAndGet();
    }

    public int numCreatedPathDecrementGet() {
        return numCreatedPath.decrementAndGet();
    }

    public static int numCreatedPathGetValue() {
        return numCreatedPath.get();
    }

   /* Today A Service Function Chain modification is catastrophic. We delete all Paths
    * and recreate them. Maybe a real patch is possible but given the complexities of the possible
    * modifications, this is the safest approach.
    */
    private void updateServicePathInstantiatedFromChain (ServiceFunctionPath serviceFunctionPath) {
        deleteServicePathInstantiatedFromChain(serviceFunctionPath);
        createServiceFunctionPathEntry(serviceFunctionPath);
    }

    // TODO:Needs change
    private void deleteServicePathInstantiatedFromChain (ServiceFunctionPath serviceFunctionPath) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionChain serviceFunctionChain;
        String serviceChainName = serviceFunctionPath.getServiceChainName();
        if ((serviceChainName == null) || ((serviceFunctionChain = SfcProviderServiceChainAPI
                .readServiceFunctionChain(serviceChainName)) == null)) {
            LOG.error("\n########## ServiceFunctionChain name for Path {} not provided",
                    serviceFunctionPath.getName());
            return;
        }


        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionChainState serviceFunctionChainState;
        ServiceFunctionChainStateKey serviceFunctionChainStateKey =
                new ServiceFunctionChainStateKey(serviceFunctionChain.getName());
        InstanceIdentifier<ServiceFunctionChainState> sfcStateIID =
                InstanceIdentifier.builder(ServiceFunctionChainsState.class)
                        .child(ServiceFunctionChainState.class, serviceFunctionChainStateKey)
                        .build();
        DataObject dataSfcStateObject = odlSfc.dataProvider.readOperationalData(sfcStateIID);
        // TODO: Remove path name from Service Function path list
        if (dataSfcStateObject instanceof ServiceFunctionChainState) {
            serviceFunctionChainState = (ServiceFunctionChainState) dataSfcStateObject;
            List<String> sfcServiceFunctionPathList =
                    serviceFunctionChainState.getSfcServiceFunctionPath();
            List<String> removedPaths = new ArrayList<>();
            for (String pathName : sfcServiceFunctionPathList) {

                ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(pathName);
                sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                        .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                        .build();

                final DataModificationTransaction t = odlSfc.dataProvider
                        .beginTransaction();
                t.removeConfigurationData(sfpIID);
                try {
                    t.commit().get();
                    // We update the list as we go
                    removedPaths.add(pathName);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("deleteServicePathInstantiatedFromChain for path {} failed ", pathName);
                }
            }

            sfcServiceFunctionPathList.removeAll(removedPaths);

            /* After we are done removing all paths from the datastore we commit the updated the path list
             * under the Service Chain operational tree
             */
            ServiceFunctionChainStateBuilder serviceFunctionChainStateBuilder  = new ServiceFunctionChainStateBuilder();
            serviceFunctionChainStateBuilder.setName(serviceFunctionChain.getName());
            serviceFunctionChainStateBuilder.setSfcServiceFunctionPath(sfcServiceFunctionPathList);
            final DataModificationTransaction t = odlSfc.dataProvider
                    .beginTransaction();
            t.putOperationalData(sfcStateIID, serviceFunctionChainStateBuilder.build());
            try {
                t.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to update SfcServiceFunctionPath List State {} failed ");
            }

        } else {
            LOG.error("Failed to get reference to Service Function Chain State {} ", serviceFunctionChain.getName());
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    private void updateServiceFunctionPathEntry (ServiceFunctionPath serviceFunctionPath) {
        this.createServiceFunctionPathEntry(serviceFunctionPath);
    }

    private void createServiceFunctionPathEntry (ServiceFunctionPath serviceFunctionPath) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        long pathId;
        ServiceFunctionChain serviceFunctionChain;
        String serviceFunctionChainName = serviceFunctionPath.getServiceChainName();
        if ((serviceFunctionChainName == null) || ((serviceFunctionChain = SfcProviderServiceChainAPI
                    .readServiceFunctionChain(serviceFunctionChainName)) == null)) {
            LOG.error("\n########## ServiceFunctionChain name for Path {} not provided",
                    serviceFunctionPath.getName());
            return;
        }


        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        ArrayList<SfpServiceFunction> sfpServiceFunctionArrayList= new ArrayList<>();
        SfpServiceFunctionBuilder sfpServiceFunctionBuilder = new SfpServiceFunctionBuilder();

        /*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         */
        List<SfcServiceFunction> SfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
        for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
            LOG.debug("\n########## ServiceFunction name: {}", sfcServiceFunction.getName());

            /*
             * We iterate thorough the list of service function types and for each one we try to get
             * get a suitable Service Function. WE need to perform lost of checking to make sure
             * we do not hit NULL Pointer exceptions
             */

            ServiceFunctionType serviceFunctionType = SfcProviderServiceTypeAPI.getServiceFunctionTypeList(sfcServiceFunction.getType());
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
                        // TODO: API to select suitable Service Function
                        String serviceFunctionName = sftServiceFunctionName.getName();
                        ServiceFunction serviceFunction;
                        if ((serviceFunctionName != null) && ((
                             serviceFunction = SfcProviderServiceFunctionAPI
                                    .readServiceFunction(serviceFunctionName)) != null)) {

                            sfpServiceFunctionBuilder.setName(serviceFunctionName)
                                        .setServiceFunctionForwarder(serviceFunction.getServiceFunctionForwarder());
                            sfpServiceFunctionArrayList.add(sfpServiceFunctionBuilder.build());
                            break;
                        } else {
                            LOG.error("\n####### Could not find suitable SF of type in data store: {}",
                                    sfcServiceFunction.getType());
                            return;
                        }
                    }
                } else {
                    LOG.error("\n########## No configured SFs of type: {}", sfcServiceFunction.getType());
                    return;
                }
            } else {
                LOG.error("\n########## No configured SFs of type: {}", sfcServiceFunction.getType());
                return;
            }

        }

        //Build the service function path so it can be committed to datastore


        pathId = (serviceFunctionPath.getPathId() != null)  ?  serviceFunctionPath.getPathId()
                : numCreatedPathIncrementGet();
        serviceFunctionPathBuilder.setSfpServiceFunction(sfpServiceFunctionArrayList);
        if (serviceFunctionPath.getName().isEmpty())  {
            serviceFunctionPathBuilder.setName(serviceFunctionChainName + "-Path-" + pathId);
        } else {
            serviceFunctionPathBuilder.setName(serviceFunctionPath.getName());

        }

        serviceFunctionPathBuilder.setPathId(pathId);
        // TODO: Find out the exact rules for service index generation
        serviceFunctionPathBuilder.setServiceIndex((short) (sfpServiceFunctionArrayList.size() + 1));

        ServiceFunctionPathKey serviceFunctionPathKey = new
                ServiceFunctionPathKey(serviceFunctionPathBuilder.getName());
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                .build();

        ServiceFunctionPath newServiceFunctionPath = serviceFunctionPathBuilder.build();
        final DataModificationTransaction t = odlSfc.dataProvider
                .beginTransaction();
        t.putConfigurationData(sfpIID, newServiceFunctionPath);

        try {
            t.commit().get();
            // Add the created path to the list of paths instantiated from this Service Chain
            //SfcProviderServiceChainAPI
            //        .addPathToServiceFunctionChainState(serviceFunctionChain, serviceFunctionPath);
            SfcProviderServiceForwarderAPI.addPathIdtoServiceFunctionForwarder(newServiceFunctionPath);
            SfcProviderServiceFunctionAPI.addPathToServiceFunctionState(newServiceFunctionPath);

        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Failed to create Service Path", e);
        }

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }

    private void deleteServiceFunctionPathEntry (ServiceFunctionChain serviceFunctionChain) {

        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        String serviceFunctionChainName = serviceFunctionChain.getName();
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionChainName + "-Path");
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey)
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

    private void deleteServicePathContainingFunction (ServiceFunction serviceFunction) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionState serviceFunctionState;
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunction.getName());
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();
        DataObject dataSfcStateObject = odlSfc.dataProvider.readOperationalData(sfStateIID);
        if (dataSfcStateObject instanceof ServiceFunctionState) {
            serviceFunctionState = (ServiceFunctionState) dataSfcStateObject;
            List<String> sfServiceFunctionPathList =
                    serviceFunctionState.getSfServiceFunctionPath();
            List<String> removedPaths = new ArrayList<>();
            for (String pathName : sfServiceFunctionPathList) {

                ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(pathName);
                sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                        .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                        .build();

                final DataModificationTransaction t = odlSfc.dataProvider
                        .beginTransaction();
                t.removeConfigurationData(sfpIID);
                try {
                    t.commit().get();
                    // We update the list as we go
                    removedPaths.add(pathName);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("deleteServicePathInstantiatedFromChain for path {} failed ", pathName);
                }
            }

            sfServiceFunctionPathList.removeAll(removedPaths);

            /* After we are done removing all paths from the datastore we commit the updated the path list
             * under the Service Chain operational tree
             */
            ServiceFunctionStateBuilder serviceFunctionStateBuilder  = new ServiceFunctionStateBuilder();
            serviceFunctionStateBuilder.setName(serviceFunction.getName());
            serviceFunctionStateBuilder.setSfServiceFunctionPath(sfServiceFunctionPathList);
            final DataModificationTransaction t = odlSfc.dataProvider
                    .beginTransaction();
            t.putOperationalData(sfStateIID, serviceFunctionStateBuilder.build());
            try {
                t.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to update ServiceFunction {} State Path List failed ", serviceFunction.getName());
            }

        } else {
            LOG.error("Failed to get reference to Service Function State {} ", serviceFunction.getName());
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }


    @Override
    public void run() {
        if (methodName != null) {
            Class c = this.getClass();
            Method method;
            try {
                method = c.getDeclaredMethod(methodName, parameterTypes);
                method.invoke (this, parameters);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e ) {
                e.printStackTrace();
            }
        }
    }
}
