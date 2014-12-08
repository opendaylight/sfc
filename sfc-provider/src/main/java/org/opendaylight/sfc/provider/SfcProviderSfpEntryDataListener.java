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
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.Map;
import java.util.Set;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

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
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        printTraceStart(LOG);
        odlSfc.getLock();

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunctionPath) {
                ServiceFunctionPath originalServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                LOG.debug("\n########## Original Service path: {}",
                        originalServiceFunctionPath.getName());
            }
        }

        // SFP CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        /* For each SFP we perform the following transactions:
         *   1 - Create RSP
         *   2 - Add Path to SFF State
         *   3 - Add path to SF state
         *   4 - Check if Chain is symmetric
         *   5 - if chain is symmetric repeat 1-3 for reverse path
         *
         * If any of these fail we delete the previous ones that succeeded.
         */

        RenderedServicePath renderedServicePath;
        RenderedServicePath revRenderedServicePath;
        boolean transactionSuccessful = false;
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet())
        {
            if (entry.getValue() instanceof ServiceFunctionPath) {
                boolean rspSuccessful = false;
                boolean revRspSuccessful = false;
                boolean addPathToSffStateSuccessul = false;
                boolean addRevPathToSffStateSuccessul = false;
                boolean addPathToSfStateSuccessul = false;
                boolean addRevPathToSfStateSuccessul = false;
                ServiceFunctionPath createdServiceFunctionPath = (ServiceFunctionPath) entry.getValue();

                // Create RSP
                if ((renderedServicePath = SfcProviderServicePathAPI
                        .createRenderedServicePathEntryExecutor(createdServiceFunctionPath)) != null) {
                    rspSuccessful = true;

                } else {
                    LOG.error("Could not create RSP. System state inconsistent. Deleting and add SFP {} back",
                            createdServiceFunctionPath.getName());
                }
                // Add Path name to SFF operational state
                if (rspSuccessful &&  SfcProviderServiceForwarderAPI
                        .addPathToServiceForwarderStateExecutor(renderedServicePath.getName())) {
                    addPathToSffStateSuccessul = true;
                } else {
                    SfcProviderServicePathAPI.deleteRenderedServicePathExecutor(createdServiceFunctionPath.getName());
                }

                // Add Path to SF operational state
                if (rspSuccessful  && addPathToSffStateSuccessul &&
                        SfcProviderServiceFunctionAPI
                                .addPathToServiceFunctionStateExecutor(renderedServicePath.getName())) {

                    addPathToSfStateSuccessul = true;
                    //Send to SB REST
                    SfcProviderServicePathAPI.checkServiceFunctionPathExecutor
                            (renderedServicePath,HttpMethod.PUT);
                } else {
                    SfcProviderServiceForwarderAPI
                            .deletePathFromServiceForwarderStateExecutor(createdServiceFunctionPath);
                    SfcProviderServicePathAPI.deleteRenderedServicePathExecutor(createdServiceFunctionPath.getName());

                }

                // Reverse Path
                ServiceFunctionChain serviceFunctionChain =  SfcProviderServiceChainAPI.readServiceFunctionChainExecutor(createdServiceFunctionPath.getServiceChainName());
                if (serviceFunctionChain.isSymmetric() != null && serviceFunctionChain.isSymmetric() && addPathToSfStateSuccessul) {
                    if ((revRenderedServicePath = SfcProviderServicePathAPI
                            .createReverseRenderedServicePathEntryExecutor(renderedServicePath)) != null) {
                        revRspSuccessful = true;
                    } else {
                        LOG.error("Could not create Reverse RSP {}",  createdServiceFunctionPath.getName());
                    }

                    // Add Path name to SFF operational state
                    if (revRspSuccessful &&  SfcProviderServiceForwarderAPI
                            .addPathToServiceForwarderStateExecutor(revRenderedServicePath.getName())) {
                        addRevPathToSffStateSuccessul = true;
                    } else {
                        SfcProviderServicePathAPI.deleteRenderedServicePathExecutor(revRenderedServicePath.getName());
                    }

                    // Add Path to SF operational state
                    if (revRspSuccessful  && addRevPathToSffStateSuccessul &&
                            SfcProviderServiceFunctionAPI
                                    .addPathToServiceFunctionStateExecutor(revRenderedServicePath.getName())) {

                        addRevPathToSfStateSuccessul = true;
                        //Send to SB REST
                        SfcProviderServicePathAPI.checkServiceFunctionPathExecutor
                                (revRenderedServicePath,HttpMethod.PUT);
                    } else {
                        SfcProviderServiceForwarderAPI
                                .deletePathFromServiceForwarderStateExecutor(revRenderedServicePath.getName());
                        SfcProviderServicePathAPI.deleteRenderedServicePathExecutor(revRenderedServicePath.getName());

                    }
                }

            }
        }


