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


import java.util.concurrent.ExecutionException;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class has the APIs to operate on the ServiceFunctionClassifier
 * datastore.
 *
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 *
 *
 * <p>
 * @author Andrej Kincel (akincel@cisco.com)
 * @version 0.1
 * @since       2014-11-04
 */
public class SfcProviderServiceClassifierAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceClassifierAPI.class);

    SfcProviderServiceClassifierAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceClassifierAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

//    public static SfcProviderServiceClassifierAPI getPut(Object[] params, Class[] paramsTypes) {
//        return new SfcProviderServiceClassifierAPI(params, paramsTypes, "putServiceFunctionClassifier");
//    }

    public static SfcProviderServiceClassifierAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceClassifierAPI(params, paramsTypes, "readServiceFunctionClassifier");
    }

    public static SfcProviderServiceClassifierAPI getReadBySfcName(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceClassifierAPI(params, paramsTypes, "readServiceFunctionClassifierBySfcName");
    }
//
//    public static SfcProviderServiceClassifierAPI getDelete(Object[] params, Class[] paramsTypes) {
//        return new SfcProviderServiceClassifierAPI(params, paramsTypes, "deleteServiceFunctionClassifier");
//    }
//
//    public static SfcProviderServiceClassifierAPI getPutAll(Object[] params, Class[] paramsTypes) {
//        return new SfcProviderServiceClassifierAPI(params, paramsTypes, "putAllServiceFunctionClassifiers");
//    }
//
    public static SfcProviderServiceClassifierAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceClassifierAPI(params, paramsTypes, "readAllServiceFunctionClassifiers");
    }
//
//    public static SfcProviderServiceClassifierAPI getDeleteAll(Object[] params, Class[] paramsTypes) {
//        return new SfcProviderServiceClassifierAPI(params, paramsTypes, "deleteAllServiceFunctionClassifiers");
//    }

    protected ServiceFunctionClassifier readServiceFunctionClassifier(String serviceFunctionClassifierName) {
        printTraceStart(LOG);
        ServiceFunctionClassifier scf = null;
        InstanceIdentifier<ServiceFunctionClassifier> sfcIID;
        ServiceFunctionClassifierKey serviceFunctionClassifierKey =
                new ServiceFunctionClassifierKey(serviceFunctionClassifierName);
        sfcIID = InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
                .child(ServiceFunctionClassifier.class, serviceFunctionClassifierKey).build();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunctionClassifier> serviceFunctionClassifierDataObject;
            try {
                serviceFunctionClassifierDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfcIID).get();
                if (serviceFunctionClassifierDataObject != null
                        && serviceFunctionClassifierDataObject.isPresent()) {
                    scf = serviceFunctionClassifierDataObject.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read Service Function Classifier " +
                        "configuration {}", serviceFunctionClassifierName);
            }
        }
        printTraceStop(LOG);
        return scf;
    }

    protected ServiceFunctionClassifiers readAllServiceFunctionClassifiers() {
        ServiceFunctionClassifiers scfs = null;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionClassifiers> scfsIID = InstanceIdentifier
                .builder(ServiceFunctionClassifiers.class).toInstance();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceFunctionClassifiers> serviceFunctionClassifiersDataObject = null;
            try {
                serviceFunctionClassifiersDataObject = readTx
                        .read(LogicalDatastoreType.CONFIGURATION, scfsIID).get();
                if (serviceFunctionClassifiersDataObject != null
                        && serviceFunctionClassifiersDataObject.isPresent()) {
                    scfs = serviceFunctionClassifiersDataObject.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read Service Function Classifiers " +
                        "configuration");
            }
        }

        return scfs;
    }

    protected ServiceFunctionClassifier readServiceFunctionClassifierBySfcName(String serviceFunctionChainName) {
        ServiceFunctionClassifiers scfs = this.readAllServiceFunctionClassifiers();

        if (scfs != null) {
            for(ServiceFunctionClassifier scf : scfs.getServiceFunctionClassifier()){
                if(scf.getServiceFunctionChain().equals(serviceFunctionChainName)){
                    printTraceStop(LOG);
                    return scf;
                }
            }
        }

        printTraceStop(LOG);
        return null;
    }
}
