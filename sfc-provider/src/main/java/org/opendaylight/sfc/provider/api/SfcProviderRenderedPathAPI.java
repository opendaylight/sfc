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
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.create.rendered.path.input.ContextHeaderAllocationType1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.create.rendered.path.input.context.header.allocation.type._1.VxlanClassifier;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
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
import org.opendaylight.yangtools.yang.binding.DataContainer;
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

    private static Supplier<SfcServiceFunctionSchedulerAPI> defaultSchedulerSupplier =
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

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static SfcServiceFunctionSchedulerAPI getDefaultServiceFunctionScheduler() {
        Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerType;

        try {
            serviceFunctionSchedulerType = SfcProviderScheduleTypeAPI.readEnabledServiceFunctionScheduleTypeEntry()
                    .getType();
        } catch (RuntimeException e) {
            LOG.debug("readEnabledServiceFunctionScheduleTypeEntry failed", e);
            serviceFunctionSchedulerType = Random.class;
        }

        SfcServiceFunctionSchedulerAPI scheduler = getServiceFunctionScheduler(serviceFunctionSchedulerType);
        LOG.info("Selected SF Schdedule Type: {}", serviceFunctionSchedulerType);
        return scheduler;
    }

    /**
     * Creates a RSP and all the associated operational state based on the given
     * service function path and scheduler.
     *
     * <p>
     *
     * @param createdServiceFunctionPath
     *            Service Function Path
     * @param createRenderedPathInput
     *            CreateRenderedPathInput object
     * @param possibleScheduler
     *            SfcServiceFunctionSchedulerAPI object
     * @return RenderedServicePath Created RSP or null
     */
    public static RenderedServicePath createRenderedServicePathAndState(ServiceFunctionPath createdServiceFunctionPath,
            CreateRenderedPathInput createRenderedPathInput,
            @Nullable SfcServiceFunctionSchedulerAPI possibleScheduler) {
        boolean rspSuccessful = false;
        boolean addPathToSffStateSuccessful = false;
        boolean addPathToSfStateSuccessful = false;
        RenderedServicePath renderedServicePath;

        // Fall back to defaultScheduler
        SfcServiceFunctionSchedulerAPI scheduler = possibleScheduler;
        if (scheduler == null) {
            scheduler = defaultSchedulerSupplier.get();
        }

        // Create RSP
        if ((renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathEntry(createdServiceFunctionPath,
                createRenderedPathInput, scheduler)) != null) {
            rspSuccessful = true;

        } else {
            LOG.error("Could not create RSP. System state inconsistent. Deleting and add SFP {} back",
                    createdServiceFunctionPath.getName());
        }

        // Add Path name to SFF operational state
        if (rspSuccessful && SfcProviderServiceForwarderAPI.addPathToServiceForwarderState(renderedServicePath)) {
            addPathToSffStateSuccessful = true;
        } else {
            if (renderedServicePath != null) {
                SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());
            }
        }

        // Add Path to SF operational state
        if (addPathToSffStateSuccessful
                && SfcProviderServiceFunctionAPI.addPathToServiceFunctionState(renderedServicePath)) {

            addPathToSfStateSuccessful = true;
        } else {
            SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(createdServiceFunctionPath);
            if (renderedServicePath != null) {
                SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());
            }
        }

        // Add RSP to SFP operational state
        if (!(addPathToSfStateSuccessful && SfcProviderServicePathAPI.addRenderedPathToServicePathState(
                createdServiceFunctionPath.getName(), renderedServicePath.getName()))) {
            SfcProviderServiceFunctionAPI
                    .deleteServicePathFromServiceFunctionState(createdServiceFunctionPath.getName());
            SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(createdServiceFunctionPath);
            if (renderedServicePath != null) {
                SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());
            }
        }

        if (renderedServicePath == null) {
            LOG.error("Failed to create RSP for SFP {}", createdServiceFunctionPath.getName());
        } else {
            LOG.info("Create RSP {} for SFP {} successfully", renderedServicePath.getName(),
                    createdServiceFunctionPath.getName());
        }

        return renderedServicePath;
    }

    /**
     * Creates a RSP and all the associated operational state based on the given
     * service function path.
     *
     * <p>
     *
     * @param createdServiceFunctionPath
     *            Service Function Path
     * @param createRenderedPathInput
     *            CreateRenderedPathInput object
     * @return RenderedServicePath Created RSP or null
     */
    public static RenderedServicePath createRenderedServicePathAndState(ServiceFunctionPath createdServiceFunctionPath,
            CreateRenderedPathInput createRenderedPathInput) {
        return createRenderedServicePathAndState(createdServiceFunctionPath, createRenderedPathInput, null);
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
    public static RenderedServicePath createSymmetricRenderedServicePathAndState(
            RenderedServicePath renderedServicePath) {

        // Reverse Path
        boolean revRspSuccessful = false;
        final RenderedServicePath revRenderedServicePath = SfcProviderRenderedPathAPI
                .createReverseRenderedServicePathEntry(renderedServicePath);
        if (revRenderedServicePath != null) {
            revRspSuccessful = true;
        } else {
            LOG.error("Could not create Reverse RSP {}", renderedServicePath.getName());
        }

        // Add Path name to SFF operational state
        boolean addRevPathToSffStateSuccessful = false;
        if (revRspSuccessful) {
            if (SfcProviderServiceForwarderAPI.addPathToServiceForwarderState(revRenderedServicePath)) {
                addRevPathToSffStateSuccessful = true;
            } else {
                deleteRenderedServicePath(revRenderedServicePath.getName());
            }
        }

        // Add Path to SF operational state
        boolean addRevPathToSfStateSuccessful = false;
        if (addRevPathToSffStateSuccessful) {
            if (SfcProviderServiceFunctionAPI.addPathToServiceFunctionState(revRenderedServicePath)) {
                addRevPathToSfStateSuccessful = true;
            } else {
                SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(revRenderedServicePath.getName());
                deleteRenderedServicePath(revRenderedServicePath.getName());
            }
        }

        // Add RSP to SFP operational state
        if (!(addRevPathToSfStateSuccessful && SfcProviderServicePathAPI.addRenderedPathToServicePathState(
                renderedServicePath.getParentServiceFunctionPath(), revRenderedServicePath.getName()))) {
            // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
            /*
             * XXX TODO this exemplifies the issue. There is no method called
             * SfcProviderServiceFunctionAPI.
             * deleteServicePathFromServiceFunctionState() with signature
             * (RspName): ie SfcProviderServiceFunctionAPI.
             * deleteServicePathFromServiceFunctionState(
             * revRenderedServicePath.getName()); There is one with signature
             * (RspName, SfName).... and there's one with signature (SfpName)
             * ... so what should this method be? Which one should I really be
             * using? If you use rspName and sfpName interchangeably because
             * they were strings, then you get this sort of confusion going on.
             * I suspect that SfpName is correct, so I'll make the change here,
             * and test.
             */

            if (revRenderedServicePath != null) {
                SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionState(
                        new SfpName(revRenderedServicePath.getName().getValue()));
                SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(revRenderedServicePath.getName());
                deleteRenderedServicePath(revRenderedServicePath.getName());
            }

        }
        return revRenderedServicePath;
    }

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
     * @param createRenderedPathInput
     *            CreateRenderedPathInput object
     * @param scheduler
     *            SfcServiceFunctionSchedulerAPI object
     * @return RenderedServicePath
     */
    protected static RenderedServicePath createRenderedServicePathEntry(ServiceFunctionPath serviceFunctionPath,
            CreateRenderedPathInput createRenderedPathInput, SfcServiceFunctionSchedulerAPI scheduler) {
        printTraceStart(LOG);

        long pathId;

        RenderedServicePath ret = null;

        // Provisional code to test new RPC parameters
        ContextHeaderAllocationType1 contextHeaderAllocationType1 = createRenderedPathInput
                .getContextHeaderAllocationType1();
        if (contextHeaderAllocationType1 != null) {
            Class<? extends DataContainer> contextHeaderAllocationType1ImplementedInterface =
                contextHeaderAllocationType1.getImplementedInterface();
            if (contextHeaderAllocationType1ImplementedInterface.equals(VxlanClassifier.class)) {
                LOG.debug("ok");
            }
        }
        // String simplectxName =
        // contextHeaderAllocationType1ImplementedInterface.getSimpleName();
        // simplectxName is VxlanClassifier
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
        if (createRenderedPathInput.getName() == null || createRenderedPathInput.getName().isEmpty()) {
            if (serviceFunctionPath.getName() != null) {
                renderedServicePathBuilder
                        .setName(new RspName(serviceFunctionPath.getName().getValue() + "-Path-" + pathId));
            } else {
                LOG.error("{}: Failed to set RSP Name as it was null and SFP Name was null.",
                        Thread.currentThread().getStackTrace()[1]);
                return null;
            }
        } else {
            renderedServicePathBuilder.setName(new RspName(createRenderedPathInput.getName()));

        }

        renderedServicePathBuilder.setPathId(pathId);
        // TODO: Find out the exact rules for service index generation
        // renderedServicePathBuilder.setStartingIndex((short)
        // renderedServicePathHopArrayList.size());
        renderedServicePathBuilder.setStartingIndex((short) MAX_STARTING_INDEX);
        renderedServicePathBuilder.setServiceChainName(serviceFunctionChainName);
        renderedServicePathBuilder.setParentServiceFunctionPath(serviceFunctionPath.getName());
        renderedServicePathBuilder.setContextMetadata(serviceFunctionPath.getContextMetadata());
        renderedServicePathBuilder.setVariableMetadata(serviceFunctionPath.getVariableMetadata());

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
        // Transport
        // in any other case
        renderedServicePathBuilder.setSfcEncapsulation(serviceFunctionPath.getSfcEncapsulation() != null
                ? serviceFunctionPath.getSfcEncapsulation()
                : renderedServicePathBuilder.getTransportType().equals(VxlanGpe.class) ? Nsh.class : Transport.class);

        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(
                renderedServicePathBuilder.getName());
        InstanceIdentifier<RenderedServicePath> rspIID;
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).build();

        RenderedServicePath renderedServicePath = renderedServicePathBuilder.build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath, LogicalDatastoreType.OPERATIONAL)) {
            ret = renderedServicePath;
        } else {
            LOG.error("{}: Failed to create Rendered Service Path: {}", Thread.currentThread().getStackTrace()[1],
                    serviceFunctionPath.getName());
        }
        printTraceStop(LOG);
        return ret;
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
        revRenderedServicePathBuilder.setKey(revRenderedServicePathKey);

        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        // Populate new array with elements from existing service path. They
        // will be replaced as we
        // go along
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
            revRenderedServicePathHopBuilder.setKey(revRenderedServicePathHopKey);
            revRenderedServicePathHopArrayList.set(revServiceHop, revRenderedServicePathHopBuilder.build());
            revServiceHop++;
        }

        revRenderedServicePathBuilder.setRenderedServicePathHop(revRenderedServicePathHopArrayList);
        revRenderedServicePathBuilder.setSymmetricPathId(renderedServicePath.getPathId());

        InstanceIdentifier<RenderedServicePath> rspIID;

        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, revRenderedServicePathKey).build();

        RenderedServicePath revRenderedServicePath = revRenderedServicePathBuilder.build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, revRenderedServicePath,
                LogicalDatastoreType.OPERATIONAL)) {
            ret = revRenderedServicePath;
        } else {
            LOG.error("{}: Failed to create Reverse Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], revPathName);
        }
        printTraceStop(LOG);
        return ret;
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

    /**
     * It returns the Symmetric RSP Name from a RspName if both exist.
     *
     * <p>
     *
     * @param rspName
     *            RspName object with the primary Rendered Service Path Name
     * @return The Symmetric (Reversed) RSP; null in case the RSP does not exist
     *         or if it has not a symmetric Path Id
     */
    public static RspName getReversedRspName(RspName rspName) {
        RspName returnRspName = null;
        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (renderedServicePath != null && renderedServicePath.getSymmetricPathId() != null) {
            // The RSP has a symmetric ("Reverse") Path
            returnRspName = SfcProviderRenderedPathAPI.generateReversedPathName(renderedServicePath.getName());
        }
        return returnRspName;
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
        printTraceStart(LOG);

        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(rspName);
        InstanceIdentifier<RenderedServicePath> rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).build();

        RenderedServicePath rsp = SfcDataStoreAPI.readTransactionAPI(rspIID, LogicalDatastoreType.OPERATIONAL);

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
    public static boolean deleteRenderedServicePaths(List<RspName> servicePaths) {

        printTraceStart(LOG);
        boolean ret = false;

        for (RspName rspName : servicePaths) {
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
     * Delete a list of RSPs and associated states.
     *
     * @param rspNames
     *            the list of RSP names.
     * @return true if everything was deleted ok, false otherwise.
     */
    public static boolean deleteRenderedServicePathsAndStates(List<RspName> rspNames) {
        boolean sfStateOk = SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionState(rspNames);
        boolean sffStateOk = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspNames);
        boolean rspOk = SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspNames);
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
    public static boolean deleteRenderedServicePath(RspName renderedServicePathName) {
        boolean ret = false;
        printTraceStart(LOG);

        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(renderedServicePathName);
        InstanceIdentifier<RenderedServicePath> rspEntryIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).build();

        RenderedServicePath renderedServicePath = SfcDataStoreAPI.readTransactionAPI(rspEntryIID,
                LogicalDatastoreType.OPERATIONAL);
        if (renderedServicePath != null) {
            long pathId = renderedServicePath.getPathId();
            if (SfcDataStoreAPI.deleteTransactionAPI(rspEntryIID, LogicalDatastoreType.OPERATIONAL)) {
                ret = true;
                // Free pathId
                SfcServicePathId.freePathId(pathId);
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
                    && sffDataPlaneLocator.getDataPlaneLocator().getLocatorType().getImplementedInterface() != null) {

                String type = sffDataPlaneLocator.getDataPlaneLocator().getLocatorType().getImplementedInterface()
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
     * This method gets all necessary information for a system to construct a
     * NSH header and associated overlay packet to target the first service hop
     * of a Rendered Service Path by ServiceFunctionTypeIdentity list.
     *
     * <p>
     *
     * @param serviceFunctionSchedulerType
     *            schedulertype for the Service Function
     * @param serviceFunctionTypeList
     *            ServiceFunctionTypeIdentity list
     * @return RenderedServicePathFirstHop first hop of the rendered path
     */
    public static RenderedServicePathFirstHop readRspFirstHopBySftList(
            Class<? extends ServiceFunctionSchedulerTypeIdentity> serviceFunctionSchedulerType,
            List<SftTypeName> serviceFunctionTypeList) {
        int index;
        String serviceTypeName;
        ServiceFunctionType serviceFunctionType = null;
        List<SfcServiceFunction> sfcServiceFunctionArrayList = new ArrayList<>();
        // TODO Not sure why we need references to GBP in here. The way the
        // integration was done, we
        // can add our own strings. This maybe an artifact of
        // driving everything through RSPs which was a bad idea, and I don't
        // believe these are
        // necessary moving forward. Suggest someone cleans this up but in
        // this patch I am simply introducing typedefs so we can move forward to
        // some data model
        // work
        SfcName sfcName = new SfcName("chain-sfc-gbp");
        SfpName pathName = new SfpName("path-sfc-gbp");

        boolean ret;

        printTraceStart(LOG);
        final SfcServiceFunctionSchedulerAPI scheduler = getServiceFunctionScheduler(serviceFunctionSchedulerType);

        /* Build sfcName, pathName and ServiceFunction list */
        for (index = 0; index < serviceFunctionTypeList.size(); index++) {
            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(serviceFunctionTypeList.get(index));
            serviceTypeName = serviceFunctionType.getType().getValue();
            if (serviceTypeName == null) {
                LOG.error("Unknown ServiceFunctionType: {}", serviceFunctionType.getType());
                return null;
            }
            sfcName = new SfcName(sfcName.getValue() + "-" + serviceTypeName);
            pathName = new SfpName(pathName.getValue() + "-" + serviceTypeName);
            SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
            sfcServiceFunctionArrayList.add(sfcServiceFunctionBuilder.setName(serviceTypeName + "-gbp-sfc")
                    .setType(serviceFunctionType.getType()).build());
        }

        /* Read service chain sfcName if it exists */
        ServiceFunctionChain serviceFunctionChain = SfcProviderServiceChainAPI.readServiceFunctionChain(sfcName);

        /* Create service chain sfcName if it doesn't exist */
        if (serviceFunctionChain == null) {
            // Create ServiceFunctionChain
            ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
            sfcBuilder.setName(sfcName).setSfcServiceFunction(sfcServiceFunctionArrayList);
            serviceFunctionChain = sfcBuilder.build();
            ret = SfcProviderServiceChainAPI.putServiceFunctionChain(serviceFunctionChain);
            if (!ret) {
                LOG.error("Failed to create ServiceFunctionChain: {}", sfcName);
                return null;
            }
        }

        /* Read ServiceFunctionPath pathName if it exists */
        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(pathName);

        /* Create ServiceFunctionPath pathName if it doesn't exist */
        if (serviceFunctionPath == null) {
            /* Create ServiceFunctionPath pathName if it doesn't exist */
            ServiceFunctionPathBuilder sfpBuilder = new ServiceFunctionPathBuilder();
            sfpBuilder.setName(pathName).setServiceChainName(sfcName);
            serviceFunctionPath = sfpBuilder.build();
            ret = SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPath);
            if (!ret) {
                LOG.error("Failed to create ServiceFunctionPath: {}", pathName);
                return null;
            }
        }

        /*
         * We need to provide the same information as we would through the RPC
         */

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();

        RenderedServicePath renderedServicePath = createRenderedServicePathAndState(serviceFunctionPath,
                createRenderedPathInputBuilder.build(), scheduler);
        if (renderedServicePath == null) {
            LOG.error("Failed to create RenderedServicePath for ServiceFunctionPath: {}", pathName);
            return null;
        }

        if (isChainSymmetric(serviceFunctionPath, renderedServicePath)) {
            final RenderedServicePath revRenderedServicePath = SfcProviderRenderedPathAPI
                    .createSymmetricRenderedServicePathAndState(renderedServicePath);
            if (revRenderedServicePath == null) {
                LOG.error("Failed to create symmetric RenderedServicePath for ServiceFunctionPath: {}", pathName);
            } else {
                // Set the symmetric path ID on the original RSP
                setSymmetricPathId(renderedServicePath, revRenderedServicePath.getPathId());
            }
        }

        RenderedServicePathFirstHop firstHop = SfcProviderRenderedPathAPI
                .readRenderedServicePathFirstHop(renderedServicePath.getName());
        printTraceStop(LOG);
        return firstHop;
    }

    /**
     * This method gets all necessary information for a system to construct a
     * NSH header and associated overlay packet to target the first service hop
     * of a Rendered Service Path by ServiceFunctionTypeIdentity list.
     *
     * <p>
     *
     * @param renderedServicePath
     *            RenderedServicePath Object
     * @param pathId
     *            Symmetric Path Id
     * @return true if symmetric path-id was set, otherwise false
     */
    public static boolean setSymmetricPathId(RenderedServicePath renderedServicePath, long pathId) {
        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(renderedServicePath.getName());
        InstanceIdentifier<RenderedServicePath> rspIID;
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey).build();
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder(renderedServicePath);
        renderedServicePathBuilder.setSymmetricPathId(pathId);
        return SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePathBuilder.build(),
                LogicalDatastoreType.OPERATIONAL);
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