/*        RenderedServicePath renderedServicePath;
        RenderedServicePath revRenderedServicePath;
        boolean transactionSuccessful = false;
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet())
        {
            ServiceFunctionPath createdServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
            if (entry.getValue() instanceof ServiceFunctionPath) {
                if ((renderedServicePath = SfcProviderServicePathAPI
                        .createRenderedServicePathEntryExecutor(createdServiceFunctionPath)) != null) {

                    if  (SfcProviderServiceForwarderAPI
                                .addPathToServiceForwarderStateExecutor(createdServiceFunctionPath)) {
                        if (SfcProviderServiceFunctionAPI
                                .addPathToServiceFunctionStateExecutor(createdServiceFunctionPath)) {

                            transactionSuccessful = true;
                            //Send to SB REST
                            SfcProviderServicePathAPI.checkServiceFunctionPathExecutor
                                    (createdServiceFunctionPath,HttpMethod.PUT);
                        } else {
                            SfcProviderServiceForwarderAPI
                                    .deletePathFromServiceForwarderStateExecutor(createdServiceFunctionPath);
                            SfcProviderServicePathAPI.deleteRenderedServicePathExecutor(createdServiceFunctionPath.getName());
                        }
                    } else {
                        //rollback RSP
                        SfcProviderServicePathAPI.deleteRenderedServicePathExecutor(createdServiceFunctionPath.getName());
                    }
                } else {
                    LOG.error("Could not create RSP. System state inconsistent. Deleting and add SFP {} back",
                            createdServiceFunctionPath.getName());
                }
                if (transactionSuccessful) {
                    if ((revRenderedServicePath = SfcProviderServicePathAPI
                            .createReverseRenderedServicePathEntryExecutor(renderedServicePath)) != null) {
                        if  (SfcProviderServiceForwarderAPI
                                .addPathToServiceForwarderStateExecutor(createdServiceFunctionPath)) {
                            if (SfcProviderServiceFunctionAPI
                                    .addPathToServiceFunctionStateExecutor(createdServiceFunctionPath)) {

                                transactionSuccessful = true;
                                //Send to SB REST
                                SfcProviderServicePathAPI.checkServiceFunctionPathExecutor
                                        (createdServiceFunctionPath,HttpMethod.PUT);
                            } else {
                                SfcProviderServiceForwarderAPI
                                        .deletePathFromServiceForwarderStateExecutor(createdServiceFunctionPath);
                                SfcProviderServicePathAPI.deleteRenderedServicePathExecutor(revRenderedServicePath.getName());
                            }
                        } else {
                            //rollback RSP
                            SfcProviderServicePathAPI.deleteRenderedServicePathExecutor(revRenderedServicePath.getName());
                        }
                    }
                } else {
                    LOG.error("Could not create RSP. System state inconsistent. Deleting and add SFP {} back",
                            createdServiceFunctionPath.getName());
                }
            }
        }*/

        // SFP UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject =
                change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionPath) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionPath updatedServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                LOG.debug("\n########## Modified Service Function Path Name {}",
                        updatedServiceFunctionPath.getName());
                Object[] servicePathObj = {updatedServiceFunctionPath};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                        .getUpdateRenderedServicePathAPI(servicePathObj, servicePathClass);
                odlSfc.executor.submit(sfcProviderServicePathAPI);
            }
        }


        // SFP DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if( dataObject instanceof ServiceFunctionPath) {

                // If a SFP is deleted we remove RSP and both SF and SFF operational states.
                ServiceFunctionPath originalServiceFunctionPath = (ServiceFunctionPath) dataObject;
                SfcProviderServiceForwarderAPI
                        .deletePathFromServiceForwarderStateExecutor(originalServiceFunctionPath);
                SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor(originalServiceFunctionPath);
                SfcProviderServicePathAPI.deleteRenderedServicePathExecutor(originalServiceFunctionPath.getName());

                //Send to SB REST
                SfcProviderServicePathAPI.checkServiceFunctionPathExecutor
                        (originalServiceFunctionPath, HttpMethod.DELETE);
            }
        }
        odlSfc.releaseLock();
        printTraceStop(LOG);
    }
}
