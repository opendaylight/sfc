/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer.openflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set of instructions in order to interact with MD-SAL datastore.
 * <p>
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @author Ricardo Noriega (ricardo.noriega.de.soto@ericsson.com)
 * @since 2015-11-25
 */

public class SfcL2FlowWriterImpl implements SfcL2FlowWriterInterface {
    private static final int SCHEDULED_THREAD_POOL_SIZE = 1;
    private static final int QUEUE_SIZE = 1000;
    private static final int ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;
    private static final long SHUTDOWN_TIME = 5;
    private static final String LOGSTR_THREAD_QUEUE_FULL = "Thread Queue is full, cant execute action: {}";
    private static final Logger LOG = LoggerFactory.getLogger(SfcL2FlowWriterImpl.class);

    private ExecutorService threadPoolExecutorService;
    private FlowBuilder flowBuilder;
    private Map<Long, List<FlowDetails>> rspNameToFlowsMap;

    public SfcL2FlowWriterImpl() {
        // Not using an Executors.newSingleThreadExecutor() here, since it creates
        // an Executor that uses a single worker thread operating off an unbounded
        // queue, and we want to be able to limit the size of the queue
        this.threadPoolExecutorService = new ThreadPoolExecutor(SCHEDULED_THREAD_POOL_SIZE, SCHEDULED_THREAD_POOL_SIZE,
                ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(QUEUE_SIZE));
        this.rspNameToFlowsMap = new HashMap<Long, List<FlowDetails>>();
        this.flowBuilder = null;
    }

