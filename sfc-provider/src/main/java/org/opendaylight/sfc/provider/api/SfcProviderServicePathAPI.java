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
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcProviderRestAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class has the APIs to operate on the ServiceFunctionPath
 * datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfpEntryDataListener
 * <p/>
 * <p/>
 * <p/>
 * @since       2014-06-30
 */
public class SfcProviderServicePathAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServicePathAPI.class);
    private static AtomicInteger numCreatedPath = new AtomicInteger(0);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    static final Comparator<SfcServiceFunction> SF_ORDER =
            new Comparator<SfcServiceFunction>() {
                public int compare(SfcServiceFunction e1, SfcServiceFunction e2) {
                    return e2.getOrder().compareTo(e1.getOrder());
                }
            };

    SfcProviderServicePathAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "putServiceFunctionPath");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "readServiceFunctionPath");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServiceFunctionPath");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getDeleteRenderedServicePath(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteRenderedServicePath");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getDeleteRenderedServicePaths(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteRenderedServicePaths");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getPutAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "putAllServiceFunctionPaths");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "readAllServiceFunctionPaths");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getDeleteAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteAllServiceFunctionPaths");
    }

    public static  SfcProviderServicePathAPI getDeleteServicePathContainingFunction (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathContainingFunction");
    }
    @SuppressWarnings("unused")
    public static  SfcProviderServicePathAPI getDeleteServicePathInstantiatedFromChain (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getCreateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "createServiceFunctionPathEntry");
    }

    public static  SfcProviderServicePathAPI getCreateRenderedServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "createRenderedServicePathEntry");
    }
    @SuppressWarnings("unused")
    public static  SfcProviderServicePathAPI getUpdateRenderedServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateRenderedServicePathEntry");
    }
    @SuppressWarnings("unused")
    public static  SfcProviderServicePathAPI getUpdateServicePathInstantiatedFromChain(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getUpdateServicePathContainingFunction(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathContainingFunction");
    }
    public static SfcProviderServicePathAPI getCheckServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "checkServiceFunctionPath");
    }

    @SuppressWarnings("unused")
    public static int numCreatedPathGetValue() {
        return numCreatedPath.get();
    }

    public int numCreatedPathIncrementGet() {
        return numCreatedPath.incrementAndGet();
    }
    @SuppressWarnings("unused")
    public int numCreatedPathDecrementGet() {
        return numCreatedPath.decrementAndGet();
    }


    @SuppressWarnings("unused")
    protected boolean putServiceFunctionPath(ServiceFunctionPath sfp) {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPath> sfpEntryIID =
                InstanceIdentifier.builder(ServiceFunctionPaths.class).
                        child(ServiceFunctionPath.class, sfp.getKey()).toInstance();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(sfpEntryIID, sfp, LogicalDatastoreType.CONFIGURATION)) {
            LOG.debug("Created Service Function Path: {}", sfp.getName());
            ret = true;
        } else {
            LOG.debug("Failed to create Service Function Path: {}", sfp.getName());
        }

        printTraceStop(LOG);
        return ret;
    }

    public static ServiceFunctionPath readServiceFunctionPath(String serviceFunctionPathName) {
        printTraceStart(LOG);
        ServiceFunctionPath sfp = null;
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).build();


        ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
        Optional<ServiceFunctionPath> serviceFunctionPathDataObject;
        try {
            serviceFunctionPathDataObject = readTx.read(LogicalDatastoreType
                    .CONFIGURATION, sfpIID).get();
            if (serviceFunctionPathDataObject != null
                    && serviceFunctionPathDataObject.isPresent()) {
                sfp = serviceFunctionPathDataObject.get();
            } else {
                LOG.debug("Failed to read Service Function Path: {}", serviceFunctionPathName);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read Service Function Path configuration data \n");
        }

        printTraceStop(LOG);
        return sfp;
    }

    /**
     * This function deletes a SFP from the datastore
     * <p>
     * @param rspName RSP name
     * @return Nothing.
     */
    public static RenderedServicePath readRenderedServicePath(String rspName) {
        printTraceStart(LOG);
        RenderedServicePath rsp = null;
        InstanceIdentifier<RenderedServicePath> rspIID;
        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(rspName);
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).build();


        ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
        Optional<RenderedServicePath> renderedServicePathOptional;
        try {
            renderedServicePathOptional = readTx.read(LogicalDatastoreType
                    .OPERATIONAL, rspIID).get();
            if (renderedServicePathOptional != null
                    && renderedServicePathOptional.isPresent()) {
                rsp = renderedServicePathOptional.get();
            } else {
                LOG.debug("Failed to read Service Function Path: {}", rspName);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read Service Function Path configuration data \n");
        }

        printTraceStop(LOG);
        return rsp;
    }

    /**
     * When a SFF is deleted directly we need to delete all associated SFPs
     *
     * <p>
     * @param servicePaths SffServicePath object
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static  boolean deleteRenderedServicePaths(List<String> servicePaths) {

        printTraceStart(LOG);
        boolean ret = false;

        for (String  rspName : servicePaths)
        {
            if (SfcProviderServicePathAPI.readRenderedServicePath(rspName) != null) {
                if (SfcProviderServicePathAPI.deleteRenderedServicePath(rspName)) {
                    ret = true;
                } else {
                    LOG.error("Could not delete RSP: {}", rspName);
                    ret = false;
                }
            } else {
                LOG.info("RSP {} already deleted by another thread or client", rspName);
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Wrapper API to deletes a list of Service Paths. It includes Executor creation
     * and response management
     *
     * <p>
     * @param servicePaths SffServicePath object
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static boolean deleteRenderedServicePathsExecutor(List<String> servicePaths) {

        printTraceStart(LOG);
        boolean ret = true;

        // SFF deletion is a critical event. If a SFF is deleted we delete all associated SFPs
        Object[] servicePathObj = {servicePaths};
        Class[] servicePathClass = {List.class};

        SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                .getDeleteRenderedServicePaths(servicePathObj, servicePathClass);
        Future future = odlSfc.executor.submit(sfcProviderServicePathAPI);
        try {
            ret = (boolean) future.get();
            LOG.info("getDeletePathsUsedByServiceForwarder: {}", ret);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return ret;
    }

    /**
     * This function deletes a SFP from the datastore
     * <p>
     * @param serviceFunctionPathName SFP name
     * @return Nothing.
     */
    public static boolean deleteServiceFunctionPath(String serviceFunctionPathName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        InstanceIdentifier<ServiceFunctionPath> sfpEntryIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).toInstance();

        if (!SfcDataStoreAPI.deleteTransactionAPI(sfpEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("Failed to delete SFP: {}", serviceFunctionPathName);
        } else {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This function deletes a RSP from the datastore
     * <p>
     * @param renderedServicePathName RSP name
     * @return Nothing.
     */
    public static boolean deleteRenderedServicePath(String renderedServicePathName) {
        boolean ret = false;
        printTraceStart(LOG);
        RenderedServicePathKey  renderedServicePathKey = new RenderedServicePathKey(renderedServicePathName);
        InstanceIdentifier<RenderedServicePath> rspEntryIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(rspEntryIID, LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("Failed to delete RSP: {}", renderedServicePathName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This function deletes a RSP from the datastore
     * <p>
     * @param renderedServicePathName RSP name
     * @return Nothing.
     */
    public static boolean deleteRenderedServicePathExecutor(String renderedServicePathName) {
        boolean ret = false;
        printTraceStart(LOG);
        Object[] servicePathObj = {renderedServicePathName};
        Class[] servicePathClass = {String.class};
        SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                .getDeleteRenderedServicePath(servicePathObj, servicePathClass);
        Future futureDeleteRSP = odlSfc.executor.submit(sfcProviderServicePathAPI);
        try {
            ret = (boolean) futureDeleteRSP.get();
            LOG.info("getDeleteRenderedServicePath: {}", futureDeleteRSP.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if (!ret) {
            LOG.error("Could not delete RSP. System state inconsistent. Deleting and add SFP {} back", renderedServicePathName);
        }
        printTraceStop(LOG);
        return ret;
    }

    protected boolean putAllServiceFunctionPaths(ServiceFunctionPaths sfps) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.
                    builder(ServiceFunctionPaths.class).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, sfpsIID, sfps);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionPaths readAllServiceFunctionPaths() {
        ServiceFunctionPaths sfps = null;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.builder(ServiceFunctionPaths.class).toInstance();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunctionPaths> serviceFunctionPathsDataObject = null;
            try {
                serviceFunctionPathsDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfpsIID).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read top-level Service Function Path " +
                        "container \n");
            }
            if (serviceFunctionPathsDataObject != null
                    && serviceFunctionPathsDataObject.isPresent()) {
                sfps = serviceFunctionPathsDataObject.get();
            }
        }
        printTraceStop(LOG);
        return sfps;
    }

    protected boolean deleteAllServiceFunctionPaths() {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPaths> sfpsIID =
                InstanceIdentifier.builder(ServiceFunctionPaths.class).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfpsIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }


    @SuppressWarnings("unused")
    protected void updateRenderedServicePathEntry (ServiceFunctionPath serviceFunctionPath) {
        this.createRenderedServicePathEntry(serviceFunctionPath);
    }

    /**
     * This function is called whenever a SFP is created or updated. It recomputes
     * the SFP information and merges any missing data
     * <p>
     * @param serviceFunctionPath Service Function Path Object
     */
/*    protected void createServiceFunctionPathEntry (ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);

        long pathId;
        short posIndex = 0;
        int serviceIndex;
        ServiceFunctionChain serviceFunctionChain = null;
        String serviceFunctionChainName = serviceFunctionPath.getServiceChainName();
        try {
            serviceFunctionChain = serviceFunctionChainName != null ?
                    (ServiceFunctionChain) odlSfc.executor
                            .submit(SfcProviderServiceChainAPI.getRead(
                                    new Object[]{serviceFunctionChainName},
                                    new Class[]{String.class})).get(): null;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(" \n Could not read Service Function Chain configuration for Service Path {}",
                    serviceFunctionPath.getName());
        }
        if (serviceFunctionChain == null) {
            LOG.error("\n ServiceFunctionChain name for Path {} not provided",
                    serviceFunctionPath.getName());
            return;
        }

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        ArrayList<ServicePathHop> servicePathHopArrayList = new ArrayList<>();
        ServicePathHopBuilder servicePathHopBuilder = new ServicePathHopBuilder();

        *//*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         *//*
        //List<SfcServiceFunction> sfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(serviceFunctionChain.getSfcServiceFunction());

        Collections.sort(sfcServiceFunctionList, Collections.reverseOrder(SF_ORDER));
        serviceIndex = sfcServiceFunctionList.size();
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.debug("\n########## ServiceFunction name: {}", sfcServiceFunction.getName());

            *//*
             * We iterate thorough the list of service function types and for each one we try to get
             * get a suitable Service Function. WE need to perform lots of checking to make sure
             * we do not hit NULL Pointer exceptions
             *//*

            ServiceFunctionType serviceFunctionType;
            try {
                serviceFunctionType = (ServiceFunctionType) odlSfc.executor.submit(SfcProviderServiceTypeAPI.getRead(
                        new Object[]{sfcServiceFunction.getType()}, new Class[]{String.class})).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error(" Could not get list of Service Functions of type {} \n", sfcServiceFunction.getType());
                return;
            }
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
                        // TODO: API to select suitable Service Function
                        String serviceFunctionName = sftServiceFunctionName.getName();
                        ServiceFunction serviceFunction = null;
                        try {
                            serviceFunction =
                                    (ServiceFunction) odlSfc.executor.submit(SfcProviderServiceFunctionAPI
                                            .getRead(new Object[]{serviceFunctionName}, new Class[]{String.class})).get();
                        } catch (InterruptedException | ExecutionException e) {
                            LOG.error(" Could not read Service Function {} " +
                                    "\n", serviceFunctionName);
                        }
                        if (serviceFunction != null) {
                            servicePathHopBuilder.setHopNumber(posIndex)
                                    .setServiceFunctionName(serviceFunctionName)
                                    .setServiceIndex((short) serviceIndex)
                                    .setServiceFunctionForwarder(serviceFunction.getSfDataPlaneLocator()
                                            .get(0)
                                            .getServiceFunctionForwarder());
                            servicePathHopArrayList.add(posIndex, servicePathHopBuilder.build());
                            serviceIndex--;
                            posIndex++;
                            break;
                        } else {
                            LOG.error("\n####### Could not find suitable SF of type in data store: {}",
                                    sfcServiceFunction.getType());
                            return;
                        }
                    }
                } else {
                    LOG.error("Could not create path because there are no configured SFs of type: {}", sfcServiceFunction.getType());
                    return;
                }
            } else {
                LOG.error("Could not create path because there are no configured SFs of type: {}", sfcServiceFunction.getType());
                return;
            }

        }

        //Build the service function path so it can be committed to datastore


        pathId = (serviceFunctionPath.getPathId() != null)  ?  serviceFunctionPath.getPathId()
                : numCreatedPathIncrementGet();
        serviceFunctionPathBuilder.setServicePathHop(servicePathHopArrayList);
        if (serviceFunctionPath.getName().isEmpty())  {
            serviceFunctionPathBuilder.setName(serviceFunctionChainName + "-Path-" + pathId);
        } else {
            serviceFunctionPathBuilder.setName(serviceFunctionPath.getName());

        }

        serviceFunctionPathBuilder.setPathId(pathId);
        // TODO: Find out the exact rules for service index generation
        serviceFunctionPathBuilder.setStartingIndex((short) servicePathHopArrayList.size());
        serviceFunctionPathBuilder.setServiceChainName(serviceFunctionChainName);

        ServiceFunctionPathKey serviceFunctionPathKey = new
                ServiceFunctionPathKey(serviceFunctionPathBuilder.getName());
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                .build();

        ServiceFunctionPath newServiceFunctionPath =
                serviceFunctionPathBuilder.build();
        if (!SfcDataStoreAPI.writeMergeTransactionAPI(sfpIID, newServiceFunctionPath, LogicalDatastoreType.CONFIGURATION)) {
            LOG.debug("Failed to create Service Function Path: {}",
                    serviceFunctionPath.getName());
        }

        *//* Prepare REST invocation *//*

        invokeServicePathRest(serviceFunctionPath, HttpMethod.PUT);

        printTraceStop(LOG);

    }*/

    public static boolean createRenderedServicePathEntryExecutor(ServiceFunctionPath serviceFunctionPath) {
        boolean ret = false;
        Object[] servicePathObj = {serviceFunctionPath};
        Class[] servicePathClass = {ServiceFunctionPath.class};
        SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                .getCreateRenderedServicePathAPI(servicePathObj, servicePathClass);
        Future futureCreateRSP = odlSfc.executor.submit(sfcProviderServicePathAPI);
        try {
            ret = (boolean) futureCreateRSP.get();
            LOG.debug("getCreateRenderedServicePathAPI: {}", futureCreateRSP.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * This function is called whenever a SFP is created or updated. It recomputes
     * the SFP information and merges any missing data
     * <p>
     * @param serviceFunctionPath Service Function Path Object
     */
    protected boolean createRenderedServicePathEntry (ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);

        long pathId;
        short posIndex = 0;
        int serviceIndex;
        boolean ret = false;
        ServiceFunctionChain serviceFunctionChain = null;
        String serviceFunctionChainName = serviceFunctionPath.getServiceChainName();
        try {
            serviceFunctionChain = serviceFunctionChainName != null ?
                    (ServiceFunctionChain) odlSfc.executor
                            .submit(SfcProviderServiceChainAPI.getRead(
                                    new Object[]{serviceFunctionChainName},
                                    new Class[]{String.class})).get(): null;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(" \n Could not read Service Function Chain configuration for Service Path {}",
                    serviceFunctionPath.getName());
            return ret;
        }
        if (serviceFunctionChain == null) {
            LOG.error("\n ServiceFunctionChain name for Path {} not provided",
                    serviceFunctionPath.getName());
            return ret;
        }

        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        ArrayList<RenderedServicePathHop> renderedServicePathHopArrayList = new ArrayList<>();
        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();

        /*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         */
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(serviceFunctionChain.getSfcServiceFunction());

        Collections.sort(sfcServiceFunctionList, Collections.reverseOrder(SF_ORDER));
        serviceIndex = sfcServiceFunctionList.size();
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.debug("\n########## ServiceFunction name: {}", sfcServiceFunction.getName());

            /*
             * We iterate thorough the list of service function types and for each one we try to get
             * get a suitable Service Function. WE need to perform lots of checking to make sure
             * we do not hit NULL Pointer exceptions
             */

            ServiceFunctionType serviceFunctionType;
            try {
                serviceFunctionType = (ServiceFunctionType) odlSfc.executor.submit(SfcProviderServiceTypeAPI.getRead(
                        new Object[]{sfcServiceFunction.getType()}, new Class[]{String.class})).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error(" Could not get list of Service Functions of type: {}", sfcServiceFunction.getType());
                return ret;
            }
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
                        // TODO: API to select suitable Service Function
                        String serviceFunctionName = sftServiceFunctionName.getName();
                        ServiceFunction serviceFunction = null;
                        try {
                            serviceFunction =
                                    (ServiceFunction) odlSfc.executor.submit(SfcProviderServiceFunctionAPI
                                            .getRead(new Object[]{serviceFunctionName}, new Class[]{String.class})).get();
                        } catch (InterruptedException | ExecutionException e) {
                            LOG.error(" Could not read Service Function: {}", serviceFunctionName);
                        }
                        if (serviceFunction != null) {
                            renderedServicePathHopBuilder.setHopNumber(posIndex)
                                    .setServiceFunctionName(serviceFunctionName)
                                    .setServiceIndex((short) serviceIndex)
                                    .setServiceFunctionForwarder(serviceFunction.getSfDataPlaneLocator()
                                            .get(0)
                                            .getServiceFunctionForwarder());
                            renderedServicePathHopArrayList.add(posIndex, renderedServicePathHopBuilder.build());
                            serviceIndex--;
                            posIndex++;
                            break;
                        } else {
                            LOG.error("Could not find suitable SF of type in data store: {}",
                                    sfcServiceFunction.getType());
                            return ret;
                        }
                    }
                } else {
                    LOG.error("Could not create path because there are no configured SFs of type: {}",
                            sfcServiceFunction.getType());
                    return ret;
                }
            } else {
                LOG.error("Could not create path because there are no configured SFs of type: {}",
                        sfcServiceFunction.getType());
                return ret;
            }

        }

        //Build the service function path so it can be committed to datastore


        pathId = (serviceFunctionPath.getPathId() != null)  ?  serviceFunctionPath.getPathId()
                : numCreatedPathIncrementGet();
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopArrayList);
        if (serviceFunctionPath.getName().isEmpty())  {
            renderedServicePathBuilder.setName(serviceFunctionChainName + "-Path-" + pathId);
        } else {
            renderedServicePathBuilder.setName(serviceFunctionPath.getName());

        }

        renderedServicePathBuilder.setPathId(pathId);
        // TODO: Find out the exact rules for service index generation
        renderedServicePathBuilder.setStartingIndex((short) renderedServicePathHopArrayList.size());
        renderedServicePathBuilder.setServiceChainName(serviceFunctionChainName);

        RenderedServicePathKey renderedServicePathKey = new
                RenderedServicePathKey(renderedServicePathBuilder.getName());
        InstanceIdentifier<RenderedServicePath> rspIID;
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey)
                .build();

        RenderedServicePath renderedServicePath =
                renderedServicePathBuilder.build();
        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath, LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.debug("Failed to create Rendered Service Path: {}",
                    serviceFunctionPath.getName());
        }

        printTraceStop(LOG);
        return ret;

    }

    /**
     * Check a SFF for consistency after datastore creation
     * <p>
     * @param serviceFunctionPath SFP object
     * @param operation HttpMethod
     * @return Nothing
     */
    public void checkServiceFunctionPath(ServiceFunctionPath serviceFunctionPath, String operation) {

        printTraceStart(LOG);

        invokeServicePathRest(serviceFunctionPath, operation);

        printTraceStop(LOG);
    }

    /**
     * This method decouples the SFP API from the SouthBound REST client.
     * SFP APIs call this method to convey SFP information to REST southbound
     * devices
     * <p>
     * @param dataobject Generic parameter that accepts both Rendered and Service Paths
     * @param httpMethod  HTTP method such as GET, PUT, POST..
     * @return Nothing.
     */
    private void invokeServicePathRest(ServiceFunctionPath dataobject, String httpMethod) {

     /* Invoke SB REST API */

        if (dataobject != null)
        {
            if (httpMethod.equals(HttpMethod.PUT))
            {
                Object[] servicePathObj = {dataobject};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                odlSfc.executor.execute(SfcProviderRestAPI.
                        getPutServiceFunctionPath(servicePathObj,
                                servicePathClass));
            } else if (httpMethod.equals(HttpMethod.DELETE))
            {
                Object[] servicePathObj = {dataobject};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                odlSfc.executor.execute(SfcProviderRestAPI.
                        getDeleteServiceFunctionPath(servicePathObj,
                                servicePathClass));
            }
        } else {
            LOG.error("Dataobject is null");
        }

    }

    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them. Additionally, since we are delete the RSP, we also
     * <p>
     * @param serviceFunction Service Function Object
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public boolean deleteServicePathContainingFunction (ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        boolean ret = true;
        List<SfServicePath> sfServicePathList;

        sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(serviceFunction.getName());
        if (sfServicePathList != null) {
            for (SfServicePath sfServicePath : sfServicePathList) {

                String rspName = sfServicePath.getName();
                if (readRenderedServicePath(rspName) != null) {
                    if (deleteRenderedServicePath(rspName)) {
                        ret = ret && true;
                    } else {
                        LOG.error("Failed to delete Path {} from Service Function {} state",
                                rspName, serviceFunction.getName());
                        ret = ret && false;
                    }
                } else {
                    LOG.info("SFP {} already deleted by another thread or client", rspName);
                }
            }
        } else {
            LOG.debug("Could not find Service function Paths using Service Function: {} ",
                    serviceFunction.getName());
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them.
     * <p>
     * @param serviceFunction Service Function Object
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static boolean deleteServicePathContainingFunctionExecutor (ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        boolean ret = true;
        Object[] functionParams = {serviceFunction};
        Class[] functionParamsTypes = {ServiceFunction.class};
        Future future = odlSfc.executor.submit(SfcProviderServicePathAPI
                .getDeleteServicePathContainingFunction(functionParams, functionParamsTypes));
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServicePathContainingFunction returns: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }
}
