/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.ServicePathIdService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePathService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProvider.class);
    private final DataBroker dataBrokerService;
    private final BindingAwareBroker broker;
    private final RpcProviderRegistry rpcProviderRegistry;

    private OpendaylightSfc opendaylightSfc = null;
    private SfcProviderSfEntryDataListener sfcProviderSfEntryDataListener = null;
    private SfcProviderScfEntryDataListener sfcProviderScfEntryDataListener = null;
    private SfcProviderSfstEntryDataListener sfcProviderSfstEntryDataListener = null;
    private ListenerRegistration<DataChangeListener> sffDataChangeListenerRegistration = null;
    private ListenerRegistration<DataChangeListener> sfEntryDataChangeListenerRegistration = null;
    private ListenerRegistration<DataChangeListener> scfEntryDataChangeListenerRegistration = null;
    private ListenerRegistration<DataChangeListener> sfstEntryDataChangeListenerRegistration = null;
    private BindingAwareBroker.RpcRegistration<ServiceFunctionService> sfRpcRegistration = null;
    private BindingAwareBroker.RpcRegistration<ServiceFunctionChainService> sfcRpcRegistration = null;
    private BindingAwareBroker.RpcRegistration<RenderedServicePathService> rspRpcRegistration = null;
    private BindingAwareBroker.RpcRegistration<ServicePathIdService> srvPathIdRpcRegistration = null;

    public SfcProvider(final DataBroker dataBrokerService,
            final BindingAwareBroker broker,
            final RpcProviderRegistry rpcProviderRegistry) {
        this.dataBrokerService = dataBrokerService;
        this.broker = broker;
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    public void init() {
        opendaylightSfc = new OpendaylightSfc();
        opendaylightSfc.setDataProvider(dataBrokerService);
        opendaylightSfc.setBroker(broker);

        final SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();

        //ServiceFunctionForwarder Entry
        SfcProviderSffEntryDataListener sfcProviderSffEntryDataListener = new SfcProviderSffEntryDataListener();
        sffDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION,
                        OpendaylightSfc.SFF_ENTRY_IID, sfcProviderSffEntryDataListener,  DataBroker.DataChangeScope.SUBTREE );

        // ServiceFunction Entry
        sfcProviderSfEntryDataListener = new SfcProviderSfEntryDataListener();
        sfEntryDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION,
                        OpendaylightSfc.SF_ENTRY_IID, sfcProviderSfEntryDataListener, DataBroker.DataChangeScope.SUBTREE);

        //ServiceClassifierEntry
        sfcProviderScfEntryDataListener = new SfcProviderScfEntryDataListener();
        scfEntryDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION,
                        OpendaylightSfc.SCF_ENTRY_IID, sfcProviderScfEntryDataListener, DataBroker.DataChangeScope.SUBTREE);

        //SF Schedule type Entry
        sfcProviderSfstEntryDataListener = new SfcProviderSfstEntryDataListener();
        sfstEntryDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION,
                        OpendaylightSfc.SFST_ENTRY_IID, sfcProviderSfstEntryDataListener, DataBroker.DataChangeScope.SUBTREE);

        sfRpcRegistration = rpcProviderRegistry
                .addRpcImplementation(ServiceFunctionService.class,
                        sfcProviderRpc);

        sfcRpcRegistration = rpcProviderRegistry
                .addRpcImplementation(ServiceFunctionChainService.class,
                        sfcProviderRpc);

        rspRpcRegistration = rpcProviderRegistry
                .addRpcImplementation(RenderedServicePathService.class,
                        sfcProviderRpc);

        srvPathIdRpcRegistration = rpcProviderRegistry
                .addRpcImplementation(ServicePathIdService.class,
                        sfcProviderRpc);

        LOG.info("SFC provider (instance {}) initialized.", this);
    }

    public void close() {
        if (sfEntryDataChangeListenerRegistration != null) {
            sfEntryDataChangeListenerRegistration.close();
        }
        if (scfEntryDataChangeListenerRegistration != null) {
            scfEntryDataChangeListenerRegistration.close();
        }
        if (sffDataChangeListenerRegistration != null) {
            sffDataChangeListenerRegistration.close();
        }
        if (sfstEntryDataChangeListenerRegistration != null) {
            sfstEntryDataChangeListenerRegistration.close();
        }
        if (sfRpcRegistration != null) {
            sfRpcRegistration.close();
        }
        if (sfcRpcRegistration != null) {
            sfcRpcRegistration.close();
        }
        if (rspRpcRegistration != null) {
            rspRpcRegistration.close();
        }
        if (srvPathIdRpcRegistration != null) {
            srvPathIdRpcRegistration.close();
        }

        try {
            opendaylightSfc.close();
        } catch (ExecutionException | InterruptedException e)
        {
            LOG.error("\nFailed to close SFC Provider (instance {}) " +
                    "cleanly", this);
        }
        LOG.info("SFC provider (instance {}) torn down", this);
    }
}
