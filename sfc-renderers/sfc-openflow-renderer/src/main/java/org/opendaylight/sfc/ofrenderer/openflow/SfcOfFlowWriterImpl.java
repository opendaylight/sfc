/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.openflow;


import com.google.common.util.concurrent.CheckedFuture;


import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.sfc.ofrenderer.processors.SfcOfRspProcessor;
import org.opendaylight.sfc.provider.OpendaylightSfc;
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
 * @author Diego Granados (diego.jesus.granados.lopez@ericsson.com)
 * @since 2015-11-25
 */

public class SfcOfFlowWriterImpl implements SfcOfFlowWriterInterface {
    private static final long SHUTDOWN_TIME = 5;
    private static final String LOGSTR_THREAD_EXCEPTION = "Exception executing Thread: {}";
    private static final Logger LOG = LoggerFactory.getLogger(SfcOfFlowWriterImpl.class);

    private ExecutorService threadPoolExecutorService;

    private FlowBuilder flowBuilder;

    // Store all flows associated w/ each rendered service path,
    // indexed by the corresponding SFFs
    private Map<Long, Map<String, List<FlowDetails>>> rspNameToFlowsMap;

    // temporary list of flows to be deleted. All of them will be deleted when
    // deleteFlowSet() invokation
    private Set<FlowDetails> setOfFlowsToDelete;
    // temporary list of flows to be added. All of them will be deleted when
    // flushFlows() invokation
    private Set<FlowDetails> setOfFlowsToAdd;

    public SfcOfFlowWriterImpl() {
        this.threadPoolExecutorService = Executors.newSingleThreadExecutor();;
        this.rspNameToFlowsMap = new ConcurrentHashMap<>();
        this.flowBuilder = null;
        this.setOfFlowsToDelete = new HashSet<>();
        this.setOfFlowsToAdd = new HashSet<>();
    }

