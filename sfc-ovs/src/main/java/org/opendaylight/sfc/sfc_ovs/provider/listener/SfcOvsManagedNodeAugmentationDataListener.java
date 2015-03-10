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

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbManagedNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SfcOvsManagedNodeAugmentationDataListener extends SfcOvsAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsManagedNodeAugmentationDataListener.class);

    public static final InstanceIdentifier<OvsdbManagedNodeAugmentation> OVSDB_MANAGED_NODE_AUGMENTATION_INSTANCE_IDENTIFIER =
            InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                    .child(Node.class)
                    .augmentation(OvsdbManagedNodeAugmentation.class);

    public SfcOvsManagedNodeAugmentationDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OVSDB_MANAGED_NODE_AUGMENTATION_INSTANCE_IDENTIFIER);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

/*        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation originalOvsdbNodeAugmentation = (OvsdbNodeAugmentation) entry.getValue();
                LOG.debug("\nOriginal Rendered Service Path: {}", originalOvsdbNodeAugmentation.toString());
            }
        }*/

        // Node CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {

            if (entry.getValue() instanceof OvsdbManagedNodeAugmentation) {
                OvsdbManagedNodeAugmentation ovsdbManagedNodeAugmentation = (OvsdbManagedNodeAugmentation) entry.getValue();
                LOG.debug("\nCreated OvsdbManagedNodeAugmentation: {}", ovsdbManagedNodeAugmentation.toString());
                SfcProviderServiceForwarderAPI.putServiceFunctionForwarderExecutor(
                        SfcOvsServiceForwarderAPI.buildServiceForwarderFromOvsdbBridge(ovsdbManagedNodeAugmentation));

            }
        }

        // NODE UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof OvsdbManagedNodeAugmentation)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                OvsdbManagedNodeAugmentation updatedOvsdbManagedNodeAugmentation = (OvsdbManagedNodeAugmentation) entry.getValue();
                LOG.debug("\nModified OvsdbManagedNodeAugmentation : {}", updatedOvsdbManagedNodeAugmentation.toString());
                SfcProviderServiceForwarderAPI.putServiceFunctionForwarderExecutor(
                        SfcOvsServiceForwarderAPI.buildServiceForwarderFromOvsdbBridge(updatedOvsdbManagedNodeAugmentation));
            }
        }


        // NODE DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof OvsdbManagedNodeAugmentation) {
                OvsdbManagedNodeAugmentation deletedOvsdbManagedNodeAugmentation = (OvsdbManagedNodeAugmentation) dataObject;
                LOG.debug("\nDeleted OvsdbManagedNodeAugmentation: {}", deletedOvsdbManagedNodeAugmentation.toString());
                SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderExecutor(
                        SfcOvsServiceForwarderAPI.getServiceForwarderNameFromOvsdbBridge(deletedOvsdbManagedNodeAugmentation));
            }
        }
        printTraceStop(LOG);
    }

}
