/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

/**
 * An interface to be implemented by concrete classes that will OpenFlow rules to MD-SAL datastore.
 *
 * @author Ricardo Noriega (ricardo.noriega.de.soto@ericsson.com)
 * @since 2015-11-25
 */


public class SfcL2FlowWriterTest implements SfcL2FlowWriterInterface {

    FlowBuilder flowBuilder = null;

    @Override
    public void writeFlow(Long rspId, String sffNodeName,
            FlowBuilder flow) {
        this.flowBuilder = flow;
    }

    @Override
    public void removeFlow(String sffNodeName, FlowKey flowKey,
            TableKey tableKey) {
    }

    @Override
    public void writeGroupToDataStore(String sffNodeName, GroupBuilder gb,
            boolean isAdd) {
    }

    @Override
    public FlowBuilder getFlowBuilder() {
        return this.flowBuilder;
    }

    @Override
    public void deleteRspFlows(Long rspId) {
    }

    @Override
    public Set<NodeId> clearSffsIfNoRspExists() {
        return null;
    }

    public void flushFlows() {
    }

    @Override
    public void purgeFlows() {
    }

    @Override
    public void shutdown() throws ExecutionException, InterruptedException {
    }

    @Override
    public void deleteFlowSet() {
    }
}
