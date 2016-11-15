/*
 * Copyright (c) 2016 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service Function
 * Forwarders taking the appropriate actions.
 *
 * @author David Suárez (david.suarez.fuentes@ericsson.com)
 *
 */
public class ServiceFunctionForwarderListener extends AbstractDataTreeChangeListener<ServiceFunctionForwarder> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionForwarderListener.class);

    private final DataBroker dataBroker;
    private ListenerRegistration<ServiceFunctionForwarderListener> listenerRegistration;

    public ServiceFunctionForwarderListener(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        LOG.debug("Initializing...");
        registerListeners();
    }

    private void registerListeners() {
        final DataTreeIdentifier<ServiceFunctionForwarder> treeId = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class));
        listenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

    @Override
    protected void add(ServiceFunctionForwarder serviceFunctionForwarder) {
        LOG.debug("Adding Service Function Forwarder: {}", serviceFunctionForwarder.getName());
    }

    @Override
    protected void remove(ServiceFunctionForwarder serviceFunctionForwarder) {
        LOG.debug("Deleting Service Function Forwarder: {}", serviceFunctionForwarder.getName());
        removeRSPs(serviceFunctionForwarder);
    }

    @Override
    protected void update(ServiceFunctionForwarder originalServiceFunctionForwarder,
            ServiceFunctionForwarder updatedServiceFunctionForwarder) {
        LOG.debug("Updating Service Function Forwarder: {}", originalServiceFunctionForwarder.getName());
        if (!compareSFFs(originalServiceFunctionForwarder, updatedServiceFunctionForwarder)) {
            // Only delete the SFF RSPs for changes the require it
            removeRSPs(updatedServiceFunctionForwarder);
        }
    }

    /**
     * Removes all the RSP in which the Service Function Forwarder is
     * referenced.
     *
     * @param serviceFunctionForwarder
     */
    private void removeRSPs(ServiceFunctionForwarder serviceFunctionForwarder) {
        // TODO: this method is almost literally copied from the previous
        // version of the listener and should be reviewed.

        /*
         * Before removing RSPs used by this Service Function, we need to remove
         * all references in the SFF/SF operational trees
         */
        SffName serviceFunctionForwarderName = serviceFunctionForwarder.getName();
        List<RspName> rspList = new ArrayList<>();
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI
                .readSffState(serviceFunctionForwarderName);
        if (sffServicePathList != null && !sffServicePathList.isEmpty()) {
            if (!SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderState(serviceFunctionForwarderName)) {
                LOG.error("Failed to delete Service Function Forwarder {} operational state.",
                        serviceFunctionForwarder);
            }
            for (SffServicePath sffServicePath : sffServicePathList) {
                // TODO Bug 4495 - RPCs hiding heuristics using Strings -
                // alagalah

                RspName rspName = new RspName(sffServicePath.getName().getValue());
                // XXX Another example of Method Overloading confusion brought
                // about
                // by Strings
                SfcProviderServiceFunctionAPI
                        .deleteServicePathFromServiceFunctionState(new SfpName(rspName.getValue()));
                rspList.add(rspName);
                LOG.info("Deleting RSP [{}] on SFF [{}]", rspName, serviceFunctionForwarderName);
            }
            SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspList);
        }

        /*
         * We do not update the SFF dictionary. Since the user configured it in
         * the first place, (s)he is also responsible for updating it.
         */
    }

    /**
     * Compare 2 Service Function Forwarders for basic equality. That is only
     * compare the ServiceNode, DataPlaneLocator, and SfDictionary
     *
     * @param originalSff
     *            - The SFF before the change
     * @param sff
     *            - The changed SFF
     * @return true on basic equality, false otherwise
     */
    private boolean compareSFFs(ServiceFunctionForwarder originalSff, ServiceFunctionForwarder sff) {
        //
        // Compare SFF Service Nodes
        //
        if (sff.getServiceNode() != null && originalSff.getServiceNode() != null) {
            if (!sff.getServiceNode().getValue().equals(originalSff.getServiceNode().getValue())) {
                LOG.info("compareSFFs: service nodes changed orig [{}] new [{}]",
                        originalSff.getServiceNode().getValue(), sff.getServiceNode().getValue());
                return false;
            }
        } else if (originalSff.getServiceNode() != null && sff.getServiceNode() == null) {
            LOG.info("compareSFFs: the service node has been removed");
            return false;
        }

        //
        // Compare SFF IP Mgmt Addresses
        //
        if (sff.getIpMgmtAddress() != null && originalSff.getIpMgmtAddress() != null) {
            if (!sff.getIpMgmtAddress().toString().equals(originalSff.getIpMgmtAddress().toString())) {
                LOG.info("compareSFFs: IP mgmt addresses changed orig [{}] new [{}]",
                        originalSff.getIpMgmtAddress().toString(), sff.getIpMgmtAddress().toString());
                return false;
            }
        } else if (originalSff.getIpMgmtAddress() != null && sff.getIpMgmtAddress() == null) {
            LOG.info("compareSFFs: the IP mgmt address has been removed");
            return false;
        }

        //
        // Compare SFF ServiceFunction Dictionaries
        //
        if (!compareSffSfDictionaries(originalSff.getServiceFunctionDictionary(), sff.getServiceFunctionDictionary())) {
            return false;
        }

        //
        // Compare SFF Data Plane Locators
        //
        if (!compareSffDpls(originalSff.getSffDataPlaneLocator(), sff.getSffDataPlaneLocator())) {
            return false;
        }

        return true;
    }

    /**
     * Compare 2 lists of SffSfDictionaries for equality
     *
     * @param origSffSfDict
     *            a list of the original SffSfDict entries before the change
     * @param sffSfDict
     *            a list of the SffSfDict entries after the change was made
     * @return true if the lists are equal, false otherwise
     */
    private boolean compareSffSfDictionaries(List<ServiceFunctionDictionary> origSffSfDict,
            List<ServiceFunctionDictionary> sffSfDict) {
        if (origSffSfDict.size() > sffSfDict.size()) {
            LOG.info("compareSffSfDictionaries An SF has been removed");
            // TODO should we check if the removed SF is used??
            return false;
        }

        Collection<ServiceFunctionDictionary> differentSffSfs = new ArrayList<>(sffSfDict);
        // This will remove everything in common, thus leaving only the
        // different values
        differentSffSfs.removeAll(origSffSfDict);

        // If the different SffSfDict entries are all contained in the
        // sffSfDict,
        // then this was a simple case of adding a new SffSfDict entry, else one
        // of the entries was modified, and the RSPs should be deleted
        if (!sffSfDict.containsAll(differentSffSfs)) {
            return false;
        }

        return true;
    }

    /**
     * Compare 2 lists of SffDataPlaneLocators for equality
     *
     * @param origSffDplList
     *            a list of the original SffDpl entries before the change
     * @param sffDplList
     *            a list of the SffDpl entries after the change was made
     * @return true if the lists are equal, false otherwise
     */
    private boolean compareSffDpls(List<SffDataPlaneLocator> origSffDplList, List<SffDataPlaneLocator> sffDplList) {
        if (origSffDplList.size() > sffDplList.size()) {
            LOG.info("compareSffDpls An SFF DPL has been removed");
            // TODO should we check if the removed SFF DPL is used??
            return false;
        }

        Collection<SffDataPlaneLocator> differentSffDpls = new ArrayList<>(sffDplList);
        // This will remove everything in common, thus leaving only the
        // different values
        differentSffDpls.removeAll(origSffDplList);

        // If the different SffDpl entries are all contained in the sffDplList,
        // then this was a simple case of adding a new SffDpl entry, else one
        // of the entries was modified, and the RSPs should be deleted
        if (!sffDplList.containsAll(differentSffDpls)) {
            return false;
        }

        return true;
    }
}
