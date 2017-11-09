/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcOpenflowUtils.class)

public class SfcScfOfUtilsTest {
    private FlowBuilder fb;

    @Before
    public void init() {
        fb = mock(FlowBuilder.class);
        PowerMockito.stub(PowerMockito.method(SfcOpenflowUtils.class, "createFlowBuilder", short.class, int.class,
                String.class, MatchBuilder.class, InstructionsBuilder.class)).toReturn(fb);
        PowerMockito.stub(PowerMockito.method(SfcOpenflowUtils.class, "writeFlowToDataStore")).toReturn(true);
        PowerMockito.stub(PowerMockito.method(SfcOpenflowUtils.class, "removeFlowFromDataStore")).toReturn(true);
    }

    @Test
    public void testInitClassifierTable() {
        assertNotNull(SfcScfOfUtils.initClassifierTable());
    }

    @Test
    public void testCreateClassifierOutFlow() {
        SfcRspInfo nsh = mock(SfcRspInfo.class);
        Ipv4Address ip = mock(Ipv4Address.class);
        nsh.setVxlanIpDst(ip);

        assertNull(SfcScfOfUtils.createClassifierOutFlow(null, null, null, 0L));
        assertNull(SfcScfOfUtils.createClassifierOutFlow(null, null, null, 0L));
        assertNull(SfcScfOfUtils.createClassifierOutFlow("flow", null, null, 0L));
        Match match = mock(Match.class);
        assertNull(SfcScfOfUtils.createClassifierOutFlow(null, match, null, 0L));
        assertNull(SfcScfOfUtils.createClassifierOutFlow(null, null, nsh, 0L));
    }

    @Test
    public void testCreateClassifierInFlow() {
        SfcRspInfo nsh = mock(SfcRspInfo.class);
        Ipv4Address ip = mock(Ipv4Address.class);
        nsh.setVxlanIpDst(ip);

        assertNull(SfcScfOfUtils.createClassifierInFlow(null, null, 0L));
        assertNull(SfcScfOfUtils.createClassifierInFlow(null, null, 0L));
        assertNull(SfcScfOfUtils.createClassifierInFlow("flow", null, 0L));
        assertNull(SfcScfOfUtils.createClassifierInFlow(null, null, 0L));
        assertNull(SfcScfOfUtils.createClassifierInFlow(null, nsh, 0L));
    }

    @Test
    public void testCreateClassifierRelayFlow() {
        SfcRspInfo nsh = mock(SfcRspInfo.class);
        Ipv4Address ip = mock(Ipv4Address.class);
        nsh.setVxlanIpDst(ip);

        assertNull(SfcScfOfUtils.createClassifierRelayFlow(null, null));
        assertNull(SfcScfOfUtils.createClassifierRelayFlow(null, null));
        assertNull(SfcScfOfUtils.createClassifierRelayFlow("flow", null));
        assertNull(SfcScfOfUtils.createClassifierRelayFlow(null, nsh));
    }

    @Test
    public void testDeleteClassifierFlow() {
        assertFalse(SfcScfOfUtils.deleteClassifierFlow(null, null));
        assertFalse(SfcScfOfUtils.deleteClassifierFlow(null, "flow"));
        assertFalse(SfcScfOfUtils.deleteClassifierFlow("node", null));
        assertTrue(SfcScfOfUtils.deleteClassifierFlow("node", "flow"));
    }
}
