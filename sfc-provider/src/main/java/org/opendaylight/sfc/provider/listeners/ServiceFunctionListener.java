/*
 * Copyright (c) 2016 Ericsson and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import java.util.ArrayList;
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
public class ServiceFunctionListener extends AbstractClusteredDataTreeChangeListener<ServiceFunction> {
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
            removeRSPs(serviceFunction);
            if (!SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(serviceFunction)) {
                LOG.error("Failed to delete Service Function: ", serviceFunction.getName());
            }
        }
    }

    /**
     * Removes all the RSP in which the Service Function is referenced.
     *
     * @param serviceFunction
     */
    private void removeRSPs(ServiceFunction serviceFunction) {
        // TODO: this method is almost literally copied from the previous
        // version of the listener and should be reviewed.

        /*
         * Before removing RSPs used by this Service Function, we need to remove
         * all references in the SFF/SF operational trees
         */
        SfName serviceFunctionName = serviceFunction.getName();
        List<RspName> rspList = SfcProviderServiceFunctionAPI.getRspsBySfName(serviceFunctionName);
        if (rspList != null && !rspList.isEmpty()) {
            LOG.debug("Removing RSP associated to Service Function: ", serviceFunctionName);
            if (!SfcProviderServiceFunctionAPI.deleteServiceFunctionState(serviceFunctionName)) {
                LOG.error("Failed to delete Service Function {} operational state.", serviceFunctionName);
            }
            SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspList);
            SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspList);
        }
    }

    @Override
    protected void update(ServiceFunction originalServiceFunction, ServiceFunction updatedServiceFunction) {
        if (originalServiceFunction != null) {
            LOG.debug("Updating Service Function: {}", originalServiceFunction.getName());

            // We only update SF type entry if type has changed
            if (!updatedServiceFunction.getType().equals(originalServiceFunction.getType())) {
                // We remove the original SF from SF type list
                SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(originalServiceFunction);
                // We create a independent entry
                SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(updatedServiceFunction);
            }
            // This convenience method isolates the code write previously in the
            // update method
            updateServiceFunctionState(originalServiceFunction);
        }
    }

    /**
     * Updates the state of a given Service Function.
     *
     * @param serviceFunction
     */
    private void updateServiceFunctionState(ServiceFunction serviceFunction) {
        // TODO: this method is almost literally copied from the previous
        // version of the listener and should be reviewed.

        /*
         * Before removing RSPs used by this Service Function, we need to remove
         * all references in the SFF/SF operational trees
         */
        SfName serviceFunctionName = serviceFunction.getName();
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI
                .readServiceFunctionState(serviceFunctionName);
        List<RspName> rspList = new ArrayList<>();
        if (sfServicePathList != null && !sfServicePathList.isEmpty()) {
            if (SfcProviderServiceFunctionAPI.deleteServiceFunctionState(serviceFunctionName)) {
                for (SfServicePath sfServicePath : sfServicePathList) {
                    // TODO Bug 4495 - RPCs hiding heuristics using
                    // Strings - alagalah
                    RspName rspName = new RspName(sfServicePath.getName().getValue());
                    SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspName);
                    rspList.add(rspName);
                }
                SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspList);

            } else {
                LOG.error("{}: Failed to delete Service Function {} operational state", serviceFunctionName);
            }
        }
        /*
         * We do not update the SFF dictionary. Since the user configured it in
         * the first place, (s)he is also responsible for updating it.
         */
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }
}
