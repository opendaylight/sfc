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
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service
 * Functions taking the appropriate actions.
 *
 * @author David Suárez (david.suarez.fuentes@ericsson.com)
 *
 */
public class ServiceFunctionListener extends AbstractDataTreeChangeListener<ServiceFunction> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionListener.class);

    private final DataBroker dataBroker;
    private ListenerRegistration<ServiceFunctionListener> listenerRegistration;

    public ServiceFunctionListener(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        LOG.debug("Initializing...");
        registerListeners();
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

    private void registerListeners() {
        final DataTreeIdentifier<ServiceFunction> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(ServiceFunctions.class).child(ServiceFunction.class));
        listenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    public void add(ServiceFunction serviceFunction) {
        if (serviceFunction != null) {
            LOG.debug("Adding Service Function: {}", serviceFunction.getName());

            if (!SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction)) {
                LOG.error("Failed to create Service Function: ", serviceFunction.getName());
            }
        }
    }

    @Override
    public void remove(ServiceFunction serviceFunction) {
        if (serviceFunction != null) {
            LOG.debug("Deleting Service Function: {}", serviceFunction.getName());
            deleteSfRsps(serviceFunction);
            if (!SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(serviceFunction)) {
                LOG.error("Failed to delete Service Function: ", serviceFunction.getName());
            }
        }
    }

    @Override
    protected void update(ServiceFunction originalServiceFunction, ServiceFunction updatedServiceFunction) {
        if (originalServiceFunction != null) {
            LOG.debug("Updating Service Function: {}", originalServiceFunction.getName());

            if (!compareSfs(originalServiceFunction, updatedServiceFunction)) {
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

    private boolean compareSfs(ServiceFunction origSf, ServiceFunction sf) {
        //
        // Compare SFF IP Mgmt Addresses
        //
        if (sf.getIpMgmtAddress() != null && origSf.getIpMgmtAddress() != null) {
            if (!sf.getIpMgmtAddress().toString().equals(origSf.getIpMgmtAddress().toString())) {
                LOG.info("compareSFs: IP mgmt addresses changed orig [{}] new [{}]",
                        origSf.getIpMgmtAddress().toString(), sf.getIpMgmtAddress().toString());
                return false;
            }
        } else if (origSf.getIpMgmtAddress() != null && sf.getIpMgmtAddress() == null) {
            LOG.info("compareSFFs: the IP mgmt address has been removed");
            return false;
        }

        //
        // Compare SF Types
        //
        if (!sf.getType().getValue().equals(origSf.getType().getValue())) {
            LOG.info("compareSFs: SF type changed orig [{}] new [{}]", origSf.getType().getValue(),
                    sf.getType().getValue());
            return false;
        }

        //
        // Compare SF DPLs
        //
        List<SfDataPlaneLocator> origSfDplList = origSf.getSfDataPlaneLocator();
        List<SfDataPlaneLocator> sfDplList = sf.getSfDataPlaneLocator();

        if (origSfDplList.size() > sfDplList.size()) {
            LOG.info("compareSfDpls An SF DPL has been removed");
            // TODO should we check if the removed SF DPL is used??
            return false;
        }

        Collection<SfDataPlaneLocator> differentSfDpls = new ArrayList<>(sfDplList);
        // This will remove everything in common, thus leaving only the
        // different values
        differentSfDpls.removeAll(origSfDplList);

        // If the different SfDpl entries are all contained in the sfDplList,
        // then this was a simple case of adding a new SfDpl entry, else one
        // of the entries was modified, and the RSPs should be deleted
        if (!sfDplList.containsAll(differentSfDpls)) {
            LOG.info("compareSfDpls An SF DPL has been modified");
            return false;
        }

        return true;
    }

    /**
     * Removes all the RSP in which the Service Function is referenced.
     *
     * @param serviceFunction
     */
    private void deleteSfRsps(ServiceFunction sf) {
        /*
         * Before removing RSPs used by this Service Function, we need to remove
         * all references in the SFF/SF operational trees
         */
        SfName sfName = sf.getName();
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(sfName);
        List<RspName> rspList = new ArrayList<>();
        if (sfServicePathList != null && !sfServicePathList.isEmpty()) {
            if (!SfcProviderServiceFunctionAPI.deleteServiceFunctionState(sfName)) {
                LOG.error("{}: Failed to delete SF {} operational state", Thread.currentThread().getStackTrace()[1],
                        sfName);
            }
            for (SfServicePath sfServicePath : sfServicePathList) {
                // TODO Bug 4495 - RPCs hiding heuristics using Strings -
                // alagalah

                RspName rspName = new RspName(sfServicePath.getName().getValue());
                SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspName);
                LOG.info("Deleting RSP [{}] on SF [{}]", rspName, sfName);
                rspList.add(rspName);
            }
            SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspList);
        }

        /*
         * We do not update the SFF dictionary. Since the user configured it in
         * the first place, (s)he is also responsible for updating it.
         */
    }
}
