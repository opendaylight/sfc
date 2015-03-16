/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
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

    public static final InstanceIdentifier<ServiceFunctionChain>  SFC_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionChains.class)
                    .child(ServiceFunctionChain.class).build();

    public static final InstanceIdentifier<ServiceFunctionClassifier> SCF_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
                    .child(ServiceFunctionClassifier.class).build();

    public static final InstanceIdentifier<ServiceFunction>  SF_ENTRY_IID =
           InstanceIdentifier.builder(ServiceFunctions.class).child(ServiceFunction.class).build();

    public static final InstanceIdentifier<ServiceFunctionForwarder>  SFF_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class).build();

    public static final InstanceIdentifier<ServiceFunctionPath>  SFP_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionPaths.class)
                    .child(ServiceFunctionPath.class).build();

    public static final InstanceIdentifier<RenderedServicePath>  RSP_ENTRY_IID =
            InstanceIdentifier.builder(RenderedServicePaths.class)
                    .child(RenderedServicePath.class).build();

    public static final InstanceIdentifier<ServiceFunctionChains>  SFC_IID =
           InstanceIdentifier.builder(ServiceFunctionChains.class).build();

    public static final InstanceIdentifier<ServiceFunctionTypes>  SFT_IID =
           InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

    public static final InstanceIdentifier<AccessList>  ACL_ENTRY_IID =
            InstanceIdentifier.builder(AccessLists.class)
                    .child(AccessList.class).build();

    public static final InstanceIdentifier<ServiceFunctionForwarders>  SFF_IID =
            InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();

    public static final int EXECUTOR_THREAD_POOL_SIZE = 100;

    private final ExecutorService executor;
    protected DataBroker dataProvider;
    protected BindingAwareBroker broker;
    private static OpendaylightSfc opendaylightSfcObj;
    private final Lock lock = new ReentrantLock();

    /* Constructors */
    public OpendaylightSfc() {

        executor = Executors.newFixedThreadPool(EXECUTOR_THREAD_POOL_SIZE);
        opendaylightSfcObj = this;
        LOG.info("Opendaylight Service Function Chaining Initialized");
    }

    public void getLock() {
        while (!lock.tryLock()) {}
        return;
    }

    public void releaseLock() {
        lock.unlock();
        return;
    }

    /* Accessors */

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setDataProvider(DataBroker salDataProvider) {
       this.dataProvider = salDataProvider;
    }

    public DataBroker getDataProvider() {
        return this.dataProvider;
    }

    public void setBroker(BindingAwareBroker broker) {
        this.broker = broker;
    }

    public BindingAwareBroker getBroker() {
        return this.broker;
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
            final InstanceIdentifier<ServiceFunctionChains>  SFC_IID =
                    InstanceIdentifier.builder(ServiceFunctionChains.class).build();

            final InstanceIdentifier<ServiceFunctionClassifiers> SCF_IID =
                    InstanceIdentifier.builder(ServiceFunctionClassifiers.class).build();

            final InstanceIdentifier<ServiceFunctions>  SF_IID =
                    InstanceIdentifier.builder(ServiceFunctions.class).build();

            final InstanceIdentifier<ServiceFunctionForwarders>  SFF_IID =
                    InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();

            final InstanceIdentifier<ServiceFunctionPaths>  SFP_IID =
                    InstanceIdentifier.builder(ServiceFunctionPaths.class).build();

            final InstanceIdentifier<RenderedServicePaths>  RSP_IID =
                    InstanceIdentifier.builder(RenderedServicePaths.class).build();

            final InstanceIdentifier<AccessLists>  ACL_IID =
                    InstanceIdentifier.builder(AccessLists.class).build();

            SfcDataStoreAPI.deleteTransactionAPI(SFC_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SCF_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SF_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SFF_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SFP_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(RSP_IID, LogicalDatastoreType.OPERATIONAL);
            SfcDataStoreAPI.deleteTransactionAPI(ACL_IID, LogicalDatastoreType.CONFIGURATION);

        }
    }
}
