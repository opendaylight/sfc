/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Nsh;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Transport;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.LoadBalance;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.LoadPathAware;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Random;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ShortestPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to operate on the Service Classifier datastore.
 *
 * <p>
 * It is normally called from onDataChanged() through a executor service. We
 * need to use an executor service because we can not operate on a datastore
 * while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 *
 * @since 2014-11-04
 */
public final class SfcProviderRenderedPathAPI {
    private static final String FUNCTION = "function";
    private static final String IP = "ip";
    private static final String LISP = "lisp";
    private static final String MAC = "mac";
    private static final String MPLS = "mpls";
    private static final int MAX_STARTING_INDEX = 255;
    private static final String REVERSED_PATH_SUFFIX = "-Reverse";

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderRenderedPathAPI.class);

    private static final Supplier<SfcServiceFunctionSchedulerAPI> DEFAULT_SCHEDULER_SUPPLIER =
            Suppliers.memoize(SfcProviderRenderedPathAPI::getDefaultServiceFunctionScheduler);

    private SfcProviderRenderedPathAPI() {
    }

    private static SfcServiceFunctionSchedulerAPI getServiceFunctionScheduler(
            Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerType) {
        SfcServiceFunctionSchedulerAPI scheduler;

        if (serviceFunctionSchedulerType == RoundRobin.class) {
            scheduler = new SfcServiceFunctionRoundRobinSchedulerAPI();
        } else if (serviceFunctionSchedulerType == LoadBalance.class) {
            scheduler = new SfcServiceFunctionLoadBalanceSchedulerAPI();
        } else if (serviceFunctionSchedulerType == Random.class) {
            scheduler = new SfcServiceFunctionRandomSchedulerAPI();
        } else if (serviceFunctionSchedulerType == ShortestPath.class) {
            scheduler = new SfcServiceFunctionShortestPathSchedulerAPI();
        } else if (serviceFunctionSchedulerType == LoadPathAware.class) {
            scheduler = new SfcServiceFunctionLoadPathAwareSchedulerAPI();
        } else {
            scheduler = new SfcServiceFunctionRandomSchedulerAPI();
        }
        return scheduler;
    }

    private static SfcServiceFunctionSchedulerAPI getDefaultServiceFunctionScheduler() {
        Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerType;
        serviceFunctionSchedulerType = SfcProviderScheduleTypeAPI.readEnabledServiceFunctionScheduleTypeEntry();
        if (serviceFunctionSchedulerType == null) {
            LOG.debug("No enabled service function scheduler type found, default to random");
            serviceFunctionSchedulerType = Random.class;
        }

        SfcServiceFunctionSchedulerAPI scheduler = getServiceFunctionScheduler(serviceFunctionSchedulerType);
        LOG.info("Selected SF Schdedule Type: {}", serviceFunctionSchedulerType);
        return scheduler;
    }

    /**
     * Creates an RSP in the configuration data store, and optionally
     * create the symmetric RSP. This will be called when an SFP has
     * been created.
     *
     * <p>
     *
     * @param serviceFunctionPath
     *            The SFP used to create this RSP
     * @return RenderedServicePath
     *            The RSP or null
     */
    public static RenderedServicePath createRenderedServicePathInConfig(ServiceFunctionPath serviceFunctionPath) {
        return createRenderedServicePathInConfig(serviceFunctionPath, null);
    }

    public static RenderedServicePath createRenderedServicePathInConfig(ServiceFunctionPath serviceFunctionPath,
                                                                        String rspName) {
        // Create the RSP
        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathEntry(
                serviceFunctionPath, rspName, DEFAULT_SCHEDULER_SUPPLIER.get());

        if (renderedServicePath == null) {
            LOG.error("Could not create RSP for SFP {}", serviceFunctionPath.getName().getValue());
            return null;
        }

        // Optionally create the Symmetric RSP
        if (SfcProviderRenderedPathAPI.isChainSymmetric(serviceFunctionPath, renderedServicePath)) {
            RenderedServicePath revRenderedServicePath = SfcProviderRenderedPathAPI
                    .createSymmetricRenderedServicePathInConfig(renderedServicePath);
            if (revRenderedServicePath == null) {
                LOG.error("Failed to create symmetric Rendered Service Path for input SFP: {}",
                        serviceFunctionPath.getName().getValue());
            } else {
                renderedServicePath = SfcProviderRenderedPathAPI.setSymmetricPathId(renderedServicePath,
                        revRenderedServicePath.getPathId(), null);
            }
        }

        // Write the RSP to the configuration data store
        writeRenderedServicePath(renderedServicePath, LogicalDatastoreType.CONFIGURATION);

        return renderedServicePath;
    }

    /**
     * Creates the RSP operational state based on the given service function path.
     *
     * <p>
     *
     * @param createdServiceFunctionPath
     *            Service Function Path
     * @param renderedServicePath
     *            The RSP used to add operational state
     * @return boolean
     *            True if the state was correctly set, False otherwise
     */
    public static boolean createRenderedServicePathState(ServiceFunctionPath createdServiceFunctionPath,
            RenderedServicePath renderedServicePath) {

        // Add Path name to SFF operational state
        if (!SfcProviderServiceForwarderAPI.addPathToServiceForwarderState(renderedServicePath)) {
            LOG.error("Failed to add RSP to SFF state {}", renderedServicePath.getName());
            SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());

            return false;
        }

        // Add Path to SF operational state
        if (!SfcProviderServiceFunctionAPI.addPathToServiceFunctionState(renderedServicePath)) {
            LOG.error("Failed to add RSP to SF state {}", renderedServicePath.getName());
            SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(createdServiceFunctionPath);
            SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());

            return false;
        }

        // Add RSP to SFP operational state
        if (!SfcProviderServicePathAPI.addRenderedPathToServicePathState(
                createdServiceFunctionPath.getName(), renderedServicePath.getName())) {
            LOG.error("Failed to add RSP to SFP state {}", renderedServicePath.getName());
            SfcProviderServiceFunctionAPI
                    .deleteServicePathFromServiceFunctionState(createdServiceFunctionPath.getName());
            SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(createdServiceFunctionPath);
            SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());

            return false;
        }

        return true;
    }

    /**
     * Creates a RSP and all the associated operational state based on the given
     * service function path and config RSP. This version is called when an RSP has
     * been created in the config data store, to create an RSP in operational.
     *
     * <p>
     *
     * @param createdServiceFunctionPath
     *            Service Function Path
     * @param createdRenderedServicePath
     *            The config RSP used to create the operational RSP
     * @return RenderedServicePath Created RSP or null
     */
    public static RenderedServicePath createRenderedServicePathAndState(
            ServiceFunctionPath createdServiceFunctionPath, RenderedServicePath createdRenderedServicePath) {

        // Create the Operational RSP based on the Config RSP
        RenderedServicePathBuilder renderedServicePathBuilder =
                new RenderedServicePathBuilder(createdRenderedServicePath);

        // These are the config false RSP attributes, that can only be written in the RSP Operational data store
        renderedServicePathBuilder.setContextMetadata(createdServiceFunctionPath.getContextMetadata());
        renderedServicePathBuilder.setVariableMetadata(createdServiceFunctionPath.getVariableMetadata());

        RenderedServicePath renderedServicePath = renderedServicePathBuilder.build();

        if (renderedServicePath == null) {
            LOG.error("Could not create RSP in operational for config RSP {}",
                    createdRenderedServicePath.getName().getValue());
            return null;
        }

        if (!writeRenderedServicePath(renderedServicePath, LogicalDatastoreType.OPERATIONAL)) {
            return null;
        }

        if (!createRenderedServicePathState(createdServiceFunctionPath, renderedServicePath)) {
            // If createRenderedServicePathState() returns false, the RSP was deleted therein
            return null;
        }

        return renderedServicePath;
    }

    /**
     * Creates a symmetric RSP in the configuration data store. This
     * will be called when an SFP has been created.
     *
     * <p>
     *
     * @param renderedServicePath
     *            The original RSP used to create this symmetric RSP
     * @return RenderedServicePath
     *            The symmetric RSP or null
     */
    public static RenderedServicePath createSymmetricRenderedServicePathInConfig(
            RenderedServicePath renderedServicePath) {
        final RenderedServicePath revRenderedServicePath = SfcProviderRenderedPathAPI
                .createReverseRenderedServicePathEntry(renderedServicePath);
        if (revRenderedServicePath == null) {
            LOG.error("Could not create Reverse RSP {}", renderedServicePath.getName());
            return null;
        }

        writeRenderedServicePath(revRenderedServicePath, LogicalDatastoreType.CONFIGURATION);

        return revRenderedServicePath;
    }

    /**
     * Create a Symmetric Path and all the associated operational state based on
     * the given rendered service path.
     *
     * <p>
     *
     * @param renderedServicePath
     *            RSP Object
     * @return Nothing.
     */

    /**
     * Given a list of Service Functions, create a RenderedServicePath Hop List.
     *
     * @param serviceFunctionNameList
     *            List of ServiceFunctions
     * @param sfgNameList
     *            List of ServiceFunctionGroups
     * @param startingServiceIndex
     *            Starting index
     * @return List of {@link RenderedServicePathHop}
     */
    protected static List<RenderedServicePathHop> createRenderedServicePathHopList(List<SfName> serviceFunctionNameList,
            List<String> sfgNameList, int startingServiceIndex) {
        List<RenderedServicePathHop> renderedServicePathHopArrayList = new ArrayList<>();
        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();

        int serviceIndex = startingServiceIndex;
        short posIndex = 0;

        if (serviceFunctionNameList == null && sfgNameList == null) {
            LOG.error("Could not create the hop list caused by empty name list");
            return null;
        }

        if (sfgNameList != null) {
            for (String sfgName : sfgNameList) {
                ServiceFunctionGroup sfg = SfcProviderServiceFunctionGroupAPI.readServiceFunctionGroup(sfgName);
                if (sfg == null) {
                    LOG.error("Could not find suitable SFG in data store by name: {}", sfgName);
                    renderedServicePathHopArrayList.clear();
                    break;
                }
                // TODO Bug 4495 - RPCs hiding heuristics using Strings -
                // alagalah
                /*
                 * Note I didn't change SFG's typing since I still am unclear as
                 * to what problem SFG is trying to solve, hence any
                 * String-String heuristics would be opaque for me to resolve in
                 * refactoring.
                 */
                ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI
                        .readServiceFunction(new SfName(sfg.getSfcServiceFunction().get(0).getName()));
                if (serviceFunction == null) {
                    LOG.error("Could not find suitable SF in data store by name: {}",
                            sfg.getSfcServiceFunction().get(0).getName());
                    renderedServicePathHopArrayList.clear();
                    break;
                }
                createSFGHopBuilder(serviceIndex, renderedServicePathHopBuilder, posIndex, sfg.getName(),
                        serviceFunction);
                renderedServicePathHopArrayList.add(posIndex, renderedServicePathHopBuilder.build());
                serviceIndex--;
                posIndex++;
            }

        } else {
            for (SfName serviceFunctionName : serviceFunctionNameList) {
                ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI
                        .readServiceFunction(serviceFunctionName);
                if (serviceFunction == null) {
                    LOG.error("Could not find suitable SF in data store by name: {}", serviceFunctionName);
                    return null;
                }
                createSFHopBuilder(serviceIndex, renderedServicePathHopBuilder, posIndex, serviceFunctionName,
                        serviceFunction);
                renderedServicePathHopArrayList.add(posIndex, renderedServicePathHopBuilder.build());
                serviceIndex--;
                posIndex++;
            }
        }

        return renderedServicePathHopArrayList;
    }

    private static void createSFHopBuilder(int serviceIndex,
            RenderedServicePathHopBuilder renderedServicePathHopBuilder, short posIndex, SfName serviceFunctionName,
            ServiceFunction serviceFunction) {
        createHopBuilderInternal(serviceIndex, renderedServicePathHopBuilder, posIndex, serviceFunction);
        renderedServicePathHopBuilder.setServiceFunctionName(serviceFunctionName);
    }

    private static void createSFGHopBuilder(int serviceIndex,
            RenderedServicePathHopBuilder renderedServicePathHopBuilder, short posIndex,
            String serviceFunctionGroupName, ServiceFunction serviceFunction) {
        createHopBuilderInternal(serviceIndex, renderedServicePathHopBuilder, posIndex, serviceFunction);
        renderedServicePathHopBuilder.setServiceFunctionGroupName(serviceFunctionGroupName);
    }

    private static void createHopBuilderInternal(int serviceIndex,
            RenderedServicePathHopBuilder renderedServicePathHopBuilder, short posIndex,
            ServiceFunction serviceFunction) {
        SffName serviceFunctionForwarderName = serviceFunction.getSfDataPlaneLocator().get(0)
                .getServiceFunctionForwarder();

        ServiceFunctionForwarder serviceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(serviceFunctionForwarderName);
        if (serviceFunctionForwarder != null && serviceFunctionForwarder.getSffDataPlaneLocator() != null) {
            if (serviceFunctionForwarder.getSffDataPlaneLocator().size() == 1) {
                renderedServicePathHopBuilder.setServiceFunctionForwarderLocator(
                        serviceFunctionForwarder.getSffDataPlaneLocator().get(0).getName());
            } else {
                // If there is more than one SFF DPL, then find
                // the one that is not associated with an SF
                List<SffDataPlaneLocator> sffNonSfDplList =
                        SfcProviderServiceForwarderAPI.getNonSfDataPlaneLocators(serviceFunctionForwarder);
                if (sffNonSfDplList.size() == 1) {
                    renderedServicePathHopBuilder.setServiceFunctionForwarderLocator(sffNonSfDplList.get(0).getName());
                }
            }
        }

        renderedServicePathHopBuilder.setHopNumber(posIndex).setServiceIndex((short) serviceIndex)
                .setServiceFunctionForwarder(serviceFunctionForwarderName);
    }

    /**
     * Create a Rendered Path and all the associated operational state based on
     * the given rendered service path and scheduler.
     *
     * <p>
     *
     * @param serviceFunctionPath
     *            RSP Object
     * @param rspName
     *            Name of the RSP to create
     * @param scheduler
     *            SfcServiceFunctionSchedulerAPI object
     * @return RenderedServicePath
     */
    protected static RenderedServicePath createRenderedServicePathEntry(ServiceFunctionPath serviceFunctionPath,
            String rspName, SfcServiceFunctionSchedulerAPI scheduler) {
        printTraceStart(LOG);

        long pathId;

        ServiceFunctionChain serviceFunctionChain;
        SfcName serviceFunctionChainName = serviceFunctionPath.getServiceChainName();
        serviceFunctionChain = serviceFunctionChainName != null
                ? SfcProviderServiceChainAPI.readServiceFunctionChain(serviceFunctionChainName) : null;
        if (serviceFunctionChain == null) {
            LOG.error("ServiceFunctionChain name for Path {} not provided", serviceFunctionPath.getName());
            return null;
        }

        // Descending order
        int serviceIndex = MAX_STARTING_INDEX;

        List<String> sfgNameList = SfcProviderServiceFunctionGroupAPI.getSfgNameList(serviceFunctionChain);
        List<SfName> sfNameList = scheduler.scheduleServiceFunctions(serviceFunctionChain, serviceIndex,
                serviceFunctionPath);
        if (sfNameList == null && sfgNameList == null) {
            LOG.warn("createRenderedServicePathEntry scheduler.scheduleServiceFunctions() returned null list");
            return null;
        }

        // Before trying to create the RSP, iterate the SFs checking for one-chain-only
        if (sfNameList != null) {
            for (SfName sfName : sfNameList) {
                List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(sfName);
                ServiceFunction sf = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
                if (Boolean.TRUE.equals(sf.isOneChainOnly()) && sfServicePathList != null
                        && !sfServicePathList.isEmpty()) {
                    LOG.error("createRenderedServicePathEntry SF [{}] is-one-chain-only is TRUE and the SF "
                            + "is already in use", sfName);
                    return null;
                }
            }
        }

        List<RenderedServicePathHop> renderedServicePathHopArrayList = createRenderedServicePathHopList(sfNameList,
                sfgNameList, serviceIndex);

        if (renderedServicePathHopArrayList == null) {
            LOG.warn("createRenderedServicePathEntry createRenderedServicePathHopList returned null list");
            return null;
        }

        // Build the service function path so it can be committed to datastore
        /*
         * pathId = (serviceFunctionPath.getPathId() != null) ?
         * serviceFunctionPath.getPathId() : numCreatedPathIncrementGet();
         */

        if (serviceFunctionPath.getPathId() == null) {
            pathId = SfcServicePathId.checkAndAllocatePathId();
        } else {
            pathId = SfcServicePathId.chechAndAllocatePathId(serviceFunctionPath.getPathId());
        }

        if (pathId == -1) {
            LOG.error("{}: Failed to allocate path-id: {}", Thread.currentThread().getStackTrace()[1], pathId);
            return null;
        }

        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopArrayList);
        // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
        if (rspName == null || rspName.isEmpty()) {
            renderedServicePathBuilder
                    .setName(getRspName(serviceFunctionPath, pathId));
        } else {
            renderedServicePathBuilder.setName(new RspName(rspName));
        }

        renderedServicePathBuilder.setPathId(pathId);
        // TODO: Find out the exact rules for service index generation
        // renderedServicePathBuilder.setStartingIndex((short)
        // renderedServicePathHopArrayList.size());
        renderedServicePathBuilder.setStartingIndex((short) MAX_STARTING_INDEX);
        renderedServicePathBuilder.setServiceChainName(serviceFunctionChainName);
        renderedServicePathBuilder.setParentServiceFunctionPath(serviceFunctionPath.getName());
        renderedServicePathBuilder.setReversePath(false);

        if (serviceFunctionPath.getTransportType() == null) {
            // TODO this is a temporary workaround to a YANG problem
            // Even though the SFP.transportType is defined with a default, if
            // its not
            // specified in the configuration, it can still return null
            renderedServicePathBuilder.setTransportType(VxlanGpe.class);
        } else {
            renderedServicePathBuilder.setTransportType(serviceFunctionPath.getTransportType());
        }

        // If no encapsulation type specified, default is NSH for VxlanGpe and
        // Transport in any other case
        renderedServicePathBuilder.setSfcEncapsulation(serviceFunctionPath.getSfcEncapsulation() != null
                ? serviceFunctionPath.getSfcEncapsulation()
                : renderedServicePathBuilder.getTransportType().equals(VxlanGpe.class) ? Nsh.class : Transport.class);

        return renderedServicePathBuilder.build();
    }

    /**
     * Creates a RSP that is mirror image of the given one. It reverses the hop
     * list and adjusts hop number and service index accordingly.
     *
     * <p>
     *
     * @param renderedServicePath
     *            RSP object
     * @return Nothing
     */
    public static RenderedServicePath createReverseRenderedServicePathEntry(RenderedServicePath renderedServicePath) {
        RenderedServicePath ret = null;

        long pathId = SfcServicePathId.checkAndAllocateSymmetricPathId(renderedServicePath.getPathId());
        printTraceStart(LOG);

        if (pathId == -1) {
            LOG.error("{}: Failed to allocate symmetric path Id for Path Id: {}",
                    Thread.currentThread().getStackTrace()[1], renderedServicePath.getPathId());
        }

        RenderedServicePathBuilder revRenderedServicePathBuilder = new RenderedServicePathBuilder(renderedServicePath);
        revRenderedServicePathBuilder.setPathId(pathId);
        RspName revPathName = generateReversedPathName(renderedServicePath.getName());
        revRenderedServicePathBuilder.setName(revPathName);
        RenderedServicePathKey revRenderedServicePathKey = new RenderedServicePathKey(revPathName);
        revRenderedServicePathBuilder.withKey(revRenderedServicePathKey);

        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        // Populate new array with elements from existing service path.
        // They will be replaced as we go along.
        List<RenderedServicePathHop> revRenderedServicePathHopArrayList = new ArrayList<>();
        revRenderedServicePathHopArrayList.addAll(renderedServicePathHopList);

        ListIterator<RenderedServicePathHop> iter = renderedServicePathHopList
                .listIterator(renderedServicePathHopList.size());
        short revServiceHop = 0;
        while (iter.hasPrevious()) {

            RenderedServicePathHop renderedServicePathHop = iter.previous();
            RenderedServicePathHopKey revRenderedServicePathHopKey = new RenderedServicePathHopKey(revServiceHop);
            RenderedServicePathHopBuilder revRenderedServicePathHopBuilder = new RenderedServicePathHopBuilder(
                    renderedServicePathHop);
            revRenderedServicePathHopBuilder.setHopNumber(revServiceHop);
            revRenderedServicePathHopBuilder.setServiceIndex((short) (MAX_STARTING_INDEX - revServiceHop));
            revRenderedServicePathHopBuilder.withKey(revRenderedServicePathHopKey);
            revRenderedServicePathHopArrayList.set(revServiceHop, revRenderedServicePathHopBuilder.build());
            revServiceHop++;
        }

        revRenderedServicePathBuilder.setRenderedServicePathHop(revRenderedServicePathHopArrayList);
        revRenderedServicePathBuilder.setSymmetricPathId(renderedServicePath.getPathId());
        revRenderedServicePathBuilder.setReversePath(true);

        return revRenderedServicePathBuilder.build();
    }

    /**
     * Given the name of an RSP, return its reverse RSP name.
     *
     * @param rspName
     *            the RSP name
     * @return the reverse RSP name
     */
    public static RspName generateReversedPathName(RspName rspName) {
        return rspName.getValue().endsWith(REVERSED_PATH_SUFFIX)
                ? new RspName(rspName.getValue().replaceFirst(REVERSED_PATH_SUFFIX, ""))
                : new RspName(rspName.getValue() + REVERSED_PATH_SUFFIX);
    }

    private static boolean writeRenderedServicePath(RenderedServicePath renderedServicePath,
                                                    LogicalDatastoreType type) {
        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(
                renderedServicePath.getName());
        InstanceIdentifier<RenderedServicePath> rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).build();

        if (!SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath, type)) {
            LOG.error("{}: Failed to create Rendered Service Path: {}", Thread.currentThread().getStackTrace()[1],
                    renderedServicePath.getName());
            return false;
        }

        LOG.info("Create RSP [{}] in {} data store successfully", renderedServicePath.getName().getValue(), type);

        return true;
    }

    /**
     * Creates an RSP name based on the SFP name and pathId.
     *
     * <p>
     *
     * @param serviceFunctionPath
     *            The SFP name is used to create the RSP name
     * @param pathId
     *            Used to create the RSP name
     * @return An RSP name
     */
    public static RspName getRspName(ServiceFunctionPath serviceFunctionPath, long pathId) {
        return new RspName(serviceFunctionPath.getName().getValue() + "-Path-" + pathId);
    }

    /**
     * This function reads a RSP from the datastore.
     *
     * <p>
     *
     * @param rspName
     *            RSP name
     * @return Nothing.
     */
    public static RenderedServicePath readRenderedServicePath(RspName rspName) {
        return readRenderedServicePath(rspName, LogicalDatastoreType.OPERATIONAL);
    }

    public static RenderedServicePath readRenderedServicePath(RspName rspName,
                                                              LogicalDatastoreType logicalDatastoreType) {
        printTraceStart(LOG);

        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(rspName);
        InstanceIdentifier<RenderedServicePath> rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).build();

        RenderedServicePath rsp = SfcDataStoreAPI.readTransactionAPI(rspIID, logicalDatastoreType);

        printTraceStop(LOG);

        return rsp;
    }

    /**
     * When a SFF is deleted directly we need to delete all associated SFPs.
     *
     * <p>
     *
     * @param servicePaths
     *            SffServicePath object
     * @return Nothing.
     */
    public static boolean deleteRenderedServicePaths(List<RspName> servicePaths,
                                                     LogicalDatastoreType logicalDatastoreType) {

        printTraceStart(LOG);
        boolean ret = false;

        for (RspName rspName : servicePaths) {
            if (SfcProviderRenderedPathAPI.readRenderedServicePath(rspName, logicalDatastoreType) != null) {
                if (SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName, logicalDatastoreType)) {
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
     * Delete a list of RSPs and associated states.
     *
     * @param rspNames
     *            the list of RSP names.
     * @return true if everything was deleted ok, false otherwise.
     */
    public static boolean deleteRenderedServicePathsAndStates(List<RspName> rspNames) {
        boolean sfStateOk = SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionState(rspNames);
        boolean sffStateOk = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspNames);
        boolean rspOk = SfcProviderRenderedPathAPI.deleteRenderedServicePaths(
                rspNames,
                LogicalDatastoreType.OPERATIONAL);
        return sfStateOk && sffStateOk && rspOk;
    }

    /**
     * This method deletes a RSP from the datastore and frees the Path ID.
     *
     * <p>
     *
     * @param renderedServicePathName
     *            RSP name
     * @return Nothing.
     */
    public static boolean deleteRenderedServicePath(RspName renderedServicePathName,
                                                    LogicalDatastoreType logicalDatastoreType) {
        boolean ret = false;
        printTraceStart(LOG);

        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(renderedServicePathName);
        InstanceIdentifier<RenderedServicePath> rspEntryIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).build();

        RenderedServicePath renderedServicePath = SfcDataStoreAPI.readTransactionAPI(rspEntryIID,
                logicalDatastoreType);
        if (renderedServicePath != null) {
            long pathId = renderedServicePath.getPathId();
            if (SfcDataStoreAPI.deleteTransactionAPI(rspEntryIID, logicalDatastoreType)) {
                ret = true;
                // Free pathId
                if (logicalDatastoreType == LogicalDatastoreType.OPERATIONAL) {
                    SfcServicePathId.freePathId(pathId);
                    SfcProviderServicePathAPI.deleteRenderedPathFromServicePathState(
                            renderedServicePath.getParentServiceFunctionPath(), renderedServicePathName);
                }
            } else {
                LOG.error("{}: Failed to delete RSP: {}", Thread.currentThread().getStackTrace()[1],
                        renderedServicePathName);
            }
        } else {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    public static boolean deleteRenderedServicePath(RspName renderedServicePathName) {
        return deleteRenderedServicePath(renderedServicePathName, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * This method provides all necessary information for a system to construct
     * a NSH header and associated overlay packet to target the first service
     * hop of a Rendered Service Path.
     *
     * <p>
     *
     * @param rspName
     *            RSP name
     * @return Nothing.
     */
    public static RenderedServicePathFirstHop readRenderedServicePathFirstHop(RspName rspName) {
        RenderedServicePathFirstHop renderedServicePathFirstHop = null;

        RenderedServicePath renderedServicePath = readRenderedServicePath(rspName);
        if (renderedServicePath != null) {
            RenderedServicePathFirstHopBuilder renderedServicePathFirstHopBuilder =
                new RenderedServicePathFirstHopBuilder();
            renderedServicePathFirstHopBuilder.setPathId(renderedServicePath.getPathId())
                    .setStartingIndex(renderedServicePath.getStartingIndex())
                    .setSymmetricPathId(renderedServicePath.getSymmetricPathId());

            List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
            RenderedServicePathHop renderedServicePathHop = renderedServicePathHopList.get(0);

            SffName sffName = renderedServicePathHop.getServiceFunctionForwarder();
            SffDataPlaneLocatorName sffLocatorName = renderedServicePathHop.getServiceFunctionForwarderLocator();

            if (sffLocatorName == null) {
                return renderedServicePathFirstHopBuilder.build();
            }

            SffDataPlaneLocator sffDataPlaneLocator = SfcProviderServiceForwarderAPI
                    .readServiceFunctionForwarderDataPlaneLocator(sffName, sffLocatorName);

            if (sffDataPlaneLocator != null && sffDataPlaneLocator.getDataPlaneLocator() != null
                    && sffDataPlaneLocator.getDataPlaneLocator().getLocatorType() != null
                    && sffDataPlaneLocator.getDataPlaneLocator().getLocatorType().implementedInterface() != null) {

                String type = sffDataPlaneLocator.getDataPlaneLocator().getLocatorType().implementedInterface()
                        .getSimpleName().toLowerCase(Locale.getDefault());

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
                    default:
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

    /**
     * Set the Symmetric pathId on an RSP and optionally store the
     * result in the specified data store.
     *
     * <p>
     *
     * @param renderedServicePath
     *            RenderedServicePath Object
     * @param pathId
     *            Symmetric Path Id
     * @param logicalDatastoreType
     *            The data store to write the resulting RSP to.
     *            If its null, dont write to the datastore.
     * @return the resulting RSP
     */
    public static RenderedServicePath setSymmetricPathId(RenderedServicePath renderedServicePath, long pathId,
                                             @Nullable LogicalDatastoreType logicalDatastoreType) {
        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(renderedServicePath.getName());
        InstanceIdentifier<RenderedServicePath> rspIID;
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).build();
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder(renderedServicePath);
        renderedServicePathBuilder.setSymmetricPathId(pathId);

        RenderedServicePath updatedRenderedServicePath = renderedServicePathBuilder.build();

        if (logicalDatastoreType != null) {
            SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, updatedRenderedServicePath, logicalDatastoreType);
        }

        return updatedRenderedServicePath;
    }

    /**
     * Determine if a Rendered Service Path should be Symmetric. The Service
     * Function Path symmetric field, if present, has priority over the SF
     * SF-type bidirectionality fields. First use the SFP symmetric value, if
     * its not present, then use the SF SF-type bidirectionality values by
     * iterating the ServiceFunctions in the RSP Hops to check if there is at
     * least one SF with an SF-Type that has the bidirectionality field set
     * true.
     *
     * @param sfp
     *            - used to get the symmetric flag
     * @param rsp
     *            - the RSP to iterate over the SFs in the hops
     * @return True is there the RSP is symmetric, false otherwise.
     */
    public static boolean isChainSymmetric(ServiceFunctionPath sfp, RenderedServicePath rsp) {
        if (sfp.isSymmetric() != null) {
            return sfp.isSymmetric();
        }

        List<RenderedServicePathHop> rspHops = rsp.getRenderedServicePathHop();
        for (RenderedServicePathHop hop : rspHops) {
            ServiceFunction sf = SfcProviderServiceFunctionAPI.readServiceFunction(hop.getServiceFunctionName());
            ServiceFunctionType sfType = SfcProviderServiceTypeAPI.readServiceFunctionType(sf.getType());
            if (sfType == null) {
                LOG.error("Service Function type [{}] for Service Function [{}] does not exist.", sf.getType(),
                        sf.getName());
                continue;
            }

            if (sfType.isBidirectional() != null && sfType.isBidirectional()) {
                return true;
            }
        }

        return false;
    }
}
