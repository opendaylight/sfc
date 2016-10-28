/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.api.SfcConcurrencyAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class gets called whenever there is a change to
 * a Service Function list entry, i.e.,
 * added/deleted/modified.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2014-06-30
 */
public class SfcProviderSfEntryDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfEntryDataListener.class);

    /**
     * This method is called whenever there is change in a SF. Before doing any changes
     * it takes a global lock in order to ensure it is the only writer.
     *
     * @param change
     */
    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        if (SfcConcurrencyAPI.getLock()) {
            try {

                // SF ORIGINAL
                Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();
                for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
                    if (entry.getValue() instanceof ServiceFunction) {
                        ServiceFunction originalServiceFunction = (ServiceFunction) entry.getValue();
                        LOG.debug("\n########## getOriginalConfigurationData {}  {}", originalServiceFunction.getType(),
                                originalServiceFunction.getName());
                    }
                }

                // SF CREATION
                Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();
                for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
                    if (entry.getValue() instanceof ServiceFunction) {
                        ServiceFunction createdServiceFunction = (ServiceFunction) entry.getValue();

                        if (!SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(createdServiceFunction)) {
                            LOG.error("Failed to create service function type: {}", createdServiceFunction.getType());
                        }
                    }
                }

                // SF DELETION
                Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
                for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
                    DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
                    if (dataObject instanceof ServiceFunction) {
                        ServiceFunction originalServiceFunction = (ServiceFunction) dataObject;

                        if (!SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(originalServiceFunction)) {
                            LOG.error("Failed to delete Service Function Type for SF: {}",
                                    originalServiceFunction.getName());
                        }
                        deleteSfRsps(originalServiceFunction);
                    }
                }

                // SF UPDATE
                Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
                for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
                    if ((entry.getValue() instanceof ServiceFunction)
                            && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                        DataObject dataObject = dataOriginalDataObject.get(entry.getKey());
                        ServiceFunction originalServiceFunction = (ServiceFunction) dataObject;
                        ServiceFunction updatedServiceFunction = (ServiceFunction) entry.getValue();

                        if(!compareSfs(originalServiceFunction, updatedServiceFunction)) {
                            // We only update SF type entry if type has changed
                            if (!updatedServiceFunction.getType().equals(originalServiceFunction.getType())) {
                                // We remove the original SF from SF type list
                                SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(originalServiceFunction);
                                // We create a independent entry
                                SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(updatedServiceFunction);
                            }

                            deleteSfRsps(originalServiceFunction);
                        }
                    }
                }
            } finally {
                SfcConcurrencyAPI.releaseLock();
            }
        } else {
            LOG.error("{}: Failed to Acquire Lock", Thread.currentThread().getStackTrace()[1]);
        }
        printTraceStop(LOG);
    }

    private boolean compareSfs(ServiceFunction origSf, ServiceFunction sf) {
        //
        // Compare SFF IP Mgmt Addresses
        //
        if(sf.getIpMgmtAddress() != null && origSf.getIpMgmtAddress() != null) {
            if(!sf.getIpMgmtAddress().toString().equals(origSf.getIpMgmtAddress().toString())) {
                LOG.info("compareSFs: IP mgmt addresses changed orig [{}] new [{}]",
                        origSf.getIpMgmtAddress().toString(),
                        sf.getIpMgmtAddress().toString());
                return false;
            }
        } else if(origSf.getIpMgmtAddress() != null && sf.getIpMgmtAddress() == null) {
            LOG.info("compareSFFs: the IP mgmt address has been removed");
            return false;
        }

        //
        // Compare SF Types
        //
        if(!sf.getType().getValue().equals(origSf.getType().getValue())) {
            LOG.info("compareSFs: SF type changed orig [{}] new [{}]",
                    origSf.getType().getValue(),
                    sf.getType().getValue());
            return false;
        }

        //
        // Compare SF DPLs
        //
        List<SfDataPlaneLocator> origSfDplList = origSf.getSfDataPlaneLocator();;
        List<SfDataPlaneLocator> sfDplList = sf.getSfDataPlaneLocator();;

        if(origSfDplList.size() > sfDplList.size()) {
            LOG.info("compareSfDpls An SF DPL has been removed");
            // TODO should we check if the removed SF DPL is used??
            return false;
        }

        Collection<SfDataPlaneLocator> differentSfDpls = new ArrayList<>(sfDplList);
        // This will remove everything in common, thus leaving only the different values
        differentSfDpls.removeAll(origSfDplList);

        // If the different SfDpl entries are all contained in the sfDplList,
        // then this was a simple case of adding a new SfDpl entry, else one
        // of the entries was modified, and the RSPs should be deleted
        if(!sfDplList.containsAll(differentSfDpls)) {
            LOG.info("compareSfDpls An SF DPL has been modified");
            return false;
        }

        return true;
    }

    private void deleteSfRsps(ServiceFunction sf) {
        /*
         * Before removing RSPs used by this Service Function, we need to remove all
         * references in the SFF/SF operational trees
         */
        SfName sfName = sf.getName();
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(sfName);
        List<RspName> rspList = new ArrayList<>();
        if ((sfServicePathList != null) && (!sfServicePathList.isEmpty())) {
            if (!SfcProviderServiceFunctionAPI.deleteServiceFunctionState(sfName)) {
                LOG.error("{}: Failed to delete SF {} operational state",
                        Thread.currentThread().getStackTrace()[1], sfName);
            }
            for (SfServicePath sfServicePath : sfServicePathList) {
                // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah

                RspName rspName = new RspName(sfServicePath.getName().getValue());
                SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspName);
                LOG.info("Deleting RSP [{}] on SF [{}]", rspName, sfName);
                rspList.add(rspName);
            }
            SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspList);
        }

        /*
         * We do not update the SFF dictionary. Since the user configured it in the
         * first place,
         * (s)he is also responsible for updating it.
         */
    }
}
