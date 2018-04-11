/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.tools.mdsal.listener.AbstractAsyncDataTreeChangeListener;
import org.opendaylight.sfc.genius.impl.SfcGeniusServiceManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for data store changes on service function state data tree.
 * sfc-genius needs to be aware of service functions participation on
 * RSPs.
 */
public class SfcGeniusSfStateListener extends AbstractAsyncDataTreeChangeListener<ServiceFunctionState> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusSfStateListener.class);
    private final SfcGeniusServiceManager interfaceManager;

    public SfcGeniusSfStateListener(DataBroker dataBroker,
                                    SfcGeniusServiceManager interfaceManager,
                                    ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, getWildCardPath(), executorService);
        this.interfaceManager = interfaceManager;
    }

    private static InstanceIdentifier<ServiceFunctionState> getWildCardPath() {
        return InstanceIdentifier.create(ServiceFunctionsState.class).child(ServiceFunctionState.class);
    }

    @Override
    public void add(@Nonnull ServiceFunctionState newServiceFunctionState) {
        // If this SF starts participating in RSPs, we need to bind to its interfaces
        LOG.debug("Received service function state add event {}", newServiceFunctionState);
        int numberOfNewPaths = newServiceFunctionState.getSfServicePath() != null
                ? newServiceFunctionState.getSfServicePath().size()
                : 0;
        String sfName = newServiceFunctionState.getName().getValue();
        if (numberOfNewPaths > 0) {
            interfaceManager.bindInterfacesOfServiceFunction(sfName);
        }
    }

    @Override
    public void remove(@Nonnull ServiceFunctionState removedServiceFunctionState) {
        // If this SF stops participating in RSPs, we need to unbind from its interfaces
        LOG.debug("Received service function state remove event {}", removedServiceFunctionState);
        int numberOfOldPaths = removedServiceFunctionState.getSfServicePath() != null
                ? removedServiceFunctionState.getSfServicePath().size()
                : 0;
        if (numberOfOldPaths > 0) {
            String sfName = removedServiceFunctionState.getName().getValue();
            interfaceManager.unbindInterfacesOfServiceFunction(sfName);
        }
    }

    @Override
    public void update(@Nonnull ServiceFunctionState originalServiceFunctionState,
                       ServiceFunctionState updatedServiceFunctionState) {
        // If this SF stops participating in RSPs, we need to unbind from its interfaces
        LOG.debug("Received service function state update event {} {}",
                originalServiceFunctionState,
                updatedServiceFunctionState);
        int numberOfNewPaths = updatedServiceFunctionState.getSfServicePath() != null
                ? updatedServiceFunctionState.getSfServicePath().size()
                : 0;
        int numberOfOldPaths = originalServiceFunctionState.getSfServicePath() != null
                ? originalServiceFunctionState.getSfServicePath().size()
                : 0;
        String sfName = updatedServiceFunctionState.getName().getValue();
        if (numberOfNewPaths > numberOfOldPaths) {
            interfaceManager.bindInterfacesOfServiceFunction(sfName);
        } else if (numberOfNewPaths < numberOfOldPaths && numberOfNewPaths <= 0) {
            interfaceManager.unbindInterfacesOfServiceFunction(sfName);
        }
    }
}
