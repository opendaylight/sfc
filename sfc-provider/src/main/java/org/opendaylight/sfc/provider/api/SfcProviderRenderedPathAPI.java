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
import org.opendaylight.sfc.provider.SfcReflection;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import javax.ws.rs.HttpMethod;
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
 * This class has the APIs to operate on the Service Classifier datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * <p/>
 * @since 2014-11-04
 */
public class SfcProviderRenderedPathAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderRenderedPathAPI.class);
    private static final String FAILED_TO_STR = "failed to ...";
    private static final int MAX_STARTING_INDEX = 255;
    private static AtomicInteger numCreatedPath = new AtomicInteger(0);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();
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

    SfcProviderRenderedPathAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderRenderedPathAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }


    public static SfcProviderRenderedPathAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderRenderedPathAPI(params, paramsTypes, "readRenderedServicePath");
    }
    public static  SfcProviderRenderedPathAPI getCreateRenderedServicePathEntryAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderRenderedPathAPI(params, paramsTypes, "createRenderedServicePathEntry");
    }
    public static  SfcProviderRenderedPathAPI getCreateReverseRenderedServicePathEntryAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderRenderedPathAPI(params, paramsTypes, "createReverseRenderedServicePathEntry");
    }
    @SuppressWarnings("unused")
    public static SfcProviderRenderedPathAPI getDeleteRenderedServicePath(Object[] params, Class[] paramsTypes) {
        return new SfcProviderRenderedPathAPI(params, paramsTypes, "deleteRenderedServicePath");
    }
    @SuppressWarnings("unused")
    public static SfcProviderRenderedPathAPI getDeleteRenderedServicePaths(Object[] params, Class[] paramsTypes) {
        return new SfcProviderRenderedPathAPI(params, paramsTypes, "deleteRenderedServicePaths");
    }
    @SuppressWarnings("unused")
    public static  SfcProviderRenderedPathAPI getUpdateRenderedServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderRenderedPathAPI(params, paramsTypes, "updateRenderedServicePathEntry");
    }

    public SfcSelectSfAlgorithmType getSfcSelectSfAlgorithmType()
    {
        return sfcSelectSfAlgorithmType;
    }

    public void setSfcSelectSfAlgorithmType(SfcSelectSfAlgorithmType sfcSelectSfAlgorithmType)
    {
        this.sfcSelectSfAlgorithmType = sfcSelectSfAlgorithmType;
    }

    public static String getRoundRobinServicePathHop(List<SftServiceFunctionName> sftServiceFunctionNameList, ServiceFunctionType serviceFunctionType)
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

    public static String getRandomServicePathHop(List<SftServiceFunctionName> sftServiceFunctionNameList)
    {
        Random rad = new Random();
        return sftServiceFunctionNameList.get(rad.nextInt(sftServiceFunctionNameList.size())).getName();
    }

    public static String sfcSelectServicePathHop(ServiceFunctionType serviceFunctionType, SfcSelectSfAlgorithmType sfcSelectSfAlgorithmType)
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
    protected void updateRenderedServicePathEntry (ServiceFunctionPath serviceFunctionPath) {
        this.createRenderedServicePathEntry(serviceFunctionPath);
    }

    public static RenderedServicePath createRenderedServicePathEntryExecutor(ServiceFunctionPath serviceFunctionPath) {
        RenderedServicePath ret = null;
        Object[] servicePathObj = {serviceFunctionPath};
        Class[] servicePathClass = {ServiceFunctionPath.class};
        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = SfcProviderRenderedPathAPI
                .getCreateRenderedServicePathEntryAPI(servicePathObj, servicePathClass);
        Future futureCreateRSP = ODL_SFC.getExecutor().submit(sfcProviderRenderedPathAPI);
        try {
            ret = (RenderedServicePath) futureCreateRSP.get();
            LOG.debug("getCreateRenderedServicePathEntryAPI: {}", futureCreateRSP.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        return ret;
    }

    public static RenderedServicePath createReverseRenderedServicePathEntryExecutor(RenderedServicePath renderedServicePath) {
        RenderedServicePath ret = null;
        Object[] servicePathObj = {renderedServicePath};
        Class[] servicePathClass = {RenderedServicePath.class};
        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = SfcProviderRenderedPathAPI
                .getCreateReverseRenderedServicePathEntryAPI(servicePathObj, servicePathClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderRenderedPathAPI);
        try {
            ret = (RenderedServicePath) future.get();
            LOG.debug("getCreateRenderedServicePathEntryAPI: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        return ret;
    }

    /**
     * Creates a RSP and all the associated operational state based on the
     * given service function path
     *
     * <p>
     * @param createdServiceFunctionPath Service Function Path
     * @return Created RSP or null
     */
    public static RenderedServicePath createRenderedServicePathAndState(ServiceFunctionPath createdServiceFunctionPath) {

        RenderedServicePath renderedServicePath;

        boolean rspSuccessful = false;
        boolean addPathToSffStateSuccessful = false;
        boolean addPathToSfStateSuccessful = false;
        boolean addPathtoSfpStateSuccessful = false;

        // Create RSP
        if ((renderedServicePath = SfcProviderRenderedPathAPI
                .createRenderedServicePathEntryExecutor(createdServiceFunctionPath)) != null) {
            rspSuccessful = true;

        } else {
            LOG.error("Could not create RSP. System state inconsistent. Deleting and add SFP {} back",
                    createdServiceFunctionPath.getName());
        }
        // Add Path name to SFF operational state
        if (rspSuccessful &&  SfcProviderServiceForwarderAPI
                .addPathToServiceForwarderStateExecutor(renderedServicePath)) {
            addPathToSffStateSuccessful = true;
        } else {
            SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(createdServiceFunctionPath.getName());
        }

        // Add Path to SF operational state
        if (addPathToSffStateSuccessful &&
                SfcProviderServiceFunctionAPI
                        .addPathToServiceFunctionStateExecutor(renderedServicePath)) {

            addPathToSfStateSuccessful = true;

            //Send to SB REST
            //SfcProviderServicePathAPI.checkServiceFunctionPathExecutor
            //        (renderedServicePath, HttpMethod.PUT);
        } else {
            SfcProviderServiceForwarderAPI
                    .deletePathFromServiceForwarderStateExecutor(createdServiceFunctionPath);
            SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(createdServiceFunctionPath.getName());

        }
        // Add RSP to SFP operational state
        if (addPathToSfStateSuccessful &&
                SfcProviderServicePathAPI.addRenderedPathToServicePathStateExecutor
                        (createdServiceFunctionPath.getName(),renderedServicePath.getName())) {
            addPathtoSfpStateSuccessful = true;

        } else {
            SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor(createdServiceFunctionPath.getName());
            SfcProviderServiceForwarderAPI
                    .deletePathFromServiceForwarderStateExecutor(createdServiceFunctionPath);
            SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(createdServiceFunctionPath.getName());

        }

        return renderedServicePath;
    }

    /**
     * Create a Symmetric Path and all the associated operational state based on the
     * given rendered service path
     *
     * <p>
     * @param renderedServicePath RSP Object
     * @return Nothing.
     */
    public static RenderedServicePath createSymmetricRenderedServicePathAndState(RenderedServicePath renderedServicePath) {

        RenderedServicePath revRenderedServicePath = null;
        boolean revRspSuccessful = false;
        boolean addRevPathToSffStateSuccessul = false;
        boolean addRevPathToSfStateSuccessul = false;
        boolean addRevPathToSfpStateSuccessul = false;

        // Reverse Path

        if ((revRenderedServicePath = SfcProviderRenderedPathAPI
                .createReverseRenderedServicePathEntryExecutor(renderedServicePath)) != null) {
            revRspSuccessful = true;
        } else {
            LOG.error("Could not create Reverse RSP {}",  renderedServicePath.getName());
        }

        // Add Path name to SFF operational state
        if (revRspSuccessful &&  SfcProviderServiceForwarderAPI
                .addPathToServiceForwarderStateExecutor(revRenderedServicePath)) {
            addRevPathToSffStateSuccessul = true;
        } else {
            SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(revRenderedServicePath.getName());
        }

        // Add Path to SF operational state
        if (addRevPathToSffStateSuccessul &&
                SfcProviderServiceFunctionAPI
                        .addPathToServiceFunctionStateExecutor(revRenderedServicePath)) {

            addRevPathToSfStateSuccessul = true;
            //Send to SB REST
/*            SfcProviderServicePathAPI.checkServiceFunctionPathExecutor
                    (revRenderedServicePath,HttpMethod.PUT);*/
        } else {
            SfcProviderServiceForwarderAPI
                    .deletePathFromServiceForwarderStateExecutor(revRenderedServicePath.getName());
            SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(revRenderedServicePath.getName());

        }
        // Add RSP to SFP operational state
        if (addRevPathToSfStateSuccessul &&
                SfcProviderServicePathAPI.addRenderedPathToServicePathStateExecutor
                        (renderedServicePath.getParentServiceFunctionPath(), revRenderedServicePath.getName())) {
            addRevPathToSfpStateSuccessul = true;

        } else {
            SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor
                    (revRenderedServicePath.getName());
            SfcProviderServiceForwarderAPI
                    .deletePathFromServiceForwarderStateExecutor(revRenderedServicePath.getName());
            SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(revRenderedServicePath.getName());

        }
        return revRenderedServicePath;
    }

    /**
     * Create a Rendered Path and all the associated operational state based on the
     * given rendered service path
     *
     * <p>
     * @param serviceFunctionPath RSP Object
     * @return Nothing.
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
        List<RenderedServicePathHop> renderedServicePathHopArrayList = new ArrayList<>();
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
        serviceIndex = MAX_STARTING_INDEX;
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.debug("ServiceFunction name: {}", sfcServiceFunction.getName());


            /*
             * We iterate thorough the list of service function types and for each one we try to get
             * get a suitable Service Function. WE need to perform lots of checking to make sure
             * we do not hit NULL Pointer exceptions
             */

            ServiceFunctionType serviceFunctionType;

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
        renderedServicePathBuilder.setStartingIndex((short) MAX_STARTING_INDEX);
        renderedServicePathBuilder.setServiceChainName(serviceFunctionChainName);
        renderedServicePathBuilder.setParentServiceFunctionPath(serviceFunctionPath.getName());

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

    /**
     * Creates a RSP that is mirror image of the given one. It reverses the
     * hop list and adjusts hop number and service index accordingly
     * <p>
     * @param renderedServicePath RSP object
     * @return Nothing
     */
    public RenderedServicePath createReverseRenderedServicePathEntry(RenderedServicePath renderedServicePath) {


        RenderedServicePath ret = null;
        long pathId = numCreatedPathIncrementGet();
        String revPathName;
        short revServiceHop;
        //int numServiceHops = renderedServicePath.getRenderedServicePathHop().size();
        List<RenderedServicePathHop> revRenderedServicePathHopArrayList = new ArrayList<>();
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
        //int serviceIndex = MAX_STARTING_INDEX - numServiceHops + 1;

        ListIterator<RenderedServicePathHop> iter = renderedServicePathHopList.listIterator(renderedServicePathHopList.size());
        revServiceHop = 0;
        while(iter.hasPrevious()) {

            RenderedServicePathHop renderedServicePathHop = iter.previous();
            RenderedServicePathHopKey revRenderedServicePathHopKey = new RenderedServicePathHopKey(revServiceHop);
            RenderedServicePathHopBuilder revRenderedServicePathHopBuilder = new RenderedServicePathHopBuilder(renderedServicePathHop);
            revRenderedServicePathHopBuilder.setHopNumber(revServiceHop);
            revRenderedServicePathHopBuilder.setServiceIndex((short) (MAX_STARTING_INDEX - revServiceHop));
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
            if (SfcProviderRenderedPathAPI.readRenderedServicePath(rspName) != null) {
                if (SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName)) {
                    ret = true;
                } else {
                    LOG.error("Could not delete RSP: {}", rspName);
                    ret = false;
                }
            } else {
                LOG.debug("RSP {} already deleted by another thread or client", rspName);
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

        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = SfcProviderRenderedPathAPI
                .getDeleteRenderedServicePaths(servicePathObj, servicePathClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderRenderedPathAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteRenderedServicePaths: {}", ret);
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }

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
        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = SfcProviderRenderedPathAPI
                .getDeleteRenderedServicePath(servicePathObj, servicePathClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderRenderedPathAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteRenderedServicePath: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        if (!ret) {
            LOG.error("{}: Failed to delete RSP {}", Thread.currentThread().getStackTrace()[1],
                    renderedServicePathName);
        }
        printTraceStop(LOG);
        return ret;
    }


}
