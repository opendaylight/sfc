/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

public class OpendaylightSfc implements AutoCloseable {


    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightSfc.class);

    public static final InstanceIdentifier<ServiceFunctionChain>  sfcEntryIID =
            InstanceIdentifier.builder(ServiceFunctionChains.class)
                    .child(ServiceFunctionChain.class).build();

    public static final InstanceIdentifier<ServiceFunctionClassifier> scfEntryIID =
            InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
                    .child(ServiceFunctionClassifier.class).build();

    public static final InstanceIdentifier<ServiceFunction>  sfEntryIID =
           InstanceIdentifier.builder(ServiceFunctions.class).child(ServiceFunction.class).build();

    public static final InstanceIdentifier<ServiceFunctionPath>  sfpEntryIID =
            InstanceIdentifier.builder(ServiceFunctionPaths.class)
                    .child(ServiceFunctionPath.class).build();

    public static final InstanceIdentifier<ServiceFunctions>  sfsIID =
            InstanceIdentifier.builder(ServiceFunctions.class).build();
    public static final InstanceIdentifier<ServiceNodes>  snIID =
           InstanceIdentifier.builder(ServiceNodes.class).build();
    public static final InstanceIdentifier<ServiceFunctionPaths>  sfpIID =
           InstanceIdentifier.builder(ServiceFunctionPaths.class).build();
    public static final InstanceIdentifier<ServiceFunctionChains>  sfcIID =
           InstanceIdentifier.builder(ServiceFunctionChains.class).build();

    public static final InstanceIdentifier<ServiceFunctionForwarders>  sffIID =
           InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();
    public static final InstanceIdentifier<ServiceFunctionTypes>  sftIID =
           InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

    public final ExecutorService executor;
    protected DataBroker dataProvider;
    private static OpendaylightSfc opendaylightSfcObj;
    private final Lock lock = new ReentrantLock();

    public OpendaylightSfc() {

       executor = Executors.newFixedThreadPool(20);
       opendaylightSfcObj = this;
    }

    public void getLock() {
        while (!lock.tryLock()) {}
        return;
    }

    public void releaseLock() {
        lock.unlock();
        return;
    }

    public void setDataProvider(DataBroker salDataProvider) {
       this.dataProvider = salDataProvider;
    }

    public DataBroker getDataProvider() {
        return this.dataProvider;
    }

    public static OpendaylightSfc getOpendaylightSfcObj () {
        return OpendaylightSfc.opendaylightSfcObj;
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
            t.delete(LogicalDatastoreType.CONFIGURATION, sfEntryIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, sfEntryIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, sfEntryIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, sfcEntryIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, sfsIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, snIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, sffIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, sfpIID);
            t.delete(LogicalDatastoreType.CONFIGURATION, sftIID);
            t.commit().get();
        }
    }
}
