/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronRouter;
import org.opendaylight.neutron.spi.NeutronFloatingIP;
import ch.icclab.netfloc.iface.IBridgeListener;
import ch.icclab.netfloc.iface.IBroadcastListener;
import ch.icclab.netfloc.iface.IBridgeIterator;
import ch.icclab.netfloc.iface.IBridgeOperator;
import ch.icclab.netfloc.iface.ILinkPort;
import ch.icclab.netfloc.iface.INetworkOperator;
import ch.icclab.netfloc.iface.INetworkPath;
import ch.icclab.netfloc.iface.INetworkTraverser;
import ch.icclab.netfloc.iface.INodeOperator;
import ch.icclab.netfloc.iface.IPortOperator;
import ch.icclab.netfloc.iface.IHostPort;
import ch.icclab.netfloc.iface.ITraversableBridge;
import ch.icclab.netfloc.iface.INetworkPathListener;
import ch.icclab.netfloc.iface.IServiceChainListener;
import ch.icclab.netfloc.iface.nbhandlers.INeutronPortHandler;
import ch.icclab.netfloc.iface.nbhandlers.INeutronSubnetHandler;
import ch.icclab.netfloc.iface.nbhandlers.INeutronNetworkHandler;
import ch.icclab.netfloc.iface.nbhandlers.INeutronRouterHandler;
import ch.icclab.netfloc.iface.nbhandlers.INeutronFloatingIPHandler;
import ch.icclab.netfloc.iface.sbhandlers.IBridgeHandler;
import ch.icclab.netfloc.iface.sbhandlers.INodeHandler;
import ch.icclab.netfloc.iface.sbhandlers.IPortHandler;
import ch.icclab.netfloc.iface.ofhandlers.ILinkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.lang.System;
import java.lang.IllegalStateException;

