/*
 * Copyright (c) 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SnName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * CRUD operations for {@link ServiceNode} entities.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
public final class SfcProviderServiceNodeAPI {

    private SfcProviderServiceNodeAPI() {
    }

    public static boolean putServiceNode(ServiceNode serviceNode) {
        InstanceIdentifier<ServiceNode> serviceNodeInstanceIdentifier = InstanceIdentifier.builder(ServiceNodes.class)
                .child(ServiceNode.class, serviceNode.getKey()).build();
        return SfcDataStoreAPI.writeMergeTransactionAPI(serviceNodeInstanceIdentifier, serviceNode,
                                                        LogicalDatastoreType.CONFIGURATION);
    }

    public static ServiceNode readServiceNodeByName(SnName serviceNodeName) {
        InstanceIdentifier<ServiceNode> serviceNodeInstanceIdentifier = InstanceIdentifier.builder(ServiceNodes.class)
                .child(ServiceNode.class, new ServiceNodeKey(serviceNodeName)).build();
        return SfcDataStoreAPI.readTransactionAPI(serviceNodeInstanceIdentifier, LogicalDatastoreType.CONFIGURATION);
    }

    public static boolean deleteServiceNodeByName(SnName serviceNodeName) {
        InstanceIdentifier<ServiceNode> serviceNodeInstanceIdentifier = InstanceIdentifier.builder(ServiceNodes.class)
                .child(ServiceNode.class, new ServiceNodeKey(serviceNodeName)).build();
        return SfcDataStoreAPI.deleteTransactionAPI(serviceNodeInstanceIdentifier, LogicalDatastoreType.CONFIGURATION);
    }

    public static ServiceNodes readAllServiceNodes() {
        InstanceIdentifier<ServiceNodes> sfsIID = InstanceIdentifier.builder(ServiceNodes.class).build();
        return SfcDataStoreAPI.readTransactionAPI(sfsIID, LogicalDatastoreType.CONFIGURATION);
    }
}
