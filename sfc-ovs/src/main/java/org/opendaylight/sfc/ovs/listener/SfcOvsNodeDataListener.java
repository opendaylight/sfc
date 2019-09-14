/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ovs.listener;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataChangeListener attached to the OVSDB southbound operational datastore.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */
@Singleton
public class SfcOvsNodeDataListener extends AbstractSyncDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsNodeDataListener.class);

    private final DataBroker dataBroker;

    @Inject
    public SfcOvsNodeDataListener(final DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class));
        this.dataBroker = dataBroker;
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node node) {
        LOG.debug("Created OVS Node: {}", node.toString());

        /*
         * NODE CREATION When user puts SFF into config DS, reading from
         * topology is involved to write OVSDB bridge and termination point
         * augmentations into config DS. Created data are handled because user
         * might put SFF into config DS before topology in operational DS gets
         * populated.
         */
        OvsdbNodeAugmentation ovsdbNodeAugmentation = node.augmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNodeAugmentation != null) {
            ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
            if (connectionInfo != null) {
                runAddOvsdbAugmentations(connectionInfo);
            }
        }

        /*
         * This is needed to get the ovs bridge OpenFlow DPID if an SFF is added
         * before the ovs bridge has been created. Its not enough to do this in
         * the SfcOvsSffEntryDataListener, since the bridge may not have been
         * created in the operational data store when that listener is executed.
         * If the bridge is created when the SFF is created, then the bridge will
         * definitely not be in the operational data store yet, so it will be
         * handled here. Only do this if the newly created ovs-bridge has the
         * DPID, it may need to be obtained in the ovs-bridge update.
         */
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.augmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridgeAugmentation != null) {
            if (ovsdbBridgeAugmentation.getBridgeName() != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
                LOG.info("SfcOvsNodeDataListener::add() bridge name [{}] DPID [{}]",
                        ovsdbBridgeAugmentation.getBridgeName(),
                        ovsdbBridgeAugmentation.getDatapathId());
                runSffOvsBridgeAugmentOpenflowNodeId(ovsdbBridgeAugmentation);
            }
        }
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node node) {
        /*
         * NODE UPDATE and NODE DELETE This case would mean, that user has
         * modified vSwitch state directly by ovs command, which is not handled
         * yet. Other modifications should be done in config DS.
         */
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node originalNode,
                       @NonNull Node updatedNode) {
        /*
         * NODE UPDATE and NODE DELETE This case would mean, that user has
         * modified vSwitch state directly by ovs command, which is not handled
         * yet. Other modifications should be done in config DS.
         */

        /* When a new bridge is modified, and the DPID is set, add it to the corresponding SFF, if there is one */
        LOG.debug("SfcOvsNodeDataListener::update()");
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = updatedNode.augmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridgeAugmentation != null) {
            if (ovsdbBridgeAugmentation.getBridgeName() != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
                LOG.debug("SfcOvsNodeDataListener::update() bridge name [{}] DPID [{}]",
                        ovsdbBridgeAugmentation.getBridgeName().getValue(),
                        ovsdbBridgeAugmentation.getDatapathId().getValue());
                runSffOvsBridgeAugmentOpenflowNodeId(ovsdbBridgeAugmentation);
            }
        }
    }

    private void runAddOvsdbAugmentations(ConnectionInfo connectionInfo) {
        ListenableFuture<Optional<ServiceFunctionForwarders>> exitsingSffs = readServiceFunctionForwarders();

        Futures.addCallback(exitsingSffs, new FutureCallback<Optional<ServiceFunctionForwarders>>() {
            @Override
            public void onSuccess(Optional<ServiceFunctionForwarders> optionalSffs) {
                if (optionalSffs != null && optionalSffs.isPresent()) {
                    ServiceFunctionForwarder sff = SfcOvsUtil.findSffByIp(optionalSffs.get(),
                            connectionInfo.getRemoteIp());
                    if (sff != null) {
                        LOG.info("SfcOvsNodeDataListener will create the necessary entities for SFF [{}]",
                                sff.getName().getValue());
                        SfcOvsSffEntryDataListener.addOvsdbAugmentations(sff);
                    }
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Failed to read SFFs from data store runAddOvsdbAugmentations.");
            }
        }, MoreExecutors.directExecutor());
    }

    private void runSffOvsBridgeAugmentOpenflowNodeId(OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        ListenableFuture<Optional<ServiceFunctionForwarders>> exitsingSffs = readServiceFunctionForwarders();
        Futures.addCallback(exitsingSffs, new FutureCallback<Optional<ServiceFunctionForwarders>>() {
            @Override
            public void onSuccess(Optional<ServiceFunctionForwarders> optionalSffs) {
                if (optionalSffs == null || !optionalSffs.isPresent()) {
                    LOG.info("SFFs empty in runSffOvsBridgeAugOpenflowNodeId.");
                    return;
                }

                OvsdbNodeAugmentation managerNode = SfcOvsUtil.getManagerNodeByBridgeNode(ovsdbBridgeAugmentation);
                if (managerNode == null) {
                    LOG.warn("Cant get managing OVS node in runSffOvsBridgeAugOpenflowNodeId.");
                    return;
                }

                ConnectionInfo connectionInfo = managerNode.getConnectionInfo();
                if (connectionInfo == null || connectionInfo.getRemoteIp() == null) {
                    LOG.warn("Managing OVS node does not have connectionInfo available");
                    return;
                }

                OvsdbBridgeName bridgeName = ovsdbBridgeAugmentation.getBridgeName();
                ServiceFunctionForwarder sff = SfcOvsUtil.findSffByIp(optionalSffs.get(), connectionInfo.getRemoteIp());
                if (sff == null) {
                    LOG.info("There are no SFFs created on this OVS bridge [{}]", bridgeName);
                    return;
                }

                SffOvsBridgeAugmentation sffOvsBridge = sff.augmentation(SffOvsBridgeAugmentation.class);
                if (sffOvsBridge == null || sffOvsBridge.getOvsBridge() == null) {
                    LOG.debug("SFF [{}] does not have sffOvsBridgeAugmentation", sff.getName().getValue());
                    return;
                }

                // If the SFF already has the OpenFlow NodeId, if its the same, then nothing to do here
                DatapathId dpid = ovsdbBridgeAugmentation.getDatapathId();
                String openFlowNodeId = SfcOvsUtil.getOpenflowNodeIdFromDpid(dpid.getValue());
                if (sffOvsBridge.getOvsBridge().getOpenflowNodeId() != null) {
                    if (openFlowNodeId.equals(sffOvsBridge.getOvsBridge().getOpenflowNodeId())) {
                        LOG.debug("SFF [{}] already has the same OpenFlowNodeId set", sff.getName().getValue());
                        return;
                    }
                }

                // Check that the SFF bridge name matches this bridge name
                if (!sffOvsBridge.getOvsBridge().getBridgeName().equals(bridgeName.getValue())) {
                    LOG.debug("SFF [{}] is not on this bridge [{}]", sff.getName().getValue(), bridgeName.getValue());
                    return;
                }

                LOG.info("Creating and storing augmentedSFF [{}] for bridge [{}] with DPID [{}] OpenFlow NodeId [{}]",
                        sff.getName().getValue(), bridgeName.getValue(), dpid.getValue(), openFlowNodeId);
                ServiceFunctionForwarder augmentedSff = SfcOvsUtil.augmentSffWithOpenFlowNodeId(
                        sff, SfcOvsUtil.getOpenflowNodeIdFromDpid(dpid.getValue()));
                InstanceIdentifier<SffOvsBridgeAugmentation> sffOvsBridgeAugIid = InstanceIdentifier
                        .builder(ServiceFunctionForwarders.class)
                        .child(ServiceFunctionForwarder.class, sff.key())
                        .augmentation(SffOvsBridgeAugmentation.class).build();

                SfcDataStoreAPI.writePutTransactionAPI(sffOvsBridgeAugIid,
                        augmentedSff.augmentation(SffOvsBridgeAugmentation.class),
                        LogicalDatastoreType.CONFIGURATION);
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Failed to read SFFs from data store from runSffOvsBridgeAugOpenflowNodeId.");
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Optional<ServiceFunctionForwarders>> readServiceFunctionForwarders() {
        ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<ServiceFunctionForwarders> sffIid = InstanceIdentifier
                .builder(ServiceFunctionForwarders.class).build();
        return transaction.read(LogicalDatastoreType.CONFIGURATION, sffIid);
    }
}
