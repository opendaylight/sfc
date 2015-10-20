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

import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePathKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to operate on the ServiceFunctionPath
 * datastore.
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since 2014-06-30
 */
public class SfcProviderServicePathAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServicePathAPI.class);

    /**
     * This function checks if the Service Path has any constraints
     *
     * @param serviceFunctionPath Service Path object
     * @return List of RSP name objects
     */
    public static boolean isDefaultServicePath(ServiceFunctionPath serviceFunctionPath) {
        boolean ret = true;
        if ((serviceFunctionPath.getServicePathHop() != null) || (serviceFunctionPath.getTransportType() != null)
                || (serviceFunctionPath.getStartingIndex() != null) || (serviceFunctionPath.getPathId() != null)) {
            ret = false;
        }
        return ret;
    }

    /**
     * API to read the Service Function Path operational state
     *
     * @param servicePathName Service Path Name
     * @return List of RSP name objects
     */
    public static List<SfpRenderedServicePath> readServicePathState(SfpName servicePathName) {

        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPathState> sfpIID;
        List<SfpRenderedServicePath> ret = null;

        ServiceFunctionPathStateKey serviceFunctionPathStateKey = new ServiceFunctionPathStateKey(servicePathName);

        sfpIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
            .child(ServiceFunctionPathState.class, serviceFunctionPathStateKey)
            .build();

        ServiceFunctionPathState serviceFunctionPathState =
                SfcDataStoreAPI.readTransactionAPI(sfpIID, LogicalDatastoreType.OPERATIONAL);
        if (serviceFunctionPathState != null) {
            ret = serviceFunctionPathState.getSfpRenderedServicePath();
        }
        printTraceStop(LOG);

        return ret;
    }

    /**
     * Wrapper API to delete the Service Function Path operational state
     *
     * @param servicePathName Service Path Name
     * @return Nothing.
     */
    protected static boolean deleteServicePathState(SfpName servicePathName) {

        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPathState> sfpIID;
        boolean ret = false;

        ServiceFunctionPathStateKey serviceFunctionPathStateKey = new ServiceFunctionPathStateKey(servicePathName);

        sfpIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
            .child(ServiceFunctionPathState.class, serviceFunctionPathStateKey)
            .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfpIID, LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("{}: Failed to delete Service Function Path {} state.", Thread.currentThread().getStackTrace()[1],
                    servicePathName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them.
     *
     * @param servicePathName Service Function Path name
     * @param renderedPathName Rendered Path name
     * @return Nothing.
     */
    public static boolean addRenderedPathToServicePathState(SfpName servicePathName, RspName renderedPathName) {

        printTraceStart(LOG);
        InstanceIdentifier<SfpRenderedServicePath> rspIID;
        boolean ret = false;

        SfpRenderedServicePathBuilder sfpRenderedServicePathBuilder = new SfpRenderedServicePathBuilder();
        SfpRenderedServicePathKey sfpRenderedServicePathKey = new SfpRenderedServicePathKey(renderedPathName);
        sfpRenderedServicePathBuilder.setKey(sfpRenderedServicePathKey).setName(renderedPathName);

        ServiceFunctionPathStateKey serviceFunctionPathStateKey = new ServiceFunctionPathStateKey(servicePathName);

        rspIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
            .child(ServiceFunctionPathState.class, serviceFunctionPathStateKey)
            .child(SfpRenderedServicePath.class, sfpRenderedServicePathKey)
            .build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, sfpRenderedServicePathBuilder.build(),
                LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("{}: Failed to create Service Function Path {} state. Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], servicePathName, renderedPathName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This function reads a SFP from the datastore
     *
     * @param serviceFunctionPathName RSP name
     * @return Nothing.
     */
    public static ServiceFunctionPath readServiceFunctionPath(SfpName serviceFunctionPathName) {
        printTraceStart(LOG);
        ServiceFunctionPath sfp;
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
            .child(ServiceFunctionPath.class, serviceFunctionPathKey)
            .build();

        sfp = SfcDataStoreAPI.readTransactionAPI(sfpIID, LogicalDatastoreType.CONFIGURATION);
        printTraceStop(LOG);
        return sfp;
    }

    /**
     * This function deletes a SFP from the datastore
     *
     * @param serviceFunctionPathName SFP name
     * @return Nothing.
     */
    public static boolean deleteServiceFunctionPath(SfpName serviceFunctionPathName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        InstanceIdentifier<ServiceFunctionPath> sfpEntryIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
            .child(ServiceFunctionPath.class, serviceFunctionPathKey)
            .build();

        if (!SfcDataStoreAPI.deleteTransactionAPI(sfpEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("Failed to delete SFP: {}", serviceFunctionPathName);
        } else {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    public static ServiceFunctionPaths readAllServiceFunctionPaths() {
        ServiceFunctionPaths sfps;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPaths> sfpsIID =
                InstanceIdentifier.builder(ServiceFunctionPaths.class).build();

        sfps = SfcDataStoreAPI.readTransactionAPI(sfpsIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return sfps;
    }

    public static boolean putServiceFunctionPath(ServiceFunctionPath sfp) {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPath> sfpEntryIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
            .child(ServiceFunctionPath.class, sfp.getKey())
            .build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(sfpEntryIID, sfp, LogicalDatastoreType.CONFIGURATION)) {
            LOG.debug("Created Service Function Path: {}", sfp.getName());
            ret = true;
        } else {
            LOG.error("Failed to create Service Function Path: {}", sfp.getName());
        }

        printTraceStop(LOG);
        return ret;
    }

    protected static boolean putAllServiceFunctionPaths(ServiceFunctionPaths sfps) {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPaths> sfpsIID =
                InstanceIdentifier.builder(ServiceFunctionPaths.class).build();

        if (SfcDataStoreAPI.writePutTransactionAPI(sfpsIID, sfps, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected static boolean deleteAllServiceFunctionPaths() {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPaths> sfpsIID =
                InstanceIdentifier.builder(ServiceFunctionPaths.class).build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfpsIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them. Additionally, since we are delete the RSP, we also
     *
     * @param serviceFunction Service Function Object
     * @return Nothing.
     */
    public static boolean deleteServicePathContainingFunction(ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        boolean ret = true;
        List<SfServicePath> sfServicePathList;

        sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(serviceFunction.getName());
        if (sfServicePathList != null) {
            for (SfServicePath sfServicePath : sfServicePathList) {
                // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
                RspName rspName = new RspName(sfServicePath.getName().getValue());
                if (SfcProviderRenderedPathAPI.readRenderedServicePath(rspName) != null) {
                    if (SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName)) {
                        ret = true;
                    } else {
                        LOG.error("Failed to delete Path {} from Service Function {} state", rspName,
                                serviceFunction.getName());
                        ret = false;
                    }
                } else {
                    LOG.debug("{}: SFP {} already deleted by another thread or client",
                            Thread.currentThread().getStackTrace()[1], rspName);
                }
            }
        } else {
            LOG.debug("Could not find Service function Paths using Service Function: {} ", serviceFunction.getName());
        }
        printTraceStop(LOG);
        return ret;
    }
}
