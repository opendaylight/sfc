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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHopBuilder;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Lisp;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mpls;
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
    private static final String FUNCTION = "function";
    private static final String IP = "ip";
    private static final String LISP = "lisp";
    private static final String MAC = "mac";
    private static final String MPLS = "mpls";
    private static final String FAILED_TO_STR = "failed to ...";
    private static final int MAX_STARTING_INDEX = 255;
    private static AtomicInteger numCreatedPath = new AtomicInteger(0);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();
    private static Map<String, Map<String, List<SffDataPlaneLocator>>> rspNameToRspHopSffDplList = new HashMap<>();

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

    private static Map<java.lang.Class<? extends ServiceFunctionTypeIdentity>, Integer> mapCountRoundRobin = new HashMap<>();
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

    public SfcSelectSfAlgorithmType getSfcSelectSfAlgorithmType()
    {
        return sfcSelectSfAlgorithmType;
    }

    public void setSfcSelectSfAlgorithmType(SfcSelectSfAlgorithmType sfcSelectSfAlgorithmType)
    {
        this.sfcSelectSfAlgorithmType = sfcSelectSfAlgorithmType;
    }

    public static String getRoundRobinServicePathHop(List<SftServiceFunctionName> sftServiceFunctionNameList,
                                                     ServiceFunctionType serviceFunctionType)
    {
        int countRoundRobin = 0;

        if (mapCountRoundRobin.size() != 0){
            for (java.lang.Class<? extends ServiceFunctionTypeIdentity> sfType: mapCountRoundRobin.keySet()){
                if (sfType.equals(serviceFunctionType.getType())) {
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

    public static String sfcSelectServicePathHop(ServiceFunctionType serviceFunctionType,
                                                 SfcSelectSfAlgorithmType sfcSelectSfAlgorithmType)
    {
        List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        LOG.debug("ServiceFunction Name List : {}", sftServiceFunctionNameList);
        String sfcSelectServicePathHopName;

        switch(sfcSelectSfAlgorithmType) {
        case ROUND_ROBIN:
            sfcSelectServicePathHopName = getRoundRobinServicePathHop(sftServiceFunctionNameList,
                    serviceFunctionType);
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
        List<RenderedServicePathHopBuilder> renderedServicePathHopBuilderList = new ArrayList<>();
        Map<String, List<SffDataPlaneLocator>> rspHopSffDplMap = new HashMap<String, List<SffDataPlaneLocator>>();

        /*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         */
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(serviceFunctionChain.getSfcServiceFunction());

        //Collections.sort(sfcServiceFunctionList, Collections.reverseOrder(SF_ORDER));

        LOG.info("BRADY createRenderedServicePathEntry Starting");

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
                        String serviceFunctionForwarderName =
                                serviceFunction.getSfDataPlaneLocator().get(0).getServiceFunctionForwarder();

                        LOG.info("BRADY createRenderedServicePathEntry serviceFunction [{}] SFF [{}]", serviceFunction.getName(), serviceFunctionForwarderName);

                        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
                        renderedServicePathHopBuilder.setHopNumber(posIndex)
                                .setServiceFunctionName(serviceFunctionName)
                                .setServiceIndex((short) serviceIndex)
                                .setServiceFunctionForwarder(serviceFunctionForwarderName);

                        // The SFF DPLs will be processed below, and are used to populate the RSP Hop Ingress DPL
                        rspHopSffDplMap.put(
                                serviceFunctionForwarderName,
                                getSffDataPlaneLocators(serviceFunctionForwarderName));

                        // Only storing builders for now. Later we'll add the sffIngressDpl and build the RSP Hop
                        renderedServicePathHopBuilderList.add(posIndex, renderedServicePathHopBuilder);
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

        LOG.info("BRADY renderedServicePathHopBuilderList size [{}]", renderedServicePathHopBuilderList.size());

        // Iterate the sffRspHopDplList and populate the renderedServicePathHopBuilder, then build it
        int index = 0;
        String prevSffName = null;
        List<RenderedServicePathHop> renderedServicePathHopArrayList = new ArrayList<>();
        ListIterator<RenderedServicePathHopBuilder> rspHopBuilderIter = renderedServicePathHopBuilderList.listIterator();
        while(rspHopBuilderIter.hasNext()) {
            RenderedServicePathHopBuilder renderedServicePathHopBuilder = rspHopBuilderIter.next();
            final String curSffName = renderedServicePathHopBuilder.getServiceFunctionForwarder();

            String sffIngressDpl = null;
            if(index == 0) {
                // Configure the RSP First Hop Ingress DPL
                if(renderedServicePathHopBuilderList.size() > 1) {
                    RenderedServicePathHopBuilder nextRspHopBuilder = renderedServicePathHopBuilderList.get(1);
                    if(nextRspHopBuilder != null && nextRspHopBuilder.getServiceFunctionForwarder() != null) {
                        LOG.info("BRADY getting first RspHop Ingress DPL index [{}] for curSff [{}] to nextSff[{}]",
                                index, curSffName, nextRspHopBuilder.getServiceFunctionForwarder());
                        sffIngressDpl = getFirstSffRspHopIngressDataPlaneLocator(
                            serviceFunctionPath.getTransportType().getName(),
                            curSffName,
                            nextRspHopBuilder.getServiceFunctionForwarder(),
                            rspHopSffDplMap);
                    }
                }
                // TODO what to do if there is only one Hop in the Service Chain
            } else {
                LOG.info("BRADY getting RspHop Ingress DPL index [{}] for Sff [{}]", index, curSffName);
                sffIngressDpl = getSffRspHopIngressDataPlaneLocator(prevSffName, curSffName, rspHopSffDplMap);
            }
            if(sffIngressDpl != null) {
                LOG.info("BRADY setting renderedServicePathHopBuilder.setServiceFunctionForwarderLocator [{}]", sffIngressDpl);
                renderedServicePathHopBuilder.setServiceFunctionForwarderLocator(sffIngressDpl);
            }
            LOG.info("BRADY Building RspHop entry [{}] for Sff [{}]", index, curSffName);
            renderedServicePathHopArrayList.add(index++, renderedServicePathHopBuilder.build());

            prevSffName = renderedServicePathHopBuilder.getServiceFunctionForwarder();
        }

        LOG.info("BRADY renderedServicePathHopArrayList size [{}], index [{}]", renderedServicePathHopArrayList.size(), index);

        // Build the service function path so it can be committed to datastore

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

        renderedServicePathBuilder.setTransportType(serviceFunctionPath.getTransportType());

        // Store the rspHopSffDplList for use in the symmetric RSP creation
        // It will be removed when the symmetric RSP is created
        if(serviceFunctionPath.isSymmetric()) {
            SfcProviderRenderedPathAPI.rspNameToRspHopSffDplList.put(renderedServicePathBuilder.getName(), rspHopSffDplMap);
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

    // Correctly and deterministically select the correct FIRST SFF DPL based
    // on the SFP.transport-type and the adjacent SFF transport details.
    private String getFirstSffRspHopIngressDataPlaneLocator(final String rspTransport, final String curSffName, final String nextSffName, final Map<String, List<SffDataPlaneLocator>> sffToDpls) {
        String adjacentSffDplName = getSffRspHopIngressDataPlaneLocator(curSffName, nextSffName, sffToDpls);
        if(adjacentSffDplName == null) {
            return null;
        }

        List<SffDataPlaneLocator> curSffDplList = sffToDpls.get(curSffName);
        for(SffDataPlaneLocator curSffDpl : curSffDplList) {
            if(curSffDpl.getName().equals(adjacentSffDplName)) {
                // We want to skip the SFF to SFF DPL
                continue;
            }
            if(curSffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                // TODO need to check that its NOT an SffSf DPL
                return curSffDpl.getName();
            }
        }

        return null;
    }

    // Correctly and deterministically select the correct
    // SFF DPL based on the adjacent SFF transport details.
    private String getSffRspHopIngressDataPlaneLocator(final String prevSffName, final String curSffName, final Map<String, List<SffDataPlaneLocator>> sffToDpls) {
        List<SffDataPlaneLocator> prevSffDplList = sffToDpls.get(prevSffName);
        List<SffDataPlaneLocator> curSffDplList = sffToDpls.get(curSffName);

        // TODO this is an O(n squared) search, can be improved using a hash table
        //      considering there should only be 3-4 DPLs, its probably not worth the
        //      extra code to improve it.
        LOG.info("BRADY prevSffDplList size [{}] curSffDplList size [{}]", prevSffDplList.size(), curSffDplList.size());

        for(SffDataPlaneLocator prevSffDpl : prevSffDplList) {
            for(SffDataPlaneLocator curSffDpl : curSffDplList) {
                LocatorType prevLocatorType = prevSffDpl.getDataPlaneLocator().getLocatorType();
                LocatorType curLocatorType = curSffDpl.getDataPlaneLocator().getLocatorType();

                LOG.info("BRADY Comparing prevSff [{}] locator type [{}] to curSff [{}] locator type [{}]",
                        prevSffName, prevLocatorType.getImplementedInterface().getSimpleName(),
                        curSffName, curLocatorType.getImplementedInterface().getSimpleName());

                if(prevLocatorType.getImplementedInterface() == curLocatorType.getImplementedInterface()) {
                    String type = prevLocatorType.getImplementedInterface().getSimpleName().toLowerCase();

                    switch (type) {
                    case FUNCTION:
                        break;
                    case IP:
                        // TODO what makes 2 IP DPLs equal? Assuming its the Port, as each IP will be different
                        if(((Ip) prevLocatorType).getPort().getValue().intValue() == ((Ip) curLocatorType).getPort().getValue().intValue()) {
                            LOG.info("BRADY Found RspHopIngress IP DPL [{}] prevSff [{}] curSff [{}]", curSffDpl.getName(), prevSffName, curSffName);
                            return curSffDpl.getName();
                        }
                        break;
                    case LISP:
                        // TODO does == work on IpAddress???
                        if(((Lisp) prevLocatorType).getEid() == ((Lisp) curLocatorType).getEid()) {
                            LOG.info("BRADY Found RspHopIngress LISP DPL [{}] prevSff [{}] curSff [{}]", curSffDpl.getName(), prevSffName, curSffName);
                            return curSffDpl.getName();
                        }
                        break;
                    case MAC:
                        // TODO for now only checking VLAN Id if present
                        LOG.info("BRADY prevVlan [{}] curVlan [{}]", ((Mac) prevLocatorType).getVlanId(), ((Mac) curLocatorType).getVlanId());
                        if(((Mac) prevLocatorType).getVlanId() != null && ((Mac) curLocatorType).getVlanId() != null) {
                            if(((Mac) prevLocatorType).getVlanId().intValue() == ((Mac) curLocatorType).getVlanId().intValue()) {
                                LOG.info("BRADY Found RspHopIngress VLAN DPL [{}] prevSff [{}] curSff [{}]", curSffDpl.getName(), prevSffName, curSffName);
                                return curSffDpl.getName();
                            }
                        }
                        break;
                    case MPLS:
                        if(((Mpls) prevLocatorType).getMplsLabel().longValue() == ((Mpls) curLocatorType).getMplsLabel().longValue()) {
                            LOG.info("BRADY Found RspHopIngress MPLD DPL [{}] prevSff [{}] curSff [{}]", curSffDpl.getName(), prevSffName, curSffName);
                            return curSffDpl.getName();
                        }
                        break;
                    }
                }
            }
        }

        return null;
    }

    private List<SffDataPlaneLocator> getSffDataPlaneLocators(final String sffName) {
        ServiceFunctionForwarder serviceFunctionForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(sffName);

        List<SffDataPlaneLocator> sffDpls = new ArrayList<SffDataPlaneLocator>();
        sffDpls.addAll(serviceFunctionForwarder.getSffDataPlaneLocator());

        return sffDpls;
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

        Map<String, List<SffDataPlaneLocator>> rspHopSffDplMap =
                SfcProviderRenderedPathAPI.rspNameToRspHopSffDplList.get(renderedServicePath.getName());
        SfcProviderRenderedPathAPI.rspNameToRspHopSffDplList.remove(renderedServicePath.getName());

        ListIterator<RenderedServicePathHop> iter = renderedServicePathHopList.listIterator(renderedServicePathHopList.size());
        revServiceHop = 0;
        String prevSffName = null;
        while(iter.hasPrevious()) {

            RenderedServicePathHop renderedServicePathHop = iter.previous();
            RenderedServicePathHopKey revRenderedServicePathHopKey = new RenderedServicePathHopKey(revServiceHop);
            RenderedServicePathHopBuilder revRenderedServicePathHopBuilder = new RenderedServicePathHopBuilder(renderedServicePathHop);
            revRenderedServicePathHopBuilder.setHopNumber(revServiceHop);
            revRenderedServicePathHopBuilder.setServiceIndex((short) (MAX_STARTING_INDEX - revServiceHop));
            revRenderedServicePathHopBuilder.setKey(revRenderedServicePathHopKey);

            // calculate the RSP Hop Ingress Locator, using the info calculated in the mirrored renderedServicePath
            String sffIngressDpl = null;
            if(revServiceHop == 0) {
                if(renderedServicePathHopList.size() > 1) {
                    // Get the penultimate SFF name to calculate the First SFF Hop DPL in the reverse direction
                    RenderedServicePathHop nextRspHop = renderedServicePathHopList.get(renderedServicePathHopList.size()-2);
                    if(nextRspHop != null && nextRspHop.getServiceFunctionForwarder() != null) {
                        LOG.info("BRADY getting first Reverse renderedServicePathHopBuilder");
                        sffIngressDpl = getFirstSffRspHopIngressDataPlaneLocator(
                                    renderedServicePath.getTransportType().getName(),
                                    revRenderedServicePathHopBuilder.getServiceFunctionForwarder(),
                                    nextRspHop.getServiceFunctionForwarder(),
                                    rspHopSffDplMap);
                    }
                }

            } else {
                sffIngressDpl = getSffRspHopIngressDataPlaneLocator(
                                          prevSffName,
                                          revRenderedServicePathHopBuilder.getServiceFunctionForwarder(),
                                          rspHopSffDplMap);
            }
            if(sffIngressDpl != null) {
                LOG.info("BRADY setting Reverse renderedServicePathHopBuilder.setServiceFunctionForwarderLocator [{}]", sffIngressDpl);
                revRenderedServicePathHopBuilder.setServiceFunctionForwarderLocator(sffIngressDpl);
            }
            revRenderedServicePathHopArrayList.set(revServiceHop, revRenderedServicePathHopBuilder.build());
            prevSffName = revRenderedServicePathHopBuilder.getServiceFunctionForwarder();
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
        revRenderedServicePathBuilder.setTransportType(renderedServicePath.getTransportType());

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

    /**
     * This method provides all necessary information for a system to construct
     * a NSH header and associated overlay packet to target the first
     * service hop of a Rendered Service Path
     * <p>
     * @param rspName RSP name
     * @return Nothing.
     */
    public static RenderedServicePathFirstHop readRenderedServicePathFirstHop (String rspName) {
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
}
