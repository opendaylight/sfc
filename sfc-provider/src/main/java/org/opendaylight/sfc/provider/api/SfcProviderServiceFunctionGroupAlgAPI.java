/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev150214.ServiceFunctionGroupAlgorithms;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev150214.service.function.group.algorithms.ServiceFunctionGroupAlgorithm;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev150214.service.function.group.algorithms.ServiceFunctionGroupAlgorithmKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


/**
 * This class has the APIs to operate on the ServiceFunctionGroupAlgorithm datastore. <p> It is normally
 * called from onDataChanged() through a executor service. We need to use an executor service because we
 * cannot operate on a datastore while on onDataChanged() context.
 * @author Kfir Yeshayahu (kfir.yeshayahu@contextream.com)
 * @version 0.1 <p>
 * @since 2015-02-14
 */
public class SfcProviderServiceFunctionGroupAlgAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceFunctionGroupAlgAPI.class);

    /**
     * Reads a SFG Algorithm from the datastore <p>
     * @param serviceFunctionGroupAlgorithmName name
     * @return ServiceFunctionGroupAlgorithm object or null if not found
     */
    protected static ServiceFunctionGroupAlgorithm readServiceFunctionGroupAlgorithm(String serviceFunctionGroupAlgorithmName) {
        printTraceStart(LOG);
        ServiceFunctionGroupAlgorithm sfgAlg;
        InstanceIdentifier<ServiceFunctionGroupAlgorithm> sfgAlgIID;
        ServiceFunctionGroupAlgorithmKey serviceFunctionGroupAlgorithmKey = new ServiceFunctionGroupAlgorithmKey(serviceFunctionGroupAlgorithmName);
        sfgAlgIID = InstanceIdentifier.builder(ServiceFunctionGroupAlgorithms.class).child(ServiceFunctionGroupAlgorithm.class, serviceFunctionGroupAlgorithmKey).build();

        sfgAlg = SfcDataStoreAPI.readTransactionAPI(sfgAlgIID, LogicalDatastoreType.CONFIGURATION);
        printTraceStop(LOG);
        return sfgAlg;
    }

    /**
     * Puts a SFG Algorithm in the datastore <p>
     * @param sfgAlg the ServiceFunctionGroupAlgorithm to put
     * @return boolean success or failure
     */
    protected static boolean putServiceFunctionGroupAlgorithm(ServiceFunctionGroupAlgorithm sfgAlg) {
        boolean ret;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionGroupAlgorithm> sfgAlgEntryIID = InstanceIdentifier.builder(ServiceFunctionGroupAlgorithms.class).child(ServiceFunctionGroupAlgorithm.class, sfgAlg.getKey()).toInstance();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sfgAlgEntryIID, sfgAlg, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Deletes a SFG Algorithm from the datastore <p>
     * @param serviceFunctionGroupAlgorithmName SFG Algorithm name
     * @return boolean success of failure
     */
    protected static boolean deleteServiceFunctionGroupAlgorithm(String serviceFunctionGroupAlgorithmName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionGroupAlgorithmKey serviceFunctionGroupAlgorithmKey = new ServiceFunctionGroupAlgorithmKey(serviceFunctionGroupAlgorithmName);
        InstanceIdentifier<ServiceFunctionGroupAlgorithm> sfgAlgEntryIID = InstanceIdentifier.builder(ServiceFunctionGroupAlgorithms.class).child(ServiceFunctionGroupAlgorithm.class, serviceFunctionGroupAlgorithmKey).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfgAlgEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("{}: Could not delete SFG Algorithm: {}", Thread.currentThread().getStackTrace()[1], serviceFunctionGroupAlgorithmName);
        }
        printTraceStop(LOG);
        return ret;
    }

    public static ServiceFunctionGroupAlgorithm readServiceFunctionGroupAlg(String serviceFunctionGroupAlgName) {
        printTraceStart(LOG);
        ServiceFunctionGroupAlgorithm ret =
                SfcProviderServiceFunctionGroupAlgAPI.readServiceFunctionGroupAlgorithm(serviceFunctionGroupAlgName);
        LOG.debug("getRead: {}", ret);
        printTraceStop(LOG);
        return ret;
    }

}
