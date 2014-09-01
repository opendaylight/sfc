/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ofsfc.provider;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;


/**
 * This class gets called whenever there is a change to
 * the Service Functions data store. Today it does not
 * do anything, it is just a big debug tool.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class OfSfcProviderSfcDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(OfSfcProviderSfcDataListener.class);

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        LOG.info("\n########## getUpdatedConfigurationData");

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChains) {
                ServiceFunctionChains updatedServiceFunctionChains = (ServiceFunctionChains) entry.getValue();
                List<ServiceFunctionChain>  serviceFunctionChainList = updatedServiceFunctionChains.getServiceFunctionChain();
                for (ServiceFunctionChain serviceFunctionChain : serviceFunctionChainList) {
                    LOG.info("\n########## Updated ServiceFunctionChain name: {}", serviceFunctionChain.getName());
                    List<SfcServiceFunction>  SfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
                    for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
                        LOG.info("\n########## Updated ServiceFunction name: {}", sfcServiceFunction.getName());
                    }
                }
            }
        }

        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject = change.getOriginalData();
        LOG.info("\n########## getOriginalConfigurationData");

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChains) {
                ServiceFunctionChains originalServiceFunctionChains = (ServiceFunctionChains) entry.getValue();
                List<ServiceFunctionChain>  serviceFunctionChainList = originalServiceFunctionChains.getServiceFunctionChain();
                for (ServiceFunctionChain serviceFunctionChain : serviceFunctionChainList) {
                    LOG.info("\n########## Original ServiceFunctionChain name: {}", serviceFunctionChain.getName());
                    List<SfcServiceFunction>  SfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
                    for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
                        LOG.info("\n########## Original ServiceFunction name: {}", sfcServiceFunction.getName());
                    }
                }
            }
        }

        Map<InstanceIdentifier<?>, DataObject> dataCreatedConfigurationObject = change.getCreatedData();
        LOG.info("\n########## getCreatedConfigurationData");

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChains) {
                ServiceFunctionChains createdServiceFunctionChains = (ServiceFunctionChains) entry.getValue();
                List<ServiceFunctionChain>  serviceFunctionChainList = createdServiceFunctionChains.getServiceFunctionChain();
                for (ServiceFunctionChain serviceFunctionChain : serviceFunctionChainList) {
                    LOG.info("\n########## Created ServiceFunctionChain name: {}", serviceFunctionChain.getName());
                    List<SfcServiceFunction>  SfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
                    for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
                        LOG.info("\n########## Created ServiceFunction name: {}", sfcServiceFunction.getName());
                    }
                }
            }
        }

        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }
}
