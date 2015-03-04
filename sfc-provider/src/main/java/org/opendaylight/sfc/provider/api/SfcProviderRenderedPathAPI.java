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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.HttpHeaderEnrichment;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Ids;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Napt44;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Qos;
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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import org.opendaylight.sfc.provider.api.SfcServiceFunctionSchedulerAPI.SfcServiceFunctionSchedulerType;


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

    private SfcServiceFunctionSchedulerType sfcServiceFunctionSchedulerType = SfcServiceFunctionSchedulerType.ROUND_ROBIN;
    private SfcServiceFunctionSchedulerAPI scheduler;

    private void initServiceFuntionScheduler()
    {
        //TODO: read schedule type from datastore and init scheduler.
        switch(sfcServiceFunctionSchedulerType) {
        case ROUND_ROBIN:
            scheduler = new SfcServiceFunctionRoundRobinSchedulerAPI();
            break;
        case RANDOM:
        default:
            scheduler = new SfcServiceFunctionRandomSchedulerAPI();
            break;
        }
    }

    public void setSfcServiceFunctionSchedulerType(SfcServiceFunctionSchedulerType type)
    {
        this.sfcServiceFunctionSchedulerType = type;
    }

    public SfcServiceFunctionSchedulerType getSfcServiceFunctionSchedulerType()
    {
        return this.sfcServiceFunctionSchedulerType;
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
        initServiceFuntionScheduler();
    }

    SfcProviderRenderedPathAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
        initServiceFuntionScheduler();
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

        // Descending order
        serviceIndex = MAX_STARTING_INDEX;

        List<RenderedServicePathHop> renderedServicePathHopArrayList = scheduler.scheduleServiceFuntions(serviceFunctionChain, serviceIndex);
        LOG.info("BRADY createRenderedServicePathEntry renderedServicePathHopArrayList size [{}]", renderedServicePathHopArrayList.size());
        if (renderedServicePathHopArrayList == null) {
            renderedServicePathHopArrayList = new ArrayList<>();
            List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
            sfcServiceFunctionList.addAll(serviceFunctionChain.getSfcServiceFunction());

            short posIndex = 0;
            serviceIndex = MAX_STARTING_INDEX;

            for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
                ServiceFunctionType serviceFunctionType;

                LOG.debug("ServiceFunction name: {}", sfcServiceFunction.getName());

                serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(sfcServiceFunction.getType());
                if (serviceFunctionType != null) {
                    List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                    if (!sftServiceFunctionNameList.isEmpty()) {
                        /* Get first available service functon from the list */
                        for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
                            String serviceFunctionName = sftServiceFunctionName.getName();

                            ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI
                                    .readServiceFunctionExecutor(serviceFunctionName);

                            if (serviceFunction != null) {
                                String serviceFunctionForwarderName =
                                        serviceFunction.getSfDataPlaneLocator().get(0).getServiceFunctionForwarder();

                                RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
                                renderedServicePathHopBuilder.setHopNumber(posIndex)
                                        .setServiceFunctionName(serviceFunctionName)
                                        .setServiceIndex((short) serviceIndex)
                                        .setServiceFunctionForwarder(serviceFunctionForwarderName);

                                // The SFF DPLs will be processed below, and are used to populate the RSP Hop Ingress DPL
                                rspHopSffDplMap.put(
                                        serviceFunctionForwarderName,
                                        getSffDataPlaneLocators(serviceFunctionForwarderName));
                                LOG.info("BRADY createRenderedServicePathEntry adding [{}] to rspHopSffDplMap", serviceFunctionForwarderName);

                                // Only storing builders for now. Later we'll add the sffIngressDpl and build the RSP Hop
                                renderedServicePathHopBuilderList.add(posIndex, renderedServicePathHopBuilder);
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

            // Iterate the sffRspHopDplList and populate the renderedServicePathHopBuilder, then build it
            int index = 0;
            String prevSffName = null;
            renderedServicePathHopArrayList = new ArrayList<>();
            ListIterator<RenderedServicePathHopBuilder> rspHopBuilderIter = renderedServicePathHopBuilderList.listIterator();
            while(rspHopBuilderIter.hasNext()) {
                RenderedServicePathHopBuilder renderedServicePathHopBuilder = rspHopBuilderIter.next();
                final String curSffName = renderedServicePathHopBuilder.getServiceFunctionForwarder();

                SffDataPlaneLocator sffIngressDpl = null;
                if(index == 0) {
                    // Configure the RSP First Hop Ingress DPL
                    if(renderedServicePathHopBuilderList.size() > 1) {
                        RenderedServicePathHopBuilder nextRspHopBuilder = renderedServicePathHopBuilderList.get(1);
                        if(nextRspHopBuilder != null && nextRspHopBuilder.getServiceFunctionForwarder() != null) {
                            sffIngressDpl = getFirstSffRspHopIngressDataPlaneLocator(
                                serviceFunctionPath.getTransportType().getName(),
                                curSffName,
                                nextRspHopBuilder.getServiceFunctionForwarder(),
                                rspHopSffDplMap);
                        }
                    }
                    // TODO what to do if there is only one Hop in the Service Chain
                } else {
                    sffIngressDpl = getSffRspHopIngressDataPlaneLocator(prevSffName, curSffName, rspHopSffDplMap);
                }
                if(sffIngressDpl != null) {
                    renderedServicePathHopBuilder.setServiceFunctionForwarderLocator(sffIngressDpl.getName());
                }

                renderedServicePathHopArrayList.add(index++, renderedServicePathHopBuilder.build());

                prevSffName = renderedServicePathHopBuilder.getServiceFunctionForwarder();
            }
        }

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

    // Correctly and deterministically select the FIRST SFF DPL based
    // on the SFP.transport-type and the adjacent SFF transport details.
    private SffDataPlaneLocator getFirstSffRspHopIngressDataPlaneLocator(
            final String rspTransport, final String curSffName, final String nextSffName, final Map<String, List<SffDataPlaneLocator>> sffToDpls) {
        // This will return the name of the nextSff Ingress DPL
        LOG.info("BRADY getFirstSffRspHopIngressDataPlaneLocator curSff [{}] nextSff [{}] sffToDpls size [{}]", curSffName, nextSffName, sffToDpls.size());
      try {
        SffDataPlaneLocator adjacentSffIngressDpl = getSffRspHopIngressDataPlaneLocator(curSffName, nextSffName, sffToDpls);
        if(adjacentSffIngressDpl == null) {
            LOG.info("BRADY getFirstSffRspHopIngressDataPlaneLocator, cant find adjacentSffIngressDpl");
            return null;
        }

        List<SffDataPlaneLocator> curSffDplList = sffToDpls.get(curSffName);
        for(SffDataPlaneLocator curSffDpl : curSffDplList) {
            if(curSffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                if(compareLocatorTypes(
                        curSffDpl.getDataPlaneLocator().getLocatorType(),
                        adjacentSffIngressDpl.getDataPlaneLocator().getLocatorType()) == false) {
                    // If the DPL transport type is the same as the RSP and
                    // the DPL is not the one connected to nextSff,
                    // then it is the First Sff Rsp Hop Ingress DPL
                    LOG.info("BRADY getFirstSffRspHopIngressDataPlaneLocator found DPL [{}]", curSffDpl.getName());
                    return curSffDpl;
                }
            }
        }
      } catch(Exception e) {
            LOG.info("BRADY getFirstSffRspHopIngressDataPlaneLocator exception [{}]", e.getMessage());
            LOG.info("BRADY getFirstSffRspHopIngressDataPlaneLocator exception [{}]", e.toString());
            e.printStackTrace(); // Not sure where this stack trace message will endup...
      }

        return null;
    }

    // Correctly and deterministically select the correct
    // SFF DPL based on the adjacent SFF transport details.
    private SffDataPlaneLocator getSffRspHopIngressDataPlaneLocator(
            final String prevSffName, final String curSffName, final Map<String, List<SffDataPlaneLocator>> sffToDpls) {
        List<SffDataPlaneLocator> prevSffDplList = sffToDpls.get(prevSffName);
        List<SffDataPlaneLocator> curSffDplList = sffToDpls.get(curSffName);

        // This is an O(n squared) search, can be improved using a hash table.
        // Considering there should only be 3-4 DPLs, its not worth the extra
        // code to improve it.
        for(SffDataPlaneLocator prevSffDpl : prevSffDplList) {
            for(SffDataPlaneLocator curSffDpl : curSffDplList) {
                LocatorType prevLocatorType = prevSffDpl.getDataPlaneLocator().getLocatorType();
                LocatorType curLocatorType = curSffDpl.getDataPlaneLocator().getLocatorType();

                if(compareLocatorTypes(prevLocatorType, curLocatorType)) {
                    return curSffDpl;
                }
            }
        }

        return null;
    }

    private boolean compareLocatorTypes(LocatorType lhs, LocatorType rhs) {
        if(lhs.getImplementedInterface() != rhs.getImplementedInterface()) {
            return false;
        }
        String type = lhs.getImplementedInterface().getSimpleName().toLowerCase();

        switch (type) {
        case FUNCTION:
            break;
        case IP:
            // TODO what makes 2 NSH IP DPLs equal? Assuming its the Port, as each IP will be different
            if(((Ip) lhs).getPort().getValue().intValue() == ((Ip) rhs).getPort().getValue().intValue()) {
                return true;
            }
            break;
        case LISP:
            if(((Lisp) lhs).getEid().equals(((Lisp) rhs).getEid())) {
                return true;
            }
            break;
        case MAC:
            // TODO for now only checking VLAN Id, if present
            if(((Mac) lhs).getVlanId() != null && ((Mac) rhs).getVlanId() != null) {
                if(((Mac) lhs).getVlanId().intValue() == ((Mac) rhs).getVlanId().intValue()) {
                    return true;
                }
            }
            break;
        case MPLS:
            if(((Mpls) lhs).getMplsLabel().longValue() == ((Mpls) rhs).getMplsLabel().longValue()) {
                return true;
            }
            break;
        }

        return false;
    }

    private List<SffDataPlaneLocator> getSffDataPlaneLocators(final String sffName) {
        // TODO assuming the SffDataPlaneLocator list does NOT include the SFF-SF DPLs in the service-function-dictionary
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
        printTraceStart(LOG);

        long pathId = numCreatedPathIncrementGet();
        //int numServiceHops = renderedServicePath.getRenderedServicePathHop().size();
        List<RenderedServicePathHop> revRenderedServicePathHopArrayList = new ArrayList<>();

        RenderedServicePathBuilder revRenderedServicePathBuilder = new RenderedServicePathBuilder(renderedServicePath);
        revRenderedServicePathBuilder.setPathId(pathId);
        String revPathName = renderedServicePath.getName() + "-Reverse";
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
        short revServiceHop = 0;
        String prevSffName = null;
        while(iter.hasPrevious()) {

            LOG.info("BRADY renderedServicePathHopList HOP [{}]", revServiceHop);
            RenderedServicePathHop renderedServicePathHop = iter.previous();
            RenderedServicePathHopKey revRenderedServicePathHopKey = new RenderedServicePathHopKey(revServiceHop);
            RenderedServicePathHopBuilder revRenderedServicePathHopBuilder = new RenderedServicePathHopBuilder(renderedServicePathHop);
            revRenderedServicePathHopBuilder.setHopNumber(revServiceHop);
            revRenderedServicePathHopBuilder.setServiceIndex((short) (MAX_STARTING_INDEX - revServiceHop));
            revRenderedServicePathHopBuilder.setKey(revRenderedServicePathHopKey);

            // calculate the RSP Hop Ingress Locator, using the info calculated in the mirrored renderedServicePath
            SffDataPlaneLocator sffIngressDpl = null;
            if(revServiceHop == 0) {
                if(renderedServicePathHopList.size() > 1) {
                    LOG.info("BRADY getting first RspHopDpl list size [{}]", renderedServicePathHopList.size());
                    // Get the penultimate SFF name to calculate the First SFF Hop DPL in the reverse direction
                    RenderedServicePathHop nextRspHop = renderedServicePathHopList.get(renderedServicePathHopList.size()-2);
                    if(nextRspHop != null && nextRspHop.getServiceFunctionForwarder() != null) {
                        sffIngressDpl = getFirstSffRspHopIngressDataPlaneLocator(
                                    renderedServicePath.getTransportType().getName(),
                                    revRenderedServicePathHopBuilder.getServiceFunctionForwarder(),
                                    nextRspHop.getServiceFunctionForwarder(),
                                    rspHopSffDplMap);
                        LOG.info("BRADY got first RspHopDpl [{}]", sffIngressDpl.getName());
                    }
                }

            } else {
                LOG.info("BRADY getting RspHopDpl");
                sffIngressDpl = getSffRspHopIngressDataPlaneLocator(
                                          prevSffName,
                                          revRenderedServicePathHopBuilder.getServiceFunctionForwarder(),
                                          rspHopSffDplMap);
            }
            if(sffIngressDpl != null) {
                LOG.info("BRADY setting RspHopDpl [{}]", sffIngressDpl.getName());
                revRenderedServicePathHopBuilder.setServiceFunctionForwarderLocator(sffIngressDpl.getName());
            }
            revRenderedServicePathHopArrayList.set(revServiceHop, revRenderedServicePathHopBuilder.build());
            prevSffName = revRenderedServicePathHopBuilder.getServiceFunctionForwarder();
            revServiceHop++;
        }

        revRenderedServicePathBuilder.setRenderedServicePathHop(revRenderedServicePathHopArrayList);
        revRenderedServicePathBuilder.setTransportType(renderedServicePath.getTransportType());

        InstanceIdentifier<RenderedServicePath> rspIID;

        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, revRenderedServicePathKey)
                .build();

        RenderedServicePath revRenderedServicePath = revRenderedServicePathBuilder.build();
        RenderedServicePath ret = null;

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
                        Mac macLocator = (Mac) sffDataPlaneLocator.getDataPlaneLocator().getLocatorType();
                        if(macLocator.getVlanId() != null) {
                            renderedServicePathFirstHopBuilder.setVlanId(macLocator.getVlanId());
                        }
                        renderedServicePathFirstHopBuilder.setMacAddress(macLocator.getMac());
                        break;
                    case MPLS:
                        Mpls mplsLocator = (Mpls) sffDataPlaneLocator.getDataPlaneLocator().getLocatorType();
                        if(mplsLocator.getMplsLabel() != null) {
                            renderedServicePathFirstHopBuilder.setMplsLabel(mplsLocator.getMplsLabel());
                        }
                        if(mplsLocator.getMacAddress() != null) {
                            renderedServicePathFirstHopBuilder.setMacAddress(mplsLocator.getMacAddress());
                        }
                        renderedServicePathFirstHopBuilder.setTransportType(
                                org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls.class);
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
        int i;
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
            LOG.error("Unknowed ServiceFunctionTypeIdentity: {}", serviceFunctionType.getName());
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
        ServiceFunctionPath path = SfcProviderServicePathAPI.readServiceFunctionPathExecutor(pathName);

        /* Create ServiceFunctionPath pathName if it doesn't exist */
        if (path == null) {
            /* Create ServiceFunctionPath pathName if it doesn't exist */
            ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
            pathBuilder.setName(pathName)
                       .setServiceChainName(sfcName);
            path = pathBuilder.build();
            ret = SfcProviderServicePathAPI.putServiceFunctionPathExecutor(path);
            if (ret == false) {
                LOG.error("Failed to create ServiceFunctionPath: {}", pathName);
                return null;
            }
        }

        /* Create RenderedServicePath */
        RenderedServicePath renderedServicePath;
        RenderedServicePath revRenderedServicePath;
        renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(path);
        if (renderedServicePath == null) {
            LOG.error("Failed to create RenderedServicePath for ServiceFunctionPath: {}", pathName);
            return null;
        }

        if ((path.getClassifier() != null) &&
             SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(path.getClassifier()) != null) {
            SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor(path.getClassifier(), renderedServicePath.getName());
        } else {
            LOG.warn("Classifier not provided or does not exist");
        }

        if ((path.isSymmetric() != null) && path.isSymmetric()) {
            revRenderedServicePath = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePath);
            if (revRenderedServicePath == null) {
                LOG.error("Failed to create symmetric RenderedServicePath for ServiceFunctionPath: {}", pathName);
            } else if ((path.getSymmetricClassifier() != null) &&
                        SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(path.getSymmetricClassifier()) != null) {
                SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor(path.getSymmetricClassifier(), revRenderedServicePath.getName());
            } else {
                LOG.warn("Symmetric Classifier not provided or does not exist");
            }
        }

        printTraceStop(LOG);
        firstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(renderedServicePath.getName());
        return firstHop;
    }
}
