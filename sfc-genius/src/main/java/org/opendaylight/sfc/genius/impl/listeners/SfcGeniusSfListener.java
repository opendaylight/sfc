/*
 * Copyright (c) 2017 SUSE LINUX GmbH.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractAsyncDataTreeChangeListener;
import org.opendaylight.sfc.genius.impl.SfcGeniusServiceManager;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcGeniusSfListener extends AbstractAsyncDataTreeChangeListener<ServiceFunction> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusSfListener.class);
    private final SfcGeniusServiceManager interfaceManager;

    public SfcGeniusSfListener(DataBroker dataBroker,
                               SfcGeniusServiceManager interfaceManager,
                               ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, getWildcardPath(), executorService);
        this.interfaceManager = interfaceManager;
    }

    private static InstanceIdentifier<ServiceFunction> getWildcardPath() {
        return InstanceIdentifier.create(ServiceFunctions.class).child(ServiceFunction.class);
    }

    @Override
    public void add(@Nonnull ServiceFunction newServiceFunction) {
        // noop
    }

    @Override
    public void remove(@Nonnull ServiceFunction removedServiceFunction) {
        LOG.debug("Received service function remove event {}", removedServiceFunction);
        String interfaceName = SfcGeniusDataUtils.getSfLogicalInterface(removedServiceFunction);
        interfaceManager.unbindInterfaces(Collections.singletonList(interfaceName));
    }

    @Override
    public void update(@Nonnull ServiceFunction originalServiceFunction, ServiceFunction updatedServiceFunction) {
        // noop
    }
}
