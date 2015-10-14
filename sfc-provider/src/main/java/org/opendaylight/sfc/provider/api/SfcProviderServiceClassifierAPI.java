/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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


/**
 * This class has the APIs to operate on the Service Classifier datastore.
 * <p>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * <p>
 * @since 2014-11-04
 */
public class SfcProviderServiceClassifierAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceClassifierAPI.class);

    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them.
     * <p>
     * @param serviceClassifierName Service Function Classifier name
     * @param renderedPathName Rendered Path name
     * @return Nothing.
     */
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
     * This method reads a classifier from DataStore
     * <p>
     * @param serviceClassifierName Classifier name
     * @return SF object or null if not found
     */
    public static ServiceFunctionClassifier readServiceClassifier(String serviceClassifierName) {
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


}
