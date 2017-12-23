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

import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.ovs.api.SfcSffToOvsMappingAPI;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SfcOvsSffEntryDataListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionForwarder> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsSffEntryDataListener.class);

    public SfcOvsSffEntryDataListener(final DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class));
    }

    @Override
    public void add(@Nonnull ServiceFunctionForwarder serviceFunctionForwarder) {
        LOG.info("Created Service Function Forwarder: {}", serviceFunctionForwarder.toString());
        // add augmentations for serviceFunctionForwarder
        addOvsdbAugmentations(serviceFunctionForwarder);
        setSffOvsBridgeAugOpenflowNodeId(serviceFunctionForwarder);
    }

    @Override
    public void remove(@Nonnull ServiceFunctionForwarder deletedServiceFunctionForwarder) {
        LOG.info("Deleted Service Function Forwarder: {}", deletedServiceFunctionForwarder.toString());
        deleteOvsdbAugmentations(deletedServiceFunctionForwarder);
    }

    @Override
    public void update(@Nonnull ServiceFunctionForwarder originalServiceFunctionForwarder,
                       @Nonnull ServiceFunctionForwarder updatedServiceFunctionForwarder) {
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

        LOG.info("Modified Service Function Forwarder : {}", updatedServiceFunctionForwarder.toString());
        // rewrite augmentations for serviceFunctionForwarder
        addOvsdbAugmentations(updatedServiceFunctionForwarder);
    }

    /**
     * Add OVSDB augmentations.
     *
     * @param sff
     *            ServiceFunctionForwarder Object.
     */
    public static void addOvsdbAugmentations(ServiceFunctionForwarder sff) {
        OvsdbBridgeAugmentation ovsdbBridge = SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(sff);

        if (ovsdbBridge != null) {
            // put Bridge
            SfcOvsUtil.putOvsdbBridge(ovsdbBridge);

            // put Termination Points
            SfcOvsUtil.putOvsdbTerminationPoints(ovsdbBridge, sff.getSffDataPlaneLocator());
        }
    }

    /**
     * Delete OVSDB augmentations.
     *
     * @param sff
     *            ServiceFunctionForwarder Object.
     */
    private void deleteOvsdbAugmentations(ServiceFunctionForwarder sff) {
        // Since in most cases, the OvsdbNode was not created by SFC, lets not
        // delete it
        // Iterate the SFF DPLs and delete the VXGPE port
        final List<SffDataPlaneLocator> sffDataPlaneLocatorList = sff.getSffDataPlaneLocator();
        if (sffDataPlaneLocatorList == null || sffDataPlaneLocatorList.isEmpty()) {
            return;
        }
        NodeId ovsdbBridgeNodeId = SfcOvsUtil.getOvsdbAugmentationNodeIdBySff(sff);
        for (SffDataPlaneLocator sffDpl : sffDataPlaneLocatorList) {
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

    /**
     * Store the Openflow NodeId in the SFF OvsBridge Augmentation.
     * This makes it easier to get the Openflow NodeId from other parts of the SFC code.
     *
     * @param sff - The SFF to modify
     */
    static void setSffOvsBridgeAugOpenflowNodeId(ServiceFunctionForwarder sff) {
        ServiceFunctionForwarder augmentedSff = SfcOvsUtil.augmentSffWithOpenFlowNodeId(sff);
        InstanceIdentifier<SffOvsBridgeAugmentation> sffOvsBridgeAugIid = InstanceIdentifier
                .builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, sff.getKey())
                .augmentation(SffOvsBridgeAugmentation.class).build();
        LOG.info("SfcOvsSffEntryDataListener::setSffOvsBridgeAugOpenflowNodeId OpenFlow ID: [{}]",
                augmentedSff.getAugmentation(SffOvsBridgeAugmentation.class).getOvsBridge().getOpenflowNodeId());

        SfcDataStoreAPI.writePutTransactionAPI(sffOvsBridgeAugIid,
                augmentedSff.getAugmentation(SffOvsBridgeAugmentation.class),
                LogicalDatastoreType.CONFIGURATION);
    }
}
