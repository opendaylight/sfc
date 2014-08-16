/*
 * Copyright (c) 2014 ConteXtream Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.sfc.provider.RegularExpressionMatcher.matchesPattern;

import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.lispflowmapping.tools.junit.BaseTestCase;
import org.opendaylight.lispflowmapping.tools.junit.BaseExpectations.ValueSaverAction;
import org.opendaylight.sfc.provider.utils.MdSalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BaseSfcFlowTest extends BaseTestCase {

    protected DataBroker dataBroker;
    protected WriteTransaction wt;
    protected ValueSaverAction<InstanceIdentifier<DataObject>> instanceIdentifierValueSaver = new ValueSaverAction<>();
    protected ValueSaverAction<DataObject> dataObjectValueSaver = new ValueSaverAction<>();
    protected InstanceIdentifier<DataObject> currentInstanceIdentifier;
    protected DataObject currentDataObject;
    protected Flow currentFlow;
    protected int currentPutIndex;

    @Before
    public void setup() {
        currentPutIndex = 0;

        dataBroker = context.mock(DataBroker.class);
        wt = context.mock(WriteTransaction.class);

        oneOf(dataBroker).newWriteOnlyTransaction();
        ret(wt);
        allowing(wt).commit();
        allowing(wt).put(with(LogicalDatastoreType.CONFIGURATION), with(instanceIdentifierValueSaver), with(dataObjectValueSaver), with(true));
    }

    protected void assertInPort(int inPort) {
        assertEquals(inPort, Integer.valueOf(currentFlow.getMatch().getInPort().getValue()).intValue());
    }

    protected void assertEthrTypeMatch(long type) {
        assertEquals(type, currentFlow.getMatch().getEthernetMatch().getEthernetType().getType().getValue().longValue());
    }

    protected void assertFlowMetadata(int sfpId) {
        assertEquals(sfpId, currentFlow.getMatch().getMetadata().getMetadata().intValue());
    }

    protected void expectFlow() {
        assertTrue(currentDataObject instanceof Flow);
        currentFlow = (Flow) currentDataObject;
    }

    protected void expectNode(String nodeId) {
        assertTrue(currentDataObject instanceof Node);
        Node currentNode = (Node) currentDataObject;
        assertEquals(nodeId, currentNode.getId().getValue());

    }

    protected void expectPutTransaction() {
        assertTrue("No Configuration", currentPutIndex < instanceIdentifierValueSaver.values.size());

        currentInstanceIdentifier = instanceIdentifierValueSaver.values.get(currentPutIndex);
        currentDataObject = dataObjectValueSaver.values.get(currentPutIndex);

        currentPutIndex++;

    }

    protected void assertFlowActionSetDlSrc(String mac, int order) {
        assertFlowContainsSubstring(MdSalUtils.createSetDlSrcAction(mac, order).toString());
    }

    protected void assertFlowActionSetDlDst(String mac, int order) {
        assertFlowContainsSubstring(MdSalUtils.createSetDlDstAction(mac, order).toString());
    }

    protected void assertFlowActionOutput(int port) {
        assertFlowContainsRegex("OutputAction.*outputNodeConnector.*_value=" + port);
    }

    private void assertFlowContainsSubstring(String substring) {
        assertThat(currentFlow.toString(), //
                containsString(substring));
    }

    private void assertFlowContainsRegex(String subregex) {
        assertThat(currentFlow.toString(), //
                matchesPattern(".*" + subregex + ".*"));
    }

}
