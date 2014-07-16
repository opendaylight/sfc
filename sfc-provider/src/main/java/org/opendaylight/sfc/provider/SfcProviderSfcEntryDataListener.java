/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class gets called whenever there is a change to
 * a Service Function Chain list entry, i.e.,
 * added/deleted/modified.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderSfcEntryDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfcDataListener.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedConfigurationData();
        LOG.info("\n########## getUpdatedConfigurationData");

        /*
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChain) {
                ServiceFunctionChain updatedServiceFunctionChain = (ServiceFunctionChain) entry.getValue();
                LOG.info("\n########## Updated ServiceFunctionChain name: {}", updatedServiceFunctionChain.getName());
                //odlSfc.executor.execute(SfcProviderExecutorDispatcher.getSfcProviderCreateProvisioningElement(updatedServiceFunctionChain));
                List<SfcServiceFunction>  SfcServiceFunctionList = updatedServiceFunctionChain.getSfcServiceFunction();
                for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
                    LOG.info("\n########## Updated ServiceFunction name: {}", sfcServiceFunction.getName());

                }
            }
        }
        */

        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject = change.getOriginalConfigurationData();
        LOG.info("\n########## getOriginalConfigurationData");

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChain) {
                ServiceFunctionChain originalServiceFunctionChain = (ServiceFunctionChain) entry.getValue();
                LOG.info("\n########## Original ServiceFunctionChain name: {}", originalServiceFunctionChain.getName());
                List<SfcServiceFunction>  SfcServiceFunctionList = originalServiceFunctionChain.getSfcServiceFunction();
                for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
                    LOG.info("\n########## Original ServiceFunction name: {}", sfcServiceFunction.getName());

                }
            }
        }

        // Create Service Function Paths
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedConfigurationData();
        LOG.info("\n########## getCreatedConfigurationData");

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChain) {
                ServiceFunctionChain createdServiceFunctionChain = (ServiceFunctionChain) entry.getValue();
                LOG.info("\n########## Created ServiceFunctionChain name: {}", createdServiceFunctionChain.getName());
                Object[] serviceChainObj = {createdServiceFunctionChain};
                Class[] serviceChainClass = {ServiceFunctionChain.class};
                odlSfc.executor.execute(SfcProviderServicePathAPI
                        .getSfcProviderCreateServicePathAPI(serviceChainObj, serviceChainClass));
                List<SfcServiceFunction>  SfcServiceFunctionList = createdServiceFunctionChain.getSfcServiceFunction();
                for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
                    LOG.info("\n########## Attached ServiceFunction name: {}", sfcServiceFunction.getName());

                }
            }
        }

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedConfigurationData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionChain) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionChain updatedServiceFunctionChain = (ServiceFunctionChain) entry.getValue();
                Object[] serviceChainObj = {updatedServiceFunctionChain};
                Class[] serviceChainClass = {ServiceFunctionChain.class};
                odlSfc.executor.execute(SfcProviderServicePathAPI
                        .getSfcProviderCreateServicePathAPI(serviceChainObj, serviceChainClass));
            }
        }

        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedConfigurationData();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalConfigurationObject.get(instanceIdentifier);
            if( dataObject instanceof ServiceFunctionChain) {
                ServiceFunctionChain originalServiceFunctionChain = (ServiceFunctionChain) dataObject;
                Object[] serviceChainParams = {originalServiceFunctionChain};
                Class[] serviceChainTypes = {ServiceFunction.class};
                odlSfc.executor.execute(SfcProviderServicePathAPI
                        .getSfcProviderDeleteServicePathInstantiatedFromChain(serviceChainParams, serviceChainTypes));
                LOG.info("\n########## getOriginalConfigurationData {}",
                        originalServiceFunctionChain.getName());
            }
        }


        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }



}
