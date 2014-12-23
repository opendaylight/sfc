/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.SfcReflection;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiersState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.ServiceFunctionClassifierState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.ServiceFunctionClassifierStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.service.function.classifier.state.SclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.service.function.classifier.state.SclRenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.service.function.classifier.state.SclRenderedServicePathKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
public class SfcProviderServiceClassifierAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceClassifierAPI.class);
    private static final String FAILED_TO_STR = "failed to ...";

    SfcProviderServiceClassifierAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceClassifierAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }


    public static SfcProviderServiceClassifierAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceClassifierAPI(params, paramsTypes, "readServiceClassifier");
    }

    public static SfcProviderServiceClassifierAPI getAddRenderedPathToServiceClassifierStateExecutor
            (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceClassifierAPI(params, paramsTypes, "addRenderedPathToServiceClassifierState");
    }

    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them.
     * <p>
     * @param serviceClassifierName Service Function Classifier name
     * @param renderedPathName Rendered Path name
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static boolean addRenderedPathToServiceClassifierState (String serviceClassifierName, String renderedPathName) {

        printTraceStart(LOG);
        InstanceIdentifier<SclRenderedServicePath> sclIID;
        boolean ret = false;

        SclRenderedServicePathBuilder sclRenderedServicePathBuilder = new SclRenderedServicePathBuilder();
        SclRenderedServicePathKey sclRenderedServicePathKey = new SclRenderedServicePathKey(renderedPathName);
        sclRenderedServicePathBuilder.setKey(sclRenderedServicePathKey).setName(renderedPathName);

        ServiceFunctionClassifierStateKey serviceFunctionClassifierStateKey = new ServiceFunctionClassifierStateKey(serviceClassifierName);

        sclIID = InstanceIdentifier.builder(ServiceFunctionClassifiersState.class)
                .child(ServiceFunctionClassifierState.class, serviceFunctionClassifierStateKey)
                .child(SclRenderedServicePath.class, sclRenderedServicePathKey).toInstance();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(sclIID, sclRenderedServicePathBuilder.build(),
                LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("{}: Failed to create Service Function Classifier {} state. Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], serviceClassifierName, renderedPathName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them.
     * <p>
     * @param serviceClassifierName Service Function Classifier name
     * @param renderedPathName Rendered Path name
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static boolean addRenderedPathToServiceClassifierStateExecutor (String serviceClassifierName, String renderedPathName) {

        printTraceStart(LOG);
        boolean ret = true;
        Object[] functionParams = {serviceClassifierName, renderedPathName};
        Class[] functionParamsTypes = {String.class, String.class};
        Future future = ODL_SFC.getExecutor().submit(SfcProviderServiceClassifierAPI
                .getAddRenderedPathToServiceClassifierStateExecutor(functionParams, functionParamsTypes));
        try {
            ret = (boolean) future.get();
            LOG.debug("getAddRenderedPathToServiceClassifierStateExecutor returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads a classifier from DataStore
     * <p>
     * @param serviceClassifierName Classifier name
     * @return SF object or null if not found
     */
    @SuppressWarnings("unused")
    @SfcReflection
    protected ServiceFunctionClassifier readServiceClassifier(String serviceClassifierName) {
        printTraceStart(LOG);
        ServiceFunctionClassifier scl;
        InstanceIdentifier<ServiceFunctionClassifier> sclIID;
        ServiceFunctionClassifierKey serviceFunctionKey = new ServiceFunctionClassifierKey(serviceClassifierName);
        sclIID = InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
                .child(ServiceFunctionClassifier.class, serviceFunctionKey).build();

        scl = SfcDataStoreAPI.readTransactionAPI(sclIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return scl;
    }


    /**
     * Wrapper API to reads a service function classifier from datastore
     * <p>
     * @param serviceClassifierName Service Classifier Name
     * @return A ServiceFunctionState object that is a list of all paths using
     * this service function, null otherwise
     */
    public static ServiceFunctionClassifier readServiceClassifierExecutor(String serviceClassifierName) {

        printTraceStart(LOG);
        ServiceFunctionClassifier ret = null;
        Object[] servicePathObj = {serviceClassifierName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceClassifierAPI sfcProviderServiceClassifierAPI = SfcProviderServiceClassifierAPI
                .getRead(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceClassifierAPI);
        try {
            ret = (ServiceFunctionClassifier) future.get();
            LOG.debug("getRead: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }


}
