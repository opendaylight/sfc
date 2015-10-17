/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * This class has the APIs to operate on the ServiceFunction
 * datastore.
 * <p>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 *      <p>
 * @since 2014-06-30
 */
public class SfcProviderServiceForwarderAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceForwarderAPI.class);

    /**
     * This method creates a SFF in the data store
     * <p>
     *
     * @param sff SFF object
     * @return true if SFF was created, false otherwise
     */
    public static boolean putServiceFunctionForwarder(ServiceFunctionForwarder sff) {
        boolean ret;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier
            .builder(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class, sff.getKey()).build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sffEntryIID, sff, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method searches for a data plane locator of a given name within a SFF
     * <p>
     *
     * @param sffName SFF name
     * @param sffLocatorName SFF data plane locator name
     * @return SffDataPlaneLocator object or null if not found
     */
    public static SffDataPlaneLocator readServiceFunctionForwarderDataPlaneLocator(SffName sffName,
            SffDataPlaneLocatorName sffLocatorName) {
        ServiceFunctionForwarder serviceFunctionForwarder = readServiceFunctionForwarder(sffName);
        if (serviceFunctionForwarder != null) {
            List<SffDataPlaneLocator> sffDataPlaneLocatorList = serviceFunctionForwarder.getSffDataPlaneLocator();
            for (SffDataPlaneLocator sffDataPlaneLocator : sffDataPlaneLocatorList) {
                if (sffDataPlaneLocator.getName().equals(sffLocatorName)) {
                    return sffDataPlaneLocator;
                }
            }
        } else {
            LOG.error("{}: Failed to read SFF: {}", Thread.currentThread().getStackTrace()[1], sffName);
        }
        return null;
    }

    /**
     * This method deletes a SffDataPlaneLocator (given by name)
     * within a SFF (given by name) from configuration DataStore
     * <p>
     *
     * @param sffName SFF name
     * @param sffLocatorName SffDataPlaneLocator name
     * @return true if SffDataPlane locator was deleted, false otherwise
     */
    protected static boolean deleteSffDataPlaneLocator(SffName sffName, SffDataPlaneLocatorName sffLocatorName) {
        boolean ret = false;
        printTraceStart(LOG);

        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey(sffName);
        SffDataPlaneLocatorKey sffDataPlaneLocatorKey = new SffDataPlaneLocatorKey(sffLocatorName);

        InstanceIdentifier<SffDataPlaneLocator> sffDataPlaneLocatorIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                    .child(SffDataPlaneLocator.class, sffDataPlaneLocatorKey)
                    .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sffDataPlaneLocatorIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not delete SffDataPlaneLocator: {} from Service Function Forwarder: {}", sffLocatorName,
                    sffName);
        }

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads a SFF from the datastore
     * <p>
     *
     * @param serviceFunctionForwarderName SFF name
     * @return SF object or null if not found
     */
    public static ServiceFunctionForwarder readServiceFunctionForwarder(SffName serviceFunctionForwarderName) {
        printTraceStart(LOG);
        ServiceFunctionForwarder sff;
        InstanceIdentifier<ServiceFunctionForwarder> sffIID;
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
            .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
            .build();

        sff = SfcDataStoreAPI.readTransactionAPI(sffIID, LogicalDatastoreType.CONFIGURATION);
        printTraceStop(LOG);
        return sff;
    }

    /**
     * This method deletes a SFF from the datastore
     * <p>
     *
     * @param serviceFunctionForwarderName SFF name
     * @return true if SF was deleted, false otherwise
     */
    public static boolean deleteServiceFunctionForwarder(SffName serviceFunctionForwarderName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                    .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sffEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not delete SFF: {}", serviceFunctionForwarderName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Put all Service Function Forwarders in the data store
     * <p>
     *
     * @param sffs
     * @return true is all SFFs were created, false otherwise
     */
    public static boolean putAllServiceFunctionForwarders(ServiceFunctionForwarders sffs) {
        boolean ret;
        printTraceStart(LOG);

        ret = SfcDataStoreAPI.writePutTransactionAPI(OpendaylightSfc.SFF_IID, sffs, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Read all Service Function Forwarders
     * devices
     * <p>
     *
     * @return ServiceFunctionForwarders object
     */
    protected static ServiceFunctionForwarders readAllServiceFunctionForwarders() {
        ServiceFunctionForwarders sffs;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionForwarders> sffsIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();

        sffs = SfcDataStoreAPI.readTransactionAPI(sffsIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return sffs;
    }

    /**
     * Delete All Service Function Forwarders in the data store
     * devices
     * <p>
     *
     * @return true is all SFFs were deleted, false otherwise
     */
    protected boolean deleteAllServiceFunctionForwarders() {
        boolean ret = false;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionForwarders> sffsIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();
        if (SfcDataStoreAPI.deleteTransactionAPI(sffsIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * When a SF is deleted, we need to remove it from the corresponding SFF
     * service function dictionary
     * devices
     * <p>
     *
     * @param serviceFunction SF object
     * @return true is SF was deleted, false otherwise
     */
    public boolean deleteServiceFunctionFromForwarder(ServiceFunction serviceFunction) {
        printTraceStart(LOG);
        boolean ret = false;
        SffName serviceFunctionForwarderName =
                serviceFunction.getSfDataPlaneLocator().get(0).getServiceFunctionForwarder();
        InstanceIdentifier<ServiceFunctionDictionary> sffIID;
        ServiceFunctionDictionaryKey serviceFunctionDictionaryKey =
                new ServiceFunctionDictionaryKey(serviceFunction.getName());
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
            .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
            .child(ServiceFunctionDictionary.class, serviceFunctionDictionaryKey)
            .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sffIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not delete SF {} from SFF {}", serviceFunction.getName(), serviceFunctionForwarderName);
        }
        printTraceStop(LOG);
        return ret;

    }

    /**
     * This method updates configuration of
     * a SFF in the data store.
     * <p>
     *
     * @param serviceFunctionForwarder ServiceFunctionForwarder object
     * @return true if SFF was updated, false otherwise
     */
    public static boolean updateServiceFunctionForwarder(ServiceFunctionForwarder serviceFunctionForwarder) {
        printTraceStart(LOG);

        Preconditions.checkNotNull(serviceFunctionForwarder);
        boolean ret = false;

        InstanceIdentifier<ServiceFunctionForwarder> sffIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class, serviceFunctionForwarder.getKey())
                    .build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(sffIID, serviceFunctionForwarder,
                LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not update SFF {}", serviceFunctionForwarder.getName());
        }

        printTraceStop(LOG);
        return ret;
    }

    /**
     * We add the path name to the operational store of each SFF.
     * <p>
     *
     * @param pathName Service Function Path Object
     * @return Nothing.
     */
    public static boolean addPathToServiceForwarderState(SfpName pathName) {

        printTraceStart(LOG);

        boolean ret = true;
        ServiceFunctionForwarderStateBuilder serviceFunctionForwarderStateBuilder =
                new ServiceFunctionForwarderStateBuilder();

        SffServicePathKey sffServicePathKey = new SffServicePathKey(pathName);
        SffServicePathBuilder sffServicePathBuilder = new SffServicePathBuilder();
        sffServicePathBuilder.setKey(sffServicePathKey);
        sffServicePathBuilder.setName(pathName);

        /*
         * TODO Note that RSP name and SFP name were used interchangeably here due to use of
         * strings.
         * This casting back and forth only masks the underlying issue which is hidden in the use of
         * RPCs I suspect.
         * There's way too many of these instances to put a detailed todo on them so raised a bug
         * and will refer to it alagalah 15-10-18
         *
         */

        // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
        RenderedServicePath renderedServicePath =
                SfcProviderRenderedPathAPI.readRenderedServicePath(new RspName(pathName.getValue()));
        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                    new ServiceFunctionForwarderStateKey(renderedServicePathHop.getServiceFunctionForwarder());
            InstanceIdentifier<SffServicePath> sfStateIID =
                    InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                        .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                        .child(SffServicePath.class, sffServicePathKey)
                        .build();
            serviceFunctionForwarderStateBuilder.setName(renderedServicePathHop.getServiceFunctionForwarder());

            if (SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, sffServicePathBuilder.build(),
                    LogicalDatastoreType.OPERATIONAL)) {
                ret = ret && true;
            } else {
                ret = ret && false;
                LOG.error("Failed to add path {} to SFF {} state.", pathName,
                        renderedServicePathHop.getServiceFunctionForwarder());
            }
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * We add the path name to the operational store of each SFF.
     * <p>
     *
     * @param renderedServicePath RSP Object
     * @return Nothing.
     */
    public static boolean addPathToServiceForwarderState(RenderedServicePath renderedServicePath) {

        printTraceStart(LOG);

        boolean ret = true;
        ServiceFunctionForwarderStateBuilder serviceFunctionForwarderStateBuilder =
                new ServiceFunctionForwarderStateBuilder();

        // TODO another example of strings being used to interchange types. Note the constructor of
        // a new SfpName. See prior TODO on RPC
        SffServicePathKey sffServicePathKey =
                new SffServicePathKey(new SfpName(renderedServicePath.getName().getValue()));
        SffServicePathBuilder sffServicePathBuilder = new SffServicePathBuilder();
        sffServicePathBuilder.setKey(sffServicePathKey);
        sffServicePathBuilder.setName(new SfpName(renderedServicePath.getName().getValue()));

        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                    new ServiceFunctionForwarderStateKey(renderedServicePathHop.getServiceFunctionForwarder());
            InstanceIdentifier<SffServicePath> sfStateIID =
                    InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                        .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                        .child(SffServicePath.class, sffServicePathKey)
                        .build();
            serviceFunctionForwarderStateBuilder.setName(renderedServicePathHop.getServiceFunctionForwarder());

            if (SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, sffServicePathBuilder.build(),
                    LogicalDatastoreType.OPERATIONAL)) {
                ret = ret && true;
            } else {
                ret = ret && false;
                LOG.error("Failed to add path {} to SFF {} state.", renderedServicePath.getName(),
                        renderedServicePathHop.getServiceFunctionForwarder());
            }
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * When a SFF is deleted we need to delete all SFPs from the
     * associated SFF operational state
     * <p>
     *
     * @param serviceFunctionPath SFP object
     * @return true if all paths were deleted, false otherwise.
     */
    public static boolean deletePathFromServiceForwarderState(ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);

        boolean ret = true;

        // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
        RspName rspName = new RspName(serviceFunctionPath.getName().getValue());
        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI
            .readRenderedServicePath(new RspName(serviceFunctionPath.getName().getValue()));

        if (renderedServicePath != null) {
            Set<SffName> sffNameSet = new HashSet<>();
            List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
            for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {

                SffName sffName = renderedServicePathHop.getServiceFunctionForwarder();
                if (sffNameSet.add(sffName)) {
                    // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
                    SffServicePathKey sffServicePathKey = new SffServicePathKey(new SfpName(rspName.getValue()));
                    ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                            new ServiceFunctionForwarderStateKey(sffName);
                    InstanceIdentifier<SffServicePath> sfStateIID =
                            InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                                .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                                .child(SffServicePath.class, sffServicePathKey)
                                .build();
                    if (SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL)) {
                        ret = true;
                    } else {
                        ret = false;
                        LOG.error("Could not delete Service Path {} from SFF {} operational state : {}", rspName,
                                sffName);
                    }
                    List<SffServicePath> sffServicePathList = readSffState(sffName);
                    if ((sffServicePathList != null) && sffServicePathList.isEmpty()) {
                        if (deleteServiceFunctionForwarderState(sffName)) {
                            ret = ret && true;
                        } else {
                            ret = ret && false;
                        }
                    }
                }
            }
        } else {
            LOG.error("{}: Rendered Service Path {} does not exist", Thread.currentThread().getStackTrace()[1],
                    serviceFunctionPath.getName());
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Delete the given list of service paths from the SFF operational
     * state
     * <p>
     *
     * @param renderedServicePaths String List of Service Path names
     * @return True if paths were deleted, false otherwise
     */
    public static boolean deletePathFromServiceForwarderState(List<RspName> renderedServicePaths) {

        printTraceStart(LOG);
        boolean ret = false;

        for (RspName rspName : renderedServicePaths) {
            if (SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspName)) {
                ret = true;
            } else {
                LOG.debug("RSP {} already deleted by another thread or client", rspName);
                ret = true;
            }
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * When a SFF is deleted we need to delete all SFPs from the
     * associated SFF operational state
     * <p>
     *
     * @param rspName SFP object
     * @return true if all path was deleted, false otherwise.
     */
    public static boolean deletePathFromServiceForwarderState(RspName rspName) {

        printTraceStart(LOG);

        boolean ret = true;

        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);

        if (renderedServicePath != null) {
            Set<SffName> sffNameSet = new HashSet<>();
            List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
            for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {

                SffName sffname = renderedServicePathHop.getServiceFunctionForwarder();
                if (sffNameSet.add(sffname)) {
                    // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
                    SffServicePathKey sffServicePathKey = new SffServicePathKey(new SfpName(rspName.getValue()));
                    ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                            new ServiceFunctionForwarderStateKey(sffname);
                    InstanceIdentifier<SffServicePath> sfStateIID =
                            InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                                .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                                .child(SffServicePath.class, sffServicePathKey)
                                .build();
                    if (SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL)) {
                        ret = ret && true;
                    } else {
                        ret = ret && false;
                        LOG.error("Could not delete Service Path {} from SFF {} operational state : {}", rspName,
                                sffname);
                    }
                    List<SffServicePath> sffServicePathList = readSffState(sffname);
                    if (sffServicePathList.isEmpty()) {
                        if (deleteServiceFunctionForwarderState(sffname)) {
                            ret = ret && true;
                        } else {
                            ret = ret && false;
                        }
                    }
                }
            }
        } else {
            LOG.error("{}: Rendered Service Path {} does not exist", Thread.currentThread().getStackTrace()[1],
                    rspName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method deletes the operational state for a service function.
     * <p>
     *
     * @param sffName SFF name
     * @return A ServiceFunctionState object that is a list of all paths using
     *         this service function, null otherwise
     */
    public static boolean deleteServiceFunctionForwarderState(SffName sffName) {
        printTraceStart(LOG);
        boolean ret = false;
        ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                new ServiceFunctionForwarderStateKey(sffName);
        InstanceIdentifier<ServiceFunctionForwarderState> sffStateIID =
                InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                    .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                    .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sffStateIID, LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("{}: Could not delete operational state for SFF: {}", Thread.currentThread().getStackTrace()[1],
                    sffName);
        }
        return ret;
    }

    /**
     * Returns the list of SFPs anchored by a SFF
     * <p>
     *
     * @param sffName SFF name
     * @return SffServicePath
     */
    public static List<SffServicePath> readSffState(SffName sffName) {
        printTraceStart(LOG);
        List<SffServicePath> ret = null;

        ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                new ServiceFunctionForwarderStateKey(sffName);

        InstanceIdentifier<ServiceFunctionForwarderState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                    .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                    .build();

        ServiceFunctionForwarderState sffStateDataObject;
        sffStateDataObject = SfcDataStoreAPI.readTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL);
        // Read the list of Service Function Path anchored by this SFF
        if (sffStateDataObject != null) {
            ret = sffStateDataObject.getSffServicePath();
        } else {
            LOG.warn("Service Function Forwarder {} has no operational state", sffName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Deletes all RSPs used by the given SFF object
     * <p>
     *
     * @param serviceFunctionForwarder SFF Object
     * @return true if all RSPs were deleted, false otherwise
     */
    public static boolean deleteRenderedPathsUsedByServiceForwarder(ServiceFunctionForwarder serviceFunctionForwarder) {

        printTraceStart(LOG);

        boolean ret = false;
        List<SffServicePath> sffServicePathList = readSffState(serviceFunctionForwarder.getName());
        if (!sffServicePathList.isEmpty()) {
            for (SffServicePath sffServicePath : sffServicePathList) {
                // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
                RspName rspName = new RspName(sffServicePath.getName().getValue());
                if (SfcProviderRenderedPathAPI.readRenderedServicePath(rspName) != null) {
                    if (SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName)) {
                        ret = true;
                    } else {
                        LOG.error("{} :Failed to delete RSP {}", Thread.currentThread().getStackTrace()[1], rspName);
                        ret = false;
                    }
                } else {
                    LOG.error("{} :Failed to read RSP {}", Thread.currentThread().getStackTrace()[1], rspName);
                    ret = true;
                }
            }
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * When a SFF is deleted we need to delete all SFPs from the
     * associated SFF operational state
     * <p>
     *
     * @param rspName SFP name
     * @param sffName SFF name
     * @return true if all paths were deleted, false otherwise.
     */
    public static boolean deletePathFromServiceForwarderState(RspName rspName, SffName sffName) {

        printTraceStart(LOG);

        boolean ret = true;
        // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
        SffServicePathKey sffServicePathKey = new SffServicePathKey(new SfpName(rspName.getValue()));
        ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                new ServiceFunctionForwarderStateKey(sffName);
        InstanceIdentifier<SffServicePath> sfStateIID = InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
            .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
            .child(SffServicePath.class, sffServicePathKey)
            .build();
        if (SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL)) {
            ret = ret && true;
        } else {
            ret = ret && false;
            LOG.error("{}: Could not delete Service Path {} from SFF {} operational state : {}",
                    Thread.currentThread().getStackTrace()[1], rspName, sffName);
        }
        List<SffServicePath> sffServicePathList = readSffState(sffName);
        if ((sffServicePathList != null) && sffServicePathList.isEmpty()) {
            if (deleteServiceFunctionForwarderState(sffName)) {
                ret = ret && true;
            } else {
                ret = ret && false;
            }
        }

        printTraceStop(LOG);
        return ret;
    }
}
