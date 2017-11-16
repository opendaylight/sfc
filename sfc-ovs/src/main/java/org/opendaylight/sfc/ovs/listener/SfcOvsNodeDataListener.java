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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void add(@Nonnull Node node) {
        /*
         * NODE CREATION When user puts SFF into config DS, reading from
         * topology is involved to write OVSDB bridge and termination point
         * augmentations into config DS. Created data are handled because user
         * might put SFF into config DS before topology in operational DS gets
         * populated.
         */
        LOG.debug("Created OVS Node: {}", node.toString());

        OvsdbNodeAugmentation ovsdbNodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNodeAugmentation != null) {
            final ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
            if (connectionInfo != null) {
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
                        LOG.error("Failed to read SFFs from data store.");
                    }
                }, MoreExecutors.directExecutor());
            }
        }
    }

    @Override
    public void remove(@Nonnull Node node) {
        /*
         * NODE UPDATE and NODE DELETE This case would mean, that user has
         * modified vSwitch state directly by ovs command, which is not handled
         * yet. Other modifications should be done in config DS.
         */
    }

    @Override
    public void update(@Nonnull Node originalNode, @Nonnull Node updatedNode) {
        /*
         * NODE UPDATE and NODE DELETE This case would mean, that user has
         * modified vSwitch state directly by ovs command, which is not handled
         * yet. Other modifications should be done in config DS.
         */
    }

    private ListenableFuture<Optional<ServiceFunctionForwarders>> readServiceFunctionForwarders() {
        ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<ServiceFunctionForwarders> sffIid = InstanceIdentifier
                .builder(ServiceFunctionForwarders.class).build();
        return transaction.read(LogicalDatastoreType.CONFIGURATION, sffIid);
    }
}
