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

import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
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
 * This class has the APIs to operate on the ServiceFunctionPath datastore. It
 * is normally called from onDataChanged() through a executor service. We need
 * to use an executor service because we can not operate on a datastore while on
 * onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since 2014-06-30
 */
public final class SfcProviderServicePathAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServicePathAPI.class);

    private SfcProviderServicePathAPI() {
    }

    /**
     * API to read the Service Function Path operational state.
     *
     * @param servicePathName
     *            Service Path Name
     * @return List of RSP name objects if the operational state exists, null
     *         otherwise.
     */
    public static List<SfpRenderedServicePath> readServicePathState(SfpName servicePathName) {
        InstanceIdentifier<ServiceFunctionPathState> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                .child(ServiceFunctionPathState.class, new ServiceFunctionPathStateKey(servicePathName))
                .build();
        ServiceFunctionPathState serviceFunctionPathState;
        serviceFunctionPathState = SfcDataStoreAPI.readTransactionAPI(sfpIID, LogicalDatastoreType.OPERATIONAL);
        if (serviceFunctionPathState == null) {
            return null;
        }
        List<SfpRenderedServicePath> pathList = serviceFunctionPathState.getSfpRenderedServicePath();
        return pathList == null ? Collections.emptyList() : pathList;
    }

    /**
     * We iterate through all service paths that use this service function and
     * if necessary, remove them.
     *
     * @param servicePathName
     *            Service Function Path name
     * @param renderedPathName
     *            Rendered Path name
     * @return Nothing.
     */
    public static boolean addRenderedPathToServicePathState(SfpName servicePathName, RspName renderedPathName) {
        printTraceStart(LOG);

        SfpRenderedServicePathKey sfpRenderedServicePathKey = new SfpRenderedServicePathKey(renderedPathName);
        SfpRenderedServicePathBuilder sfpRenderedServicePathBuilder = new SfpRenderedServicePathBuilder();
        sfpRenderedServicePathBuilder.setKey(sfpRenderedServicePathKey).setName(renderedPathName);

        InstanceIdentifier<SfpRenderedServicePath> rspIID =
                InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                        .child(ServiceFunctionPathState.class, new ServiceFunctionPathStateKey(servicePathName))
                        .child(SfpRenderedServicePath.class, sfpRenderedServicePathKey).build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, sfpRenderedServicePathBuilder.build(),
                LogicalDatastoreType.OPERATIONAL)) {
            return true;
        } else {
            LOG.error("{}: Failed to create Service Function Path {} state. Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], servicePathName, renderedPathName);
        }
        printTraceStop(LOG);
        return false;
    }

    public static boolean deleteRenderedPathFromServicePathState(SfpName sfpName, RspName rspName) {
        InstanceIdentifier<SfpRenderedServicePath> rspIID =
                InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                        .child(ServiceFunctionPathState.class, new ServiceFunctionPathStateKey(sfpName))
                        .child(SfpRenderedServicePath.class, new SfpRenderedServicePathKey(rspName))
                        .build();

        return SfcDataStoreAPI.deleteTransactionAPI(rspIID, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * This function reads a SFP from the datastore.
     *
     * @param serviceFunctionPathName
     *            RSP name
     * @return Nothing.
     */
    public static ServiceFunctionPath readServiceFunctionPath(SfpName serviceFunctionPathName) {
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

    public static ServiceFunctionPaths readAllServiceFunctionPaths() {
        ServiceFunctionPaths sfps;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .build();

        sfps = SfcDataStoreAPI.readTransactionAPI(sfpsIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return sfps;
    }

    public static boolean putServiceFunctionPath(ServiceFunctionPath sfp) {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPath> sfpEntryIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, sfp.getKey()).build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(sfpEntryIID, sfp, LogicalDatastoreType.CONFIGURATION)) {
            LOG.debug("Created Service Function Path: {}", sfp.getName());
            ret = true;
        } else {
            LOG.error("Failed to create Service Function Path: {}", sfp.getName());
        }

        printTraceStop(LOG);
        return ret;
    }

    public static boolean deleteServiceFunctionPath(SfpName sfpName) {
        ServiceFunctionPath sfp = readServiceFunctionPath(sfpName);
        if (sfp == null) {
            LOG.error("Failed to delete non-existent Service Function Path: {}", sfpName);
            return false;
        }

        return deleteServiceFunctionPath(sfp);
    }

    public static boolean deleteServiceFunctionPath(ServiceFunctionPath sfp) {
        InstanceIdentifier<ServiceFunctionPath> sfpEntryIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, sfp.getKey()).build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfpEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            LOG.info("Deleted Service Function Path: {}", sfp.getName());
            return true;
        }

        return false;
    }

    /**
     * This method deletes the operational state for a service function.
     *
     * @param sfpName
     *            Service Path Name
     * @return A ServiceFunctionState object that is a list of all paths using
     *         this service function, null otherwise
     */
    public static boolean deleteServiceFunctionState(SfpName sfpName) {
        InstanceIdentifier<ServiceFunctionPathState> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                .child(ServiceFunctionPathState.class, new ServiceFunctionPathStateKey(sfpName))
                .build();
        return SfcDataStoreAPI.deleteTransactionAPI(sfpIID, LogicalDatastoreType.OPERATIONAL);
    }
}
