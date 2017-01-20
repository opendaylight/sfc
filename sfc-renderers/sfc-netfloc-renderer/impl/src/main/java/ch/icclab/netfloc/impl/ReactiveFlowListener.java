/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;
import ch.icclab.netfloc.iface.IMacLearningListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

public class ReactiveFlowListener implements PacketProcessingListener {

	private static final Logger logger = LoggerFactory.getLogger(ReactiveFlowListener.class);
	private List<IMacLearningListener> macLearningListeners = new LinkedList<IMacLearningListener>();

	public void registerMacLearningListener(IMacLearningListener listener) {
		logger.info("ReactiveFlowListener New mac listener added: {}", listener);
		this.macLearningListeners.add(listener);
	}

	public void unregisterMacLearningListener(IMacLearningListener listener) {
		if(this.macLearningListeners.remove(listener)) {
			logger.info("ReactiveFlowListener Mac listener removed: {}", listener);
		}
	}

	@Override
	public void onPacketReceived(PacketReceived notification) {
		byte[] etherTypeRaw = this.extractEtherType(notification.getPayload());
    int etherType = (0x0000ffff & ByteBuffer.wrap(etherTypeRaw).getShort());
    if (etherType == 0x88cc) {
        return;
    }
    NodeConnectorRef ingressNodeConnectorRef = notification.getIngress();
    NodeRef ingressNodeRef = ReactiveFlowListener.getNodeRef(ingressNodeConnectorRef);
    NodeConnectorId ingressNodeConnectorId = ReactiveFlowListener.getNodeConnectorId(ingressNodeConnectorRef);
    NodeId ingressNodeId = ReactiveFlowListener.getNodeId(ingressNodeConnectorRef);

		// Learn mac addresses
		byte[] dstMacRaw = Arrays.copyOfRange(notification.getPayload(), 0, 6);
		byte[] srcMacRaw = Arrays.copyOfRange(notification.getPayload(), 6, 12);

		// Notify listeners
		MacAddress srcMac = ReactiveFlowListener.getMacAddress(srcMacRaw);
		MacAddress dstMac = ReactiveFlowListener.getMacAddress(dstMacRaw);
		logger.info("ReactiveFlowListener onPacketReceived: packet_in ingressNodeConnectorId: {},  srcMac: {}, dstMac: {}", ingressNodeConnectorId, srcMac, dstMac);

		for (IMacLearningListener listener : this.macLearningListeners) {
			listener.macAddressesLearned(ingressNodeConnectorId, srcMac, dstMac);
			logger.info("ReactiveFlowListener onPacketReceived: notification for new mac address pair ");
		}
	}

	public static byte[] extractEtherType(final byte[] payload) {
    return Arrays.copyOfRange(payload, 12, 14);
  }

  public static String rawMacToString(byte[] rawMac) {
    if (rawMac != null && rawMac.length == 6) {
        StringBuffer sb = new StringBuffer();
        for (byte octet : rawMac) {
            sb.append(String.format(":%02X", octet));
        }
        return sb.substring(1);
    }
    return null;
  }

  public static MacAddress getMacAddress(final byte[] mac) {
  	return new MacAddress(ReactiveFlowListener.rawMacToString(mac));
  }

	public static NodeId getNodeId(NodeConnectorRef nodeConnectorRef) {
	return nodeConnectorRef.getValue()
        .firstKeyOf(Node.class, NodeKey.class)
        .getId();
	}

	public static NodeRef getNodeRef(NodeConnectorRef nodeConnectorRef) {
	  InstanceIdentifier<Node> nodeIID = nodeConnectorRef.getValue()
		.firstIdentifierOf(Node.class);
	  return new NodeRef(nodeIID);
	}

	public static NodeConnectorId getNodeConnectorId(NodeConnectorRef nodeConnectorRef) {
    return nodeConnectorRef.getValue()
        .firstKeyOf(NodeConnector.class, NodeConnectorKey.class)
        .getId();
	}
}
