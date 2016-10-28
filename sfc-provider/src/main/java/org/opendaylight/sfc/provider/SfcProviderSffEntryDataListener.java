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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.md.features.rev151010.SffVxlanClassifierType1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.md.features.rev151010.service.function.forwarders.service.function.forwarder.VxlanClassifierType1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class is the DataListener for SFF changes.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2014-06-30
 */
public class SfcProviderSffEntryDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSffEntryDataListener.class);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);
        if (SfcConcurrencyAPI.getLock()) {
            try {

                // SFF ORIGINAL

                Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

                for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
                    if (entry.getValue() instanceof ServiceFunctionForwarder) {
                        ServiceFunctionForwarder originalServiceFunctionForwarder =
                                (ServiceFunctionForwarder) entry.getValue();
                        LOG.debug("Original SFF: {}", originalServiceFunctionForwarder.getName());
                    }
                }

                // SFF DELETION
                Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
                for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
                    DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
                    if (dataObject instanceof ServiceFunctionForwarder) {
                        ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) dataObject;
                        LOG.info("Delete SFF [{}]", serviceFunctionForwarder.getName());
                        deleteSffRsps(serviceFunctionForwarder);
                    }
                }

                // SFF CREATION

                Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

                for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
                    if (entry.getValue() instanceof ServiceFunctionForwarder) {
                        ServiceFunctionForwarder createdServiceFunctionForwarder =
                                (ServiceFunctionForwarder) entry.getValue();
                        SffVxlanClassifierType1 sffVxlanOverlayClassifierType1 =
                                createdServiceFunctionForwarder.getAugmentation(SffVxlanClassifierType1.class);
                        if (sffVxlanOverlayClassifierType1 != null) {
                            VxlanClassifierType1 vxlanClassifierType1 =
                                    sffVxlanOverlayClassifierType1.getVxlanClassifierType1();
                        }
                        LOG.info("{}: Create SFF [{}]", Thread.currentThread().getStackTrace()[1],
                                createdServiceFunctionForwarder.getName());
                    }
                }

                // SFF UPDATE
                Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
                for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
                    if ((entry.getValue() instanceof ServiceFunctionForwarder) &&
                        (!(dataCreatedObject.containsKey(entry.getKey())))) {

                        ServiceFunctionForwarder originalSff = ((ServiceFunctionForwarder) dataOriginalDataObject.get(entry.getKey()));
                        ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                        LOG.info("Update SFF [{}]", serviceFunctionForwarder.getName());
                        if(originalSff == null) {
                            // This case should never happen, but just in case
                            LOG.info("Deleting RSPs on SFF update since the original SFF is not available");
                            deleteSffRsps(serviceFunctionForwarder);
                        } else if(!compareSFFs(originalSff, serviceFunctionForwarder)) {
                            // Only delete the SFF RSPs for changes the require it
                            deleteSffRsps(serviceFunctionForwarder);
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

    /**
     * Compare 2 Service Function Forwarders for basic equality. That is
     * only compare the ServiceNode, DataPlaneLocator, and SfDictionary
     *
     * @param originalSff - The SFF before the change
     * @param sff - The changed SFF
     * @return true on basic equality, false otherwise
     */
    private boolean compareSFFs(ServiceFunctionForwarder originalSff, ServiceFunctionForwarder sff) {
        //
        // Compare SFF Service Nodes
        //
        if(sff.getServiceNode() != null && originalSff.getServiceNode() != null) {
            if(!sff.getServiceNode().getValue().equals(originalSff.getServiceNode().getValue())) {
                LOG.info("compareSFFs: service nodes changed orig [{}] new [{}]",
                        originalSff.getServiceNode().getValue(),
                        sff.getServiceNode().getValue());
                return false;
            }
        } else if(originalSff.getServiceNode() != null && sff.getServiceNode() == null) {
            LOG.info("compareSFFs: the service node has been removed");
            return false;
        }

        //
        // Compare SFF IP Mgmt Addresses
        //
        if(sff.getIpMgmtAddress() != null && originalSff.getIpMgmtAddress() != null) {
            if(!sff.getIpMgmtAddress().toString().equals(originalSff.getIpMgmtAddress().toString())) {
                LOG.info("compareSFFs: IP mgmt addresses changed orig [{}] new [{}]",
                        originalSff.getIpMgmtAddress().toString(),
                        sff.getIpMgmtAddress().toString());
                return false;
            }
        } else if(originalSff.getIpMgmtAddress() != null && sff.getIpMgmtAddress() == null) {
            LOG.info("compareSFFs: the IP mgmt address has been removed");
            return false;
        }

        //
        // Compare SFF ServiceFunction Dictionaries
        //
        if(!compareSffSfDictionaries(originalSff.getServiceFunctionDictionary(), sff.getServiceFunctionDictionary())) {
            return false;
        }

        //
        // Compare SFF Data Plane Locators
        //
        if(!compareSffDpls(originalSff.getSffDataPlaneLocator(), sff.getSffDataPlaneLocator())) {
            return false;
        }

        return true;
    }

    /**
     * Compare 2 lists of SffSfDictionaries for equality
     *
     * @param origSffSfDict a list of the original SffSfDict entries before the change
     * @param sffSfDict a list of the SffSfDict entries after the change was made
     * @return true if the lists are equal, false otherwise
     */
    private boolean compareSffSfDictionaries(List<ServiceFunctionDictionary> origSffSfDict,List<ServiceFunctionDictionary> sffSfDict) {
        if(origSffSfDict.size() > sffSfDict.size()) {
            LOG.info("compareSffSfDictionaries An SF has been removed");
            // TODO should we check if the removed SF is used??
            return false;
        }

        Collection<ServiceFunctionDictionary> differentSffSfs = new ArrayList<>(sffSfDict);
        // This will remove everything in common, thus leaving only the different values
        differentSffSfs.removeAll(origSffSfDict);

        // If the different SffSfDict entries are all contained in the sffSfDict,
        // then this was a simple case of adding a new SffSfDict entry, else one
        // of the entries was modified, and the RSPs should be deleted
        if(!sffSfDict.containsAll(differentSffSfs)) {
            return false;
        }

        return true;
    }

    /**
     * Compare 2 lists of SffDataPlaneLocators for equality
     *
     * @param origSffDplList a list of the original SffDpl entries before the change
     * @param sffDplList a list of the SffDpl entries after the change was made
     * @return true if the lists are equal, false otherwise
     */
    private boolean compareSffDpls(List<SffDataPlaneLocator> origSffDplList, List<SffDataPlaneLocator> sffDplList) {
        if(origSffDplList.size() > sffDplList.size()) {
            LOG.info("compareSffDpls An SFF DPL has been removed");
            // TODO should we check if the removed SFF DPL is used??
            return false;
        }

        Collection<SffDataPlaneLocator> differentSffDpls = new ArrayList<>(sffDplList);
        // This will remove everything in common, thus leaving only the different values
        differentSffDpls.removeAll(origSffDplList);

        // If the different SffDpl entries are all contained in the sffDplList,
        // then this was a simple case of adding a new SffDpl entry, else one
        // of the entries was modified, and the RSPs should be deleted
        if(!sffDplList.containsAll(differentSffDpls)) {
            return false;
        }

        return true;
    }

    private void deleteSffRsps(ServiceFunctionForwarder sff) {
        /*
         * Before removing RSPs used by this Service Function, we need to remove all
         * references in the SFF/SF operational trees
         */
        SffName sffName = sff.getName();
        List<RspName> rspList = new ArrayList<>();
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        if ((sffServicePathList != null) && !sffServicePathList.isEmpty()) {
            if (SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderState(sffName)) {

            } else {
                LOG.error("{}: Failed to delete SFF {} operational state",
                        Thread.currentThread().getStackTrace()[1], sffName);
            }
            for (SffServicePath sffServicePath : sffServicePathList) {
                // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah

                RspName rspName = new RspName(sffServicePath.getName().getValue());
                // XXX Another example of Method Overloading confusion brought about
                // by Strings
                SfcProviderServiceFunctionAPI
                    .deleteServicePathFromServiceFunctionState(new SfpName(rspName.getValue()));
                rspList.add(rspName);
                LOG.info("Deleting RSP [{}] on SFF [{}]", rspName, sffName);
            }
            SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspList);
        }

    }
}
