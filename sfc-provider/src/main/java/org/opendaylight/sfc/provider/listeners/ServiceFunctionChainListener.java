/*
 * Copyright (c) 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.validators.util.SfcDatastoreCache;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service
 * Functions chains taking the appropriate actions.
 */
@Singleton
public class ServiceFunctionChainListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionChain> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionChainListener.class);

    @Inject
    public ServiceFunctionChainListener(final DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ServiceFunctionChains.class).child(ServiceFunctionChain.class));
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<ServiceFunctionChain> instanceIdentifier,
                    @Nonnull ServiceFunctionChain serviceFunctionChain) {
        LOG.debug("add:starting..(new sfc name: {})", serviceFunctionChain.getName());
        List<String> serviceFunctionTypesForChain = new ArrayList<>();
        for (SfcServiceFunction sfcSf : serviceFunctionChain.getSfcServiceFunction()) {
            LOG.debug("add:new sfc sf found; name={}, type={})", sfcSf.getName(), sfcSf.getType().getValue());
            serviceFunctionTypesForChain.add(sfcSf.getType().getValue());
        }
        SfcDatastoreCache.getSfChainToSfTypeList().put(serviceFunctionChain.getName(), serviceFunctionTypesForChain);
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<ServiceFunctionChain> instanceIdentifier,
                       @Nonnull ServiceFunctionChain serviceFunctionChain) {
        LOG.debug("remove: Deleting Service Function chain: {}", serviceFunctionChain.getName());
        SfcDatastoreCache.getSfChainToSfTypeList().invalidate(serviceFunctionChain.getName());
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<ServiceFunctionChain> instanceIdentifier,
                       @Nonnull ServiceFunctionChain originalServiceFunctionChain,
                       @Nonnull ServiceFunctionChain updatedServiceFunctionChain) {
        LOG.debug("update:Updating Service Function chain: {}", originalServiceFunctionChain.getName());
        SfcDatastoreCache.getSfChainToSfTypeList().invalidate(originalServiceFunctionChain.getName());
        add(updatedServiceFunctionChain);
    }
}
