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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.md.features.rev151010.SffVxlanClassifierType1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.md.features.rev151010.service.function.forwarders.service.function.forwarder.VxlanClassifierType1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service
 * Function Forwarders taking the appropriate actions.
 *
 * @author David Suárez (david.suarez.fuentes@ericsson.com)
 *
 */
public class ServiceFunctionForwarderListener extends AbstractClusteredDataTreeChangeListener<ServiceFunctionForwarder> {
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
    protected void add(ServiceFunctionForwarder serviceFunctionForwarder) {
        LOG.debug("Adding Service Function Forwarder: {}", serviceFunctionForwarder.getName());

        // TODO: the following code is literally copied from the previous
        // listener, but it looks like doing nothing
        SffVxlanClassifierType1 sffVxlanOverlayClassifierType1 = serviceFunctionForwarder
                .getAugmentation(SffVxlanClassifierType1.class);
        if (sffVxlanOverlayClassifierType1 != null) {
            VxlanClassifierType1 vxlanClassifierType1 = sffVxlanOverlayClassifierType1.getVxlanClassifierType1();
        }
    }

    @Override
    protected void remove(ServiceFunctionForwarder serviceFunctionForwarder) {
        LOG.debug("Deleting Service Function Forwarder: {}", serviceFunctionForwarder.getName());
        removeRSPs(serviceFunctionForwarder);
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
            LOG.debug("Removing RSP associated to Service Function Forwarder: ", serviceFunctionForwarderName);
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
            }
            SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspList);
        }
    }

    @Override
    protected void update(ServiceFunctionForwarder originalServiceFunctionForwarder,
            ServiceFunctionForwarder updatedServiceFunctionForwarder) {
        LOG.debug("Deleting Service Function Forwarder: {}", originalServiceFunctionForwarder.getName());
        updateServiceFunctionForwarderState(originalServiceFunctionForwarder);
    }

    /**
     * Updates the state of a given Service Function Forwarder.
     *
     * @param serviceFunctionForwarder
     */
    private void updateServiceFunctionForwarderState(ServiceFunctionForwarder serviceFunctionForwarder) {
        // TODO: this method is almost literally copied from the previous
        // version of the listener and should be reviewed.

        /*
         * Before removing RSPs used by this Service Function, we need to remove
         * all references in the SFF/SF operational trees
         */
        SffName serviceFunctionForwarderName = serviceFunctionForwarder.getName();
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI
                .readSffState(serviceFunctionForwarderName);
        List<RspName> rspList = new ArrayList<>();
        if (sffServicePathList != null && !sffServicePathList.isEmpty()) {
            if (SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderState(serviceFunctionForwarderName)) {
                for (SffServicePath sffServicePath : sffServicePathList) {
                    // TODO Bug 4495 - RPCs hiding heuristics using Strings -
                    // alagalah

                    RspName rspName = new RspName(sffServicePath.getName().getValue());
                    // XXX Another example of Method Overloading confusion
                    // brought about
                    // by Strings
                    SfcProviderServiceFunctionAPI
                            .deleteServicePathFromServiceFunctionState(new SfpName(rspName.getValue()));
                    rspList.add(rspName);
                }
                SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspList);
            } else {
                LOG.error("{}: Failed to delete Service Function Forwarder {} operational state",
                        serviceFunctionForwarderName);
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
