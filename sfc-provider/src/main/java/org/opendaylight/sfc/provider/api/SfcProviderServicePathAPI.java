/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

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
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

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
}
