/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcProviderRestAPI;
import org.opendaylight.sfc.provider.SfcReflection;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopKey;
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
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

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
    private static final int maxStartingIndex = 255;
    private static AtomicInteger numCreatedPath = new AtomicInteger(0);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    static final Comparator<SfcServiceFunction> SF_ORDER =
            new Comparator<SfcServiceFunction>() {
                public int compare(SfcServiceFunction e1, SfcServiceFunction e2) {
                    return e2.getOrder().compareTo(e1.getOrder());
                }
            };

    static final Comparator<SfcServiceFunction> SF_ORDER_REV =
            new Comparator<SfcServiceFunction>() {
                public int compare(SfcServiceFunction e1, SfcServiceFunction e2) {
                    return e1.getOrder().compareTo(e2.getOrder());
                }
            };

    private static Map<java.lang.Class<? extends org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity>, Integer> mapCountRoundRobin = new HashMap<>();
    private enum SfcSelectSfAlgorithmType{
        ROUND_ROBIN, RANDOM;
    }

    SfcSelectSfAlgorithmType sfcSelectSfAlgorithmType = SfcSelectSfAlgorithmType.ROUND_ROBIN;

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
    public static  SfcProviderServicePathAPI getCreateReverseRenderedServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "createReverseRenderedServicePath");
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
    private String getRoundRobinServicePathHop(List<SftServiceFunctionName> sftServiceFunctionNameList, ServiceFunctionType serviceFunctionType)
    {
        int countRoundRobin = 0;

        if(mapCountRoundRobin.size() != 0){
            for(java.lang.Class<? extends org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity> sfType: mapCountRoundRobin.keySet()){        
                if(sfType.equals(serviceFunctionType.getType())){
                    countRoundRobin = mapCountRoundRobin.get(sfType);
                    LOG.debug("countRoundRobin: {}", countRoundRobin);
                    break;
                }
            }
        }

        SftServiceFunctionName sftServiceFunctionName = sftServiceFunctionNameList.get(countRoundRobin);
        countRoundRobin = (countRoundRobin + 1) % sftServiceFunctionNameList.size();
        mapCountRoundRobin.put(serviceFunctionType.getType(), countRoundRobin);
        return sftServiceFunctionName.getName();
    }

    private String getRandomServicePathHop(List<SftServiceFunctionName> sftServiceFunctionNameList)
    {
        Random rad = new Random();
        return sftServiceFunctionNameList.get(rad.nextInt(sftServiceFunctionNameList.size())).getName();
    }

    public String sfcSelectServicePathHop(ServiceFunctionType serviceFunctionType, SfcSelectSfAlgorithmType sfcSelectSfAlgorithmType)
    {
        List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        LOG.debug("ServiceFunction Name List : {}", sftServiceFunctionNameList);
        String sfcSelectServicePathHopName = "";

        switch(sfcSelectSfAlgorithmType){
        case ROUND_ROBIN:
            sfcSelectServicePathHopName = getRoundRobinServicePathHop(sftServiceFunctionNameList, serviceFunctionType);
            break;
        case RANDOM:
        default:
            sfcSelectServicePathHopName = getRandomServicePathHop(sftServiceFunctionNameList);
            break;
        }

        return sfcSelectServicePathHopName;
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
            LOG.error("Failed to create Service Function Path: {}", sfp.getName());
        }

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This function reads a SFP from the datastore
     * <p>
     * @param serviceFunctionPathName RSP name
     * @return Nothing.
     */
    public static ServiceFunctionPath readServiceFunctionPath(String serviceFunctionPathName) {
        printTraceStart(LOG);
        ServiceFunctionPath sfp;
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).build();

        sfp = SfcDataStoreAPI.readTransactionAPI(sfpIID, LogicalDatastoreType.CONFIGURATION);
        printTraceStop(LOG);
        return sfp;
    }

    /**
     * This function reads a RSP from the datastore
     * <p>
     * @param rspName RSP name
     * @return Nothing.
     */
    public static RenderedServicePath readRenderedServicePath(String rspName) {
        printTraceStart(LOG);
        RenderedServicePath rsp;
        InstanceIdentifier<RenderedServicePath> rspIID;
        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(rspName);
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).build();

        rsp = SfcDataStoreAPI.readTransactionAPI(rspIID, LogicalDatastoreType.OPERATIONAL);

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
    @SfcReflection
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
            LOG.info("getDeleteRenderedServicePaths: {}", ret);
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }

        return ret;
    }

    /**
     * This function deletes a SFP from the datastore
     * <p>
     * @param serviceFunctionPathName SFP name
     * @return Nothing.
     */
    @SfcReflection
    @SuppressWarnings("unused")
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
            LOG.error("{}: Failed to delete RSP: {}", Thread.currentThread().getStackTrace()[1],
                    renderedServicePathName);
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
        Future future = odlSfc.executor.submit(sfcProviderServicePathAPI);
        try {
            ret = (boolean) future.get();
            LOG.info("getDeleteRenderedServicePath: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        if (!ret) {
            LOG.error("{}: Failed to delete RSP {}", Thread.currentThread().getStackTrace()[1],
                    renderedServicePathName);
        }
        printTraceStop(LOG);
        return ret;
    }

    @SuppressWarnings("unused")
    protected boolean putAllServiceFunctionPaths(ServiceFunctionPaths sfps) {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.
                builder(ServiceFunctionPaths.class).toInstance();

        if (SfcDataStoreAPI.writePutTransactionAPI(sfpsIID, sfps, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    @SuppressWarnings("unused")
    public static ServiceFunctionPaths readAllServiceFunctionPaths() {
        ServiceFunctionPaths sfps;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.builder(ServiceFunctionPaths.class).toInstance();

        sfps = SfcDataStoreAPI.readTransactionAPI(sfpsIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return sfps;
    }

    @SuppressWarnings("unused")
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

    public static RenderedServicePath createRenderedServicePathEntryExecutor(ServiceFunctionPath serviceFunctionPath) {
        RenderedServicePath ret = null;
        Object[] servicePathObj = {serviceFunctionPath};
        Class[] servicePathClass = {ServiceFunctionPath.class};
        SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                .getCreateRenderedServicePathAPI(servicePathObj, servicePathClass);
        Future futureCreateRSP = odlSfc.executor.submit(sfcProviderServicePathAPI);
        try {
            ret = (RenderedServicePath) futureCreateRSP.get();
            LOG.debug("getCreateRenderedServicePathAPI: {}", futureCreateRSP.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        return ret;
    }

    /**
     * This function is called whenever a SFP is created or updated. It recomputes
     * the SFP information and merges any missing data
     * <p>
     * @param serviceFunctionPath Service Function Path Object
     */
    protected RenderedServicePath createRenderedServicePathEntry (ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);

        long pathId;
        short posIndex = 0;
        int serviceIndex;
        RenderedServicePath ret = null;
        ServiceFunctionChain serviceFunctionChain;
        String serviceFunctionChainName = serviceFunctionPath.getServiceChainName();
        serviceFunctionChain = serviceFunctionChainName != null ?
                SfcProviderServiceChainAPI.readServiceFunctionChain(serviceFunctionChainName)
                : null;
        if (serviceFunctionChain == null) {
            LOG.error("ServiceFunctionChain name for Path {} not provided",
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

        //Collections.sort(sfcServiceFunctionList, Collections.reverseOrder(SF_ORDER));

        // Descending order
        //Collections.sort(sfcServiceFunctionList, Collections.reverseOrder(SF_ORDER_REV));
        //serviceIndex = sfcServiceFunctionList.size();
        serviceIndex = maxStartingIndex;
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.debug("ServiceFunction name: {}", sfcServiceFunction.getName());


            /*
             * We iterate thorough the list of service function types and for each one we try to get
             * get a suitable Service Function. WE need to perform lots of checking to make sure
             * we do not hit NULL Pointer exceptions
             */

            ServiceFunctionType serviceFunctionType;
/*            try {
                serviceFunctionType = (ServiceFunctionType) odlSfc.executor.submit(SfcProviderServiceTypeAPI.getRead(
                        new Object[]{sfcServiceFunction.getType()}, new Class[]{Class.class})).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error(" Could not get list of Service Functions of type: {}", sfcServiceFunction.getType());
                return ret;
            }*/
            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(sfcServiceFunction.getType());
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    String serviceFunctionName = sfcSelectServicePathHop(serviceFunctionType, sfcSelectSfAlgorithmType);
                    LOG.debug("SelectSfAlgorithmType: {}, Selected ServiceFunction name: {}", sfcSelectSfAlgorithmType, serviceFunctionName);
                    ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI
                            .readServiceFunctionExecutor(serviceFunctionName);
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
                    } else {
                        LOG.error("Could not find suitable SF of type in data store: {}",
                                sfcServiceFunction.getType());
                        return ret;
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
        //renderedServicePathBuilder.setStartingIndex((short) renderedServicePathHopArrayList.size());
        renderedServicePathBuilder.setStartingIndex((short) maxStartingIndex);
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
            ret = renderedServicePath;
        } else {
            LOG.error("{}: Failed to create Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], serviceFunctionPath.getName());
        }
        printTraceStop(LOG);
        return ret;

    }


    public static RenderedServicePath createReverseRenderedServicePathEntryExecutor(RenderedServicePath renderedServicePath) {
        RenderedServicePath ret = null;
        Object[] servicePathObj = {renderedServicePath};
        Class[] servicePathClass = {RenderedServicePath.class};
        SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                .getCreateReverseRenderedServicePathAPI(servicePathObj, servicePathClass);
        Future future = odlSfc.executor.submit(sfcProviderServicePathAPI);
        try {
            ret = (RenderedServicePath) future.get();
            LOG.debug("getCreateRenderedServicePathAPI: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        return ret;
    }

    /**
     * Creates a RSP that is mirror image of the given one. It reverses the
     * hop list and adjusts hop number and service index accordingly
     * <p>
     * @param renderedServicePath RSP object
     * @return Nothing
     */
    public RenderedServicePath createReverseRenderedServicePath(RenderedServicePath renderedServicePath) {


        RenderedServicePath ret = null;
        long pathId = numCreatedPathIncrementGet();
        String revPathName;
        short revServiceHop;
        //int numServiceHops = renderedServicePath.getRenderedServicePathHop().size();
        ArrayList<RenderedServicePathHop> revRenderedServicePathHopArrayList = new ArrayList<>();
        printTraceStart(LOG);

        RenderedServicePathBuilder revRenderedServicePathBuilder = new RenderedServicePathBuilder(renderedServicePath);
        revRenderedServicePathBuilder.setPathId(pathId);
        revPathName = renderedServicePath.getName() + "-Reverse";
        revRenderedServicePathBuilder.setName(revPathName);
        RenderedServicePathKey revRenderedServicePathKey = new RenderedServicePathKey(revPathName);
        revRenderedServicePathBuilder.setKey(revRenderedServicePathKey);

        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        // Populate new array with elements from existing service path. They will be replaced as we go along
        revRenderedServicePathHopArrayList.addAll(renderedServicePathHopList);
        //int serviceIndex = maxStartingIndex - numServiceHops + 1;

        ListIterator<RenderedServicePathHop> iter = renderedServicePathHopList.listIterator(renderedServicePathHopList.size());
        revServiceHop = 0;
        while(iter.hasPrevious()) {

            RenderedServicePathHop renderedServicePathHop = iter.previous();
            RenderedServicePathHopKey revRenderedServicePathHopKey = new RenderedServicePathHopKey(revServiceHop);
            RenderedServicePathHopBuilder revRenderedServicePathHopBuilder = new RenderedServicePathHopBuilder(renderedServicePathHop);
            revRenderedServicePathHopBuilder.setHopNumber(revServiceHop);
            revRenderedServicePathHopBuilder.setServiceIndex((short) (maxStartingIndex - revServiceHop));
            revRenderedServicePathHopBuilder.setKey(revRenderedServicePathHopKey);
            revRenderedServicePathHopArrayList.set(revServiceHop, revRenderedServicePathHopBuilder.build());
            revServiceHop++;
        }

/*
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {

            revServiceHop = (short) (numServiceHops - renderedServicePathHop.getHopNumber() - 1);
            RenderedServicePathHopKey revRenderedServicePathHopKey = new RenderedServicePathHopKey(revServiceHop);
            RenderedServicePathHopBuilder revRenderedServicePathHopBuilder = new RenderedServicePathHopBuilder(renderedServicePathHop);
            revRenderedServicePathHopBuilder.setHopNumber(revServiceHop);
            revRenderedServicePathHopBuilder.setServiceIndex((short) (serviceIndex + renderedServicePathHop.getHopNumber()));
            revRenderedServicePathHopBuilder.setKey(revRenderedServicePathHopKey);
            revRenderedServicePathHopArrayList.set(revServiceHop, revRenderedServicePathHopBuilder.build());
        }
*/

        revRenderedServicePathBuilder.setRenderedServicePathHop(revRenderedServicePathHopArrayList);

        InstanceIdentifier<RenderedServicePath> rspIID;

        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, revRenderedServicePathKey)
                .build();

        RenderedServicePath revRenderedServicePath = revRenderedServicePathBuilder.build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, revRenderedServicePath, LogicalDatastoreType.OPERATIONAL)) {
            ret = revRenderedServicePath;
        } else {
            LOG.error("{}: Failed to create Reverse Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], revPathName);
        }
        printTraceStop(LOG);
        return ret;

    }


    /**
     * Check a SFF for consistency after datastore creation
     * <p>
     * @param renderedServicePath RSP object
     * @param httpMethod HttpMethod
     * @return Nothing
     */
    public boolean checkServiceFunctionPath(RenderedServicePath renderedServicePath, String httpMethod) {

        printTraceStart(LOG);
        boolean ret;

        ret = invokeServicePathRest(renderedServicePath, httpMethod);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method decouples the SFP API from the SouthBound REST client.
     * SFP APIs call this method to convey SFP information to REST southbound
     * devices
     * <p>
     * @param renderedServicePath Rendered Service Paths
     * @param httpMethod  HTTP method such as GET, PUT, POST..
     * @return Nothing.
     */
    private boolean invokeServicePathRest(RenderedServicePath renderedServicePath, String httpMethod) {

     /* Invoke SB REST API */
        boolean ret = true;

        if (renderedServicePath != null)
        {
            if (httpMethod.equals(HttpMethod.PUT))
            {
                Object[] servicePathObj = {renderedServicePath};
                Class[] servicePathClass = {RenderedServicePath.class};
                 odlSfc.executor.execute(SfcProviderRestAPI.
                        getPutRenderedServicePath(servicePathObj,
                                servicePathClass));
            } else if (httpMethod.equals(HttpMethod.DELETE))
            {
                Object[] servicePathObj = {renderedServicePath};
                Class[] servicePathClass = {RenderedServicePath.class};
                odlSfc.executor.execute(SfcProviderRestAPI.
                        getDeleteRenderedServicePath(servicePathObj,
                                servicePathClass));
            }
        } else {
            LOG.error("Data object is null");
        }
        return ret;

    }

    /**
     * Check a SFF for consistency after datastore creation
     * <p>
     * @param serviceFunctionPath SFP object
     * @param operation HttpMethod
     * @return Nothing
     */
    public static boolean checkServiceFunctionPathExecutor(ServiceFunctionPath serviceFunctionPath, String operation) {

        printTraceStart(LOG);
        boolean ret = false;

        //Send to SB REST
        RenderedServicePath renderedServicePath = SfcProviderServicePathAPI
                .readRenderedServicePath(serviceFunctionPath.getName());
        if (renderedServicePath == null) {
            LOG.error("Failed to find RSP:  {}", serviceFunctionPath.getName());
            return ret;
        }
        Object[] renderedPathObj = {renderedServicePath, operation};
        Class[] renderedPathClass = {RenderedServicePath.class, String.class};
        Future future = odlSfc.executor.submit(SfcProviderServicePathAPI.getCheckServicePathAPI(
                renderedPathObj, renderedPathClass));
        try {
            ret = (boolean) future.get();
            LOG.info("getCheckServicePathAPI returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * Check a SFF for consistency after datastore creation
     * <p>
     * @param renderedServicePath RSP object
     * @param operation HttpMethod
     * @return Nothing
     */
    public static boolean checkServiceFunctionPathExecutor(RenderedServicePath renderedServicePath, String operation) {

        printTraceStart(LOG);
        boolean ret = false;

        Object[] renderedPathObj = {renderedServicePath, operation};
        Class[] renderedPathClass = {RenderedServicePath.class, String.class};
        Future future = odlSfc.executor.submit(SfcProviderServicePathAPI.getCheckServicePathAPI(
                renderedPathObj, renderedPathClass));
        try {
            ret = (boolean) future.get();
            LOG.debug("getCheckServicePathAPI returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("Failed to ...", e);
        } catch (ExecutionException e) {
            LOG.warn("Failed to ...", e);
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them. Additionally, since we are delete the RSP, we also
     * <p>
     * @param serviceFunction Service Function Object
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    @SfcReflection
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
                    LOG.info("{}: SFP {} already deleted by another thread or client",
                            Thread.currentThread().getStackTrace()[1], rspName);
                }
            }
        } else {
            LOG.info("Could not find Service function Paths using Service Function: {} ",
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
            LOG.info("getDeleteServicePathContainingFunction returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }
}
