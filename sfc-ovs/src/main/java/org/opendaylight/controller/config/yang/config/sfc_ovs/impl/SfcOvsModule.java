/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the OVSDB southbound operational datastore
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2015-02-13
 */

package org.opendaylight.controller.config.yang.config.sfc_ovs.impl;

import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsRpc;
import org.opendaylight.sfc.sfc_ovs.provider.listener.SfcOvsNodeDataListener;
import org.opendaylight.sfc.sfc_ovs.provider.listener.SfcOvsSffEntryDataListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarderOvsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcOvsModule extends org.opendaylight.controller.config.yang.config.sfc_ovs.impl.AbstractSfcOvsModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsModule.class);

    public SfcOvsModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcOvsModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_ovs.impl.SfcOvsModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final OpendaylightSfc opendaylightSfc = OpendaylightSfc.getOpendaylightSfcObj();

        final SfcOvsRpc sfcOvsRpc = new SfcOvsRpc();
        final BindingAwareBroker.RpcRegistration<ServiceFunctionForwarderOvsService> sfcOvsRpcRegistration =
                getRpcRegistryDependency().addRpcImplementation(ServiceFunctionForwarderOvsService.class, sfcOvsRpc);

        final SfcOvsNodeDataListener sfcOvsNodeDataListener = new SfcOvsNodeDataListener(opendaylightSfc);
        final SfcOvsSffEntryDataListener sfcOvsSffEntryDataListener = new SfcOvsSffEntryDataListener(opendaylightSfc);

        LOG.info("SFC OVS module initialized");

        // close()
        final class AutoCloseableSfcOvs implements AutoCloseable {

            @Override
            public void close() {
                sfcOvsRpcRegistration.close();
                sfcOvsNodeDataListener.getDataChangeListenerRegistration().close();
                sfcOvsSffEntryDataListener.getDataChangeListenerRegistration().close();

                try {
                    opendaylightSfc.close();
                } catch (ExecutionException | InterruptedException e) {
                    LOG.error("\nFailed to close SfcOvsModule instance {} cleanly", this);
                }
                LOG.info("SfcOvsModule (instance {}) torn down", this);
            }
        }

        return new AutoCloseableSfcOvs();
    }

}
