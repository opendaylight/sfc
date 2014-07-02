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
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcProviderRpc;
import org.opendaylight.sfc.provider.SfcProviderSfDataListener;
import org.opendaylight.sfc.provider.SfcProviderSfcDataListener;
import org.opendaylight.sfc.provider.SfcProviderSfpDataListener;
import org.opendaylight.sfc.provider.SfcProviderSnDataListener;
import org.opendaylight.sfc.provider.SfcProviderSffDataListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140630.ServiceFunctionChainService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140629.*;

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

        SfcProviderSfDataListener sfcProviderSfDataListener = new SfcProviderSfDataListener();
        SfcProviderSfcDataListener sfcProviderSfcDataListener = new SfcProviderSfcDataListener();
        SfcProviderSfpDataListener sfcProviderSfpDataListener = new SfcProviderSfpDataListener();
        SfcProviderSnDataListener sfcProviderSnDataListener = new SfcProviderSnDataListener();
        SfcProviderSffDataListener sfcProviderSffDataListener = new SfcProviderSffDataListener();

        // We listen for changes in all datastore trees with specific objects for each one
        final ListenerRegistration<DataChangeListener> sfDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfIID, sfcProviderSfDataListener);
        final ListenerRegistration<DataChangeListener> snDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.snIID, sfcProviderSnDataListener );
        final ListenerRegistration<DataChangeListener> sfpDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfpIID, sfcProviderSfpDataListener );
        final ListenerRegistration<DataChangeListener> sfcDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfcIID, sfcProviderSfcDataListener  );
        final ListenerRegistration<DataChangeListener> sffDataChangeListenerRegistration =
                dataBrokerService.registerDataChangeListener( OpendaylightSfc.sffIID, sfcProviderSffDataListener );

        final BindingAwareBroker.RpcRegistration<ServiceFunctionService> sfRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServiceFunctionService.class,
                                sfcProviderRpc);
        final BindingAwareBroker.RpcRegistration<ServiceFunctionChainService> sfcRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServiceFunctionChainService.class,
                                sfcProviderRpc);

        // close()
        final class AutoCloseableSfc implements AutoCloseable {

            @Override
            public void close() throws Exception {
                sfDataChangeListenerRegistration.close();
                snDataChangeListenerRegistration.close();
                sfpDataChangeListenerRegistration.close();
                sfcDataChangeListenerRegistration.close();
                sffDataChangeListenerRegistration.close();
                sfRpcRegistration.close();
                sfcRpcRegistration.close();

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
