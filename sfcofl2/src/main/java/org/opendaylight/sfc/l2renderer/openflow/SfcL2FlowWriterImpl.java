package org.opendaylight.sfc.l2renderer.openflow;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl;
//import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl.FlowDetails;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcL2FlowWriterImpl implements SfcL2FlowWriterInterface {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2FlowWriterImpl.class);
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

        if (!SfcDataStoreAPI.writeMergeTransactionAPI(flowInstanceId, flow.build(),
                LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("{}: Failed to create Flow on node: {}", Thread.currentThread().getStackTrace()[1], sffNodeName);
        }
        storeFlowDetails(rspId, sffNodeName, flow.getKey(), flow.getTableId());
    }

    private static BigInteger getMetadataSFP(long sfpId) {
        return (BigInteger.valueOf(sfpId).and(new BigInteger("FFFF", SfcL2FlowProgrammerOFimpl.COOKIE_BIGINT_HEX_RADIX)));
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

        if (!SfcDataStoreAPI.deleteTransactionAPI(flowInstanceId, LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("{}: Failed to remove Flow on node: {}", Thread.currentThread().getStackTrace()[1], sffNodeName);
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
    public void storeFlowDetails(Long rspId, String sffNodeName,
            FlowKey flowKey, short tableId) {

//        List<FlowDetails> flowDetails = rspNameToFlowsMap.get(rspId);
//        if (flowDetails == null) {
//            flowDetails = new ArrayList<FlowDetails>();
//            rspNameToFlowsMap.put(rspId, flowDetails);
//        }
//        flowDetails.add(new FlowDetails(sffNodeName, flowKey, new TableKey(tableId)));
    }

    @Override
    public void writeGroupToDataStore(String sffNodeName, GroupBuilder gb,
            boolean isAdd) {
        // TODO Auto-generated method stub

    }

}
