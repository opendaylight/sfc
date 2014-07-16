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

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


/**
 * This class gets called whenever there is a change to
 * a Service Function list entry, i.e.,
 * added/deleted/modified.
 *
 * <p>
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

        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);



        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalConfigurationData();
        LOG.info("\n########## getOriginalConfigurationData");

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction originalServiceFunction = (ServiceFunction) entry.getValue();
                LOG.info("\n########## getOriginalConfigurationData {}  {}",
                        originalServiceFunction.getType(), originalServiceFunction.getName());
            }
            //System.out.println(entry.getKey() + "/" + entry.getValue());
        }
        /*
        DataObject dataUpdatedObject = change.getUpdatedConfigurationSubtree();
        LOG.info("\n########## getUpdatedConfigurationSubtree");
        if( dataUpdatedObject instanceof  ServiceFunction) {
            ServiceFunction updatedServiceFunction = (ServiceFunction) dataUpdatedObject;
            LOG.info("\n########## UpdatedConfigurationSubstree {}  {}",
                    updatedServiceFunction.getType(), updatedServiceFunction.getName());

        }
        */

        // Created ServiceFunctions trigger creation of SFFs and SFTs
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedConfigurationData();
        LOG.info("\n########## getCreatedConfigurationData");

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction createdServiceFunction = (ServiceFunction) entry.getValue();

                Object[] serviceTypeObj = {createdServiceFunction};
                Class[] serviceTypeClass = {ServiceFunction.class};

                odlSfc.executor.execute(SfcProviderServiceTypeAPI
                        .getSfcProviderCreateServiceFunctionToServiceType(serviceTypeObj, serviceTypeClass));

                Object[] sfParams = {createdServiceFunction};
                Class[] sfParamsTypes = {ServiceFunction.class};
                odlSfc.executor.execute(SfcProviderServiceForwarderAPI
                        .getSfcProviderCreateServiceForwarderAPI(sfParams, sfParamsTypes));
                LOG.info("\n########## getCreatedConfigurationData {}  {}",
                            createdServiceFunction.getType(), createdServiceFunction.getName());
            }
            //System.out.println(entry.getKey() + "/" + entry.getValue());
        }


        // Build a list of all Service Functions that were explicitly removed.
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedConfigurationData();
        LinkedHashMap<InstanceIdentifier<?>, ServiceFunction> removedServiceFunctionLinkedHashMap = new LinkedHashMap<>();
        LinkedHashMap<InstanceIdentifier<?>, DataObject> removedSubElementsLinkedHashMap = new LinkedHashMap<>();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if( dataObject instanceof  ServiceFunction) {
                removedServiceFunctionLinkedHashMap.put(instanceIdentifier, (ServiceFunction)dataObject);
                ServiceFunction originalServiceFunction = (ServiceFunction) dataObject;
                Object[] serviceTypeObj = {originalServiceFunction};
                Class[] serviceTypeClass = {ServiceFunction.class};

                odlSfc.executor.execute(SfcProviderServiceTypeAPI
                        .getSfcProviderDeleteServiceFunctionFromServiceType(serviceTypeObj, serviceTypeClass));


                Object[] sfParams = {originalServiceFunction};
                Class[] sfParamsTypes = {ServiceFunction.class};
                odlSfc.executor.execute(SfcProviderServiceForwarderAPI
                        .getDeleteServiceFunctionFromForwarderAPI(sfParams, sfParamsTypes ));
                // This deletion will trigger a callback to the SFC Entry listener
                Object[] chainsParams = {originalServiceFunction};
                Class[] chainsParamsTypes = {ServiceFunction.class};
                odlSfc.executor.execute(SfcProviderServiceChainAPI
                        .getRemoveServiceFunctionFromChain(chainsParams, chainsParamsTypes));
            } else {
                removedSubElementsLinkedHashMap.put(instanceIdentifier, dataObject);
            }
        }


        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedConfigurationData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunction) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunction updatedServiceFunction = (ServiceFunction) entry.getValue();
                Object[] serviceTypeObj = {updatedServiceFunction};
                Class[] serviceTypeClass = {ServiceFunction.class};
                odlSfc.executor.execute(SfcProviderServiceTypeAPI
                        .getSfcProviderCreateServiceFunctionToServiceType(serviceTypeObj, serviceTypeClass));

                Object[] sfParams = {updatedServiceFunction};
                Class[] sfParamsTypes = {ServiceFunction.class};
                odlSfc.executor.execute(SfcProviderServiceForwarderAPI
                        .getUpdateServiceForwarderAPI(sfParams, sfParamsTypes ));
                LOG.info("\n########## getUpdatedConfigurationData {}  {}",
                        updatedServiceFunction.getType(), updatedServiceFunction.getName());
            }
        }
/*
        LOG.info("\n########## getUpdatedConfigurationData");

        // PUT on sub-container
        // If this condition holds there was a 'replace' to an element of a ServiceFunction, i.e.,
        // something under the ServiceFunction entry was updated with a PUT.
        // It is very problematic to find out
        // which element and the difference from before, therefore we just commit the
        // entire affected elements  again and rely on datastore merge.
        //
        if ((dataRemovedConfigurationIID.isEmpty()) && (dataCreatedObject.isEmpty())) {

            for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
                if (entry.getValue() instanceof ServiceFunction) {
                    ServiceFunction updatedServiceFunction = (ServiceFunction) entry.getValue();
                    Object[] serviceTypeObj = {updatedServiceFunction};
                    Class[] serviceTypeClass = {ServiceFunction.class};
                    //odlSfc.executor.execute(SfcProviderServiceTypeAPI
                    //        .getSfcProviderCreateServiceFunctionToServiceType(serviceTypeObj, serviceTypeClass));
                    odlSfc.executor.execute(SfcProviderServiceForwarderAPI
                            .getSfcProviderCreateServiceForwarderAPI(updatedServiceFunction));
                    LOG.info("\n########## getUpdatedConfigurationData {}  {}",
                            updatedServiceFunction.getType(), updatedServiceFunction.getName());
                }
            }
        }
        */


        // Debug and Unit Test. We trigger the unit test code by adding a service function to the datastore.
        if (SfcProviderDebug.ON) {
            SfcProviderDebug.ON = false;
            SfcProviderUnitTest.sfcProviderUnitTest(SfcProviderRpc.getSfcProviderRpc());
        }

        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

}
