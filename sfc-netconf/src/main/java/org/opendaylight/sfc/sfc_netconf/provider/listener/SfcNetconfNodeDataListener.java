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
 * @version 0.1
 * @since       2015-02-13
 */

package org.opendaylight.sfc.sfc_netconf.provider.listener;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SfcNetconfNodeDataListener extends SfcNetconfAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfNodeDataListener.class);

    public static final InstanceIdentifier<Topology> NETCONF_TOPO_IID =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    public SfcNetconfNodeDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(NETCONF_TOPO_IID);
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
            if (entry.getValue() instanceof Node) {
                Node node = (Node) entry.getValue();
                String nodeName = node.getNodeId().getValue();
                LOG.debug("\nCreated OvsdbNodeAugmentation: {}", node.toString());

            }
        }


        // EXAMPLE: New node discovery
        // React to new Netconf nodes added to the Netconf topology or existing
        // Netconf nodes deleted from the Netconf topology
        for ( Map.Entry<InstanceIdentifier<?>,
                DataObject> entry : change.getCreatedData().entrySet()) {
            if (entry.getKey().getTargetType() == NetconfNode.class) {
                // We have a Netconf device
                NetconfNode nnode = (NetconfNode)entry.getValue();
                NetconfNodeFields.ConnectionStatus csts = nnode.getConnectionStatus();
                if (csts == NetconfNodeFields.ConnectionStatus.Connected) {
                    List<String> capabilities = nnode.getAvailableCapabilities()
                            .getAvailableCapability();
                    LOG.info("Capabilities: {}", capabilities);
                }
            }
        }

        // EXAMPLE: Status change in existing node(s)
        // React to data changes in Netconf nodes present in the Netconf
        // topology
        for ( Map.Entry<InstanceIdentifier<?>,
                DataObject> entry : change.getUpdatedData().entrySet()) {
            if (entry.getKey().getTargetType() == NetconfNode.class) {
                // We have a Netconf device
                NetconfNode nnode = (NetconfNode)entry.getValue();
                NetconfNodeFields.ConnectionStatus csts = nnode.getConnectionStatus();
                if (csts == NetconfNodeFields.ConnectionStatus.Connected) {
                    List<String> capabilities = nnode
                            .getAvailableCapabilities()
                            .getAvailableCapability();
                    LOG.info("Capabilities: {}", capabilities);
                }
            }
        }

/*       // NODE UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof OvsdbNodeAugmentation)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                OvsdbNodeAugmentation updatedOvsdbNodeAugmentation = (OvsdbNodeAugmentation) entry.getValue();
                LOG.debug("\nModified OvsdbNodeAugmentation : {}", updatedOvsdbNodeAugmentation.toString());

            }
        }


        // NODE DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof OvsdbNodeAugmentation) {

                OvsdbNodeAugmentation deletedOvsdbNodeAugmentation = (OvsdbNodeAugmentation) dataObject;
                LOG.debug("\nDeleted OvsdbNodeAugmentation: {}", deletedOvsdbNodeAugmentation.toString());

            }
        }*/
        printTraceStop(LOG);
    }


}
