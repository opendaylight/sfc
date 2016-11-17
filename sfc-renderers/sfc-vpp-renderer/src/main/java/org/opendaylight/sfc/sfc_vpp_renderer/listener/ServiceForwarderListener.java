/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_vpp_renderer.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
import org.opendaylight.sfc.sfc_vpp_renderer.renderer.VppSffManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceForwarderListener extends AbstractDataTreeChangeListener<ServiceFunctionForwarder> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceForwarderListener.class);
    private final ListenerRegistration<ServiceForwarderListener> vppSffListenerRegistration;
    private final VppSffManager sffManager;

    public ServiceForwarderListener(DataBroker dataBroker, VppSffManager sffManager) {
        this.sffManager = sffManager;
        // Register listener
        final DataTreeIdentifier<ServiceFunctionForwarder> treeId = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class));
        vppSffListenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (vppSffListenerRegistration != null) {
            vppSffListenerRegistration.close();
        }
    }

    @Override
    protected void add(ServiceFunctionForwarder newDataObject) {
        LOG.info("SFF added [{}]", newDataObject.getName());
        sffManager.disposeSff(newDataObject, false);
    }

    @Override
    protected void remove(ServiceFunctionForwarder removedDataObject) {
        LOG.info("SFF removed [{}]", removedDataObject.getName());
        sffManager.disposeSff(removedDataObject, true);
    }

    @Override
    protected void update(ServiceFunctionForwarder originalDataObject, ServiceFunctionForwarder updatedDataObject) {
        LOG.info("SFF updated original [{}] updated [{}]", originalDataObject.getName(), updatedDataObject.getName());
        sffManager.disposeSff(updatedDataObject, false);
    }

}
