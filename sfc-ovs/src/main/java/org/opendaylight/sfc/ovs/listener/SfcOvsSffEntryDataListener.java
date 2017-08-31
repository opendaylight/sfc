/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others. All rights reserved.
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

package org.opendaylight.sfc.ovs.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.ovs.api.SfcSffToOvsMappingAPI;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
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

        // Since in most cases, the OvsdbNode was not created by SFC, lets not
        // delete it

        // Delete the VXGPE port

        // Iterate the SFF DPLs
        NodeId ovsdbBridgeNodeId = SfcOvsUtil.getOvsdbAugmentationNodeIdBySff(deletedServiceFunctionForwarder);
        for (SffDataPlaneLocator sffDpl : deletedServiceFunctionForwarder.getSffDataPlaneLocator()) {

            // Only delete the port if this SFF is OVS augmented and the
            // transport is VxGpe
            SffOvsLocatorOptionsAugmentation sffOvsOptions = sffDpl
                    .getAugmentation(SffOvsLocatorOptionsAugmentation.class);
            if (sffOvsOptions != null && sffDpl.getDataPlaneLocator().getTransport().equals(VxlanGpe.class)) {
                // delete OvsdbTerminationPoint
                SfcOvsUtil.deleteOvsdbTerminationPoint(
                        SfcOvsUtil.buildOvsdbTerminationPointIID(ovsdbBridgeNodeId, sffDpl.getName().getValue()));
            }
        }
    }

    @Override
    protected void update(ServiceFunctionForwarder originalServiceFunctionForwarder,
            ServiceFunctionForwarder updatedServiceFunctionForwarder) {
        // Notice: Adding an SffDpl to an existing SFF will trigger this
        // listener, not a separate SffDplListener
        // which means 2 different listeners are not needed. This was tested
        // with the following command for an existing SFF:
        // curl -i -H "Content-Type: application/json" --data '{
        // "sff-data-plane-locator": [ { "name": "vxgpe1", "data-plane-locator":
        // { "ip": "192.168.1.54", "port": 6633, "transport":
        // "service-locator:vxlan-gpe" } } ] }'
        // -X PUT --user admin:admin
        // http://localhost:${PORT}/restconf/config/service-function-forwarder:service-function-forwarders/service-function-forwarder/sff1/sff-data-plane-locator/vxgpe1

        LOG.info("\nModified Service Function Forwarder : {}", updatedServiceFunctionForwarder.toString());
        // rewrite augmentations for serviceFunctionForwarder
        addOvsdbAugmentations(updatedServiceFunctionForwarder);
    }

    /**
     * Add OVSDB augmentations.
     *
     * @param sff
     *            ServiceFunctionForwarder Object.
     * @param executor
     *            ExecutorService Object
     */
    static void addOvsdbAugmentations(ServiceFunctionForwarder sff) {

        OvsdbBridgeAugmentation ovsdbBridge = SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(sff);

        if (ovsdbBridge != null) {
            // put Bridge
            SfcOvsUtil.putOvsdbBridge(ovsdbBridge);

            // put Termination Points
            SfcOvsUtil.putOvsdbTerminationPoints(ovsdbBridge, sff.getSffDataPlaneLocator());
        }
    }
}