    public void shutdown() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!
        threadPoolExecutorService.shutdown();
        if (!threadPoolExecutorService.awaitTermination(SHUTDOWN_TIME, TimeUnit.SECONDS)) {
            LOG.error("SfcL2FlowProgrammerOFimpl Executor did not terminate in the specified time.");
            List<Runnable> droppedTasks = threadPoolExecutorService.shutdownNow();
            LOG.error("SfcL2FlowProgrammerOFimpl Executor was abruptly shut down. [{}] tasks will not be executed.",
                    droppedTasks.size());
        }
    }

    /**
     * A thread class used to write the flows to the data store.
     */
    class FlowWriterTask implements Runnable {
        String sffNodeName;
        InstanceIdentifier<Flow> flowInstanceId;
        FlowBuilder flowBuilder;

        public FlowWriterTask(String sffNodeName, InstanceIdentifier<Flow> flowInstanceId, FlowBuilder flowBuilder) {
            this.sffNodeName = sffNodeName;
            this.flowInstanceId = flowInstanceId;
            this.flowBuilder = flowBuilder;
        }

        public void run(){
            if (!SfcDataStoreAPI.writeMergeTransactionAPI(
                    this.flowInstanceId,
                    this.flowBuilder.build(),
                    LogicalDatastoreType.CONFIGURATION)) {
                LOG.error("{}: Failed to create Flow on node: {}", Thread.currentThread().getStackTrace()[1], this.sffNodeName);
            }
        }
    }

    /**
     * A thread class used to remove flows from the data store.
     */
    class FlowRemoverTask implements Runnable {
        String sffNodeName;
        InstanceIdentifier<Flow> flowInstanceId;

        public FlowRemoverTask(String sffNodeName, InstanceIdentifier<Flow> flowInstanceId) {
            this.flowInstanceId = flowInstanceId;
            this.sffNodeName = sffNodeName;
        }

        public void run(){
            if (!SfcDataStoreAPI.deleteTransactionAPI(flowInstanceId, LogicalDatastoreType.CONFIGURATION)) {
                LOG.error("{}: Failed to remove Flow on node: {}", Thread.currentThread().getStackTrace()[1], sffNodeName);
            }
        }
    }

    /**
     * Internal class used to store the details of a flow for easy deletion later
     */
    private class FlowDetails {

        public String sffNodeName;
        public FlowKey flowKey;
        public TableKey tableKey;

        public FlowDetails(final String sffNodeName, FlowKey flowKey, TableKey tableKey) {
            this.sffNodeName = sffNodeName;
            this.flowKey = flowKey;
            this.tableKey = tableKey;
        }
    }

    /**
     * Write a flow to the DataStore
     *
     * @param sffNodeName - which SFF to write the flow to
     * @param flow - details of the flow to be written
     */
    @Override
    public void writeFlowToConfig(Long rspId, String sffNodeName,
            FlowBuilder flow) {

        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path, which will include the Node, Table, and Flow
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeBuilder.getKey())
            .augmentation(FlowCapableNode.class)
            .child(Table.class, new TableKey(flow.getTableId()))
            .child(Flow.class, flow.getKey())
            .build();

        LOG.debug("writeFlowToConfig writing flow to Node {}, table {}", sffNodeName, flow.getTableId());

        storeFlowDetails(rspId, sffNodeName, flow.getKey(), flow.getTableId());

        FlowWriterTask writerThread = new FlowWriterTask(sffNodeName, flowInstanceId, flow);
        try {
            threadPoolExecutorService.execute(writerThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    /**
     * Remove a Flow from the DataStore
     *
     * @param sffNodeName - which SFF the flow is in
     * @param flowKey - The flow key of the flow to be removed
     * @param tableKey - The table the flow was written to
     */
    @Override
    public void removeFlowFromConfig(String sffNodeName, FlowKey flowKey,
            TableKey tableKey) {

        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeBuilder.getKey())
            .augmentation(FlowCapableNode.class)
            .child(Table.class, tableKey)
            .child(Flow.class, flowKey)
            .build();

        FlowRemoverTask removerThread = new FlowRemoverTask(sffNodeName, flowInstanceId);
        try {
            threadPoolExecutorService.execute(removerThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    /**
     * storeFlowDetails
     * Store the flow details so the flows are easy to delete later
     *
     * @param sffNodeName - the SFF the flow is written to
     * @param flowKey - the flow key of the new flow
     * @param tableId - the table the flow was written to
     */
    @Override
    public void storeFlowDetails(final Long rspId, final String sffNodeName, FlowKey flowKey, short tableId) {
        List<FlowDetails> flowDetails = rspNameToFlowsMap.get(rspId);
        if (flowDetails == null) {
            flowDetails = new ArrayList<FlowDetails>();
            rspNameToFlowsMap.put(rspId, flowDetails);
        }
        flowDetails.add(new FlowDetails(sffNodeName, flowKey, new TableKey(tableId)));
    }

    @Override
    public void writeGroupToDataStore(String sffNodeName, GroupBuilder gb, boolean isAdd) {
        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        GroupKey gk = new GroupKey(gb.getGroupId());
        InstanceIdentifier<Group> groupIID;

        groupIID = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeBuilder.getKey())
            .augmentation(FlowCapableNode.class)
            .child(Group.class, gk)
            .build();
        Group group = gb.build();
        LOG.debug("about to write group to data store \nID: {}\nGroup: {}", groupIID, group);
        if (isAdd) {
            if (!SfcDataStoreAPI.writeMergeTransactionAPI(groupIID, group, LogicalDatastoreType.CONFIGURATION)) {
                LOG.warn("Failed to write group to data store");
            }
        } else {
            if (!SfcDataStoreAPI.deleteTransactionAPI(groupIID, LogicalDatastoreType.CONFIGURATION)) {
                LOG.warn("Failed to remove group from data store");
            }
        }
    }

    @Override
    public FlowBuilder getFlowBuilder() {
        return this.flowBuilder;
    }

    /**
     * Delete all flows created for the given rspId
     *
     * @param rspId - the rspId to delete flows for
     */
    @Override
    public void deleteRspFlows(final Long rspId) {
        List<FlowDetails> flowDetailsList = rspNameToFlowsMap.get(rspId);
        if (flowDetailsList == null) {
            LOG.warn("deleteRspFlows() no flows exist for RSP [{}]", rspId);
            return;
        }

        rspNameToFlowsMap.remove(rspId);
        for (FlowDetails flowDetails : flowDetailsList) {
            removeFlowFromConfig(flowDetails.sffNodeName, flowDetails.flowKey, flowDetails.tableKey);
        }
    }

    @Override
    public Set<NodeId> clearSffsIfNoRspExists() {
        // If there is just one entry left in the rsp-flows mapping, then all flows for RSPs
        // have been deleted, and the only flows remaining are those that are common to all
        // RSPs, which can be deleted.
        Set<NodeId> sffNodeIDs = new HashSet<>();
        if (rspNameToFlowsMap.size() == 1) {
            Set<Entry<Long, List<FlowDetails>>> entries = rspNameToFlowsMap.entrySet();
            List<FlowDetails> flowDetailsList = entries.iterator().next().getValue();
            for (FlowDetails flowDetails : flowDetailsList) {
                removeFlowFromConfig(flowDetails.sffNodeName, flowDetails.flowKey, flowDetails.tableKey);
                sffNodeIDs.add(new NodeId(flowDetails.sffNodeName));
            }
            rspNameToFlowsMap.clear();
        }
        return sffNodeIDs;
    }
}
