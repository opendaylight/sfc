/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ofsfc.provider;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;

/**
 * This the main SFC Provider class. It is instantiated from the
 * SFCProviderModule class.
 *
 * <p>
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 * @see org.opendaylight.controller.config.yang.config.sfc_provider.impl.SfcProviderModule
 */

public class OpenFlowSfcRenderer implements AutoCloseable {


    private static final Logger LOG = LoggerFactory.getLogger(OpenFlowSfcRenderer.class);

    public static final InstanceIdentifier<ServiceFunctionChain>  ofsfcEntryIID =
            InstanceIdentifier.builder(ServiceFunctionChains.class)
                    .child(ServiceFunctionChain.class).build();

    public static final InstanceIdentifier<ServiceFunction>  ofsfEntryIID =
           InstanceIdentifier.builder(ServiceFunctions.class).child(ServiceFunction.class).build();

    public static final InstanceIdentifier<ServiceFunctionPath>  ofsfpEntryIID =
            InstanceIdentifier.builder(ServiceFunctionPaths.class)
                    .child(ServiceFunctionPath.class).build();

    public static final InstanceIdentifier<ServiceFunctions>  ofsfsIID =
            InstanceIdentifier.builder(ServiceFunctions.class).build();
    public static final InstanceIdentifier<ServiceNodes>  ofsnIID =
           InstanceIdentifier.builder(ServiceNodes.class).build();
    public static final InstanceIdentifier<ServiceFunctionPaths>  ofsfpIID =
           InstanceIdentifier.builder(ServiceFunctionPaths.class).build();
    public static final InstanceIdentifier<ServiceFunctionChains>  ofsfcIID =
           InstanceIdentifier.builder(ServiceFunctionChains.class).build();

    public static final InstanceIdentifier<ServiceFunctionForwarders>  ofsffIID =
           InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();
    public static final InstanceIdentifier<ServiceFunctionTypes>  ofsftIID =
           InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

    protected ExecutorService executor;
    protected DataBroker dataProvider;
    private static OpenFlowSfcRenderer opendaylightSfcObj;

    private Future<RpcResult<Void>> currentTask;

    public OpenFlowSfcRenderer() {
       executor = Executors.newFixedThreadPool(1);
       opendaylightSfcObj = this;
    }

    private Future<RpcResult<Void>> inProgressError() {
       RpcResult<Void> result = Rpcs.<Void> getRpcResult(false, null, Collections.<RpcError> emptySet());
       return Futures.immediateFuture(result);
    }

    public void setDataProvider(DataBroker salDataProvider) {
       this.dataProvider = salDataProvider;
    }

    public DataBroker getDataProvider() {
        return this.dataProvider;
    }

    public static OpenFlowSfcRenderer getOpendaylightSfcObj () {
        return OpenFlowSfcRenderer.opendaylightSfcObj;
    }

    /**
    * Implemented from the AutoCloseable interface.
    */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!
        executor.shutdown();

        if (dataProvider != null) {
            final AsyncReadWriteTransaction t = dataProvider.newReadWriteTransaction();
            t.delete(LogicalDatastoreType.CONFIGURATION, ofsfEntryIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, ofsfEntryIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, ofsfEntryIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, ofsfcEntryIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, ofsfsIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, ofsnIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, ofsffIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, ofsfpIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, ofsftIID);
            t.commit().get();
        }
    }
}