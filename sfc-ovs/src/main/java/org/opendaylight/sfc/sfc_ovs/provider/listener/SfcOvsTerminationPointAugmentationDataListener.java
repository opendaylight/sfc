/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the OVSDB southbound operational datastore
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsToSffMappingAPI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOvsTerminationPointAugmentationDataListener extends SfcOvsAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsTerminationPointAugmentationDataListener.class);

    public static final InstanceIdentifier<OvsdbTerminationPointAugmentation> OVSDB_TERMINATION_POINT_AUGMENTATION_INSTANCE_IDENTIFIER =
            InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                    .child(Node.class)
                    .child(TerminationPoint.class)
                    .augmentation(OvsdbTerminationPointAugmentation.class);

    public SfcOvsTerminationPointAugmentationDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OVSDB_TERMINATION_POINT_AUGMENTATION_INSTANCE_IDENTIFIER);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        // OVSDB TERMINATION POINT CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof OvsdbTerminationPointAugmentation) {
                OvsdbTerminationPointAugmentation terminationPointAugmentation =
                        (OvsdbTerminationPointAugmentation) entry.getValue();

                InstanceIdentifier<OvsdbTerminationPointAugmentation> terminationPointAugmentationIID =
                        (InstanceIdentifier<OvsdbTerminationPointAugmentation>) entry.getKey();

                LOG.debug("\nCreated OvsdbTerminationPointAugmentation: {}", terminationPointAugmentation.toString());

                SfcProviderServiceForwarderAPI.putServiceFunctionForwarderExecutor(
                        SfcOvsToSffMappingAPI.getServiceForwarderForOvsdbTerminationPoint(terminationPointAugmentationIID));
            }
        }

        // OVSDB TERMINATION POINT UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof OvsdbTerminationPointAugmentation)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                OvsdbTerminationPointAugmentation terminationPointAugmentation =
                        (OvsdbTerminationPointAugmentation) entry.getValue();

                InstanceIdentifier<OvsdbTerminationPointAugmentation> terminationPointAugmentationIID =
                        (InstanceIdentifier<OvsdbTerminationPointAugmentation>) entry.getKey();

                LOG.debug("\nUpdated OvsdbTerminationPointAugmentation: {}", terminationPointAugmentation.toString());

                SfcProviderServiceForwarderAPI.putServiceFunctionForwarderExecutor(
                        SfcOvsToSffMappingAPI.getServiceForwarderForOvsdbTerminationPoint(terminationPointAugmentationIID));
            }
        }

        // OVSDB TERMINATION POINT DELETE
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof OvsdbTerminationPointAugmentation) {
                OvsdbTerminationPointAugmentation terminationPointAugmentation =
                        (OvsdbTerminationPointAugmentation) dataObject;

                InstanceIdentifier<OvsdbTerminationPointAugmentation> terminationPointAugmentationIID =
                        (InstanceIdentifier<OvsdbTerminationPointAugmentation>) instanceIdentifier;

                LOG.debug("\nDeleted OvsdbTerminationPointAugmentation: {}", terminationPointAugmentation.toString());

                SfcProviderServiceForwarderAPI.putServiceFunctionForwarderExecutor(
                        SfcOvsToSffMappingAPI.getServiceForwarderForOvsdbTerminationPoint(terminationPointAugmentationIID));
            }
        }
        printTraceStop(LOG);
    }

}
