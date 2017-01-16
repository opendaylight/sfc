/*
 * Copyright (c) 2017 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.validators.util.SfcDatastoreCache;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service
 * Functions chains taking the appropriate actions.
 */
public class ServiceFunctionChainListener extends AbstractDataTreeChangeListener<ServiceFunctionChain> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionChainListener.class);

    private final DataBroker dataBroker;

    private ListenerRegistration<ServiceFunctionChainListener> listenerRegistration;

    public ServiceFunctionChainListener(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    private void registerListeners() {
        final DataTreeIdentifier<ServiceFunctionChain> treeId = new DataTreeIdentifier<ServiceFunctionChain>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(ServiceFunctionChains.class).child(ServiceFunctionChain.class));
        listenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);

    }

    public void init() {
        LOG.debug("ServiceFunctionChainListener:Initializing...");
        registerListeners();
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

    @Override
    public void add(ServiceFunctionChain serviceFunctionChain) {
        if (serviceFunctionChain != null) {
            LOG.debug("add:starting..(new sfc name: {})", serviceFunctionChain.getName());
            List<String> serviceFunctionTypesForChain = new ArrayList<String>();
            for (SfcServiceFunction sfcSf: serviceFunctionChain.getSfcServiceFunction()) {
                LOG.debug("add:new sfc sf found; name={}, type={})", sfcSf.getName(), sfcSf.getType().getValue());
                serviceFunctionTypesForChain.add(sfcSf.getType().getValue());
            }
            SfcDatastoreCache.sfChainToSfTypeList.put(serviceFunctionChain.getName(), serviceFunctionTypesForChain);
        }
    }

    @Override
    public void remove(ServiceFunctionChain serviceFunctionChain) {
        if (serviceFunctionChain != null) {
            LOG.debug("remove: Deleting Service Function chain: {}", serviceFunctionChain.getName());
            SfcDatastoreCache.sfChainToSfTypeList.invalidate(serviceFunctionChain.getName());
        }
    }

    @Override
    protected void update(ServiceFunctionChain originalServiceFunctionChain, ServiceFunctionChain updatedServiceFunctionChain) {
        if (originalServiceFunctionChain != null && updatedServiceFunctionChain != null) {
            LOG.debug("update:Updating Service Function chain: {}", originalServiceFunctionChain.getName());
            SfcDatastoreCache.sfChainToSfTypeList.invalidate(originalServiceFunctionChain.getName());
            add(updatedServiceFunctionChain);
        }
    }
}
