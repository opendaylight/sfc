/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.util.openflow.transactional_writer;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * A thread class used to remove flows on the data store.
 * It receives the list of flows to remove at object instantiation time - AKA constructor.
 * The flows are removed in a single data store transaction.
 */
public class FlowSetRemoverTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowSetRemoverTask.class);
    private Set<FlowDetails> flowsToDelete = new HashSet<>();
    private WriteTransaction tx;
    private DataBroker dataProvider = null;

    public FlowSetRemoverTask(DataBroker dataBroker, Set<FlowDetails> flowsToDelete) {
        tx = null;
        dataProvider = dataBroker;
        this.flowsToDelete.addAll(flowsToDelete);
    }

    public FlowSetRemoverTask(Set<FlowDetails> flowsToDelete, WriteTransaction theTx) {
        this(null, flowsToDelete);
        tx = theTx;
    }

    @Override
    public void run(){

        WriteTransaction writeTx =
                tx == null ? dataProvider.newWriteOnlyTransaction() : tx;

        LOG.debug("FlowSetRemoverTask: starting deletion of {} flows", flowsToDelete.size());

        for (FlowDetails f: flowsToDelete) {
            NodeKey theKey = new NodeKey(new NodeId(f.getSffNodeName()));
            InstanceIdentifier<Flow> iidFlow = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, theKey)
                    .augmentation(FlowCapableNode.class)
                    .child(Table.class, f.getTableKey())
                    .child(Flow.class, f.getFlowKey())
                    .build();

            writeTx.delete(LogicalDatastoreType.CONFIGURATION, iidFlow);
        }

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        try {
            submitFuture.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("deleteTransactionAPI: Transaction failed. Message: {}", e.getMessage(), e);
        }
    }
}
