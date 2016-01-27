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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
 * Set of instructions in order to interact with MD-SAL datastore.
 * <p>
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @author Ricardo Noriega (ricardo.noriega.de.soto@ericsson.com)
 * @since 2015-11-25
 */

public class SfcL2FlowWriterImpl implements SfcL2FlowWriterInterface {
    private static final long SHUTDOWN_TIME = 5;
    private static final String LOGSTR_THREAD_EXCEPTION = "Exception executing Thread: {}";
    private static final Logger LOG = LoggerFactory.getLogger(SfcL2FlowWriterImpl.class);

    private ExecutorService threadPoolExecutorService;
    private FlowBuilder flowBuilder;
    // Store RspId to List of FlowDetails, to be able
    // to delete all flows for a particular RSP
    private Map<Long, List<FlowDetails>> rspNameToFlowsMap;
    // Store Table IID to a List of Flows to be able to write the flows one
    // table at a time, which is much faster than writing a flow at a time.
    // Notice the Table IID includes the nodeId and the tableId
    private Map<InstanceIdentifier<Table>, TableBuilder> tableIdToFlowsMap;

    public SfcL2FlowWriterImpl() {
        this.threadPoolExecutorService = Executors.newSingleThreadExecutor();
        this.rspNameToFlowsMap = new HashMap<Long, List<FlowDetails>>();
        this.tableIdToFlowsMap = new HashMap<InstanceIdentifier<Table>, TableBuilder>();
        this.flowBuilder = null;
    }

    /**
     * Return the last flow builder
     * Used mainly in Unit Testing
     */
    @Override
    public FlowBuilder getFlowBuilder() {
        return this.flowBuilder;
    }

    /**
     * Shutdown the thread pool
     */
    @Override
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
    class TableWriterTask implements Runnable {
        InstanceIdentifier<Table> tableInstanceId;
        TableBuilder tableBuilder;

        public TableWriterTask(InstanceIdentifier<Table> tableInstanceId, TableBuilder tableBuilder) {
            this.tableInstanceId = tableInstanceId;
            this.tableBuilder = tableBuilder;
        }

        public void run(){
            try {
                ReadWriteTransaction trans = OpendaylightSfc.getOpendaylightSfcObj().getDataProvider().newReadWriteTransaction();
                Optional<Table> read = trans.read(LogicalDatastoreType.CONFIGURATION, this.tableInstanceId).get();

                if (read.isPresent()) {
                    Table currentTable = read.get();
                    tableBuilder.getFlow().addAll(currentTable.getFlow());
                    LOG.info("TableWriterTask adding [{}] current flows to tableId [{}]",
                            currentTable.getFlow().size(), this.tableInstanceId.toString());
                }

                trans.put(LogicalDatastoreType.CONFIGURATION, this.tableInstanceId, this.tableBuilder.build(), true);

                CheckedFuture<Void, TransactionCommitFailedException> f = trans.submit();
                Futures.addCallback(f, new FutureCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        LOG.error("Could not write flow table {}: {}", tableInstanceId, t);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        LOG.debug("Flow table {} updated.", tableInstanceId);
                    }
                });
            } catch(Exception e) {
                LOG.error("Exception in TableWriterTask: {}", e.toString());
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
     * From previous calls to writeFlowToConfig(), flows were stored per table
     * and per SFF. Now the flows will be written, one table at at time per SFF.
     */
    @Override
    public void flushFlows() {
        for(Map.Entry<InstanceIdentifier<Table>, TableBuilder> entry : tableIdToFlowsMap.entrySet()) {

            LOG.info("flushFlows: TableIID [{}], writing [{}] flows.",
                    entry.getKey().toString(), entry.getValue().getFlow().size());

            TableWriterTask writerThread = new TableWriterTask(entry.getKey(), entry.getValue());
            try {
                threadPoolExecutorService.execute(writerThread);
            } catch (Exception ex) {
                LOG.error(LOGSTR_THREAD_EXCEPTION, ex.toString());
            }
        }

        // Clear the entries
        tableIdToFlowsMap.clear();
    }

    /**
     * Purge any unwritten flows not written yet. This should be called upon
     * errors, when the remaining buffered flows should not be written.
     */
    @Override
    public void purgeFlows() {
        tableIdToFlowsMap.clear();
    }

    /**
     * Store a flow to be written later. The flows will be stored per
     * SFF and table. Later, when flushFlows() is called, all the flows
     * will be written. The tableId is taken from the FlowBuilder.
     *
     * @param sffNodeName - which SFF to write the flow to
     * @param flow - details of the flow to be written
     */
    @Override
    public void writeFlow(Long rspId, String sffNodeName, FlowBuilder flow) {
        this.flowBuilder = flow;

        InstanceIdentifier<Table> tablePath =
                SfcOpenflowUtils.createTablePath(
                        new NodeId(sffNodeName), flow.getTableId());

        // Get the TableBuilder based on the TablePath
        // If it doesnt exist, create it
        TableBuilder tableBuilder = tableIdToFlowsMap.get(tablePath);
        if(tableBuilder == null) {
            // The entry didnt exist, creating it
            tableBuilder = new TableBuilder();
            tableBuilder.setFlow(new ArrayList<Flow>());
            tableIdToFlowsMap.put(tablePath, tableBuilder);
        }

        // Add the flow to the TableBuiler for this tableId and sffNodeName
        tableBuilder.getFlow().add(flow.build());

        // This will store the flow info and rspId for removal later
        storeFlowDetails(rspId, sffNodeName, flow.getKey(), flow.getTableId());
    }

    /**
     * storeFlowDetails
     * Store the flow details so the flows are easy to delete later
     *
     * @param sffNodeName - the SFF the flow is written to
     * @param flowKey - the flow key of the new flow
     * @param tableId - the table the flow was written to
     */
    private void storeFlowDetails(final Long rspId, final String sffNodeName, FlowKey flowKey, short tableId) {
        List<FlowDetails> flowDetails = rspNameToFlowsMap.get(rspId);
        if (flowDetails == null) {
            flowDetails = new ArrayList<FlowDetails>();
            rspNameToFlowsMap.put(rspId, flowDetails);
        }
        flowDetails.add(new FlowDetails(sffNodeName, flowKey, new TableKey(tableId)));
    }

    /**
     * Remove a Flow from the DataStore
     *
     * @param sffNodeName - which SFF the flow is in
     * @param flowKey - The flow key of the flow to be removed
     * @param tableKey - The table the flow was written to
     */
    @Override
    public void removeFlow(String sffNodeName, FlowKey flowKey,
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
            LOG.error(LOGSTR_THREAD_EXCEPTION, ex.toString());
        }
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
            removeFlow(flowDetails.sffNodeName, flowDetails.flowKey, flowDetails.tableKey);
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
                removeFlow(flowDetails.sffNodeName, flowDetails.flowKey, flowDetails.tableKey);
                sffNodeIDs.add(new NodeId(flowDetails.sffNodeName));
            }
            rspNameToFlowsMap.clear();
        }
        return sffNodeIDs;
    }
}
