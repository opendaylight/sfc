/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.util.openflow.transactional_writer;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

/**
 * An interface to be implemented by concrete classes that will OpenFlow rules to MD-SAL datastore.
 * <p>
 *
 * @author Ricardo Noriega (ricardo.noriega.de.soto@ericsson.com)
 * @since 2015-11-25
 */

public interface SfcOfFlowWriterInterface {

    void injectTransaction(WriteTransaction theTx);

    void updateTransactionObject();

    void registerTransactionListener(Object interestedParty);

    void deregisterTransactionListener(Object interestedParty);

    //Write flows to MD-SAL datastore
    void writeFlow(final Long rspId, final String sffNodeName, FlowBuilder flow);

    void writeFlow(FlowDetails theFlowData);

    boolean writeFlows(Collection<FlowDetails> theFlows);

    //Remove flows from MD-SAL datastore
    void removeFlow(final String sffNodeName, FlowKey flowKey, TableKey tableKey);

    void removeFlow(FlowDetails theFlowData);

    boolean removeFlows(Collection<FlowDetails> theFlows);

    //Write group to MD-SAL datastore
    void writeGroupToDataStore(String sffNodeName, GroupBuilder gb, boolean isAdd);

    /**
     * Delete all flows created for a particular RSP.
     *
     * @param rspId the ID of the RSP
     */
    void deleteRspFlows(final Long rspId);

    /**
     * Delete initialization flows from SFFs whenever they're no longer featured
     * in a RenderedServicePath
     *
     * @return Set of NodeIDs of cleared SFFs (ex: "openflow:99344160872776")
     */
    Set<NodeId> clearSffsIfNoRspExists();

    // Get the most recent Flow Builder
    FlowBuilder getFlowBuilder();

    // Flush any flows that havent been written to the data store yet
    void flushFlows();

    // Performs the deletion of any flows that havent been deleted from the data store yet
    void deleteFlowSet();

    // Purge any flows that havent been written/deleted to/from the data store yet
    void purgeFlows();

    // If the impl uses threads, shut it down
    void shutdown() throws ExecutionException, InterruptedException;


}
