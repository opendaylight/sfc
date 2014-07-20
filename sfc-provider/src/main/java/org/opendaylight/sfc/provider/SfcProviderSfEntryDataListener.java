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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;


/**
 * This class gets called whenever there is a change to
 * a Service Function list entry, i.e.,
 * added/deleted/modified.
 *
 * <p
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderSfEntryDataListener implements DataChangeListener  {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfEntryDataListener.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalConfigurationData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction originalServiceFunction = (ServiceFunction) entry.getValue();
                LOG.debug("\n########## getOriginalConfigurationData {}  {}",
                        originalServiceFunction.getType(), originalServiceFunction.getName());
            }
        }

        // SF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedConfigurationData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction createdServiceFunction = (ServiceFunction) entry.getValue();

                Object[] serviceTypeObj = {createdServiceFunction};
                Class[] serviceTypeClass = {ServiceFunction.class};

                odlSfc.executor.execute(SfcProviderServiceTypeAPI
                        .getCreateServiceFunctionToServiceType(serviceTypeObj, serviceTypeClass));

                Object[] sfParams = {createdServiceFunction};
                Class[] sfParamsTypes = {ServiceFunction.class};
                odlSfc.executor.execute(SfcProviderServiceForwarderAPI
                        .getCreateServiceForwarderAPI(sfParams, sfParamsTypes));
                LOG.debug("\n########## getCreatedConfigurationData {}  {}",
                            createdServiceFunction.getType(), createdServiceFunction.getName());
            }

        }

        // SF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedConfigurationData();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if( dataObject instanceof  ServiceFunction) {
                ServiceFunction originalServiceFunction = (ServiceFunction) dataObject;
                Object[] serviceTypeObj = {originalServiceFunction};
                Class[] serviceTypeClass = {ServiceFunction.class};

                odlSfc.executor.execute(SfcProviderServiceTypeAPI
                        .getDeleteServiceFunctionFromServiceType(serviceTypeObj, serviceTypeClass));


                Object[] sfParams = {originalServiceFunction};
                Class[] sfParamsTypes = {ServiceFunction.class};
                odlSfc.executor.execute(SfcProviderServiceForwarderAPI
                        .getDeleteServiceFunctionFromForwarder(sfParams, sfParamsTypes ));
                // This deletion will trigger a callback to the SFC Entry listener
                Object[] functionParams = {originalServiceFunction};
                Class[] functionParamsTypes = {ServiceFunction.class};
                //odlSfc.executor.execute(SfcProviderServiceChainAPI
                //        .getRemoveServiceFunctionFromChain(chainsParams, chainsParamsTypes));

                odlSfc.executor.execute(SfcProviderServicePathAPI
                        .getDeleteServicePathContainingFunction(functionParams, functionParamsTypes));
            }
        }

        // SF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject
                = change.getUpdatedConfigurationData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunction) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunction updatedServiceFunction = (ServiceFunction) entry.getValue();
                Object[] serviceTypeObj = {updatedServiceFunction};
                Class[] serviceTypeClass = {ServiceFunction.class};
                odlSfc.executor.execute(SfcProviderServiceTypeAPI
                        .getCreateServiceFunctionToServiceType(serviceTypeObj, serviceTypeClass));

                Object[] sfParams = {updatedServiceFunction};
                Class[] sfParamsTypes = {ServiceFunction.class};
                odlSfc.executor.execute(SfcProviderServiceForwarderAPI
                        .getUpdateServiceForwarderAPI(sfParams, sfParamsTypes ));

                odlSfc.executor.execute(SfcProviderServicePathAPI
                        .getUpdateServicePathContainingFunction(sfParams, sfParamsTypes));
            }
        }
        // Debug and Unit Test. We trigger the unit test code by adding a service function to the datastore.
        if (SfcProviderDebug.ON) {
            SfcProviderDebug.ON = false;
            SfcProviderUnitTest.sfcProviderUnitTest(SfcProviderRpc.getSfcProviderRpc());
        }

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

}
