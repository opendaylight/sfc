/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IFlowprogrammer;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.INetworkPath;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IFlowBridgePattern;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IBridgeOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IFlowPathPattern;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.controller.md.sal.common.api.data.DataValidationFailedException;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;

public class Flowprogrammer implements IFlowprogrammer {
	static final Logger logger = LoggerFactory.getLogger(Flowprogrammer.class);
	private DataBroker dataBroker;
	public Flowprogrammer(DataBroker dataBroker) {
		this.dataBroker = dataBroker;
	}

	public void programFlow(Flow flow, IBridgeOperator bridge, FutureCallback<Void> cb) {
		logger.info("Flowprogrammer Programming flow {} for bridge {}", flow.getId(), bridge.getDatapathId());
		WriteTransaction wt = this.dataBroker.newWriteOnlyTransaction();
		InstanceIdentifier<Flow> flowId = buildFlowId(flow, bridge.getDatapathId());
		wt.merge(LogicalDatastoreType.CONFIGURATION, flowId, flow, true);
        commitWriteTransaction(wt, cb, 3, 3);
	}

	public void deleteFlow(Flow flow, IBridgeOperator bridge, FutureCallback<Void> cb) {
		logger.info("Flowprogrammer Deleting flow {} for bridge {}", flow.getId(), bridge.getDatapathId());
		WriteTransaction wt = this.dataBroker.newWriteOnlyTransaction();
		InstanceIdentifier<Flow> flowId = buildFlowId(flow, bridge.getDatapathId());
		wt.delete(LogicalDatastoreType.CONFIGURATION, flowId);
		commitWriteTransaction(wt, cb, 3, 3);
	}

	private void commitWriteTransaction(final WriteTransaction wt, final FutureCallback<Void> cb, final int totalTries, final int tries) {
        Futures.addCallback(wt.submit(), new FutureCallback<Void>() {
			public void onSuccess(Void result) {
				logger.info("Flowprogrammer Transaction success after {} tries for write transaction {}", totalTries - tries + 1, wt);
				cb.onSuccess(result);
			}

			public void onFailure(Throwable t) {
				if (t instanceof OptimisticLockFailedException) {
					if((tries - 1) > 0) {
						logger.warn("Flowprogrammer Transaction retry {} for write transaction {}", totalTries - tries + 1, wt);
						commitWriteTransaction(wt, cb, totalTries, tries - 1);
					} else {
						logger.error("Flowprogrammer Transaction out of retries: ", wt);
						cb.onFailure(t);
					}
				} else {
					if (t instanceof DataValidationFailedException) {
						logger.error("Flowprogrammer Transaction validation failed {}", t.getMessage());
					} else {
						logger.error("Flowprogrammer Transaction failed {}", t.getMessage());
					}
					cb.onFailure(t);
				}
			}
		});
	}

	private InstanceIdentifier<Node> buildNodeId(Node node) {
		return InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, node.getKey()).build();
	}

	private InstanceIdentifier<Flow> buildFlowId(Flow flow, String datapathId) {
		return InstanceIdentifier.builder(Nodes.class)
			.child(Node.class, new NodeKey(buildNode("openflow:" + Long.parseLong(datapathId.replace(":", ""), 16)).getKey()))
			.augmentation(FlowCapableNode.class)
			.child(Table.class, new TableKey(flow.getTableId()))
			.child(Flow.class, flow.getKey())
			.build();
	}

	private Node buildNode(String nodeId) {
        NodeBuilder builder = new NodeBuilder();
        builder.setId(new NodeId(nodeId));
        builder.setKey(new NodeKey(builder.getId()));
        return builder.build();
    }
}
