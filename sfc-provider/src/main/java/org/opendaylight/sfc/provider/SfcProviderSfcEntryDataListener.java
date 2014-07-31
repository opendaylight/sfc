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

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChain) {
                ServiceFunctionChain originalServiceFunctionChain = (ServiceFunctionChain) entry.getValue();
                LOG.debug("\n########## Original ServiceFunctionChainEntry name: {}", originalServiceFunctionChain.getName());
                List<SfcServiceFunction>  SfcServiceFunctionList = originalServiceFunctionChain.getSfcServiceFunction();
                for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
                    LOG.debug("\n########## Original ServiceFunctionEntry name: {}", sfcServiceFunction.getName());

                }
            }
        }

        // SFC CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionChain) {
                ServiceFunctionChain createdServiceFunctionChain = (ServiceFunctionChain) entry.getValue();
                LOG.debug("\n########## Created ServiceFunctionChain name: {}", createdServiceFunctionChain.getName());
                List<SfcServiceFunction>  SfcServiceFunctionList = createdServiceFunctionChain.getSfcServiceFunction();
                for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
                    LOG.debug("\n########## Attached ServiceFunction name: {}", sfcServiceFunction.getName());

                }
            }
        }

        // SFC UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionChain) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionChain updatedServiceFunctionChain = (ServiceFunctionChain) entry.getValue();
                LOG.info("\n########## Modified Service Function Chain Name {}",
                        updatedServiceFunctionChain.getName());
            }
        }

        // SFC DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalConfigurationObject.get(instanceIdentifier);
            if( dataObject instanceof ServiceFunctionChain) {
                ServiceFunctionChain originalServiceFunctionChain = (ServiceFunctionChain) dataObject;
                LOG.debug("\n########## getOriginalConfigurationData {}",
                        originalServiceFunctionChain.getName());
            }
        }


        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }



}
