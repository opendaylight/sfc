/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.vpp.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.renderers.vpp.VppSffManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ServiceForwarderListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionForwarder> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceForwarderListener.class);

    private final VppSffManager sffManager;

    @Inject
    public ServiceForwarderListener(DataBroker dataBroker, VppSffManager sffManager) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class));
        this.sffManager = sffManager;
    }

    @Override
    public void add(@Nonnull ServiceFunctionForwarder serviceFunctionForwarder) {
        LOG.debug("SFF added [{}]", serviceFunctionForwarder.getName());
        sffManager.disposeSff(serviceFunctionForwarder, false);
    }

    @Override
    public void remove(@Nonnull ServiceFunctionForwarder serviceFunctionForwarder) {
        LOG.debug("SFF removed [{}]", serviceFunctionForwarder.getName());
        sffManager.disposeSff(serviceFunctionForwarder, true);
    }

    @Override
    public void update(@Nonnull ServiceFunctionForwarder originalServiceFunctionForwarder,
                       @Nonnull ServiceFunctionForwarder updatedServiceFunctionForwarder) {
        LOG.info("SFF updated original [{}] updated [{}]", originalServiceFunctionForwarder.getName(),
                 updatedServiceFunctionForwarder.getName());
        sffManager.disposeSff(updatedServiceFunctionForwarder, false);
    }
}
