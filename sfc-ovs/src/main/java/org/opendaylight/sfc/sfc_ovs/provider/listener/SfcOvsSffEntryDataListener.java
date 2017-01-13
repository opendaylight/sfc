/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the SFC SFF config datastore
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcSffToOvsMappingAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOvsSffEntryDataListener extends AbstractDataTreeChangeListener<ServiceFunctionForwarder> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsSffEntryDataListener.class);
    private final DataBroker dataBroker;
    private ListenerRegistration<SfcOvsSffEntryDataListener> listenerRegistration;

    // TODO is this necessary????
    protected static ExecutorService executor = Executors.newFixedThreadPool(5);


    public SfcOvsSffEntryDataListener(final DataBroker dataBroker) {
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
        LOG.info("\nCreated Service Function Forwarder: {}", serviceFunctionForwarder.toString());
        // add augmentations for serviceFunctionForwarder
        addOvsdbAugmentations(serviceFunctionForwarder);
    }

    @Override
    protected void remove(ServiceFunctionForwarder deletedServiceFunctionForwarder) {
        LOG.info("\nDeleted Service Function Forwarder: {}", deletedServiceFunctionForwarder.toString());

        // delete OvsdbNode
        SfcOvsUtil.deleteOvsdbNode(SfcOvsUtil.buildOvsdbNodeIID(deletedServiceFunctionForwarder.getName().getValue()),
                executor);

        NodeId ovsdbBridgeNodeId = SfcOvsUtil.getOvsdbAugmentationNodeIdBySff(deletedServiceFunctionForwarder);

        // Delete the VXGPE port

        // Iterate the SFF DPLs
        for(SffDataPlaneLocator sffDpl : deletedServiceFunctionForwarder.getSffDataPlaneLocator()) {

            // Only delete the port if this SFF is OVS augmented and the transport is VxGpe
            SffOvsLocatorOptionsAugmentation sffOvsOptions = sffDpl.getAugmentation(SffOvsLocatorOptionsAugmentation.class);
            if(sffOvsOptions != null && sffDpl.getDataPlaneLocator().getTransport().equals(VxlanGpe.class)) {
                // delete OvsdbTerminationPoint
                SfcOvsUtil.deleteOvsdbTerminationPoint(
                        SfcOvsUtil.buildOvsdbTerminationPointIID(ovsdbBridgeNodeId, sffDpl.getName().getValue()),
                        executor);
            }
        }
    }

    @Override
    protected void update(ServiceFunctionForwarder originalServiceFunctionForwarder,
            ServiceFunctionForwarder updatedServiceFunctionForwarder) {
        LOG.info("\nModified Service Function Forwarder : {}", updatedServiceFunctionForwarder.toString());
        // rewrite augmentations for serviceFunctionForwarder
        addOvsdbAugmentations(updatedServiceFunctionForwarder);
    }


    /**
     * @param sff ServiceFunctionForwarder Object
     * @param executor ExecutorService Object
     */
    static void addOvsdbAugmentations(ServiceFunctionForwarder sff) {

        OvsdbBridgeAugmentation ovsdbBridge = SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(sff, executor);

        if (ovsdbBridge != null) {
            // put Bridge
            SfcOvsUtil.putOvsdbBridge(ovsdbBridge, executor);

            // put Termination Points
            SfcOvsUtil.putOvsdbTerminationPoints(ovsdbBridge, sff.getSffDataPlaneLocator(), executor);
        }
    }
}
