/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_provider.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.sfc.provider.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodeService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is called from the MD-SAL infra in order to bootstrap
 * the SFC Provider plug-in
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov ()
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


        DataProviderService dataBrokerService = getDataBrokerDependency();
        opendaylightSfc.setDataProvider(dataBrokerService);

        final SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();

        SfcProviderSfpDataListener sfcProviderSfpDataListener = new SfcProviderSfpDataListener();
        SfcProviderSnDataListener sfcProviderSnDataListener = new SfcProviderSnDataListener();
        final ListenerRegistration<DataChangeListener> snDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.snIID, sfcProviderSnDataListener );
        final ListenerRegistration<DataChangeListener> sfpDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfpIID, sfcProviderSfpDataListener );


        //ServiceFunctionForwarder
        SfcProviderSffDataListener sfcProviderSffDataListener = new SfcProviderSffDataListener();
        final ListenerRegistration<DataChangeListener> sffDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.sffIID, sfcProviderSffDataListener );


        // ServiceFunction Entry
        SfcProviderSfEntryDataListener sfcProviderSfEntryDataListener = new SfcProviderSfEntryDataListener();
        final ListenerRegistration<DataChangeListener> sfEntryDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfEntryIID, sfcProviderSfEntryDataListener);

        // ServiceFunctionChainEntry Entry
        SfcProviderSfcEntryDataListener sfcProviderSfcEntryDataListener = new SfcProviderSfcEntryDataListener();
         final ListenerRegistration<DataChangeListener> sfcEntryDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfcEntryIID, sfcProviderSfcEntryDataListener  );


        // ServiceFunctionPathEntry Entry
        SfcProviderSfpEntryDataListener sfcProviderSfpEntryDataListener = new SfcProviderSfpEntryDataListener();
        final ListenerRegistration<DataChangeListener> sfpEntryDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfpEntryIID, sfcProviderSfpEntryDataListener  );

        // ServiceFunctions
        //SfcProviderSfsDataListener sfcProviderSfsDataListener = new SfcProviderSfsDataListener();
        //final ListenerRegistration<DataChangeListener> sfsDataChangeListenerRegistration =
        //        dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfsIID, sfcProviderSfsDataListener);

        // ServiceFunctionChains
        //SfcProviderSfcDataListener sfcProviderSfcDataListener = new SfcProviderSfcDataListener();
        //final ListenerRegistration<DataChangeListener> sfcDataChangeListenerRegistration =
        //        dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfcIID, sfcProviderSfcDataListener  );


        final BindingAwareBroker.RpcRegistration<ServiceFunctionService> sfRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServiceFunctionService.class,
                                sfcProviderRpc);

        final BindingAwareBroker.RpcRegistration<ServiceFunctionChainService> sfcRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServiceFunctionChainService.class,
                                sfcProviderRpc);

        final BindingAwareBroker.RpcRegistration<ServiceNodeService> snRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServiceNodeService.class,
                                sfcProviderRpc);

        // close()
        final class AutoCloseableSfc implements AutoCloseable {

            @Override
            public void close() throws Exception {
                sfEntryDataChangeListenerRegistration.close();
                //sfsDataChangeListenerRegistration.close();
                //sfcEntryDataChangeListenerRegistration.close();
                sfpEntryDataChangeListenerRegistration.close();
                snDataChangeListenerRegistration.close();
                sfpDataChangeListenerRegistration.close();
                //sfcDataChangeListenerRegistration.close();
                sffDataChangeListenerRegistration.close();
                sfRpcRegistration.close();
                sfcRpcRegistration.close();
                snRpcRegistration.close();

                //runtimeReg.close();
                opendaylightSfc.close();
                LOG.info("SFC provider (instance {}) torn down.", this);
            }
        }

        AutoCloseable ret = new AutoCloseableSfc();
        LOG.info("SFC provider (instance {}) initialized.", ret);
        return ret;
    }


}
