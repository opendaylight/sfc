/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev160202.ServiceFunctionGroupAlgorithms;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev160202.service.function.group.algorithms.ServiceFunctionGroupAlgorithm;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev160202.service.function.group.algorithms.ServiceFunctionGroupAlgorithmKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to operate on the ServiceFunctionGroupAlgorithm
 * datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor service. We
 * need to use an executor service because we can not operate on a datastore
 * while on onDataChanged() context.
 *
 * @author Kfir Yeshayahu (kfir.yeshayahu@contextream.com)
 * @version 0.1
 * <p/>
 * @since 2015-02-14
 */
public class SfcProviderServiceFunctionGroupAlgAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceTypeAPI.class);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();

    SfcProviderServiceFunctionGroupAlgAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceFunctionGroupAlgAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    @SuppressWarnings("rawtypes")
    public static SfcProviderServiceFunctionGroupAlgAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAlgAPI(params, paramsTypes, "putServiceFunctionGroupAlgorithm");
    }
    @SuppressWarnings("rawtypes")
    public static SfcProviderServiceFunctionGroupAlgAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAlgAPI(params, paramsTypes, "readServiceFunctionGroupAlgorithm");
    }
    @SuppressWarnings("rawtypes")
    public static SfcProviderServiceFunctionGroupAlgAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAlgAPI(params, paramsTypes, "deleteServiceFunctionGroupAlgorithm");
    }

    /**
     * Reads a SFG Algorithm from the datastore
     * <p>
     * @param serviceFunctionGroupAlgorithm name
     * @return ServiceFunctionGroupAlgorithm object or null if not found
     */
    protected  ServiceFunctionGroupAlgorithm readServiceFunctionGroupAlgorithm(String serviceFunctionGroupAlgorithmName) {
        printTraceStart(LOG);
        ServiceFunctionGroupAlgorithm sfgAlg;
        InstanceIdentifier<ServiceFunctionGroupAlgorithm> sfgAlgIID;
        ServiceFunctionGroupAlgorithmKey serviceFunctionGroupAlgorithmKey = new ServiceFunctionGroupAlgorithmKey(serviceFunctionGroupAlgorithmName);
        sfgAlgIID = InstanceIdentifier.builder(ServiceFunctionGroupAlgorithms.class)
                .child(ServiceFunctionGroupAlgorithm.class, serviceFunctionGroupAlgorithmKey).build();

        sfgAlg = SfcDataStoreAPI.readTransactionAPI(sfgAlgIID, LogicalDatastoreType.CONFIGURATION);
        printTraceStop(LOG);
        return sfgAlg;
    }

    /**
     * Puts a SFG Algorithm in the datastore
     * <p>
     * @param sfgAlg the ServiceFunctionGroupAlgorithm to put
     * @return boolean success or failure
     */
    protected boolean putServiceFunctionGroupAlgorithm (ServiceFunctionGroupAlgorithm sfgAlg) {
        boolean ret;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionGroupAlgorithm > sfgAlgEntryIID = InstanceIdentifier.builder(ServiceFunctionGroupAlgorithms.class).
                child(ServiceFunctionGroupAlgorithm.class, sfgAlg.getKey()).toInstance();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sfgAlgEntryIID, sfgAlg, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Deletes a SFG Algorithm from the datastore
     * <p>
     * @param serviceFunctionGroupAlgorithmName SFG Algorithm name
     * @return boolean success of failure
     */
    protected boolean deleteServiceFunctionGroupAlgorithm(String serviceFunctionGroupAlgorithmName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionGroupAlgorithmKey serviceFunctionGroupAlgorithmKey = new ServiceFunctionGroupAlgorithmKey(serviceFunctionGroupAlgorithmName);
        InstanceIdentifier<ServiceFunctionGroupAlgorithm> sfgAlgEntryIID = InstanceIdentifier.builder(ServiceFunctionGroupAlgorithms.class).
                child(ServiceFunctionGroupAlgorithm.class, serviceFunctionGroupAlgorithmKey).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfgAlgEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not delete SFG Algorithm: {}", serviceFunctionGroupAlgorithmName);
        }
        printTraceStop(LOG);
        return ret;
    }
}