/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

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

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


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
public class SfcProviderSfcDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfcDataListener.class);

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChains) {
                ServiceFunctionChains updatedServiceFunctionChains = (ServiceFunctionChains) entry.getValue();
                List<ServiceFunctionChain>  serviceFunctionChainList =
                        updatedServiceFunctionChains.getServiceFunctionChain();
                for (ServiceFunctionChain serviceFunctionChain : serviceFunctionChainList) {
                    LOG.debug("\n########## Updated ServiceFunctionChain: " +
                            "{}", serviceFunctionChain.getName());
                    List<SfcServiceFunction>  sfcServiceFunctionList =
                            serviceFunctionChain.getSfcServiceFunction();
                    for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
                        LOG.debug("\n########## Updated ServiceFunction::" +
                                " {}", sfcServiceFunction.getName());
                    }
                }
            }
        }

        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChains) {
                ServiceFunctionChains originalServiceFunctionChains = (ServiceFunctionChains) entry.getValue();
                List<ServiceFunctionChain>  serviceFunctionChainList =
                        originalServiceFunctionChains.getServiceFunctionChain();
                for (ServiceFunctionChain serviceFunctionChain : serviceFunctionChainList) {
                    LOG.debug("\n########## Original ServiceFunctionChain: " +
                            "{}", serviceFunctionChain.getName());
                    List<SfcServiceFunction>  sfcServiceFunctionList =
                            serviceFunctionChain.getSfcServiceFunction();
                    for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
                        LOG.debug("\n########## Original ServiceFunction: " +
                                "{}", sfcServiceFunction.getName());
                    }
                }
            }
        }

        Map<InstanceIdentifier<?>, DataObject> dataCreatedConfigurationObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChains) {
                ServiceFunctionChains createdServiceFunctionChains = (ServiceFunctionChains) entry.getValue();
                List<ServiceFunctionChain>  serviceFunctionChainList =
                        createdServiceFunctionChains.getServiceFunctionChain();
                for (ServiceFunctionChain serviceFunctionChain : serviceFunctionChainList) {
                    LOG.debug("\n########## Created ServiceFunctionChain: " +
                            "{}", serviceFunctionChain.getName());
                    List<SfcServiceFunction>  sfcServiceFunctionList =
                            serviceFunctionChain.getSfcServiceFunction();
                    for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
                        LOG.debug("\n########## Created ServiceFunction: " +
                                " {}", sfcServiceFunction.getName());
                    }
                }
            }
        }
        printTraceStop(LOG);
    }
}
