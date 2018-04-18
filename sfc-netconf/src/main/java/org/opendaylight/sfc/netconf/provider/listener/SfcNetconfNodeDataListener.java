/*
 * Copyright (c) 2015, 2018 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the OVSDB southbound operational datastore
 * <p>
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */

package org.opendaylight.sfc.netconf.provider.listener;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.netconf.provider.api.SfcNetconfServiceForwarderAPI;
import org.opendaylight.sfc.netconf.provider.api.SfcNetconfServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SfcNetconfNodeDataListener extends AbstractSyncDataTreeChangeListener<Node> {

    private static final String CONTROLLER_CONFIG = "controller-config";

    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfNodeDataListener.class);

    private final SfcNetconfServiceFunctionAPI sfcNetconfServiceFunctionAPI;

    @Inject
    public SfcNetconfNodeDataListener(DataBroker dataBroker,
                                      SfcNetconfServiceFunctionAPI sfcNetconfServiceFunctionAPI) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())))
                .child(Node.class));
        this.sfcNetconfServiceFunctionAPI = sfcNetconfServiceFunctionAPI;
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<Node> instanceIdentifier, @Nonnull Node newDataObject) {
        // NOOP
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<Node> instanceIdentifier, @Nonnull Node removedDataObject) {
        // NOOP
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<Node> instanceIdentifier, @Nonnull Node originalDataObject,
                       @Nonnull Node updatedDataObject) {
        // React to data changes in Netconf nodes present in the Netconf topology
        netconfNodeUpdated(getNodeId(instanceIdentifier), updatedDataObject.getAugmentation(NetconfNode.class));
    }

    private void netconfNodeUpdated(NodeId nodeId, NetconfNode nnode) {
        String nodeName = nodeId.getValue();

        // We bypass the internal Netconf server
        if (!CONTROLLER_CONFIG.equals(nodeName)) {
            NetconfNodeFields.ConnectionStatus csts = nnode.getConnectionStatus();

            switch (csts) {
                case Connected: {
                    // Fully connected, all services for remote device
                    // available from
                    // MountPointService
                    LOG.debug("NETCONF Node: {} is fully connected", nodeId.getValue());
                    List<AvailableCapability> capabilities = nnode.getAvailableCapabilities().getAvailableCapability();
                    LOG.debug("Capabilities: {}", capabilities);

                    /* Identify it is SF or SFF */
                    if (isServiceFunction(nnode)) { // SF
                        DescriptionInfo descInfo = sfcNetconfServiceFunctionAPI.getServiceFunctionDescription(nodeName);
                        String type = descInfo.getType();
                        if (type == null || type.isEmpty()) {
                            LOG.error("SF type is empty");
                            break;
                        }
                        SftTypeName sfType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName(type))
                                .getType();
                        if (sfType == null) {
                            LOG.error("Invalid SF type {}", type);
                            break;
                        }
                        SfName sfNodeName = new SfName(nodeName);
                        ServiceFunction sf = SfcNetconfServiceFunctionAPI
                                .buildServiceFunctionFromNetconf(sfNodeName, descInfo.getDataPlaneIp(),
                                                                 descInfo.getDataPlanePort(), sfType);
                        if (SfcProviderServiceFunctionAPI.putServiceFunction(sf)) {
                            LOG.info("Successfully created SF from Netconf node {}", nodeName);
                            sfcNetconfServiceFunctionAPI.putServiceFunctionDescription(descInfo, sfNodeName);
                            MonitoringInfo monInfo = sfcNetconfServiceFunctionAPI.getServiceFunctionMonitor(nodeName);
                            if (monInfo != null) {
                                sfcNetconfServiceFunctionAPI.putServiceFunctionMonitor(monInfo, sfNodeName);
                            }
                        } else {
                            LOG.error("Failed to create SF from Netconf node {}", nodeName);
                        }

                        SfDescriptionMonitoringThread monitoringThread = new SfDescriptionMonitoringThread(nodeName);
                        Thread thread = new Thread(monitoringThread);
                        thread.start();
                    } else { // SFF
                        ServiceFunctionForwarder sff = SfcNetconfServiceForwarderAPI
                                .buildServiceForwarderFromNetconf(nodeName, nnode);
                        if (SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff)) {
                            LOG.info("Successfully created SFF from Netconf node {}", nodeName);
                        } else {
                            LOG.error("Failed to create SFF from Netconf node {}", nodeName);
                        }
                    }
                    break;
                }
                case Connecting: {
                    // Connecting state is set initially but netconf
                    // device can get back to
                    // it after a disconnect
                    // Note that device could jump back and forth
                    // between connected and
                    // connecting for various reasons:
                    // disconnect from remote device, network
                    // connectivity loss etc.
                    LOG.info("Netconf device disconnected, deleting SFF {}", nodeName);
                    if (SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarder(new SffName(nodeName))) {
                        LOG.info("SFF {} deleted successfully", nodeName);
                    } else {
                        LOG.error("Failed to delete SFF {}", nodeName);
                    }
                    break;
                }
                case UnableToConnect: {
                    // Its over for the device, no more reconnects
                    LOG.info("Unable to connected to Netconf device, deleting SFF {}", nodeName);
                    if (SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarder(new SffName(nodeName))) {
                        LOG.info("SFF {} deleted successfully", nodeName);
                    } else {
                        LOG.error("Failed to delete SFF {}", nodeName);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private NodeId getNodeId(final InstanceIdentifier<?> iid) {
        NodeId nodeId = null;
        for (InstanceIdentifier.PathArgument pathArgument : iid.getPathArguments()) {
            if (pathArgument instanceof InstanceIdentifier.IdentifiableItem<?, ?>) {

                final Identifier<?> key = ((InstanceIdentifier.IdentifiableItem) pathArgument).getKey();
                if (key instanceof NodeKey) {
                    nodeId = ((NodeKey) key).getNodeId();
                }
            }
        }
        return nodeId;
    }

    private static boolean isServiceFunction(NetconfNode netconfNode) {
        boolean ret = false;
        List<AvailableCapability> capabilities = netconfNode.getAvailableCapabilities().getAvailableCapability();
        for (AvailableCapability cap : capabilities) {
            if (cap.getCapability().endsWith("service-function-description-monitor-report")) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    class SfDescriptionMonitoringThread implements Runnable {
        private final String nodeName;

        SfDescriptionMonitoringThread(String nodeName) {
            this.nodeName = nodeName;
        }

        @Override
        public void run() {
            while (true) {
                printTraceStart(LOG);
                MonitoringInfo monInfo = sfcNetconfServiceFunctionAPI.getServiceFunctionMonitor(nodeName);
                if (monInfo != null) {
                    SfName sfNodeName = new SfName(nodeName);
                    sfcNetconfServiceFunctionAPI.putServiceFunctionMonitor(monInfo, sfNodeName);
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    LOG.warn("failed to ....", e);
                }
                printTraceStop(LOG);
            }
        }
    }
}
