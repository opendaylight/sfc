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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This class is the DataListener for SFP Entry changes.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */

public class SfcProviderSfpEntryDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfpEntryDataListener.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        /*
         * when a SFF is created we will process and send it to southbound devices. But first we need
         * to mae sure all info is present or we will pass.
         */
        // SFC CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedConfigurationData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionPath) {
                ServiceFunctionPath createdServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                LOG.debug("\n########## Created ServiceFunctionChain name: {}", createdServiceFunctionPath.getName());
                Object[] servicePathObj = {createdServiceFunctionPath};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                odlSfc.executor.execute(SfcProviderServicePathAPI
                        .getCreateServicePathAPI(servicePathObj, servicePathClass));
            }
        }

        // SFP UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject =
                change.getUpdatedConfigurationData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionPath) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionPath updatedServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                LOG.info("\n########## Modified Service Function Chain Name {}",
                        updatedServiceFunctionPath.getName());
                Object[] servicePathObj = {updatedServiceFunctionPath};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                odlSfc.executor.execute(SfcProviderServicePathAPI
                        .getUpdateServicePathAPI(servicePathObj, servicePathClass));
            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }
}
