/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.utils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;

import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;

import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcPotNetconfReaderWriterAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotNetconfReaderWriterAPI.class);
    protected static volatile MountPointService mountPointService = null;
    public static final InstanceIdentifier<Topology> NETCONF_TOPO_IID =
        InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
        new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    /* Reads node information from the operational datastore */
    public static NetconfNode readNode(NodeId nodeId) {
        try {
            Node topoNode = SfcDataStoreAPI.readTransactionAPI(NETCONF_TOPO_IID.
                            child(Node.class, new NodeKey(nodeId)),
                            LogicalDatastoreType.OPERATIONAL);

            return topoNode.getAugmentation(NetconfNode.class);
        } catch (NullPointerException e) {
            LOG.warn("iOAM:PoT:SB:Node {} does not exist", nodeId.getValue(), e);
        }
        return null;
    }

    /* Put method */
    public static <T extends DataObject> boolean put(NodeId nodeId,
            LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<T> iid, T dataObject) {
        boolean ret;

        if (mountPointService == null) {
            mountPointService = SfcPotNetconfDataProvider.getNetconfDataProvider().
                getMountService();
        }

        try {
            Optional<MountPoint> mountPointOptional =
                mountPointService.getMountPoint(NETCONF_TOPO_IID.child(Node.class,
                new NodeKey(nodeId)));
            DataBroker broker = mountPointOptional.get().getService(DataBroker.class).get();

            WriteTransaction tx = broker.newWriteOnlyTransaction();
            tx.put(logicalDatastoreType, iid, dataObject);
            CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
            future.checkedGet();

            ret = true;
        } catch (Exception e) {
            LOG.warn("iOAM:PoT:SB:Netconf put to nodeid:{} failed:", nodeId, e);
            ret = false;
        }

        return ret;
    }

    /* Merge method */
    public static <T extends DataObject> boolean merge(NodeId nodeId,
            LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<T> iid, T dataObject) {
        boolean ret;

        if (mountPointService == null) {
            mountPointService = SfcPotNetconfDataProvider.getNetconfDataProvider().
                getMountService();
        }
        try {
            Optional<MountPoint> mountPointOptional =
                mountPointService.getMountPoint(NETCONF_TOPO_IID.child(Node.class,
                new NodeKey(nodeId)));
            DataBroker broker = mountPointOptional.get().getService(DataBroker.class).get();

            WriteTransaction tx = broker.newWriteOnlyTransaction();
            tx.merge(logicalDatastoreType, iid, dataObject);
            CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
            future.checkedGet();

            ret = true;
        } catch (Exception e) {
            LOG.warn("iOAM:PoT:SB:Netconf merge to nodeid:{} failed:", nodeId, e);
            ret = false;
        }

        return ret;
    }

    /* Delete method */
    public static <T extends DataObject> boolean delete(NodeId nodeId,
            LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<T> iid) {
        boolean ret;

        if (mountPointService == null) {
            mountPointService = SfcPotNetconfDataProvider.getNetconfDataProvider().
                getMountService();
        }
        try {
            Optional<MountPoint> mountPointOptional =
                mountPointService.getMountPoint(NETCONF_TOPO_IID.child(Node.class,
                new NodeKey(nodeId)));
            DataBroker broker = mountPointOptional.get().getService(DataBroker.class).get();

            WriteTransaction tx = broker.newWriteOnlyTransaction();
            tx.delete(logicalDatastoreType, iid);
            CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
            future.checkedGet();

            ret = true;
        } catch (Exception e) {
            LOG.warn("iOAM:PoT:SB:Netconf delete to nodeid:{} failed:", nodeId, e);
            ret = false;
        }

        return ret;
    }
}