    /**
     * Shutdown the thread pool
     *
     * @throws ExecutionException     thrown when attempting to retrieve the result of an aborted task
     * @throws InterruptedException   thrown when the executor is waiting, sleeping, or occupied, and it is interrupted
     */
    @Override
    public void shutdown() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!
        threadPoolExecutorService.shutdown();
        if (!threadPoolExecutorService.awaitTermination(SHUTDOWN_TIME, TimeUnit.SECONDS)) {
            LOG.error("SfcOfFlowProgrammerImpl Executor did not terminate in the specified time.");
            List<Runnable> droppedTasks = threadPoolExecutorService.shutdownNow();
            LOG.error("SfcOfFlowProgrammerImpl Executor was abruptly shut down. [{}] tasks will not be executed.",
                    droppedTasks.size());
        }
    }

    /**
     * A thread class used to write the flows to the data store. It receives the list of flows to create at creation time.
     * The flows are written together in a single data store transaction
     */
    class FlowSetWriterTask implements Runnable {
        Set<FlowDetails> flowsToWrite = new HashSet<>();

        public FlowSetWriterTask(Set<FlowDetails> flowsToWrite) {
            this.flowsToWrite.addAll(flowsToWrite);
        }

        @Override
        public void run(){
            WriteTransaction trans = OpendaylightSfc.getOpendaylightSfcObj().getDataProvider().newWriteOnlyTransaction();

            LOG.debug("FlowSetWriterTask: starting addition of {} flows", flowsToWrite.size());

            for (FlowDetails f: flowsToWrite) {
                NodeKey theKey = new NodeKey(new NodeId(f.sffNodeName));

                InstanceIdentifier<Flow> iidFlow = InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, theKey)
                            .augmentation(FlowCapableNode.class)
                            .child(Table.class, f.tableKey)
                            .child(Flow.class, f.flowKey)
                            .build();

                trans.put(LogicalDatastoreType.CONFIGURATION, iidFlow, f.flow, true);
            }

            CheckedFuture<Void, TransactionCommitFailedException> submitFuture = trans.submit();

            try {
                submitFuture.checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("deleteTransactionAPI: Transaction failed. Message: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * A thread class used to transactionally delete a set of flows belonging to a given RSP in a single transaction
     */
    class FlowSetRemoverTask implements Runnable {

        Set<FlowDetails> flowsToDelete = new HashSet<>();

        public FlowSetRemoverTask(Set<FlowDetails> flowsToDelete) {
            this.flowsToDelete.addAll(flowsToDelete);
        }

        @Override
        public void run(){

            WriteTransaction writeTx = OpendaylightSfc.getOpendaylightSfcObj().getDataProvider().newWriteOnlyTransaction();

            LOG.debug("FlowSetRemoverTask: starting deletion of {} flows", flowsToDelete.size());

            for (FlowDetails f: flowsToDelete) {
                NodeKey theKey = new NodeKey(new NodeId(f.sffNodeName));
                InstanceIdentifier<Flow> iidFlow = InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, theKey)
                            .augmentation(FlowCapableNode.class)
                            .child(Table.class, f.tableKey)
                            .child(Flow.class, f.flowKey)
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

    /**
     * Internal class used to store the details of a flow for easy creation / deletion later
     */
    public class FlowDetails {

        public String sffNodeName;
        public FlowKey flowKey;
        public TableKey tableKey;
        public Flow flow;

        /**
         * This constructor is used for storing flows to be added
         *
         * @param sffNodeName - which SFF to write the flow to
         * @param flowKey - the flow identifier
         * @param tableKey - the table identifier
         * @param flow - The flow to be written
         */
        protected FlowDetails(final String sffNodeName, FlowKey flowKey, TableKey tableKey, Flow flow) {
            this.sffNodeName = sffNodeName;
            this.flowKey = flowKey;
            this.tableKey = tableKey;
            this.flow = flow;
        }

        /**
         * This constructor is used for storing flows to be deleted. Only the path ids are needed
         *
         * @param sffNodeName - which SFF to write the flow to
         * @param flowKey - the flow identifier
         * @param tableKey - the table identifier
         */
        protected FlowDetails(final String sffNodeName, FlowKey flowKey, TableKey tableKey) {
            this(sffNodeName, flowKey, tableKey, null);
        }
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

        LOG.debug("writeFlow storing flow to Node {}, table {}", sffNodeName, flow.getTableId());

        // Add the flow to the set of flows to be added in a single transaction
        setOfFlowsToAdd.add(new FlowDetails(sffNodeName, flow.getKey(), new TableKey(flow.getTableId()), flowBuilder.build()));

        // This will store the flow info and rspId for removal later
        storeFlowDetails(rspId, sffNodeName, flow.getKey(), flow.getTableId());
    }

    @Override
    public void removeFlow(String sffNodeName, FlowKey flowKey,
            TableKey tableKey) {

      LOG.debug("removeFlow: removing flow with key {} from table {} in sff {}", flowKey, tableKey, sffNodeName);

      FlowDetails flowDetail = new FlowDetails(sffNodeName, flowKey, tableKey);
      setOfFlowsToDelete.add(flowDetail);
    }

    /**
     * From previous calls to writeFlowToConfig(), flows were stored per table
     * and per SFF. Now the flows will be written, one table at at time per SFF.
     */
    @Override
    public void flushFlows() {
        LOG.info("flushFlows: creating flowWriter task, writing [{}] flows.",
                setOfFlowsToAdd.size());

        FlowSetWriterTask writerThread = new FlowSetWriterTask(setOfFlowsToAdd);

        try {
            threadPoolExecutorService.execute(writerThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_EXCEPTION, ex.toString(), ex);
        }

        // Clear the entries
        setOfFlowsToAdd.clear();
    }

    /**
     * Purge any unwritten flows not written-deleted yet. This should be called upon
     * errors, when the remaining buffered flows should not be persisted
     */
    @Override
    public void purgeFlows() {
        setOfFlowsToAdd.clear();
        setOfFlowsToDelete.clear();
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
        Optional<Map<String, List<FlowDetails>>> theFlowsPerSffMap =
                Optional.ofNullable(rspNameToFlowsMap.get(rspId));
        if (!theFlowsPerSffMap.isPresent()) {
            rspNameToFlowsMap.put(rspId, new HashMap<> ());
        }

        Optional<List<FlowDetails>> theFlowsPerSff =
                Optional.ofNullable(rspNameToFlowsMap.get(rspId).get(sffNodeName));
        if (!theFlowsPerSff.isPresent()) {
            rspNameToFlowsMap.get(rspId).put(sffNodeName, new ArrayList<>());
        }

        List<FlowDetails> flowDetails = rspNameToFlowsMap.get(rspId).get(sffNodeName);
        flowDetails.add(new FlowDetails(sffNodeName, flowKey, new TableKey(tableId)));
    }

    @Override
    public void writeGroupToDataStore(String sffNodeName, GroupBuilder gb, boolean isAdd) {
        GroupKey gk = new GroupKey(gb.getGroupId());
        NodeKey theNodeKey = new NodeKey(new NodeId(sffNodeName));

        InstanceIdentifier<Group> groupIID =
                InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, theNodeKey)
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
     * Return the last flow builder
     * Used mainly in Unit Testing
     */
    @Override
    public FlowBuilder getFlowBuilder() {
        return this.flowBuilder;
    }

    /**
     * Delete all flows created for the given rspId (flows are stored in a deletion buffer;
     * actual transactional deletion is performed upon deleteFlowSet() invokation
     *
     * @param rspId - the rspId to delete flows for
     */
    @Override
    public void deleteRspFlows(final Long rspId) {
        if (!rspNameToFlowsMap.containsKey(rspId)) {
            LOG.warn("deleteRspFlows() Attempting to delete RSP [{}], and it does not exist", rspId);
            return;
        }

        List<FlowDetails> flowDetailsList = new ArrayList<>();
        rspNameToFlowsMap.get(rspId).forEach(
                (sffName, flowsPerSff) -> {
                    flowDetailsList.addAll(flowsPerSff);
                    flowsPerSff.clear();
                }
            );

        if (flowDetailsList.size() == 0) {
            LOG.warn("deleteRspFlows() no flows exist for RSP [{}]", rspId);
            return;
        }

        rspNameToFlowsMap.remove(rspId);
        setOfFlowsToDelete.addAll(flowDetailsList);
    }

    @Override
    public void deleteFlowSet() {
        LOG.info("deleteFlowSet: deleting {} flows", setOfFlowsToDelete.size());
        FlowSetRemoverTask fsrt = new FlowSetRemoverTask(setOfFlowsToDelete);
            try {
                threadPoolExecutorService.execute(fsrt);
            } catch (Exception ex) {
                LOG.error(LOGSTR_THREAD_EXCEPTION, ex.toString(), ex);
            }

        // Clear the entries
        setOfFlowsToDelete.clear();
    }

    /**
     * Clear all flows from the SFFs whenever they are not featured in any RSP
     * @return the SFFs that had their flows removed because they did not feature in any RSP
     */
    @Override
    public Set<NodeId> clearSffsIfNoRspExists() {
        Set<NodeId> sffNodeIDs = new HashSet<>();

        if (!rspNameToFlowsMap.containsKey(SfcOfRspProcessor.SFC_FLOWS)) {
            LOG.warn("clearSffsIfNoRspExists() - Attempting to delete initialization flows, and they do not exist");
            return sffNodeIDs;
        }

        Map<String, List<FlowDetails>> theInitializationFlows =
                rspNameToFlowsMap.get(SfcOfRspProcessor.SFC_FLOWS);

        // an orphan SFF is a forwarder not featured in any RSP
        Predicate <String> isOrphanSff =
                sffName -> this.timesFeaturedInRsps(sffName) == 1;

        Set<String> orphanSffs =
                theInitializationFlows.entrySet().stream()
                    .map(Entry::getKey)
                    .filter(isOrphanSff)
                    .collect(Collectors.toSet());

        orphanSffs.forEach(
                sffName -> {
                    // mark this SFF as orphan
                    sffNodeIDs.add(new NodeId(sffName));
                    // mark the set of flows to remove from the switches
                    setOfFlowsToDelete.addAll(theInitializationFlows.get(sffName));
                    // delete the cache of initialization flows of orphan SFFs
                    theInitializationFlows.remove(sffName);
                });

        return sffNodeIDs;
    }

    /**
     * Given the name of an sff, returns the number of times that SFF is featured in RSPs
     *
     * @param sffName the name of a service function forwarder
     * @return the number of times that SFF is featured in RSPs
     */
    private long timesFeaturedInRsps(String sffName) {
        Predicate<Entry<Long, Map<String, List<FlowDetails>>>> isSffFeatured =
                theInputEntry -> theInputEntry.getValue().containsKey(sffName);
        return rspNameToFlowsMap.entrySet().stream().filter(isSffFeatured).count();
    }
}
