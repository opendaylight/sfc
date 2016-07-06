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
 * @since 2015-02-13
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


public class SfcOvsNodeDataListener extends SfcOvsAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsNodeDataListener.class);

    public static final InstanceIdentifier<Node> OVSDB_NODE_AUGMENTATION_INSTANCE_IDENTIFIER = InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class);

    public SfcOvsNodeDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OVSDB_NODE_AUGMENTATION_INSTANCE_IDENTIFIER);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        registerAsDataChangeListener(DataBroker.DataChangeScope.BASE);
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  Node)
            {
                Node originalNode = (Node) entry.getValue();
                LOG.debug("\nOriginal Node: {}", originalNode.toString());
            }
        }

        /* NODE CREATION
         * When user puts SFF into config DS, reading from topology is involved to
         * write OVSDB bridge and termination point augmentations into config DS.
         * Created data are handled because user might put SFF into config DS
         * before topology in operational DS gets populated.
         */
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {

            if (entry.getValue() instanceof Node) {
                Node createdNode = (Node) entry.getValue();
                LOG.debug("\nCreated OVS Node: {}", createdNode.toString());

                OvsdbNodeAugmentation ovsdbNodeAugmentation = createdNode.getAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeAugmentation != null) {
                    final ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
                    if (connectionInfo != null) {
                        CheckedFuture<Optional<ServiceFunctionForwarders>, ReadFailedException> exitsingSffs = readServiceFunctionForwarders();
                        Futures.addCallback(exitsingSffs, new FutureCallback<Optional<ServiceFunctionForwarders>>() {

                            @Override
                            public void onSuccess(Optional<ServiceFunctionForwarders> optionalSffs) {
                                if (optionalSffs.isPresent()) {
                                    ServiceFunctionForwarder sff = findSffByIp(optionalSffs.get(), connectionInfo.getRemoteIp());
                                    if(sff != null) {
                                        SfcOvsSffEntryDataListener.addOvsdbAugmentations(sff,
                                                opendaylightSfc.getExecutor());
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                LOG.error("Failed to read SFFs from data store.");
                            }
                        });
                    }
                }
            }
        }
        /* NODE UPDATE and NODE DELETE
         * This case would mean, that user has modified vSwitch state
         * directly by ovs command, which is not handled yet.
         * Other modifications should be done in config DS.
         */
        printTraceStop(LOG);
    }

    private CheckedFuture<Optional<ServiceFunctionForwarders>, ReadFailedException> readServiceFunctionForwarders() {
        ReadTransaction rTx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<ServiceFunctionForwarders> sffIid = InstanceIdentifier.builder(
                ServiceFunctionForwarders.class).build();
        return rTx.read(LogicalDatastoreType.CONFIGURATION, sffIid);
    }

    private ServiceFunctionForwarder findSffByIp(ServiceFunctionForwarders sffs, final IpAddress remoteIp) {
        List<ServiceFunctionForwarder> serviceFunctionForwarders = sffs.getServiceFunctionForwarder();
        if (serviceFunctionForwarders != null && !serviceFunctionForwarders.isEmpty())
            for (ServiceFunctionForwarder sff : serviceFunctionForwarders) {
                List<SffDataPlaneLocator> sffDataPlaneLocator = sff.getSffDataPlaneLocator();
                for (SffDataPlaneLocator sffLocator : sffDataPlaneLocator) {
                    LocatorType locatorType = sffLocator.getDataPlaneLocator().getLocatorType();
                    if (locatorType instanceof Ip) {
                        Ip ip = (Ip) locatorType;
                        if (ip.getIp().equals(remoteIp)) {
                            return sff;
                        }
                    }
                }
            }
        return null;
    }
}
