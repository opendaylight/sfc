/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcReflection;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Random;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.LoadBalance;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ShortestPath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

//import javax.ws.rs.HttpMethod;

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

    private SfcServiceFunctionSchedulerAPI scheduler;

    private void initServiceFunctionScheduler()
    {
        java.lang.Class<? extends org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerType;

        try {
            serviceFunctionSchedulerType = SfcProviderScheduleTypeAPI
                    .readEnabledServiceFunctionScheduleTypeEntryExecutor().getType();
        } catch (Exception e) {
            serviceFunctionSchedulerType = Random.class;
        }

        if (serviceFunctionSchedulerType == RoundRobin.class) {
            scheduler = new SfcServiceFunctionRoundRobinSchedulerAPI();
        } else if (serviceFunctionSchedulerType == LoadBalance.class) {
            scheduler = new SfcServiceFunctionLoadBalanceSchedulerAPI();
        } else if (serviceFunctionSchedulerType == Random.class) {
            scheduler = new SfcServiceFunctionRandomSchedulerAPI();
        } else if (serviceFunctionSchedulerType == ShortestPath.class) {
            scheduler = new SfcServiceFunctionShortestPathSchedulerAPI();
        } else {
            scheduler = new SfcServiceFunctionRandomSchedulerAPI();
        }

        LOG.info("Selected SF Schdedule Type: {}",  serviceFunctionSchedulerType);
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

    SfcProviderRenderedPathAPI(Object[] params, String m) {
        super(params, m);
        initServiceFunctionScheduler();
    }

    SfcProviderRenderedPathAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
        initServiceFunctionScheduler();
    }


    public static SfcProviderRenderedPathAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderRenderedPathAPI(params, paramsTypes, "readRenderedServicePath");
    }
    public static SfcProviderRenderedPathAPI getReadRenderedServicePath(Object[] params, Class[] paramsTypes) {
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

    @SuppressWarnings("unused")
/*    protected void updateRenderedServicePathEntry (ServiceFunctionPath serviceFunctionPath) {
        this.createRenderedServicePathEntry(serviceFunctionPath);
    }*/

    public static RenderedServicePath createRenderedServicePathEntryExecutor(ServiceFunctionPath serviceFunctionPath,
                                                                             CreateRenderedPathInput createRenderedPathInput) {
        RenderedServicePath ret = null;
        Object[] servicePathObj = {serviceFunctionPath, createRenderedPathInput};
        Class[] servicePathClass = {ServiceFunctionPath.class, CreateRenderedPathInput.class};
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
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
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
    public static RenderedServicePath createRenderedServicePathAndState(ServiceFunctionPath createdServiceFunctionPath,
                                                                        CreateRenderedPathInput createRenderedPathInput) {

        RenderedServicePath renderedServicePath;

        boolean rspSuccessful = false;
        boolean addPathToSffStateSuccessful = false;
        boolean addPathToSfStateSuccessful = false;
        boolean addPathtoSfpStateSuccessful = false;

        // Create RSP
        if ((renderedServicePath = SfcProviderRenderedPathAPI
                .createRenderedServicePathEntryExecutor(createdServiceFunctionPath, createRenderedPathInput)) != null) {
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
            SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(renderedServicePath.getName());
        }

        // Add Path to SF operational state
        if (addPathToSffStateSuccessful &&
                SfcProviderServiceFunctionAPI
                        .addPathToServiceFunctionStateExecutor(renderedServicePath)) {

            addPathToSfStateSuccessful = true;
        } else {
            SfcProviderServiceForwarderAPI
                    .deletePathFromServiceForwarderStateExecutor(createdServiceFunctionPath);
            SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(renderedServicePath.getName());

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
            SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(renderedServicePath.getName());

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
     * Given a list of Service Functions, create a RenderedServicePath Hop List
     *
     * @param serviceFunctionNameList
     * @param sfgNameList
     * @param serviceIndex
     * @return
     */
    protected List<RenderedServicePathHop> createRenderedServicePathHopList(List<String> serviceFunctionNameList, List<String> sfgNameList, int serviceIndex) {
        List<RenderedServicePathHop> renderedServicePathHopArrayList = new ArrayList<>();
        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();

        int initialServiceIndex = serviceIndex;
        short posIndex = 0;

        if (serviceFunctionNameList == null && sfgNameList == null) {
            LOG.error("Could not create the hop list caused by empty name list");
            return null;
        }

        if (sfgNameList != null) {
            boolean loopBroken = false;
            for (String sfgName : sfgNameList) {
                ServiceFunctionGroup sfg = SfcProviderServiceFunctionGroupAPI.readServiceFunctionGroupExecutor(sfgName);
                if (sfg == null) {
                    LOG.error("Could not find suitable SFG in data store by name: {}", sfgName);
                    loopBroken = true;
                    break;
                }
                ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(sfg.getSfcServiceFunction().get(0).getName());
                if (serviceFunction == null) {
                    LOG.error("Could not find suitable SF in data store by name: {}", sfg.getSfcServiceFunction().get(0).getName());
                    loopBroken = true;
                    break;
                }
                createSFGHopBuilder(serviceIndex, renderedServicePathHopBuilder, posIndex, sfg.getName(), serviceFunction);
                renderedServicePathHopArrayList.add(posIndex, renderedServicePathHopBuilder.build());
                serviceIndex--;
                posIndex++;
            }
            if (loopBroken) {
                renderedServicePathHopArrayList.clear();
                posIndex = 0;
                serviceIndex = initialServiceIndex;
            }
        } else {
            for (String serviceFunctionName : serviceFunctionNameList) {
                ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(serviceFunctionName);
                if (serviceFunction == null) {
                    LOG.error("Could not find suitable SF in data store by name: {}", serviceFunctionName);
                    return null;
                }
                createSFHopBuilder(serviceIndex, renderedServicePathHopBuilder, posIndex, serviceFunctionName, serviceFunction);
                renderedServicePathHopArrayList.add(posIndex, renderedServicePathHopBuilder.build());
                serviceIndex--;
                posIndex++;
            }
        }

        return renderedServicePathHopArrayList;
    }

    private void createSFHopBuilder(int serviceIndex, RenderedServicePathHopBuilder renderedServicePathHopBuilder,
            short posIndex, String serviceFunctionName, ServiceFunction serviceFunction) {
        createHopBuilderInternal(serviceIndex, renderedServicePathHopBuilder, posIndex, serviceFunction);
        renderedServicePathHopBuilder.setServiceFunctionName(serviceFunctionName);
    }

    private void createSFGHopBuilder(int serviceIndex, RenderedServicePathHopBuilder renderedServicePathHopBuilder,
            short posIndex, String serviceFunctionGroupName, ServiceFunction serviceFunction) {
        createHopBuilderInternal(serviceIndex, renderedServicePathHopBuilder, posIndex, serviceFunction);
        renderedServicePathHopBuilder.setServiceFunctionGroupName(serviceFunctionGroupName);
    }

    private void createHopBuilderInternal(int serviceIndex, RenderedServicePathHopBuilder renderedServicePathHopBuilder,
            short posIndex, ServiceFunction serviceFunction) {
        String serviceFunctionForwarderName =
                serviceFunction.getSfDataPlaneLocator().get(0).getServiceFunctionForwarder();

        ServiceFunctionForwarder serviceFunctionForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(serviceFunctionForwarderName);
        if (serviceFunctionForwarder != null &&
                serviceFunctionForwarder.getSffDataPlaneLocator() != null &&
                serviceFunctionForwarder.getSffDataPlaneLocator().get(0) != null) {
            renderedServicePathHopBuilder.
                    setServiceFunctionForwarderLocator(serviceFunctionForwarder.getSffDataPlaneLocator().get(0).getName());
        }

        renderedServicePathHopBuilder.setHopNumber(posIndex)
        .setServiceIndex((short) serviceIndex)
        .setServiceFunctionForwarder(serviceFunctionForwarderName);
    }

    /**
     * Create a Rendered Path and all the associated operational state based on the
     * given rendered service path
     *
     * <p>
     * @param serviceFunctionPath RSP Object
     * @return Nothing.
     */
    protected RenderedServicePath createRenderedServicePathEntry (ServiceFunctionPath serviceFunctionPath,
                                                                  CreateRenderedPathInput createRenderedPathInput) {

        printTraceStart(LOG);

        long pathId;
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


        // Descending order
        serviceIndex = MAX_STARTING_INDEX;

        List<String> sfgNameList = getSfgNameList(serviceFunctionChain);
        List<String> sfNameList = scheduler.scheduleServiceFunctions(serviceFunctionChain, serviceIndex);
        if(sfNameList == null && sfgNameList == null) {
            LOG.warn("createRenderedServicePathEntry scheduler.scheduleServiceFunctions() returned null list");
            return null;
        }
        List<RenderedServicePathHop> renderedServicePathHopArrayList = createRenderedServicePathHopList(sfNameList, sfgNameList, serviceIndex);

        if (renderedServicePathHopArrayList == null) {
            LOG.warn("createRenderedServicePathEntry createRenderedServicePathHopList returned null list");
            return null;
        }

        //Build the service function path so it can be committed to datastore
        pathId = (serviceFunctionPath.getPathId() != null)  ?
                        serviceFunctionPath.getPathId() :
                        numCreatedPathIncrementGet();
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopArrayList);
        if (createRenderedPathInput.getName() == null || createRenderedPathInput.getName().isEmpty())  {
            renderedServicePathBuilder.setName(serviceFunctionPath.getName() + "-Path-" + pathId);
        } else {
            renderedServicePathBuilder.setName(createRenderedPathInput.getName());

        }

        renderedServicePathBuilder.setPathId(pathId);
        // TODO: Find out the exact rules for service index generation
        //renderedServicePathBuilder.setStartingIndex((short) renderedServicePathHopArrayList.size());
        renderedServicePathBuilder.setStartingIndex((short) MAX_STARTING_INDEX);
        renderedServicePathBuilder.setServiceChainName(serviceFunctionChainName);
        renderedServicePathBuilder.setParentServiceFunctionPath(serviceFunctionPath.getName());
        if(serviceFunctionPath.getTransportType() == null) {
            // TODO this is a temporary workaround to a YANG problem
            //      Even though the SFP.transportType is defined with a default, if its not
            //      specified in the configuration, it can still return null
            renderedServicePathBuilder.setTransportType(VxlanGpe.class);
        } else {
            renderedServicePathBuilder.setTransportType(serviceFunctionPath.getTransportType());
        }

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

    private List<String> getSfgNameList(ServiceFunctionChain serviceFunctionChain) {
        List<String> ret = new ArrayList<String>();
        List<SfcServiceFunction> sfcServiceFunction = serviceFunctionChain.getSfcServiceFunction();
        LOG.debug("searching groups for chain {} which has the elements {}", serviceFunctionChain.getName(), serviceFunctionChain.getSfcServiceFunction());
        for(SfcServiceFunction sf : sfcServiceFunction){
            ServiceFunctionGroup sfg = SfcProviderServiceFunctionGroupAPI.getServiceFunctionGroupbyTypeExecutor(sf.getType());
            LOG.debug("look for service function group of type {} and found {}", sf.getType() , sfg);
            if(sfg != null){
                ret.add(sfg.getName());
            } else {
                return null;
            }
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

    @SuppressWarnings("unused")
    public static RenderedServicePath readRenderedServicePathExecutor(String rspName) {

        printTraceStart(LOG);
        RenderedServicePath ret = null;

        Object[] rspNameObj = {rspName};
        Class[] rspNameClass = {String.class};

        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = SfcProviderRenderedPathAPI
                .getReadRenderedServicePath(rspNameObj, rspNameClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderRenderedPathAPI);
        try {
            ret = (RenderedServicePath) future.get();
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
        }
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

        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(rspName);
        InstanceIdentifier<RenderedServicePath> rspIID =
                InstanceIdentifier.builder(RenderedServicePaths.class)
                    .child(RenderedServicePath.class, renderedServicePathKey)
                    .build();

        RenderedServicePath rsp = SfcDataStoreAPI.readTransactionAPI(rspIID, LogicalDatastoreType.OPERATIONAL);

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
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
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
                .child(RenderedServicePath.class, renderedServicePathKey).build();

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
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
        }
        if (!ret) {
            LOG.error("{}: Failed to delete RSP {}", Thread.currentThread().getStackTrace()[1],
                    renderedServicePathName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method provides all necessary information for a system to construct
     * a NSH header and associated overlay packet to target the first
     * service hop of a Rendered Service Path
     * <p>
     * @param rspName RSP name
     * @return Nothing.
     */
    public static RenderedServicePathFirstHop readRenderedServicePathFirstHop (String rspName) {
        final String FUNCTION = "function";
        final String IP = "ip";
        final String LISP = "lisp";
        final String MAC = "mac";
        final String MPLS = "mpls";

        RenderedServicePathFirstHop renderedServicePathFirstHop = null;

        RenderedServicePath renderedServicePath = readRenderedServicePath(rspName);
        if (renderedServicePath != null) {
            RenderedServicePathFirstHopBuilder renderedServicePathFirstHopBuilder = new RenderedServicePathFirstHopBuilder();
            renderedServicePathFirstHopBuilder.setPathId(renderedServicePath.getPathId())
                    .setStartingIndex(renderedServicePath.getStartingIndex());

            List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
            RenderedServicePathHop renderedServicePathHop = renderedServicePathHopList.get(0);

            String sffName = renderedServicePathHop.getServiceFunctionForwarder();
            String sffLocatorName  = renderedServicePathHop.getServiceFunctionForwarderLocator();
            SffDataPlaneLocator sffDataPlaneLocator = SfcProviderServiceForwarderAPI
                    .readServiceFunctionForwarderDataPlaneLocator(sffName, sffLocatorName);

            if (sffDataPlaneLocator != null) {
                String type = sffDataPlaneLocator.getDataPlaneLocator().getLocatorType().getImplementedInterface()
                        .getSimpleName().toLowerCase();

                switch (type) {
                    case FUNCTION:
                        break;
                    case IP:
                        Ip ipLocator = (Ip) sffDataPlaneLocator.getDataPlaneLocator().getLocatorType();
                        if (ipLocator.getIp() != null) {
                            renderedServicePathFirstHopBuilder.setIp(ipLocator.getIp());
                            if (ipLocator.getPort() != null) {
                                renderedServicePathFirstHopBuilder.setPort(ipLocator.getPort());
                            }
                        }
                        // IP means VXLAN-GPE, later we might have other options...
                        renderedServicePathFirstHopBuilder.setTransportType(VxlanGpe.class);
                        break;
                    case LISP:
                        break;
                    case MAC:
                        break;
                    case MPLS:
                        // TODO: Brady
                        break;
                }
            } else {
                LOG.error("{}: Failed to read data plane locator {} for SFF {}",
                        Thread.currentThread().getStackTrace()[1], sffLocatorName, sffName);
            }
            renderedServicePathFirstHop = renderedServicePathFirstHopBuilder.build();
        }

        return renderedServicePathFirstHop;
    }

    private static String getServiceTypeName(Class<? extends ServiceFunctionTypeIdentity> serviceFunctionType) {
        String serviceTypeName = null;

        printTraceStart(LOG);
        if (serviceFunctionType == Dpi.class) {
            serviceTypeName = "dpi";
        } else if (serviceFunctionType == Firewall.class) {
            serviceTypeName = "firewall";
        } else if (serviceFunctionType == HttpHeaderEnrichment.class) {
            serviceTypeName = "http-header-enrichment";
        } else if (serviceFunctionType == Ids.class) {
            serviceTypeName = "ids";
        } else if (serviceFunctionType == Napt44.class) {
            serviceTypeName = "napt44";
        } else if (serviceFunctionType == Qos.class) {
            serviceTypeName = "qos";
        } else {
            LOG.error("Unknown ServiceFunctionTypeIdentity: {}", serviceFunctionType.getName());
        }
        printTraceStop(LOG);
        return serviceTypeName;
    }

    /**
     * This method gets all necessary information for a system to construct
     * a NSH header and associated overlay packet to target the first
     * service hop of a Rendered Service Path by ServiceFunctionTypeIdentity
     * list
     * <p>
     * @param serviceFunctionTypeList ServiceFunctionTypeIdentity list
     * @return RenderedServicePathFirstHop.
     */
    public static RenderedServicePathFirstHop readRspFirstHopBySftList(List<Class<? extends ServiceFunctionTypeIdentity>> serviceFunctionTypeList) {
        int i;
        String serviceTypeName;
        Class serviceFunctionType = null;
        List<SfcServiceFunction> sfcServiceFunctionArrayList = new ArrayList<>();
        String sfcName = "chain-sfc-gbp";
        String pathName = "path-sfc-gbp";
        ServiceFunctionChain serviceFunctionChain = null;
        boolean ret = false;
        RenderedServicePathFirstHop firstHop = null;

        printTraceStart(LOG);

        /* Build sfcName, pathName and ServiceFunction list */
        for (i = 0; i < serviceFunctionTypeList.size(); i++) {
            serviceFunctionType = serviceFunctionTypeList.get(i);
            serviceTypeName = getServiceTypeName(serviceFunctionType);
            if (serviceTypeName == null) {
                LOG.error("Unknowed ServiceFunctionTypeIdentity: {}", serviceFunctionType.getName());
                return null;
            }
            sfcName = sfcName + "-" + serviceTypeName;
            pathName = pathName + "-" + serviceTypeName;
            SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
            sfcServiceFunctionArrayList.add(sfcServiceFunctionBuilder.setName(serviceTypeName + "-gbp-sfc").setType(serviceFunctionType).build());
        }

        /* Read service chain sfcName if it exists */
        serviceFunctionChain = SfcProviderServiceChainAPI.readServiceFunctionChainExecutor(sfcName);

        /* Create service chain sfcName if it doesn't exist */
        if (serviceFunctionChain == null) {
            //Create ServiceFunctionChain
            ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
            sfcBuilder.setName(sfcName).setSfcServiceFunction(sfcServiceFunctionArrayList);
            serviceFunctionChain = sfcBuilder.build();
            ret = SfcProviderServiceChainAPI.putServiceFunctionChainExecutor(serviceFunctionChain);
            if (ret == false) {
                LOG.error("Failed to create ServiceFunctionChain: {}", sfcName);
                return null;
            }
        }

        /* Read ServiceFunctionPath pathName if it exists */
        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPathExecutor(pathName);

        /* Create ServiceFunctionPath pathName if it doesn't exist */
        if (serviceFunctionPath == null) {
            /* Create ServiceFunctionPath pathName if it doesn't exist */
            ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
            pathBuilder.setName(pathName)
                       .setServiceChainName(sfcName);
            serviceFunctionPath = pathBuilder.build();
            ret = SfcProviderServicePathAPI.putServiceFunctionPathExecutor(serviceFunctionPath);
            if (ret == false) {
                LOG.error("Failed to create ServiceFunctionPath: {}", pathName);
                return null;
            }
        }

        /* Create RenderedServicePath */
        RenderedServicePath renderedServicePath;
        RenderedServicePath revRenderedServicePath;

        /* We need to provide the same information as we would through the RPC */

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setSymmetric(serviceFunctionPath.isSymmetric());

        renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath,
                createRenderedPathInputBuilder.build());
        if (renderedServicePath == null) {
            LOG.error("Failed to create RenderedServicePath for ServiceFunctionPath: {}", pathName);
            return null;
        }

        if ((serviceFunctionPath.getClassifier() != null) &&
             SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(serviceFunctionPath.getClassifier()) != null) {
            SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor(serviceFunctionPath.getClassifier(), renderedServicePath.getName());
        } else {
            LOG.warn("Classifier not provided or does not exist");
        }

        if ((serviceFunctionPath.isSymmetric() != null) && serviceFunctionPath.isSymmetric()) {
            revRenderedServicePath = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePath);
            if (revRenderedServicePath == null) {
                LOG.error("Failed to create symmetric RenderedServicePath for ServiceFunctionPath: {}", pathName);
            } else if ((serviceFunctionPath.getSymmetricClassifier() != null) &&
                        SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(serviceFunctionPath.getSymmetricClassifier()) != null) {
                SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor(serviceFunctionPath.getSymmetricClassifier(), revRenderedServicePath.getName());
            } else {
                LOG.warn("Symmetric Classifier not provided or does not exist");
            }
        }

        printTraceStop(LOG);
        firstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(renderedServicePath.getName());
        return firstHop;
    }
}
