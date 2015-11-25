/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;

/**
 * An interface to be implemented by concrete classes that will OpenFlow rules to MD-SAL datastore.
 *
 * @author Ricardo Noriega (ricardo.noriega.de.soto@ericsson.com)
 * @since 2015-11-25
 */


public class SfcL2FlowWriterTest implements SfcL2FlowWriterInterface {

    FlowBuilder flowBuilder = null;

    @Override
    public void writeFlowToConfig(Long rspId, String sffNodeName,
            FlowBuilder flow) {
        this.flowBuilder = flow;
        // TODO Auto-generated method stub

    }

    @Override
    public void removeFlowFromConfig(String sffNodeName, FlowKey flowKey,
            TableKey tableKey) {
        // TODO Auto-generated method stub

    }

    @Override
    public void storeFlowDetails(Long rspId, String sffNodeName,
            FlowKey flowKey, short tableId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeGroupToDataStore(String sffNodeName, GroupBuilder gb,
            boolean isAdd) {
        // TODO Auto-generated method stub

    }

    @Override
    public FlowBuilder getFlowBuilder() {
        // TODO Auto-generated method stub
        return this.flowBuilder;
    }

}
