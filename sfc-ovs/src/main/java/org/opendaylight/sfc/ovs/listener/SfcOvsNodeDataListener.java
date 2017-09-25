/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
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
 * @since 2015-02-13
 */

package org.opendaylight.sfc.ovs.listener;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOvsNodeDataListener extends AbstractDataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsNodeDataListener.class);
    private final DataBroker dataBroker;
    private ListenerRegistration<SfcOvsNodeDataListener> listenerRegistration;

    public SfcOvsNodeDataListener(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        LOG.debug("Initializing...");
        registerListeners();
    }

    private void registerListeners() {
        final DataTreeIdentifier<Node> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class));
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
    protected void add(Node createdNode) {
        /*
         * NODE CREATION When user puts SFF into config DS, reading from
         * topology is involved to write OVSDB bridge and termination point
         * augmentations into config DS. Created data are handled because user
         * might put SFF into config DS before topology in operational DS gets
         * populated.
         */

        LOG.debug("\nCreated OVS Node: {}", createdNode.toString());

        OvsdbNodeAugmentation ovsdbNodeAugmentation = createdNode.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNodeAugmentation != null) {
            final ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
            if (connectionInfo != null) {
                CheckedFuture<Optional<ServiceFunctionForwarders>, ReadFailedException>
                    exitsingSffs = readServiceFunctionForwarders();
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
                        LOG.error("Failed to read SFFs from data store.");
                    }
                }, MoreExecutors.directExecutor());
            }
        }
    }

    @Override
    protected void remove(Node deletedNode) {
        /*
         * NODE UPDATE and NODE DELETE This case would mean, that user has
         * modified vSwitch state directly by ovs command, which is not handled
         * yet. Other modifications should be done in config DS.
         */
    }

    @Override
    protected void update(Node originalNode, Node updatedNode) {
        /*
         * NODE UPDATE and NODE DELETE This case would mean, that user has
         * modified vSwitch state directly by ovs command, which is not handled
         * yet. Other modifications should be done in config DS.
         */
    }

    private CheckedFuture<Optional<ServiceFunctionForwarders>, ReadFailedException> readServiceFunctionForwarders() {
        ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<ServiceFunctionForwarders> sffIid = InstanceIdentifier
                .builder(ServiceFunctionForwarders.class).build();
        return transaction.read(LogicalDatastoreType.CONFIGURATION, sffIid);
    }

}