public class NetworkGraph implements
	INetworkTraverser,
	INetworkOperator {

	static final Logger logger = LoggerFactory.getLogger(NetworkGraph.class);

	List<INodeOperator> nodes = new LinkedList<INodeOperator>();
	private List<INetworkPathListener> networkPathListeners = new LinkedList<INetworkPathListener>();
	private List<IBridgeListener> bridgeListeners = new LinkedList<IBridgeListener>();
	private List<IBroadcastListener> broadcastListeners = new LinkedList<IBroadcastListener>();

	public void registerNetworkPathListener(INetworkPathListener npl) {
		this.networkPathListeners.add(npl);
	}

	public void registerBridgeListener(IBridgeListener bl) {
		this.bridgeListeners.add(bl);
	}

	public void registerBroadcastListener(IBroadcastListener bcl) {
		this.broadcastListeners.add(bcl);
	}

	public void notifyNetworkPathListenersCreate(INetworkPath networkPath) {
		for (INetworkPathListener npl : this.networkPathListeners) {
			npl.networkPathCreated(networkPath);
		}
	}

	public void notifyNetworkPathListenersDelete(INetworkPath networkPath) {
		for (INetworkPathListener npl : this.networkPathListeners) {
			npl.networkPathDeleted(networkPath);
		}
	}

	public void notifyNetworkPathListenersUpdate(INetworkPath oldPath, INetworkPath newPath) {
		for (INetworkPathListener npl : this.networkPathListeners) {
			npl.networkPathUpdated(oldPath, newPath);
		}
	}

	public void notifyBridgeListenersCreate(IBridgeOperator bo) {
		for (IBridgeListener bl : this.bridgeListeners) {
			bl.bridgeCreated(bo);
		}
	}

	public void notifyBroadcastListenersCreate(Set<INetworkPath> paths) {
		for (IBroadcastListener bcl : this.broadcastListeners) {
			bcl.broadcastCreated(paths);
		}
	}

	public void notifyBroadcastListenersDelete(Set<INetworkPath> paths) {
		for (IBroadcastListener bcl : this.broadcastListeners) {
			bcl.broadcastDeleted(paths);
		}
	}

	public void traverse(IBridgeIterator bridgeIterator) {
		List<ITraversableBridge> bridgesToVisit = new LinkedList<ITraversableBridge>();
		bridgesToVisit.add(new TraversableBridge(this.getBridges().get(0)));
		this.traverseFromBridge(
			bridgesToVisit,
			bridgeIterator);
	}

	private void traverseFromBridge(
		List<ITraversableBridge> bridgesToVisit,
		IBridgeIterator bridgeIterator) {

		List<ITraversableBridge> visitedBridges = new LinkedList<ITraversableBridge>();

		while (!bridgesToVisit.isEmpty()) {

			ITraversableBridge currentBridge;
			List<ITraversableBridge> currentBridges = new LinkedList<ITraversableBridge>();

			while (!bridgesToVisit.isEmpty()) {
				currentBridge = bridgesToVisit.remove(0);
				if (!bridgeIterator.visitBridge(currentBridge)) {
					return;
				}
				visitedBridges.add(currentBridge);
				currentBridges.add(currentBridge);
			}

			for (ITraversableBridge bridgeToExtendFrom : currentBridges) {
				for (ITraversableBridge br : getAdjacentBridges(bridgeToExtendFrom)) {
					if (!visitedBridges.contains(br)) {
						bridgesToVisit.add(br);
					}
				}
			}
		}
	}

	public List<ITraversableBridge> getAdjacentBridges(ITraversableBridge br) {
		List<ITraversableBridge> adjacentBridges = new LinkedList<ITraversableBridge>();
		for (IPortOperator port : br.getBridge().getPorts()) {
			if (port instanceof ILinkPort && ((ILinkPort)port).getLinkedPort() != null) {
				adjacentBridges.add(new TraversableBridge(((ILinkPort)port).getLinkedPort().getBridge(), br));
			}
		}
		return adjacentBridges;
	}

	public List<INetworkPath> getNetworkPaths() {
		List<INetworkPath> networkPaths1 = new LinkedList<INetworkPath>();
		List<IHostPort> hostPorts = new LinkedList<IHostPort>();
		for (IHostPort beginPort : hostPorts) {
			for (IHostPort endPort : hostPorts) {
				if (!beginPort.equals(endPort)) {
					networkPaths1.add(getNetworkPath(beginPort, endPort));
				}
			}
		}
		return networkPaths1;
	}

	public List<INetworkPath> getConnectableNetworkPaths(IHostPort src) {
		List<INetworkPath> paths = new LinkedList<INetworkPath>();
		for (IHostPort dst : this.getHostPorts()) {
			if (src.canConnectTo(dst)) {
				INetworkPath np = this.getNetworkPath(src, dst);
				if (np != null) {
					paths.add(np);
				}
			}
		}
		return paths;
	}

	public List<INetworkPath> getConnectableNetworkPaths(List<IHostPort> ports) {
		List<INetworkPath> paths = new LinkedList<INetworkPath>();
		for (IHostPort src : ports) {
			paths.addAll(this.getConnectableNetworkPaths(src));
		}
		return paths;
	}

	public INetworkPath getNetworkPath(final IHostPort begin, final IHostPort end) {
		final ITraversableBridge beginBridge = new TraversableBridge(begin.getBridge());
		final ITraversableBridge endBridge = new TraversableBridge(end.getBridge());

		assert begin.getBridge() != null : "NetworkGraph Begin bridge cannot be null";
		assert end.getBridge() != null : "NetworkGraph End bridge cannot be null";

		List<ITraversableBridge> bridgesToVisit = new LinkedList<ITraversableBridge>();

		bridgesToVisit.add(beginBridge);

		IBridgeIterator<INetworkPath> iterator = new IBridgeIterator<INetworkPath>() {

			private INetworkPath shortestPath = new NetworkPath(begin, end);
			private List<INetworkPath> paths = new LinkedList<INetworkPath>();
			private int iterations = 0;

			public boolean visitBridge(ITraversableBridge currentBridge) {
				iterations++;
				// Termination when final bridge is reached
				if (currentBridge.equals(endBridge)) {
					// bridges in path > 1
					if (!currentBridge.equals(beginBridge)) {
						this.shortestPath = this.getRootPath(currentBridge);
					}
					this.shortestPath.append(currentBridge.getBridge());
					this.shortestPath.close();
					return false;
				}

				INetworkPath newPath = new NetworkPath(begin, end);

				if (!paths.isEmpty()) {
					INetworkPath rootPath = this.getRootPath(currentBridge);
					newPath.addBridges(rootPath.getBridges());
				}

				newPath.append(currentBridge.getBridge());

				if (!paths.isEmpty()) {
					assert newPath.getLength() >= paths.get(0).getLength() : newPath.getLength() + " should >= " + paths.get(0).getLength() + " iterations: " + iterations;
				}
				paths.add(newPath);

				return true;
			}

			public INetworkPath getResult() {
				return (this.shortestPath.isClosed()) ? this.shortestPath : null;
			}

			private INetworkPath getRootPath(ITraversableBridge currentBridge) {
				for (INetworkPath path : this.paths) {
					if (path.getEnd().equals(currentBridge.getRoot().getBridge())) {
						return path;
					}
				}
				assert false : "This should never happen";
				return null;
			}
		};

		this.traverseFromBridge(bridgesToVisit, iterator);
		INetworkPath resultPath = iterator.getResult();
		return resultPath;
	}

	// Concurrency issue: links that are not established through LLDP
	private List<IHostPort> getLinkedHostPorts() {
		IBridgeIterator<List<IHostPort>> iterator = new IBridgeIterator<List<IHostPort>>() {

			private List<IHostPort> hostPorts = new LinkedList<IHostPort>();

			public boolean visitBridge(ITraversableBridge br) {
				hostPorts.addAll(br.getBridge().getHostPorts());
				return true;
			}

			public List<IHostPort> getResult() {
				return this.hostPorts;
			}
		};
		this.traverse(iterator);
		return iterator.getResult();
	}

	public List<IHostPort> getHostPorts() {
		List<IHostPort> hostPorts = new LinkedList<IHostPort>();
		for (IBridgeOperator bridge : this.getBridges()) {
			hostPorts.addAll(bridge.getHostPorts());
		}
		return hostPorts;
	}

	private Set<INetworkPath> checkNewConnections(IHostPort srcPort) {
		Set<INetworkPath> paths = new HashSet<INetworkPath>();
		for (IHostPort port : this.getHostPorts()) {
			if (srcPort.canConnectTo(port)) {
				INetworkPath networkPath = this.getNetworkPath(srcPort, port);
				if (networkPath == null) {
					logger.info("NetworkGraph Network Path is not closed for {}, {}", srcPort, port);
				} else {
					paths.add(networkPath);
				}
			}
		}
		return paths;
	}

	private void notifyNewConnections(Set<INetworkPath> paths) {
		logger.info("NetworkGraph notifying for new network paths.");
		for (INetworkPath path : paths) {
			logger.info("NetworkGraph NetworkPath created: {}", path.toString());
			this.notifyNetworkPathListenersCreate(path);
		}
		if (!paths.isEmpty()) {
			logger.info("NetworkGraph notifying BroadcastListener for deletion.");
			this.notifyBroadcastListenersDelete(paths);
			this.notifyBroadcastListenersCreate(paths);
			for (INetworkPath path : paths) {
				Set<INetworkPath> bcUpdatePaths = checkNewConnections(path.getEndPort());
				this.notifyBroadcastListenersCreate(bcUpdatePaths);
			}
		}
		if (paths.size() == 1) {
			for (INetworkPath path : paths) {
				Set<INetworkPath> rpaths = new HashSet<INetworkPath>();
				rpaths.add(path.getReversePath());
				this.notifyBroadcastListenersCreate(rpaths);
			}
		}
	}

	public List<INodeOperator> getNodes() {
		return this.nodes;
	}

	public INodeOperator getNode(NodeId id) {

		if (id == null) {
			throw new IllegalArgumentException("NetworkGraph node id cannot be null");
		}
		logger.info("NetworkGraph search node: " + id.toString());

		for (INodeOperator nd : this.nodes) {
			NodeId ndid = nd.getNodeId();
			if (ndid == null) {
				throw new IllegalStateException("NetworkGraph cached node id is null");
			}
			logger.info("NetworkGraph compare node: " + ndid.toString());
			if (ndid.equals(id)) {
				return nd;
			}
		}
		return null;
	}

	public void addNode(INodeOperator node) {
		this.nodes.add(node);
	}

	public void removeNode(INodeOperator node) {
		this.nodes.remove(node);
	}

	public void removeNode(NodeId id) {
		INodeOperator node = this.getNode(id);
		if (node == null) {
			return;
		}
		this.nodes.remove(node);
	}

	public List<IBridgeOperator> getBridges() {
		List<IBridgeOperator> bridges = new LinkedList<IBridgeOperator>();
		for (INodeOperator node : this.getNodes()) {
			bridges.addAll(node.getBridges());
		}
		return bridges;
	}

	public void addPort(Node node, TerminationPoint tp, OvsdbTerminationPointAugmentation tpa) {
		IBridgeOperator bo = getParentBridge(node.getNodeId());
		if (bo == null) {
			throw new IllegalStateException("NetworkGraph bridge is null on port create");
		}
		IPortOperator port = SouthboundHelper.maybeCreateInternalPort(bo, tp, tpa);
		if (port != null) {
			logger.info("NetworkGraph Port {} is an internal port", port);
			bo.addPort(port);
			return;
		}
		logger.info("NetworkGraph Port {} is a link port", port);
		port = new LinkPort(bo, tp, tpa);
		bo.addPort(port);
	}

	public void addPort(Node node, TerminationPoint tp, OvsdbTerminationPointAugmentation tpa, NeutronPort neutronPort) {
		IBridgeOperator bo = getParentBridge(node.getNodeId());
		if (bo == null) {
			throw new IllegalStateException("NetworkGraph Bridge is null on port create");
		}
		logger.info("NetworkGraph Port {} is a neutron port", neutronPort);
		IHostPort port = new HostPort(bo, tp, tpa, neutronPort);
		bo.addPort(port);
		Set<INetworkPath> paths = this.checkNewConnections(port);
		this.notifyNewConnections(paths);
	}

	public void removePort(Node node, OvsdbTerminationPointAugmentation tpa) {
		IBridgeOperator bo = this.getParentBridge(node.getNodeId());
		IPortOperator po = bo.getPort(tpa.getPortUuid());
		if (po instanceof IHostPort) {
			IHostPort hostPort = (IHostPort)po;
			for (INetworkPath removedPath : this.getConnectableNetworkPaths(hostPort)) {
				this.notifyNetworkPathListenersDelete(removedPath);
			}
		}
		bo.removePort(po);
	}

	public void updatePort(Node node, TerminationPoint tp, OvsdbTerminationPointAugmentation tpa) {
		IBridgeOperator bo = this.getParentBridge(node.getNodeId());
		IPortOperator po = bo.getPort(tpa.getPortUuid());
		po.update(tp, tpa);
		if (po instanceof IHostPort) {
			Set<INetworkPath> paths = this.checkNewConnections((IHostPort)po);
			this.notifyNewConnections(paths);
		}
	}

	private IBridgeOperator getParentBridge(NodeId id) {

		INodeOperator no = this.getParentNode(id);

		if (no == null) {
			throw new IllegalStateException("NetworkGraph Node is null on port create");
		}

		for (IBridgeOperator br : no.getBridges()) {
			if (br.getNodeId().equals(id)) {
				return br;
			}
		}
		return null;
	}

	public INodeOperator getParentNode(NodeId id) {
		if (id == null) {
			throw new IllegalArgumentException("NetworkGraph Node id cannot be null");
		}
		logger.info("NetworkGraph search node: " + id.getValue());

		for (INodeOperator nd : this.nodes) {
			NodeId ndid = nd.getNodeId();
			if (ndid == null) {
				throw new IllegalStateException("NetworkGraph Cached node id is null");
			}
			logger.info("NetworkGraph Compare node: " + ndid.getValue());
			if (id.getValue().contains(ndid.getValue())) {
				return nd;
			}
		}
		return null;
	}

	public void createLink(Link link) {
		TpId tpIdSrc = link.getSource().getSourceTp();
		TpId tpIdDst = link.getDestination().getDestTp();

		if (tpIdSrc == null || tpIdDst == null) {
			logger.error("NetworkGraph TpId is null for tpIdSrc: <{}>, tpIdDst: <{}>", tpIdSrc, tpIdDst);
		}

		ILinkPort portSrc = this.getLinkPort(tpIdSrc);
		ILinkPort portDst = this.getLinkPort(tpIdDst);

		if (portSrc == null || portDst == null) {
			logger.error("NetworkGraph Link is null for link ports portSrc: <{}>, portDst: <{}>", portSrc, portDst);
		}
		portSrc.setLinkedPort(portDst);
	}

	public void deleteLink(Link link) {
		TpId tpIdSrc = link.getSource().getSourceTp();
		TpId tpIdDst = link.getDestination().getDestTp();

		if (tpIdSrc == null || tpIdDst == null) {
			logger.error("NetworkGraph TpId is null for tpIdSrc: <{}>, tpIdDst: <{}>", tpIdSrc, tpIdDst);
		}

		ILinkPort portSrc = this.getLinkPort(tpIdSrc);
		ILinkPort portDst = this.getLinkPort(tpIdDst);

		if (portSrc == null || portDst == null) {
			logger.error("NetworkGraph Link is null for link ports portSrc: <{}>, portDst: <{}>", portSrc, portDst);
		}

		// Delete old paths with broken link
		LinkedList<INetworkPath> updateablePaths = new LinkedList<INetworkPath>();
		for (INetworkPath path : this.getConnectableNetworkPaths(this.getHostPorts())) {
			if (path.hasLinkPort(portSrc) || path.hasLinkPort(portDst)) {
				logger.info("NetworkGraph Delete Path {}", path);
				updateablePaths.add(path);
				this.notifyNetworkPathListenersDelete(path);
			}
		}

		// Create new paths if possible
		if (portSrc != null) {
			portSrc.removeLinkedPort(portDst);
		} else if (portDst != null) {
			portDst.removeLinkedPort(portSrc);
		}

		for (INetworkPath path : updateablePaths) {
			INetworkPath newPath = this.getNetworkPath(path.getBeginPort(), path.getEndPort());
			if (newPath != null) {
				logger.info("NetworkGraph New Path {} found for path {}", newPath, path);
				this.notifyNetworkPathListenersCreate(newPath);
			} else {
				logger.warn("NetworkGraph New Path not found for path {}", path);
			}
		}
	}

	public ILinkPort getLinkPort(TpId tpid) {
		for (IBridgeOperator br : this.getBridges()) {
			for (ILinkPort port : br.getLinkPorts()) {
				if (port.isOvsPort(tpid)) {
					return port;
				}
			}
		}
		return null;
	}
}
