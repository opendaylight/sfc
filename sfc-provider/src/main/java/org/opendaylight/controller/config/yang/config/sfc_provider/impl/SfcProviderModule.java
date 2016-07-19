/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_provider.impl;

import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcProviderRpc;
import org.opendaylight.sfc.provider.SfcProviderScfEntryDataListener;
import org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener;
import org.opendaylight.sfc.provider.SfcProviderSffEntryDataListener;
import org.opendaylight.sfc.provider.SfcProviderSfstEntryDataListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.ServicePathIdService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePathService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is called from the MD-SAL infra in order to bootstrap
 * the SFC Provider plug-in
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since       2014-06-30
 * @see org.opendaylight.controller.config.yang.config.sfc_provider.impl.SfcProviderModule
 */
public class SfcProviderModule extends org.opendaylight.controller.config.yang.config.sfc_provider.impl.AbstractSfcProviderModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderModule.class);

    public SfcProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_provider.impl.SfcProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    /*
     *  This is the entry point for SFc Provider
     */
    @Override
    public java.lang.AutoCloseable createInstance() {

        final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();


        DataBroker dataBrokerService = getDataBrokerDependency();
        opendaylightSfc.setDataProvider(dataBrokerService);
        BindingAwareBroker broker = getBindingRegistryDependency();
        opendaylightSfc.setBroker(broker);

        final SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();

        //ServiceFunctionForwarder Entry
        SfcProviderSffEntryDataListener sfcProviderSffEntryDataListener = new SfcProviderSffEntryDataListener();
        final ListenerRegistration<DataChangeListener> sffDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION,
                       OpendaylightSfc.SFF_ENTRY_IID, sfcProviderSffEntryDataListener,  DataBroker.DataChangeScope.SUBTREE );


        // ServiceFunction Entry
        SfcProviderSfEntryDataListener sfcProviderSfEntryDataListener = new SfcProviderSfEntryDataListener();
        final ListenerRegistration<DataChangeListener> sfEntryDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION,
                        OpendaylightSfc.SF_ENTRY_IID, sfcProviderSfEntryDataListener, DataBroker.DataChangeScope.SUBTREE);

        //ServiceClassifierEntry
        SfcProviderScfEntryDataListener sfcProviderScfEntryDataListener = new SfcProviderScfEntryDataListener();
        final ListenerRegistration<DataChangeListener> scfEntryDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION,
                        OpendaylightSfc.SCF_ENTRY_IID, sfcProviderScfEntryDataListener, DataBroker.DataChangeScope.SUBTREE);

        //SF Schedule type Entry
        SfcProviderSfstEntryDataListener sfcProviderSfstEntryDataListener = new SfcProviderSfstEntryDataListener();
        final ListenerRegistration<DataChangeListener> sfstEntryDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION,
                        OpendaylightSfc.SFST_ENTRY_IID, sfcProviderSfstEntryDataListener, DataBroker.DataChangeScope.SUBTREE  );

        final BindingAwareBroker.RpcRegistration<ServiceFunctionService> sfRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServiceFunctionService.class,
                                sfcProviderRpc);

        final BindingAwareBroker.RpcRegistration<ServiceFunctionChainService> sfcRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServiceFunctionChainService.class,
                                sfcProviderRpc);

        final BindingAwareBroker.RpcRegistration<RenderedServicePathService> rspRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(RenderedServicePathService.class,
                                sfcProviderRpc);

        final BindingAwareBroker.RpcRegistration<ServicePathIdService> srvPathIdRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServicePathIdService.class,
                                sfcProviderRpc);

        // close()
        final class AutoCloseableSfc implements AutoCloseable {

            @Override
            public void close() {
                sfEntryDataChangeListenerRegistration.close();
                scfEntryDataChangeListenerRegistration.close();
                sffDataChangeListenerRegistration.close();
                sfstEntryDataChangeListenerRegistration.close();
                sfRpcRegistration.close();
                sfcRpcRegistration.close();
                rspRpcRegistration.close();
                srvPathIdRpcRegistration.close();

                try
                {
                    opendaylightSfc.close();
                } catch (ExecutionException | InterruptedException e)
                {
                    LOG.error("\nFailed to close SFC Provider (instance {}) " +
                            "cleanly", this);
                }


                LOG.info("SFC provider (instance {}) torn down", this);
            }
        }

        AutoCloseable ret = new AutoCloseableSfc();
        LOG.info("SFC provider (instance {}) initialized.", ret);
        return ret;
    }


}
