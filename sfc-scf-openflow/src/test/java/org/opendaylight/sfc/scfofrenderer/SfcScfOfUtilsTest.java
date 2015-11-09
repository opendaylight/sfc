/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
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
    private Match match;
    private SfcNshHeader nsh;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        match = mock(Match.class);
        nsh = mock(SfcNshHeader.class);
        fb = mock(FlowBuilder.class);

        PowerMockito.stub(PowerMockito.method(SfcOpenflowUtils.class, "createFlowBuilder",
                short.class, int.class, String.class, MatchBuilder.class, InstructionsBuilder.class))
            .toReturn(fb);
        PowerMockito.stub(PowerMockito.method(SfcOpenflowUtils.class, "writeFlowToDataStore"))
            .toReturn(true);
        PowerMockito.stub(PowerMockito.method(SfcOpenflowUtils.class, "removeFlowFromDataStore"))
            .toReturn(true);
    }

    @Test
    public void testInitClassifierTable() {
        SfcScfOfUtils.initClassifierTable("node");
    }

    @Test
    public void testCreateClassifierOutFlow() {
        SfcScfOfUtils.createClassifierOutFlow("node", "flow", match, nsh, 0L);
    }

    @Test
    public void testCreateClassifierInFlow() {
        SfcScfOfUtils.createClassifierInFlow("node", "flow", nsh, 0L);
    }

    @Test
    public void testCreateClassifierRelayFlow() {
        SfcScfOfUtils.createClassifierRelayFlow("node", "flow", nsh);
    }

    @Test
    public void testDeleteClassifierFlow() {
        SfcScfOfUtils.deleteClassifierFlow("node", "flow");
    }
}
